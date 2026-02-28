# Secure Document Service (SDS)
## Teknisk Uppgift – Senior Javautvecklare

---

## Syfte

Denna uppgift är utformad för att utvärdera arkitekturförståelse, säkerhetsdesign och produktionsmognad.

Fokus ligger på:

- Säkerhetsintegrerad design
- Organisationsisolering
- Tydlig lagerstruktur
- Testbarhet
- Produktionsberedskap

Du får modifiera och utöka den befintliga koden.
Du får inte försvaga den befintliga säkerhetsmodellen.

---

## Teknisk miljö

- Java 25
- Spring Boot 4
- Maven
- OAuth2 Resource Server (JWT)
- PostgreSQL
- OpenShift

Autentisering sker via extern OIDC-leverantör (t.ex. Keycloak eller motsvarande).

---

# Uppgifter

---

## 1. Organisationsstöd

Systemet ska stödja flera organisationer.

### Krav:

- Varje användare tillhör exakt en organisation (`organisation_id` i JWT).
- All data måste vara strikt isolerad per organisation.
- Cross-organisation access får inte vara möjlig.
- Gäller alla endpoints och alla entiteter.

Lösningen ska vara arkitekturellt robust, inte beroende av manuella kontroller i controllers.

---

## 2. Ny Domän: Case

Inför en ny entitet: **Case**

### Fält:

- id (UUID)
- organisationId
- title
- description
- status (OPEN / CLOSED)
- createdAt
- createdBy

### Affärsregler:

- Case tillhör en organisation.
- Endast användare med rollen ADMIN får stänga ett case.
- Stängda case får inte ändras.
- Dokument kan kopplas till ett case.
- Dokument och case måste tillhöra samma organisation.

---

## 3. Dokumentklassificering

Utöka Document med klassificering:

- PUBLIC
- INTERNAL
- CONFIDENTIAL
- SECRET

### Krav:

- Klassificering ska styra läsrättigheter.
- SUPER_ADMIN får läsa alla dokument.
- Säkerhetslogik ska vara centraliserad.
- Ingen säkerhetslogik får ligga direkt i controllers.

---

## 4. Säker filhantering

Vid uppladdning:

- Maximal filstorlek ska definieras.
- Endast tillåtna content-types.
- Skydd mot path traversal.
- Filer får inte lagras i web-root.
- Ingen exekverbar kod får accepteras.

---

## 5. Audit Logging

Implementera strukturerad audit-loggning för:

- Skapande av dokument
- Uppdateringar
- Case-ändringar
- Misslyckade åtkomstförsök

### Krav:

- Logga aktör och organisation.
- Logga inte känslig information.
- Skydda mot log injection.
- Loggning får inte påverka affärslogik.

---

## 6. Produktionskrav (OpenShift)

Applikationen ska:

- Köras som non-root.
- Använda secrets för känslig konfiguration.
- Definiera resource limits.
- Ha liveness/readiness probes.
- Vara redo för horisontell skalning.

---

## Leverabler

- Källkod
- Automatiserade tester
- README med körinstruktioner
- Kort designbeskrivning
- Enkel hotmodell

---

## Bedömningskriterier

- Organisationsisolering
- Säkerhetsdesign
- Arkitektur och kodstruktur
- Testkvalitet (inklusive negativa tester)
- Produktionsmognad
- Dokumentation