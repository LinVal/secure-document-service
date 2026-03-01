# Intervjuguide – Hur du diskuterar lösningen

Syftet med intervjun är inte att kontrollera om kandidaten har "rätt svar", utan att förstå hur personen tänker kring arkitektur, säkerhet och designval.

Fokusera på resonemang, riskmedvetenhet och hur kandidaten prioriterar i en verklig miljö.

---

## 🔹 A. Multipla organisationer

Börja med öppna frågor om organisations-isolering och JWT-hantering:

- Hur designade du organisations-isoleringen?
- Varför valde du just den strategin?
- Hur säkerställer du att ingen cross-organisation access är möjlig?
- Vad händer om någon av misstag glömmer organisations-filter i en query?
- Hur hämtar du `organisation_id` från token?
- Hur validerar du JWT?
- Hur verifierar du audience?
- Hur skiljer du scope från roller?

### 🔎 Senior-svar bör inkludera:

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

## 🔹 B. Domänmodell & åtkomstkontroll

Här testar du domänförståelse, affärsregler och centraliserad auktorisering.

Frågor:

- Varför kontrolleras organisation i service-lagret och inte i controller?
- Hur säkerställer du att dokument inte kan kopplas till fel organisation?
- Vad händer vid race condition?
- Hur hanterar du att ett stängt arende inte ska kunna ändras?
- Hur designade du åtkomst baserat på klassificering?
- Varför centraliserade du policyn?
- Hur skulle du ändra designen om nya klassificeringsnivåer införs?

### Senior bör prata om:

- Transaktioner och domäninvarians
- Skydd mot inkonsekvent data
- Separation mellan presentation och affärslogik
- Open/Closed Principle för policyn
- Policy engine kontra if-satser i kod
- Hur systemet kan växa utan att bli svårt att underhålla

En bra lösning ska vara enkel att utöka utan att ändra överallt.

---

## Sammanfattning

En bra intervju fokuserar på:

- Hur kandidaten tänker
- Hur designbeslut motiveras
- Hur säkerhet integreras i arkitekturen
- Hur risker identifieras och hanteras

Det är resonemanget som är viktigt – inte exakt implementation.
