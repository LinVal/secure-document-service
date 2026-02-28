# 1. Secure Document Service (SDS)
## 1.1 Referenslösning

Detta dokument beskriver en strukturerad och praktisk lösningsstrategi kopplad till uppgifterna.  
Syftet är att visa hur en säker och arkitekturellt korrekt implementation kan se ut.

---

# 2. Arkitektur

## 2.1 Rekommenderad struktur

- controller/
- service/
- repository/
- security/
- domain/
- config/
- exception/

## 2.2 Principer

- Controllers är tunna och innehåller ingen affärslogik.
- Affärsregler implementeras i service-lagret.
- Säkerhetslogik centraliseras i en policy-/auktoriseringstjänst.
- Organisation hämtas enbart från JWT-token.
- Alla databasfrågor filtreras på organisation.
- Systemet ska vara testbart med fokus på säkerhet.

Denna struktur säkerställer tydlig separation av ansvar.

---

# 3. Multi-Organisation Strategi

## 3.1 Lösning

Organisation hämtas från JWT (`organisation_id`).  
Den lagras i en principal-klass och används i hela applikationen.

Åtkomst kontrolleras genom:

- Repository-metoder som filtrerar på organisation
  - exempel: `findByIdAndOrganisationId`
- Alternativt globalt filter (t.ex. Hibernate-filter)
- Alternativt explicit kontroll i service-lagret

## 3.2 Viktiga säkerhetsprinciper

- Organisation får aldrig komma från klientens request-body.
- All verifiering sker server-side.
- Cross-organisation access ska alltid resultera i 403 Forbidden.
- Tester ska verifiera att isoleringen fungerar.

Detta skyddar mot horizontal privilege escalation.

---

# 4. Case-implementation

## 4.1 Domänmodell

Case är en separat domänentitet som också tillhör en organisation.  
Den innehåller en status (OPEN/CLOSED) och metadata om skapandet.

## 4.2 Affärsregler

- Endast användare med ADMIN-roll får stänga ett case.
- Ett stängt case får inte ändras.
- Dokument som kopplas till ett case måste tillhöra samma organisation.
- Alla regler kontrolleras i service-lagret.

Detta säkerställer att domänregler och säkerhetsregler upprätthålls konsekvent.

---

# 5. Klassificeringspolicy

## 5.1 Lösning

Inför en centraliserad policy-tjänst (t.ex. `AuthorizationService` eller `DocumentPolicy`).

## 5.2 Ansvar

- Kontroll av roller
- Kontroll av dokumentklassificering
- Kontroll av organisationstillhörighet

## 5.3 Regler

- Dokument har en klassificering (enum).
- SECRET kräver SUPER_ADMIN.
- Policyn ska vara centraliserad.
- Ingen säkerhetslogik ska ligga i controllers.

Detta gör systemet utbyggbart och konsekvent.

---

# 6. Säker Filhantering

## 6.1 Lösning

- Validera maximal filstorlek.
- Tillåt endast godkända content-types.
- Generera säkra filnamn (t.ex. UUID).
- Lagra filer utanför web-root.
- Separera lagring i en dedikerad service om möjligt.

## 6.2 Säkerhetsprinciper

- Ingen användarkontrollerad sökväg får användas direkt.
- Skydda mot path traversal.
- Förhindra uppladdning av exekverbara filer.
- Filhantering ska vara oberoende av affärslogik.

---

# 7. Audit-design

## 7.1 Lösning

Inför en `AuditService` som ansvarar för loggning av säkerhetskritiska händelser.

Logga exempelvis:

- Vem som utförde en åtgärd
- Vilken organisation
- Vilken operation
- Vilket objekt
- Tidsstämpel

## 7.2 Viktiga principer

- Använd strukturerad loggning.
- Logga aldrig tokens eller känsliga data.
- Audit ska vara spårbart men inte innehålla sekretessbelagd information.
- Loggningen ska inte påverka affärslogik.

---

# 8. OpenShift och Produktion

## 8.1 Produktionskrav

- Applikationen ska köras som non-root.
- Känslig konfiguration ska hanteras via secrets.
- Resource limits ska vara definierade.
- Liveness- och readiness-probes ska finnas.
- Applikationen ska kunna skalas horisontellt.

## 8.2 Implementation

- Använd minimal Docker-image.
- Konfiguration ska ske via miljövariabler.
- Health-endpoint ska användas för probes.
- Inga hemligheter får finnas i koden.

Detta säkerställer att systemet är redo för drift i container-miljö.

---

# 9. Teststrategi

## 9.1 Obligatoriska tester

- Test av organisationsisolering.
- Test av rollbaserad åtkomst.
- Negativa säkerhetstester (403-scenarier).
- Integrationstester med verklig databas (t.ex. Testcontainers).

## 9.2 Syfte

Tester ska verifiera säkerhetsregler, inte enbart funktionalitet.  
Det är särskilt viktigt att testa att felaktig åtkomst nekas korrekt.

---

# 10. Vad kännetecknar en stark lösning

En stark implementation har:

- Centraliserad och konsekvent auktorisering.
- Tydlig separation mellan domän, säkerhet och infrastruktur.
- Robust organisationsisolering.
- Tydliga och omfattande tester.
- Produktionsmedveten konfiguration.
- Dokumenterade designval och säkerhetsresonemang.