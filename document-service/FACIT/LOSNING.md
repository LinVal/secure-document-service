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

# B. Domänmodell & åtkomstkontroll

## B.1 Arende

Arende är en separat domänentitet som också tillhör en organisation.
Den innehåller en status (OPEN/CLOSED) och metadata om skapandet.

Affärsregler:

- Endast användare med ADMIN-roll får stänga ett arende.
- Ett stängt arende får inte ändras.
- Dokument som kopplas till ett arende måste tillhöra samma organisation.
- Alla regler kontrolleras i service-lagret.

## B.2 Dokument & klassificering

Inför en centraliserad policy-tjänst (t.ex. `AuthorizationService`).

Ansvar:

- Kontroll av roller
- Kontroll av dokumentklassificering
- Kontroll av organisationstillhörighet

Regler:

- Dokument har en klassificering (enum).
- SECRET kräver SUPER_ADMIN.
- Policyn ska vara centraliserad.
- Ingen säkerhetslogik ska ligga i controllers.

Detta gör systemet utbyggbart och konsekvent.

---

# Teststrategi

## Obligatoriska tester

- Test av organisationsisolering.
- Test av rollbaserad åtkomst.
- Negativa säkerhetstester (403-scenarier).
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
