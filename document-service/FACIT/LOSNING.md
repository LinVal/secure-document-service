# Secure Dokument Service (SDS)
## Referenslösning

Detta dokument beskriver en strukturerad och praktisk lösningsstrategi kopplad till uppgifterna.
Syftet är att visa hur en säker och arkitekturellt korrekt implementation kan se ut.

---

# Arkitektur

## Rekommenderad struktur

- controller/
- service/
- repository/
- security/
- domain/
- config/
- exception/

## Principer

- Controllers är tunna och innehåller ingen affärslogik.
- Affärsregler implementeras i service-lagret.
- Säkerhetslogik centraliseras i en policy-/auktoriseringstjänst.
- Organisation hämtas enbart från JWT-token.
- Alla databasfrågor filtreras på organisation.
- Systemet ska vara testbart med fokus på säkerhet.

Denna struktur säkerställer tydlig separation av ansvar.

---

# A. Multipla organisationer

## A.1 Lösning

Organisation hämtas från JWT (`organisation_id`).
Den lagras i en principal-klass och används i hela applikationen.

Åtkomst kontrolleras genom:

- Repository-metoder som filtrerar på organisation
  - exempel: `findByIdAndOrganisationId`
- Alternativt globalt filter (t.ex. Hibernate-filter)
- Alternativt explicit kontroll i service-lagret

## A.2 Viktiga säkerhetsprinciper

- Organisation får aldrig komma från klientens request-body.
- All verifiering sker server-side.
- Cross-organisation access ska alltid resultera i 403 Forbidden.
- Tester ska verifiera att isoleringen fungerar.

Detta skyddar mot horizontal privilege escalation.

---

# B. Utöka befintligt API

## B.1 Lösning: GET /api/dokuments/{id}

Endpoint-metoden är tre till fyra rader och delegerar direkt till service-lagret.
Ingen organisations- eller säkerhetskontroll i controllern.

Service-lagret hämtar dokumentet via `findByIdAndOrganisationId` – organisationsisolering
garanteras av att repository-metoden alltid kräver `organisationId` som parameter.

Vid saknat ID eller fel organisation kastas ett undantag som `GlobalExceptionHandler`
mappar till `404 Not Found`. Att returnera 404 (och inte 403) vid cross-org-access är
ett medvetet val för att undvika informationsläcka om vilka resurser som existerar.

## B.2 Viktiga designprinciper

- `findById()` utan `organisationId` ska aldrig användas.
- Controllern innehåller ingen logik – enbart parameter-mappning och delegering.
- Tester ska verifiera att fel organisations-ID ger 404, inte 200 eller 403.

---

# C. Filtrera arenden på status

## C.1 Lösning

Ändringen berör tre lager: repository, service och controller.

**Repository** – ny Spring Data-metod med namnbaserad query:

```
findAllByOrganisationIdAndStatus(String organisationId, ArendeStatus status)
```

**Service** – `listArenden` tar emot en nullable `ArendeStatus`. Om den är `null` används
befintlig `findAllByOrganisationId`, annars den nya metoden med statusfilter.

**Controller** – `listArenden` utökas med `@RequestParam(required = false) ArendeStatus status`
som skickas vidare till service-lagret.

## C.2 Viktiga designprinciper

- `organisationId` kombineras alltid med statusfiltret – aldrig ett anrop utan org-filter.
- `null`-status returnerar hela listan (bakåtkompatibelt).
- Spring Data härleder SQL automatiskt från metodnamnet – ingen `@Query` behövs.
- Controllern innehåller noll logik – enbart parameter-mappning.

---

# Teststrategi

## Obligatoriska tester

- Test av organisationsisolering.
- Test av rollbaserad åtkomst.
- Negativa säkerhetstester (403/404-scenarier).
- Integrationstester med verklig databas (t.ex. Testcontainers).

## Syfte

Tester ska verifiera säkerhetsregler, inte enbart funktionalitet.
Det är särskilt viktigt att testa att felaktig åtkomst nekas korrekt.

---

# Vad kännetecknar en stark lösning

En stark implementation har:

- Centraliserad och konsekvent auktorisering.
- Tydlig separation mellan domän, säkerhet och infrastruktur.
- Robust organisationsisolering.
- Tydliga och omfattande tester.
- Dokumenterade designval och säkerhetsresonemang.
