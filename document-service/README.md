# Secure Dokument Service (SDS)

En Spring Boot-baserad REST-tjänst för säker dokumenthantering med organisationsisolering,
rollbaserad åtkomstkontroll och klassificering av dokument.

---

## Förutsättningar

| Verktyg | Version |
|---------|---------|
| Java | 25 |
| Maven | 3.9+ |
| (Prod) MySQL | 8+ |
| (Prod) Keycloak | 26+ |

---

## Starta applikationen

Projektet har två profiler: `dev` (H2 in-memory) och `prod` (MySQL).

### Dev-profil

Kör med H2 in-memory-databas. Ingen extern databas eller Keycloak krävs för att starta,
men JWT-validering är aktiv och kräver en giltig token mot den konfigurerade issuer-uri.

```bash
# Med Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Eller via JAR
java -jar target/document-service.jar --spring.profiles.active=dev
```

**IntelliJ IDEA:** Lägg till `-Dspring.profiles.active=dev` under *Run/Debug Configurations → VM options*.

> **OBS:** Dev-profilen använder `ddl-auto: none`. Databasschemat definieras i
> `src/main/resources/schema.sql` som läses automatiskt av H2 vid uppstart.

### Prod-profil

Kräver en körande MySQL-instans och Keycloak. Konfigurera via miljövariabler:

```bash
export DB_URL=jdbc:mysql://localhost:3306/sds
export DB_USERNAME=sdsuser
export DB_PASSWORD=hemligt
export OIDC_ISSUER_URI=https://auth.example.com/realms/sds
export STORAGE_DIR=/var/data/sds-uploads

java -jar target/document-service.jar --spring.profiles.active=prod
```

---

## H2-konsolen (dev)

När applikationen körs med dev-profilen finns H2:s webbkonsol tillgänglig:

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:sdsdev`
- Användarnamn: `sa`
- Lösenord: *(tomt)*

Konsolen är användbar för att verifiera att `schema.sql` applicerades korrekt och
för att inspektera data under utveckling.

---

## Autentisering & JWT

Alla endpoints (utom `/actuator/health`) kräver en giltig JWT utfärdad av konfigurerad OIDC-leverantör.

JWT-token förväntas innehålla följande claims:

| Claim | Beskrivning |
|-------|-------------|
| `sub` | Användarens ID |
| `organisation_id` | Organisationen användaren tillhör |
| `roles` | Lista med roller, t.ex. `["USER", "ADMIN", "SUPER_ADMIN"]` |

Tokenen skickas som `Authorization: Bearer <token>` i varje request.

### Dev-setup med Keycloak

Dev-profilen pekar mot `http://localhost:8180/realms/sds`.
Starta Keycloak lokalt och konfigurera ett realm `sds` med lämpliga clients och användare.

För att temporärt testa utan Keycloak kan `issuer-uri` i `application.yaml` bytas ut mot
en annan OIDC-kompatibel leverantör (t.ex. Auth0 eller Azure AD).

---

## API-översikt

### Dokument

| Metod | URL | Beskrivning | Roll |
|-------|-----|-------------|------|
| `GET` | `/api/dokuments` | Lista alla dokument | USER |
| `GET` | `/api/dokuments/{id}` | Hämta ett specifikt dokument | USER |
| `GET` | `/api/dokuments/by-arende/{arendeId}` | Hämta dokument kopplade till ett arende | USER |
| `POST` | `/api/dokuments` | Skapa dokument | USER |
| `PUT` | `/api/dokuments/{id}` | Uppdatera dokument | USER |
| `DELETE` | `/api/dokuments/{id}` | Ta bort dokument | USER |
| `POST` | `/api/dokuments/{id}/upload` | Ladda upp fil till dokument | USER |

### Arenden

| Metod | URL | Beskrivning | Roll |
|-------|-----|-------------|------|
| `GET` | `/api/arenden` | Lista alla arenden | USER |
| `GET` | `/api/arenden/{id}` | Hämta ett specifikt arende | USER |
| `POST` | `/api/arenden` | Skapa arende | USER |
| `PUT` | `/api/arenden/{id}/close` | Stäng arende | ADMIN |

### Hälsokontroll

| Metod | URL | Beskrivning |
|-------|-----|-------------|
| `GET` | `/actuator/health` | Applikationsstatus (ingen auth krävs) |
| `GET` | `/actuator/health/liveness` | Liveness probe (Kubernetes/OpenShift) |
| `GET` | `/actuator/health/readiness` | Readiness probe (Kubernetes/OpenShift) |

---

## Projektstruktur

```
src/main/java/se/exempel/sds/
├── controller/        # Tunna REST-controllers, ingen affärslogik
├── service/           # Affärsregler och säkerhetskontroller
├── repository/        # Spring Data JPA-repositories
├── domain/            # JPA-entiteter och enums
├── dto/               # Request/response-objekt
├── security/          # JWT-konvertering och principal-klass
├── config/            # Spring-konfiguration
└── exception/         # Globala undantagshanterare

src/main/resources/
├── application.yaml   # Konfiguration för dev- och prod-profiler
├── schema.sql         # Databasschema för dev (H2)
└── logback-spring.xml # Loggkonfiguration
```

---

## Bygga projektet

```bash
# Kompilera och kör tester
mvn verify

# Bygg körbar JAR
mvn package -DskipTests

# Kör enbart tester
mvn test
```
