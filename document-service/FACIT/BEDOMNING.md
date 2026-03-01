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

## B. Utöka befintligt API

- Affärs- eller säkerhetslogik placeras i controllern.
- Använder `findById()` utan `organisationId`-filter.
- Returnerar 200 eller 403 vid cross-org access istället för 404.
- Ingen medvetenhet om informationsläcka via statuskoder.
- Controller-metoden är onödigt komplex.

---

## C. Filtrera arenden på status

- Lägger filterlogiken i controllern istället för service/repository.
- Glömmer organisations-filter i den nya repository-metoden.
- Hanterar inte `null`-fallet – anrop utan `?status=` slutar fungera.
- Skriver manuell `@Query` när Spring Data-namnkonvention räcker.
- Ingen förståelse för var enum-validering sker.

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
| Controller-design       | Logik i controller | Delegerar till service | Thin controller konsekvent          | Styr mönstret i hela teamet              |
| Statuskod-medvetenhet   | Returnerar 200/500 | Känner till 404/403   | Väljer medvetet utifrån säkerhet    | Definierar API-kontrakt med säkerhet     |
| Arkitektur              | Blandar lager      | Delvis separation     | Tydlig separation                   | Arkitekturdriver beslut                  |
| Säkerhetstänk           | Reaktivt           | Funktionellt          | Integrerat i design                 | Zero-trust mindset                       |
| Teststrategi            | Happy path         | Några negativa tester | Systematiska säkerhetstester        | Riskbaserad strategi                     |
| Dokumentation           | Minimal            | Grundläggande         | Tydlig designförklaring             | Förklarar trade-offs och risker          |

---

# Användning

- Flera markeringar i röda flaggor kan indikera risk.
- Majoritet i Senior-kolumnen indikerar stark kandidat.
- Lead-nivå innebär systemtänk, riskmedvetenhet och arkitekturperspektiv.
