# Secure Dokument Service (SDS)
## Referenslösning

Detta dokument beskriver en strukturerad och praktisk lösningsstrategi kopplad till uppgifterna.
Syftet är att visa hur en säker och arkitekturellt korrekt implementation kan se ut.

---

# Arkitektur

## Rekommenderad struktur

- controller/
- service/
- repository/
- security/
- domain/
- config/
- exception/

## Principer

- Controllers är tunna och innehåller ingen affärslogik.
- Affärsregler implementeras i service-lagret.
- Säkerhetslogik centraliseras i en policy-/auktoriseringstjänst.
- Organisation hämtas enbart från JWT-token.
- Alla databasfrågor filtreras på organisation.
- Systemet ska vara testbart med fokus på säkerhet.

Denna struktur säkerställer tydlig separation av ansvar.

---

# A. Multipla organisationer

## A.1 Lösning: JwtOrganisationConverter.convert()

`organisation_id` hämtas från JWT-claim och lagras i `OrganisationPrincipal`.
Principalen används sedan i hela applikationen för att filtrera data per organisation.

Stegen i `convert()`:
1. Läs `organisation_id` via `jwt.getClaim("organisation_id")` – kasta `BadCredentialsException` om null/blank.
2. Läs `roles` via `jwt.getClaimAsStringList("roles")` – hantera null defensivt.
3. Mappa varje roll till `new SimpleGrantedAuthority("ROLE_" + roll)`.
4. Skapa `new OrganisationPrincipal(jwt.getSubject(), organisationId, authorities)`.
5. Anropa `OrganisationContext.set(principal)` (ThreadLocal för services).
6. Returnera `new UsernamePasswordAuthenticationToken(principal, jwt, authorities)`.

## A.2 Dev-infrastruktur: JWT utan Keycloak

`DevJwtConfig` registrerar en `JwtDecoder`-bean (HMAC-SHA256) aktiv enbart i dev-profilen.
`SecurityConfig.filterChain` tar `JwtDecoder` som explicit parameter och sätter `.decoder(jwtDecoder)` –
detta gör beroendet synligt för Spring och fungerar oavsett om decodern kommer från dev eller prod.

`DevTokenController` (`POST /dev/token`) signerar tokens med samma nyckel och exponeras via `/dev/**`
som är permit-all i `SecurityConfig`. Endpointen existerar inte i prod (klassen är `@Profile("dev")`).

## A.3 Viktiga säkerhetsprinciper

- Organisation får aldrig komma från klientens request-body.
- All verifiering sker server-side.
- Cross-organisation access ska alltid resultera i 403 Forbidden.
- Tester ska verifiera att isoleringen fungerar.

Detta skyddar mot horizontal privilege escalation.

---

# B. Utöka befintligt API

## B.1 Lösning: GET /api/dokuments/{id}

Endpoint-metoden är tre till fyra rader och delegerar direkt till service-lagret.
Ingen organisations- eller säkerhetskontroll i controllern.

Service-lagret hämtar dokumentet via `findByIdAndOrganisationId` – organisationsisolering
garanteras av att repository-metoden alltid kräver `organisationId` som parameter.

Vid saknat ID eller fel organisation kastas ett undantag som `GlobalExceptionHandler`
mappar till `404 Not Found`. Att returnera 404 (och inte 403) vid cross-org-access är
ett medvetet val för att undvika informationsläcka om vilka resurser som existerar.

## B.2 Viktiga designprinciper

- `findById()` utan `organisationId` ska aldrig användas.
- Controllern innehåller ingen logik – enbart parameter-mappning och delegering.
- Tester ska verifiera att fel organisations-ID ger 404, inte 200 eller 403.

---

# C. Filtrera arenden på status

## C.1 Lösning

Ändringen berör tre lager: repository, service och controller.

**Repository** – ny Spring Data-metod med namnbaserad query:

```
findAllByOrganisationIdAndStatus(String organisationId, ArendeStatus status)
```

**Service** – `listArenden` tar emot en nullable `ArendeStatus`. Om den är `null` används
befintlig `findAllByOrganisationId`, annars den nya metoden med statusfilter.

**Controller** – `listArenden` utökas med `@RequestParam(required = false) ArendeStatus status`
som skickas vidare till service-lagret.

## C.2 Viktiga designprinciper

- `organisationId` kombineras alltid med statusfiltret – aldrig ett anrop utan org-filter.
- `null`-status returnerar hela listan (bakåtkompatibelt).
- Spring Data härleder SQL automatiskt från metodnamnet – ingen `@Query` behövs.
- Controllern innehåller noll logik – enbart parameter-mappning.

---

# Teststrategi

## Obligatoriska tester

- Test av organisationsisolering.
- Test av rollbaserad åtkomst.
- Negativa säkerhetstester (403/404-scenarier).
- Integrationstester med verklig databas (t.ex. Testcontainers).

## Syfte

Tester ska verifiera säkerhetsregler, inte enbart funktionalitet.
Det är särskilt viktigt att testa att felaktig åtkomst nekas korrekt.

---

# Vad kännetecknar en stark lösning

En stark implementation har:

- Centraliserad och konsekvent auktorisering.
- Tydlig separation mellan domän, säkerhet och infrastruktur.
- Robust organisationsisolering.
- Tydliga och omfattande tester.
- Dokumenterade designval och säkerhetsresonemang.
