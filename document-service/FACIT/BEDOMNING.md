# Röda flaggor och kompetensnivåer

Detta dokument används som stöd vid intervju och bedömning av kandidater.

---

# Röda flaggor att vara uppmärksam på

## A. Multipla organisationer

- Organisation hämtas från request istället för från JWT.
- Ingen förståelse för trust boundaries.
- Ingen tanke på horizontal privilege escalation.
- Ingen validering av `issuer` och `audience`.
- Ingen skillnad mellan scope och roller.
- Endast frontend-validering.

---

## B. Domänmodell & åtkomstkontroll

- Inga invariant-tankar (t.ex. att CLOSED inte får ändras).
- Ingen diskussion om transaktioner.
- Ingen hänsyn till race conditions.
- Säkerhetskontroller i controller istället för i service/policy.
- Ingen central policykomponent.
- Affärslogik och säkerhetslogik i samma klass.
- Säkerhet ses som ett separat "tillägg" istället för integrerad del.

---

## Teststrategi

- Endast positiva tester.
- Ingen test av säkerhetsregler.
- Inga integrationstester.
- Ingen test av cross-organisation-scenarier.

---

# Jämförelsetabell – Kompetensnivåer

| Område                  | Junior             | Mid                   | Senior                              | Lead                                     |
|-------------------------|--------------------|-----------------------|-------------------------------------|------------------------------------------|
| Organisations-säkerhet  | Förstår koncept    | Implementerar korrekt | Centraliserar och designar skalbart | Designar isolering som arkitekturprincip |
| JWT-förståelse          | Använder bibliotek | Förstår claims        | Förstår trust boundaries            | Designar hela auth-modellen              |
| Policy-design           | If-satser i kod    | Separat service       | Central policy/engine               | Skalbar auktoriseringsstrategi           |
| Arkitektur              | Blandar lager      | Delvis separation     | Tydlig separation                   | Arkitekturdriver beslut                  |
| Säkerhetstänk           | Reaktivt           | Funktionellt          | Integrerat i design                 | Zero-trust mindset                       |
| Teststrategi            | Happy path         | Några negativa tester | Systematiska säkerhetstester        | Riskbaserad strategi                     |
| Dokumentation           | Minimal            | Grundläggande         | Tydlig designförklaring             | Förklarar trade-offs och risker          |

---

# Användning

- Flera markeringar i röda flaggor kan indikera risk.
- Majoritet i Senior-kolumnen indikerar stark kandidat.
- Lead-nivå innebär systemtänk, riskmedvetenhet och arkitekturperspektiv.
