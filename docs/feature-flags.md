# Feature flags and personalization

## Cel

Az Autoforge-ban feature flaggelest es szemelyre szabast akarunk hasznalni ugy, hogy a megoldas teljesen ingyenes, self-hostolhato es Dockerrel illesztheto legyen a jelenlegi OCI stackbe.

Jelenlegi stack:

- React frontend
- Spring Boot backend
- Docker Compose public/private hostokon
- GitHub Actions deploy
- OCI private host belso workloadokra

## Verziózás

- Minden feature flag, rollout vagy personalization valtozas celverziohoz kotott.
- A kapcsolodo task fajlban kotelezo a `Verzió` mezo.
- A valtozast fel kell venni a `docs/releases.md` manifestbe a megfelelo verzio alatt.

## Mit akarunk szemelyre szabni?

A feature flag nem csak `true/false` kapcsolo lehet. Az Autoforge-ban hasznalhatjuk runtime konfiguraciokent is.

Pelda flag tipusok:

- boolean: `aiAssistantEnabled`
- string variant: `dashboardVariant = compact | full | experimental`
- number: `maxAgents = 1 | 3 | 5`
- JSON config: `agentDefaults`, `theme`, `onboardingFlow`

Pelda targeting context:

- `userId`
- `tenantId`
- `role`
- `plan`
- `locale`
- `beta`
- `capabilities`
- `createdAt`

## Alapelv

A feature flag UI-szemelyre szabashoz es rollout kontrollhoz jo, de nem jogosultsagkezeles.

- Frontend flag elrejthet vagy megjelenithet UI elemeket.
- Backend oldalon ugyanazt a dontest ujra ellenorizni kell, ha a muvelet valodi hozzaferest vagy koltseges AI futtatast indit.
- Jogosultsagot tovabbra is backend permission modellel kell vedeni.

## Javasolt architektura

Elso fazisban:

- Private hoston fusson egy konnyu feature flag service Dockerben.
- Backend belso Docker networkon vagy private hoston keresztul erje el.
- Frontend ne kozvetlenul a flag service-t hivja, hanem a backend `/api/features` endpointjat.
- A backend epitse fel a flag contextet a bejelentkezett user/tenant adataibol.

Pelda frontend payload:

```json
{
  "newDashboard": true,
  "themeVariant": "compact",
  "aiAssistantMode": "advanced",
  "maxAgents": 3
}
```

## Ingyenes self-hosted opciok

### 1. OpenFeature + flagd

Ez a legkonnyebb kezdo megoldas.

Elonyok:

- teljesen open source;
- Docker image-bol fut;
- nincs kulon adatbazis igenye;
- flag definiciok lehetnek JSON fajlban;
- jol illeszkedik GitHub CI-hez, mert a flag config gitben is verziozhato;
- OpenFeature kompatibilis, tehat kesobb cserelheto a backend.

Korlatozasok:

- nincs termekes admin UI olyan szinten, mint a nagyobb platformokban;
- a flag szerkesztes inkabb fejlesztoi workflow;
- komplex product/marketing kezeleshez kesobb szuk lehet nagyobb platformra.

Hasznalati irany:

- private Compose service: `flagd`
- config mount: `/etc/flagd/flags.json`
- backend SDK: OpenFeature Java SDK + flagd provider
- frontend: backend `/api/features` endpointon keresztul kapja a kiertelt flag eredmenyt

### 2. Flipt

Jo valasztas, ha kell UI es meg mindig konnyu self-hosted modell.

Elonyok:

- self-hosted;
- van dashboard;
- tud segmenteket, constraint-eket, variantokat;
- variant attachmenttel JSON runtime konfiguracio is adhato;
- Git-backed storage opcio miatt jol passzolhat a GitHub alapu workflowhoz.

Korlatozasok:

- tobb uzemeltetesi felelosseg, mint `flagd`;
- UI es storage miatt nagyobb komponens, de meg vallalhato a jelenlegi stackben.

### 3. Unleash

Erett, ismert feature flag platform.

Elonyok:

- eros rollout, strategy es variant modell;
- jol dokumentalt;
- Postgres alapon mukodik;
- feature flag lifecycle szemleletet ad.

Korlatozasok:

- nehezebb, mint `flagd` vagy Flipt;
- Postgres kell hozza;
- a mi 1 GB-os AMD gepeinken csak akkor erdemes, ha tenyleg kell az admin UI es a termekes flag eletciklus.

### 4. GrowthBook

Akkor erdekes, ha feature flag + A/B testing + analytics iranyba megyunk.

Elonyok:

- feature flag es experimentation egyutt;
- React es Java SDK;
- targeting attributumokkal jol tamogatja a szemelyre szabast.

Korlatozasok:

- nagyobb platform;
- tobb infrastruktura es adatkapcsolat kell, ha az analytics reszet is hasznalni akarjuk;
- jelenlegi kicsi OCI gepen nem az elso lepes.

### 5. Flagsmith

Feature flag + remote config + segment platform.

Elonyok:

- self-hostolhato;
- React es Java integracio;
- remote configra es multivariate flagekre alkalmas;
- segmentek es user trait-ek tamogatottak.

Korlatozasok:

- a teljes platform nehezebb, mint a `flagd`;
- akkor eri meg, ha product/admin UI es remote config kezeles is kell.

## OCI Always Free DB opciok kesobbre

Elso korben nem kell adatbazis a feature flag rendszerhez, ha `flagd`-vel indulunk. Kesobb viszont tudunk OCI Always Free adatbazist hasznalni, ha UI-s vagy DB-backed platformra valtunk.

Hivatalos OCI Always Free adatbazis opciok, 2026-05-05-i ellenorzes alapjan:

- Oracle Autonomous Database: maximum 2 Always Free Autonomous Database instance tenancy szinten, kb. 20 GB storage per adatbazis.
- Oracle MySQL HeatWave Always Free: 1 standalone MySQL HeatWave DB system a home regionben, 50 GB data/log storage es 50 GB backup storage.
- Oracle NoSQL Database: 3 tabla, tablankent 25 GB storage, havi olvasasi/irasi kerettel.

Hogyan illeszkedhet ez az Autoforge-hoz:

- Unleash esetben Postgres kellene, ehhez OCI Always Free DB nem idealis, mert nincs Always Free managed PostgreSQL.
- GrowthBook alapbol MongoDB iranyba megy, ehhez OCI Always Free DB nem kozvetlen drop-in.
- Flagsmith tipikusan relacios DB-t igenyel; managed OCI Always Free opciok kozul ezt kulon ellenorizni kell az aktualis deploy modellhez.
- Saját minimal feature flag service eseten az Oracle Autonomous Database vagy MySQL HeatWave Always Free jo lehet flag metadata, tenant profile, personalization config es audit log tarolasra.
- Flipt Git-backed/filesystem storage mellett elindulhat DB nelkul; kesobb DB-backed vagy git-backed modell kozott tudunk donteni.

Praktikus dontes:

- Most: `OpenFeature + flagd`, file/Git alapu flag definiciokkal.
- Kovetkezo lepes, ha admin UI kell: Flipt.
- Kovetkezo lepes, ha sajat personalization store kell: kis sajat Spring Boot modul OCI MySQL HeatWave Always Free vagy Autonomous Database alapon.

## Javasolt bevezetes

1. `flagd` private Docker service.
2. Minimal `features.json` vagy `flags.json` repo-controlled konfiguracio.
3. Spring Boot `/api/features` endpoint.
4. React oldali feature context provider.
5. Backend enforcement az AI es fizetos/koltseges muveleteknel.
6. Kesobb OCI Always Free DB alapú personalization store, ha a flag file mar keves.

## Forrasok

- OpenFeature: https://openfeature.dev/docs/reference/intro/
- flagd: https://flagd.dev/
- flagd quick start: https://flagd.dev/quick-start/
- Flipt concepts: https://docs.flipt.io/v2/concepts
- Unleash feature flags: https://docs.getunleash.io/concepts/feature-flags
- GrowthBook docs: https://docs.growthbook.io/
- Flagsmith docs: https://docs.flagsmith.com/
- OCI Always Free resources: https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm
- OCI MySQL HeatWave Always Free: https://docs.oracle.com/iaas/mysql-database/doc/creating-always-free-db-system.html
