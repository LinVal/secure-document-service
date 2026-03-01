# Intervjuguide – Hur du diskuterar lösningen

Syftet med intervjun är inte att kontrollera om kandidaten har "rätt svar", utan att förstå hur personen tänker kring arkitektur, säkerhet och designval.

Fokusera på resonemang, riskmedvetenhet och hur kandidaten prioriterar i en verklig miljö.

---

## A. Multipla organisationer

Börja med öppna frågor om organisations-isolering och JWT-hantering:

- Hur designade du organisations-isoleringen?
- Varför valde du just den strategin?
- Hur säkerställer du att ingen cross-organisation access är möjlig?
- Vad händer om någon av misstag glömmer organisations-filter i en query?
- Hur hämtar du `organisation_id` från token?
- Hur validerar du JWT?
- Hur verifierar du audience?
- Hur skiljer du scope från roller?

### Senior-svar bör inkludera:

- Centralisering av ansvar
- Risk för horizontal privilege escalation
- Tydlig teststrategi för säkerhetskrav
- Möjlig användning av filter, interceptor eller policy-lager
- Skillnaden mellan Resource Server och Authorization Server
- Claims och trust boundaries
- Var policy enforcement bör ske
- Hur man förhindrar att säkerhet blir beroende av enskilda utvecklare

En senior bör förstå att tokenen är en verifierad input – inte en absolut sanning utan kontext.

---

## B. Utöka befintligt API

Testar om kandidaten förstår thin-controller-mönstret och var logik hör hemma.

Frågor:

- Hur implementerade du `GET /api/dokuments/{id}`?
- Var lade du organisations-kontrollen – i controller eller service?
- Vad returnerar du om dokumentet inte finns?
- Vad returnerar du om dokumentet finns men tillhör en annan organisation?
- Varför just 404 och inte 403 i det fallet?

### Senior-svar bör inkludera:

- Medveten skillnad mellan 404 och 403 (information disclosure)
- Controller delegerar direkt, innehåller ingen logik
- Repository-metoden `findByIdAndOrganisationId` som enda garanti för isolering
- Testfall som verifierar att cross-org access ger 404, inte 200

---

## C. Filtrera arenden på status

Testar förståelse för Spring Data-queries, lagerstruktur och bakåtkompatibilitet.

Frågor:

- Vilka lager behövde du ändra?
- Hur definierade du query:n i repository-lagret?
- Hur hanterar du anropet utan `?status=`-parameter?
- Var valideras att status-värdet är giltigt?
- Vad skulle du göra om filtren blev fler – t.ex. status + skapad-datum?

### Senior-svar bör inkludera:

- Spring Data-namnkonvention kontra `@Query` – när passar vad
- Nullable parameter i service-lagret med tydlig grenlogik
- Bakåtkompatibilitet – befintliga anrop utan parameter fortsätter fungera
- Tankar kring skalbarhet (t.ex. Specification/Criteria API vid många filter)
- Validering av enum-värde sker automatiskt av Spring via `@RequestParam`

---

## Sammanfattning

En bra intervju fokuserar på:

- Hur kandidaten tänker
- Hur designbeslut motiveras
- Hur säkerhet integreras i arkitekturen
- Hur risker identifieras och hanteras

Det är resonemanget som är viktigt – inte exakt implementation.
