# Secure Dokument Service (SDS)


Detta repo innehåller en enkel API-tjänst för hantering av dokument.
Du kan göra uppgifterna nedan på det sätt du vill genom att modifiera och utöka den befintliga koden.
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

## B. Utöka befintligt API

Den befintliga `DokumentController` exponerar idag en lista med dokument men saknar möjligheten
att hämta ett enskilt dokument via ID.

Implementera följande endpoint i `DokumentController`:

| Metod | URL | Beskrivning |
|-------|-----|-------------|
| `GET` | `/api/dokuments/{id}` | Hämta ett specifikt dokument via ID |

Krav:

- Organisationsisolering måste gälla – en användare får aldrig se ett dokument från en annan organisation.
- Returnera `404 Not Found` om dokumentet inte finns eller tillhör annan organisation.
- Ingen säkerhetslogik får ligga i controllern.

---

## C. Filtrera arenden på status

`GET /api/arenden` returnerar idag alla arenden för organisationen oavsett status.
Utöka endpointen med en valfri frågeparameter för att filtrera på status.

Önskat beteende:

| Request | Svar |
|---------|------|
| `GET /api/arenden` | Alla arenden (oförändrat) |
| `GET /api/arenden?status=OPEN` | Endast öppna arenden |
| `GET /api/arenden?status=CLOSED` | Endast stängda arenden |

Krav:

- Ändringen berör repository, service och controller.
- Organisationsisolering gäller oavsett filter.
- Ingen logik får ligga i controllern.
