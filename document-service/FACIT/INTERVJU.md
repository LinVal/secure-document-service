# Intervjuguide – Hur du diskuterar lösningen

Syftet med intervjun är inte att kontrollera om kandidaten har “rätt svar”, utan att förstå hur personen tänker kring arkitektur, säkerhet och designval.

Fokusera på resonemang, riskmedvetenhet och hur kandidaten prioriterar i en verklig miljö.

---

## 🔹 A. Starta med arkitekturfrågor

Börja med öppna frågor som låter kandidaten beskriva sin design:

- Hur designade du organisations-isoleringen?
- Varför valde du just den strategin?
- Hur säkerställer du att ingen cross-organisation access är möjlig?
- Vad händer om någon av misstag glömmer organisations-filter i en query?

Här vill du förstå om kandidaten tänker systemiskt eller lokalt i koden.

### 🔎 Senior-svar bör inkludera:

- Centralisering av ansvar
- Risk för horizontal privilege escalation
- Tydlig teststrategi för säkerhetskrav
- Möjlig användning av filter, interceptor eller policy-lager
- Förståelse för konsekvenser om isolering bryts

En senior bör resonera kring hur man förhindrar att säkerhet blir beroende av enskilda utvecklare.

---

## 🔹 B. Säkerhetsdjup

Här går du djupare i autentisering och auktorisering:

- Hur validerar du JWT?
- Hur verifierar du audience?
- Vad händer om issuer är felkonfigurerad?
- Hur skiljer du scope från roller?
- Hur hanterar du problemet med confused deputy?

Målet är att se om kandidaten förstår säkerhetsarkitektur och inte bara använder färdiga bibliotek utan förståelse.

### Senior bör kunna resonera om:

- Skillnaden mellan Resource Server och Authorization Server
- Claims och trust boundaries
- Var policy enforcement bör ske
- Hur man undviker att lita blint på tokeninnehåll

En stark kandidat förstår att tokenen är en verifierad input – inte en absolut sanning utan kontext.

---

## 🔹 C. Case + Document Relation

Här testar du domänförståelse och datasäkerhet.

Frågor:

- Varför kontrolleras organisation i service-lagret och inte i controller?
- Hur säkerställer du att dokument inte kan kopplas till fel organisation?
- Vad händer vid race condition?

Detta handlar om hur affärsregler och säkerhet kombineras på rätt nivå.

### Senior bör prata om:

- Transaktioner
- Domäninvarians
- Skydd mot inkonsekvent data
- Separation mellan presentation och affärslogik
- Hur man säkerställer integritet även under parallella anrop

---

## 🔹 D. Classification Policy

Här testar du hur kandidaten tänker kring skalbar auktorisering.

Frågor:

- Hur designade du åtkomst baserat på klassificering?
- Varför centraliserade du policyn?
- Hur skulle du ändra designen om nya klassificeringsnivåer införs?

Detta visar om kandidaten tänker långsiktigt eller skriver punktlösningar.

### Senior tänker kring:

- Open/Closed Principle
- Policy engine kontra if-satser i kod
- Hur systemet kan växa utan att bli svårt att underhålla
- Skalbar och konsekvent auktoriseringsmodell

En bra lösning ska vara enkel att utöka utan att ändra överallt.

---

## 🔹 E. Produktionsmognad

Här går fokus från kod till drift och verklig miljö.

Frågor:

- Hur är tjänsten förberedd för OpenShift?
- Hur hanterar du secrets?
- Vad händer vid horisontell skalning?
- Hur säkerställer du att logging inte läcker känslig information?

Du vill förstå om kandidaten tänker på drift, inte bara utveckling.

### En stark kandidat diskuterar:

- Användning av secrets istället för hårdkodade värden
- Non-root containers
- Resource limits
- Health checks (liveness/readiness)
- Säker loggning utan att exponera känsliga data
- Skalbar arkitektur utan delad state i minnet

---

## Sammanfattning

En bra intervju fokuserar på:

- Hur kandidaten tänker
- Hur designbeslut motiveras
- Hur säkerhet integreras i arkitekturen
- Hur systemet fungerar i produktion
- Hur risker identifieras och hanteras

Det är resonemanget som är viktigt – inte exakt implementation.