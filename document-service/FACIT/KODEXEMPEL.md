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

# B. Utöka befintligt API

## B.1 Controller – GET /api/dokuments/{id}

Controllern är tre rader. All logik delegeras till service-lagret.

```java
@GetMapping("/{id}")
public DokumentResponse getDokument(
        @PathVariable UUID id,
        @AuthenticationPrincipal OrganisationPrincipal principal) {
    return dokumentService.getDokument(id, principal);
}
```

## B.2 Service – hämta och auktorisera

```java
public DokumentResponse getDokument(UUID id, OrganisationPrincipal principal) {

    Dokument dokument = dokumentRepository
            .findByIdAndOrganisationId(id, principal.organisationId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    authorizationService.assertReadAccess(dokument, principal);

    return DokumentResponse.from(dokument);
}
```

`findByIdAndOrganisationId` returnerar `empty` om ID:t finns men tillhör annan organisation.
Det ger `404 Not Found` – inte `403` – vilket undviker att avslöja att resursen existerar.

## B.3 Test – cross-org ger 404

```java
@Test
void getDokument_wrongOrganisation_returns404() {

    // given dokument tillhör org-A

    // when hämtas med principal för org-B

    // then 404 Not Found
}
```

---

# C. Filtrera arenden på status

## C.1 Repository – ny Spring Data-metod

```java
List<DokumentArende> findAllByOrganisationIdAndStatus(
        String organisationId,
        ArendeStatus status
);
```

Spring Data härleder SQL från metodnamnet – ingen `@Query` behövs.
`organisationId` kombineras alltid med statusfiltret.

## C.2 Service – hantera nullable status

```java
public List<ArendeResponse> listArenden(
        ArendeStatus status,
        OrganisationPrincipal principal) {

    List<DokumentArende> arenden = (status != null)
            ? arendeRepository.findAllByOrganisationIdAndStatus(
                    principal.organisationId(), status)
            : arendeRepository.findAllByOrganisationId(
                    principal.organisationId());

    return arenden.stream()
            .map(ArendeResponse::from)
            .toList();
}
```

`null`-status returnerar hela listan – befintliga anrop utan parameter fortsätter fungera.

## C.3 Controller – valfri frågeparameter

```java
@GetMapping
public List<ArendeResponse> listArenden(
        @RequestParam(required = false) ArendeStatus status,
        @AuthenticationPrincipal OrganisationPrincipal principal) {
    return arendeService.listArenden(status, principal);
}
```

Spring validerar automatiskt att `?status=`-värdet matchar ett giltigt `ArendeStatus`-värde
och returnerar `400 Bad Request` annars.

---

# Sammanfattning av lösning

En stark lösning har:

- Centraliserad auktorisering
- Organisation från JWT
- Repository-filter på organisation
- Negativa säkerhetstester
- Policy-baserad design
- Ren lagerstruktur
