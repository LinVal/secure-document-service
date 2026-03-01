# Secure Dokument Service (SDS)


Detta repo innehåller en enkel API-tjänst för hantering av dokument.
Du kan göra uppgifterna nedan på det sätt du vill genom att modifiera och utöka den befintliga koden.

### Teknisk miljö

- Java 25
- Spring Boot 4
- Maven
- OAuth2 Resource Server (JWT)
- MySQL
- OpenShift

Autentisering sker via extern OIDC-leverantör (t.ex. Keycloak eller motsvarande).

---

## A. Multipla organisationer

Systemet ska kunna stödja att det finns flera olika organisationer.

### Önskad funktionalitet:

- Varje användare tillhör exakt en organisation (`organisation_id` i JWT).
- All data måste vara strikt isolerad per organisation.
- Access till andra än sin egen organisation får inte vara möjlig.

Lösningen ska vara arkitekturellt robust (t.ex inte beroende av manuella kontroller i controllers).

---

## B. Domänmodell & åtkomstkontroll

### Arende

Inför en ny entitet **Arende** med nedanstående fält:

- id (UUID)
- organisationId
- title
- description
- status (OPEN / CLOSED)
- createdAt
- createdBy

Önskad funktionalitet:

- Arende tillhör en organisation.
- Endast användare med rollen ADMIN får stänga ett arende.
- Stängda arende får inte ändras.
- Dokument kan kopplas till ett arende.
- Dokument och arende måste tillhöra samma organisation.

### Dokument

Utöka entiteten Dokument med klassificeringarna:

- PUBLIC
- INTERNAL
- CONFIDENTIAL
- SECRET

Önskad funktionalitet:

- Klassificering ska styra läsrättigheter.
- SUPER_ADMIN får läsa alla dokument.
- Säkerhetslogiken ska vara centraliserad.
- Ingen säkerhetslogik får ligga direkt i controllers.
