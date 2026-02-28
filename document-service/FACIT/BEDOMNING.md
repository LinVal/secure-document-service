# Röda flaggor och kompetensnivåer

Detta dokument används som stöd vid intervju och bedömning av kandidater.

---

# Röda flaggor att vara uppmärksam på

## Säkerhet

- Säkerhetskontroller i controller istället för i service/policy.
- Organisation/tenant hämtas från request istället för från JWT.
- Ingen validering av `issuer` och `audience`.
- Ingen förståelse för trust boundaries.
- Ingen skillnad mellan scope och roller.
- Endast frontend-validering.
- Inga negativa säkerhetstester.
- Ingen tanke på horizontal privilege escalation.

---

## Arkitektur

- Affärslogik och säkerhetslogik i samma klass.
- Repository anropas direkt från controller.
- Otydlig lagerindelning.
- Ingen central policykomponent.
- För många ansvar i en och samma serviceklass.

---

## Domänförståelse

- Inga invariant-tankar (t.ex. att CLOSED inte får ändras).
- Ingen diskussion om transaktioner.
- Ingen hänsyn till race conditions.
- Säkerhet ses som ett separat “tillägg” istället för integrerad del.

---

## Produktionsmognad

- Hårdkodade hemligheter.
- Ingen användning av secrets.
- Ingen förståelse för non-root containers.
- Ingen tanke på horisontell skalning.
- Stateful design utan behov.
- Ingen diskussion om säker loggning.
- Saknar health checks.

---

## Teststrategi

- Endast positiva tester.
- Ingen test av säkerhetsregler.
- Inga integrationstester.
- Ingen test av cross-tenant-scenarier.

---

# Jämförelsetabell – Kompetensnivåer

| Område                | Junior             | Mid                   | Senior                              | Lead                                     |
|-----------------------|--------------------|-----------------------|-------------------------------------|------------------------------------------|
| Tenant-säkerhet       | Förstår koncept    | Implementerar korrekt | Centraliserar och designar skalbart | Designar isolering som arkitekturprincip |
| JWT-förståelse        | Använder bibliotek | Förstår claims        | Förstår trust boundaries            | Designar hela auth-modellen              |
| Policy-design         | If-satser i kod    | Separat service       | Central policy/engine               | Skalbar auktoriseringsstrategi           |
| Arkitektur            | Blandar lager      | Delvis separation     | Tydlig separation                   | Arkitekturdriver beslut                  |
| Säkerhetstänk         | Reaktivt           | Funktionellt          | Integrerat i design                 | Zero-trust mindset                       |
| Teststrategi          | Happy path         | Några negativa tester | Systematiska säkerhetstester        | Riskbaserad strategi                     |
| Produktionsförståelse | Begränsad          | Grundläggande         | Förstår containers & scaling        | Förstår drift, risk och governance       |
| Dokumentation         | Minimal            | Grundläggande         | Tydlig designförklaring             | Förklarar trade-offs och risker          |

---

# Användning

- Flera markeringar i röda flaggor kan indikera risk.
- Majoritet i Senior-kolumnen indikerar stark kandidat.
- Lead-nivå innebär systemtänk, riskmedvetenhet och arkitekturperspektiv.