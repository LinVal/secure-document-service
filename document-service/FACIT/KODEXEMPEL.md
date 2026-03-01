# Kodexempel – Referensimplementation

Grundprinciper:

- Controllers är tunna
- Affärslogik ligger i service-lagret
- Auktorisering är centraliserad
- Organisation hämtas från JWT
- All data filtreras per organisation
- Tester inkluderar negativa scenarier

---

# A. Multipla organisationer

## A.1 Säkerhetskonfiguration

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

## A.2 JWT → organisationsID

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

Principal-modell:
```java
public record OrganisationPrincipal(
        String userId,
        String organisationId
) {}
```

## A.3 Repository med organisations-filter

```java
public interface DokumentRepository
        extends JpaRepository<Dokument, UUID> {

    Optional<Dokument> findByIdAndOrganisationId(
            UUID id,
            String organisationId
    );

    List<Dokument> findAllByOrganisationId(
            String organisationId
    );
}
```

## A.4 Exempel på organisations-test

```java
@Test
void shouldPreventCrossOrganisationAccess() {

    // given dokument in organisation A

    // when accessed with organisation B

    // then expect AccessDeniedException
}
```

---

# B. Domänmodell & åtkomstkontroll

## B.1 Arende-entitet

```java
@Entity
public class DokumentArende {

    @Id
    private UUID id;
    private String organisationId;
    private String title;
    @Enumerated(EnumType.STRING)
    private ArendeStatus status;
    private Instant createdAt;
    private String createdBy;
}
```
```java
public enum ArendeStatus {
    OPEN,
    CLOSED
}
```

Regler:
- Endast ADMIN får stänga arende
- Stängt arende får inte ändras
- Arende måste tillhöra rätt organisation
- Organisation kontrolleras i service-lagret

## B.2 ArendeService (affärsregler)

```java
@Service
public class ArendeService {

    private final ArendeRepository repository;
    private final AuthorizationService authorizationService;

    public void closeArende(UUID id,
                            OrganisationPrincipal principal) {

        DokumentArende arende =
                repository.findByIdAndOrganisationId(
                        id,
                        principal.organisationId()
                ).orElseThrow();

        authorizationService.assertCloseAccess(arende, principal);

        arende.setStatus(ArendeStatus.CLOSED);
        repository.save(arende);
    }
}
```

## B.3 Dokument-entitet med klassificering

```java
@Entity
public class Dokument {

    @Id
    private UUID id;
    private String organisationId;
    private String filename;
    @Enumerated(EnumType.STRING)
    private Classification classification;
    private UUID arendeId;
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

Regler:
- Klassificering styr åtkomst
- SUPER_ADMIN får läsa allt
- Policyn ska vara centraliserad i en tjänst
- Ingen säkerhetslogik i controllers

## B.4 Centraliserad auktorisering

```java
@Service
public class AuthorizationService {

    public void assertReadAccess(
            Dokument dokument,
            OrganisationPrincipal principal
    ) {
        if (!dokument.getOrganisationId()
                .equals(principal.organisationId())) {
            throw new AccessDeniedException("Otillåten åtkomst");
        }

        if (dokument.getClassification() == Classification.SECRET
                && !principal.hasRole("SUPER_ADMIN")) {
            throw new AccessDeniedException("Otillräcklig behörighet");
        }
    }
}
```

---

# Sammanfattning av lösning

En stark lösning har:

- Centraliserad auktorisering
- Organisation från JWT
- Repository-filter på organisation
- Negativa säkerhetstester
- Policy-baserad design
- Ren lagerstruktur
