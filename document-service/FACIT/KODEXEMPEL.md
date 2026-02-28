# 1. Secure Document Service (SDS)
## 1.1 Referenslösning

Detta dokument innehåller både en enkel och en avancerad lösningsnivå.
Den avancerade delen ligger direkt under respektive avsnitt.

---

# 2. Övergripande Arkitektur

## 2.1 Grundprinciper

- Controllers är tunna
- Affärslogik ligger i service-lagret
- Auktorisering är centraliserad
- Organisation hämtas från JWT
- All data filtreras per organisation
- Tester inkluderar negativa scenarier
- Systemet är redo för produktion

## 2.2 Lagerstruktur

- controller/
- service/
- repository/
- security/
- domain/
- config/
- exception/

---

# 3. Säkerhetskonfiguration

## 3.1 Enkel version (Resource Server)

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken>
    jwtAuthenticationConverter() {
        return new JwtOrganisationConverter();
    }
}
```
## 3.2 Avancerad version (method security)

Aktiverar metodnivå-säkerhet:
```kod
@EnableMethodSecurity
```
Användning i servicen:
```java
@PreAuthorize("hasRole('ADMIN')")
public void closeCase(UUID id) {
}
```
Detta minskar behovet av manuell rollkontroll i koden.

# 4. Hantering av organisation från JWT

## 4.1 Principal-modell
```java
public record OrganisationPrincipal(
        String userId,
        String organisationId
) {}
```

## 4.2 JWT -> organisationsID
```java
public class JwtOrganisationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        String organisationId =
                jwt.getClaimAsString("organisation_id");

        List<GrantedAuthority> authorities =
                jwt.getClaimAsStringList("roles")
                        .stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();

        OrganisationPrincipal principal =
                new OrganisationPrincipal(jwt.getSubject(), organisationId);

        return new UsernamePasswordAuthenticationToken(
                principal,
                jwt,
                authorities
        );
    }
}
```

## 4.3 Organisationskontext
För mer robust isolering kan en ThreadLocal användas:
```java
public final class OrganisationContext {

    private static final ThreadLocal<String> CURRENT =
            new ThreadLocal<>();

    public static void set(String organisationId) {
        CURRENT.set(organisationId);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
```

# 5. Case
```java
@Entity
public class Case {

    @Id
    private UUID id;
    private String organisationId;
    private String title;
    @Enumerated(EnumType.STRING)
    private CaseStatus status;
    private Instant createdAt;
    private String createdBy;
}
```
```java
public enum CaseStatus {
OPEN,
CLOSED
}
```
Endast ADMIN får stänga case
Closed case får inte ändras
Case måste alltid tillhöra rätt organisation
Organisation kontrolleras i service-lagret

# 6. Document
```java
@Entity
public class Document {

    @Id
    private UUID id;
    private String organisationId;
    private String filename;
    @Enumerated(EnumType.STRING)
    private Classification classification;
    private UUID caseId;
}
```

```java
public enum Classification {
    PUBLIC,
    INTERNAL,
    CONFIDENTIAL,
    SECRET
}
```

Klassificering styr åtkomst
SUPER_ADMIN får läsa allt
Policyn ska vara centraliserad i en tjänst
Ingen säkerhetslogik i controllers
        
# 7. Repository med Organisations-filter
```java
public interface DocumentRepository
        extends JpaRepository<Document, UUID> {

    Optional<Document> findByIdAndOrganisationId(
            UUID id,
            String organisationId
    );

    List<Document> findAllByOrganisationId(
            String organisationId
    );
}
```

# 8. Centraliserad Auktorisering
```java
@Service
public class DocumentPolicy {

    public void checkReadAccess(
            Document document,
            OrganisationPrincipal principal
    ) {

        if (!document.getOrganisationId()
                .equals(principal.organisationId())) {
            throw new AccessDeniedException("Otillåten åtkomst");
        }

        if (document.getClassification() == Classification.SECRET
                && !hasRole("SUPER_ADMIN")) {
            throw new AccessDeniedException("Otillräcklig behörighet");
        }
    }

    private boolean hasRole(String role) {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("ROLE_" + role));
    }
}
```

# 9. Case Service (Affärsregler)
```java
@Service
public class CaseService {

    private final CaseRepository repository;

    public CaseService(CaseRepository repository) {
        this.repository = repository;
    }

    public void closeCase(UUID id,
                          OrganisationPrincipal principal) {

        Case caseEntity =
                repository.findByIdAndOrganisationId(
                        id,
                        principal.organisationId()
                ).orElseThrow();

        if (!hasRole("ADMIN")) {
            throw new AccessDeniedException("Endast ADMIN");
        }

        caseEntity.setStatus(CaseStatus.CLOSED);
        repository.save(caseEntity);
    }

    private boolean hasRole(String role) {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("ROLE_" + role));
    }
}
```

# 10. Audit Logging
```java
@Service
public class AuditService {

    private static final Logger log =
            LoggerFactory.getLogger(AuditService.class);

    public void logEvent(String actor, String organisation,
                         String action, String entityId) {

        Map<String, String> event = Map.of(
                "actor", actor,
                "organisation", organisation,
                "action", action,
                "entityId", entityId,
                "timestamp", Instant.now().toString()
        );

        log.info("AUDIT_EVENT {}", event);
    }
}
```

# 11. Säker Filuppladdning
```java
@Service
public class FileStorageService {

    private static final long MAX_SIZE = 5_000_000;

    public String store(MultipartFile file) throws IOException {

        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("File too large");
        }

        if (!List.of("application/pdf", "text/plain")
                .contains(file.getContentType())) {
            throw new IllegalArgumentException("Invalid file type");
        }

        String filename = UUID.randomUUID() + ".bin";

        Path target = Paths.get("/data/uploads").resolve(filename);

        Files.copy(file.getInputStream(), target);

        return filename;
    }
}
```

Max filstorlek
Content-type whitelist
Filer lagras utanför web-root
UUID-baserade filnamn
Ingen exekverbar kod

# 12. OpenShift-kompatibel Dockerfile
```bash
FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S app && adduser -S app -G app

USER app

COPY target/sds.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

Max filstorlek
Content-type whitelist
Filer lagras utanför web-root
UUID-baserade filnamn
Ingen exekverbar kod

# 13. Exempel på Organisations-test
```java
@Test
void shouldPreventCrossOrganisationAccess() {

    // given document in organisation A

    // when accessed with organisation B

    // then expect AccessDeniedE
}
```
Non-root container
Secrets för känslig konfiguration
Resource limits
Liveness/readiness probes
Horisontell skalbarhet


# Sammanfattning av Lösning
En stark lösning har:
Centraliserad auktorisering
Organisation från JWT
Repository-filter
Negativa tester
Policy-baserad design
Ren lagerstruktur
Produktionsklar container

