# Secure Dokument Service (SDS)

Spring Boot-baserad REST-tjänst för säker dokumenthantering med rollbaserad åtkomstkontroll.

## Kom igång

| Verktyg | Version |
|---------|---------|
| Java | 21 |
| Maven | 3.9+ |

```bash
# Starta med dev-profil (H2 in-memory)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Kör tester
mvn test
```

**IntelliJ IDEA:** `-Dspring.profiles.active=dev` under *Run/Debug Configurations → VM options*.

H2-konsol: `http://localhost:8080/h2-console` · JDBC URL: `jdbc:h2:mem:sdsdev` · Användare: `sa` · Lösenord: *(tomt)*

## Testa API:et

`POST /dev/token` genererar en JWT lokalt:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/dev/token \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-1","organisationId":"org-a","roles":["USER","ADMIN"]}' \
  | jq -r .token)

curl http://localhost:8080/api/dokuments \
  -H "Authorization: Bearer $TOKEN"
```

> **OBS:** `POST /dev/token` fungerar direkt. Själva API:et för tjänsten svarar `401` tills uppgift A är klar.

---

# Uppgifter

Nedan finns tre uppgifter, A–C. Rekommenderas att börja med A.

### A. Multipla organisationer

Systemet hanterar flera organisationer och all data är strikt isolerad per organisation.
Grunden för isoleringen är att varje inloggad användare bär sin `organisation_id` i JWT-token.

Klassen `JwtOrganisationConverter` ansvarar för att översätta en validerad JWT-token till
ett Spring Security-autentiseringsobjekt med en `OrganisationPrincipal` som principal.
Metoden `convert()` är inte implementerad – det är din uppgift.

JWT-token innehåller följande claims:

| Claim | Typ | Beskrivning |
|-------|-----|-------------|
| `sub` | String | Användarens ID |
| `organisation_id` | String | Organisationens ID (obligatorisk) |
| `roles` | List\<String\> | Roller, t.ex. `["USER", "ADMIN"]` |

Önskat resultat:

- Saknas `organisation_id` ska autentiseringen misslyckas med `BadCredentialsException`.
- Roller ska mappas till Spring Security-auktoriteter med prefixet `ROLE_` (t.ex. `"ADMIN"` → `"ROLE_ADMIN"`).
- `organisation_id` får aldrig komma från request-body – enbart från token.

---

### B. Utöka befintligt API

Den befintliga `DokumentController` exponerar idag en lista med dokument men saknar möjligheten
att hämta ett enskilt dokument via ID.

Implementera följande endpoint i `DokumentController`:

| Metod | URL | Beskrivning |
|-------|-----|-------------|
| `GET` | `/api/dokuments/{id}` | Hämta ett specifikt dokument via ID |

Önskat resultat:

- Isolering per organisation måste gälla – en användare får aldrig se ett dokument från en annan organisation.
- Returnera `404 Not Found` om dokumentet inte finns eller tillhör annan organisation.
- Ingen säkerhetslogik får ligga i controllern.

---

### C. Filtrera arenden på status

`GET /api/arenden` returnerar idag alla arenden för organisationen oavsett status.
Utöka endpointen med en valfri frågeparameter för att filtrera på status.

Önskat beteende:

| Request | Svar |
|---------|------|
| `GET /api/arenden` | Alla arenden (oförändrat) |
| `GET /api/arenden?status=OPEN` | Endast öppna arenden |
| `GET /api/arenden?status=CLOSED` | Endast stängda arenden |

Önskat resultat:

- Ändringen berör repository, service och controller.
- Organisationsisolering gäller oavsett filter.
- Ingen logik får ligga i controllern.
