# AI Tooling Stack

## Cel

Ez a dokumentum rogzit egy ajanlott MCP + skill + group rendszert az Autoforge stackhez.
Nem minimal csomagra optimalizal, hanem arra, hogy:

- a kod tiszta maradjon,
- az auth/gateway/deploy hibak gyorsan izolalhatok legyenek,
- a live OCI runtime allapot ellenorizheto legyen,
- a dokumentacio es az architektura ne csusszon szet a mukodo rendszerhez kepest.

## Konfiguracios fajlok

- `ops/ai/mcps.yaml`: ajanlott MCP katalogus
- `ops/ai/skills.yaml`: ajanlott skill katalogus
- `ops/ai/groups.yaml`: ajanlott capability groupok

## MCP-k

### `playwright`

Feladata:

- valos bongeszos login flow reprodukcio
- redirect URL-ek, callback allapotok es session viselkedes ellenorzese
- request/response es console hibak rogzitese
- annak bizonyitasa, hogy a javitas tenyleg mukodik a browserben

### `github`

Feladata:

- PR allapotok, merge-elhetoseg es review/check informaciok olvasasa
- GitHub Actions logok es workflow runok kiolvasasa
- merge utani deploy viselkedes kovetese
- release es release-note traceability tamogatasa

### `ssh-remote-shell`

Feladata:

- public es private OCI hostok olvasasa
- compose env, log, runtime allapot ellenorzese
- production-only hibak celzott diagnosztikaja
- host oldali allapot valos ideju visszaellenorzese

### `docker-compose`

Feladata:

- melyik image verzio fut valojaban
- compose env drift felderitese
- kontenerlogok es exec ellenorzesek
- deploy eredmenyenek validalasa kontenerszinten

### `http-api`

Feladata:

- `/api/v1/me`, `/api/v1/status`, `/actuator/health` es hasonlo endpointok tesztelese
- 401/404/502 tipusu hibak gyors szetvalasztasa
- Keycloak discovery, JWKS es userinfo endpointok ellenorzese
- frontend vs gateway vs backend hibak gyors lokalizalasa

### `keycloak-admin`

Feladata:

- realm, client, redirect URI es web origin ellenorzes
- felhasznalok es role-ok ellenorzese
- auth regressziok forrasanak behatarolasa Keycloak oldalon
- frontend es backend auth elvarasok osszehangolasa

### `oci`

Feladata:

- compute instancek, halozat, security list / NSG allapot ellenorzese
- Vault, volume es OCI infrastruktura metadata ellenorzese
- private/public kapcsolat es host elhelyezes validalasa
- runtime problemak OCI-oldali okainak kizarasa vagy bizonyitasa

### `cloudflare`

Feladata:

- DNS rekordok es proxied allapot ellenorzese
- SSL mode ellenorzese
- public deploy utani edge allapot validalasa
- Cloudflare vs origin hibak kulonvalasztasa

### `vault-secrets`

Feladata:

- secret jelenlet es metadata ellenorzes
- deployhoz szukseges secret referenciak validalasa
- rotacios vagy stale secret helyzetek feltarasa
- runtime hibak csokkentese hianyzó/stale secret miatt

### `maven-java-deps`

Feladata:

- Spring Boot / Spring Cloud / runtime dependency mismatch felderitese
- dependency tree ellenorzes
- BOM drift kimutatasa
- Java runtime osztalykonyvtar hibak gyors diagnosztikaja

### `node-workspace`

Feladata:

- root es workspace dependency konzisztencia
- lockfile drift csokkentese
- frontend csomagok telepitett allapotanak ellenorzese
- verziozasi es package szintu meglepetesek csokkentese

### `diagram-architecture`

Feladata:

- SVG / diagram allapot karbantartasa
- arch leiras es abra kozti drift csokkentese
- topology vagy auth/deploy valtozas utani vizualis frissites tamogatasa
- stale arch allitasok gyors felfedese

### `context7`

Feladata:

- legfrissebb dokumentáció lekérése külső könyvtárakhoz vagy Autoforge-specifikus API-khoz
- ügynök környezetének kiegészítése valós idejű dokumentációs adatokkal

### `prompt-library`

Feladata:

- ujrafelhasznalhato prompt mintak es guardrail-ek karbantartasa
- prompt cel, scope, bemenet, kimenet es acceptance criteria tisztazasa
- trusted instrukciok es untrusted tartalom szetvalasztasanak tamogatasa
- prompt injection es secret exposure kockazatok csokkentese hosszu eletu promptoknal

### `sast-scanner`

Feladata:

- forráskód sebezhetőségeinek (pl. OWASP Top 10) statikus elemzése
- automatizált szkennelés PR-ok előtt

### `secret-scanner`

Feladata:

- tárolt titkok (API kulcsok, jelszavak) keresése a git történetben
- biztonsági gyakorlatoknak való megfelelés biztosítása

### `dependency-audit`

Feladata:

- ismert sebezhetőségek keresése a `npm`/`maven` függőségekben
- biztonsági tanácsadók összevetése a függőségi fával

### `observability`

Feladata:

- jovo beli log / metric / trace integracio
- host shell helyett aggregalt runtime diagnozis
- csak akkor prioritas, ha Grafana/Loki/Prometheus/Sentry is bekerul a stackbe

## Skillek

### `auth-debug`

Feladata:

- Keycloak redirect, callback es PKCE flow diagnosztika
- token jelenlet, token tovabbitas es `/me` bootstrap ellenorzese
- frontend auth state es backend auth reject szetvalasztasa
- claim, issuer, audience, role es protected endpoint problemak feltarasa

### `gateway-debug`

Feladata:

- Caddy -> API gateway -> backend lanc hibainak bontasa
- 404 / 401 / 502 jellegu routing hibak izolalasa
- route config package/runtime allapot ellenorzese
- upstream eleresi es dependency mismatch hibak feltarasa

### `oci-deploy-debug`

Feladata:

- public/private host deploy anomaliak feltarasa
- workflow trigger hianyok es image drift vizsgalata
- host env drift es compose drift ellenorzese
- DNS, OCI, Vault es deploy script egyuttmozgasanak validalasa

### `backend-auth-runtime`

Feladata:

- Spring Security es JWT runtime validacio ellenorzese
- local vs production tokenelfogadas kulonbsegenek feltarasa
- issuer/JWKS/static key hibak szetvalasztasa
- backend auth config es Keycloak realm viselkedes osszehangolasa

### `e2e-repro`

Feladata:

- valos UI regressziok determinisztikus reprodukcioja
- request/response es redirect bizonyitek gyujtese
- auth es session hibak tenyleges bongeszos reprodukcioja
- javitas utan vegso bizonyitas a mukodesre

### `security-audit`

Feladata:

- átfogó automatizált biztonsági audit és sebezhetőségi felmérés
- SAST szkennelés futtatása
- titokszivárgás ellenőrzése
- függőségek sebezhetőségi vizsgálata
- biztonsági jelentés készítése PR-review-khoz

### `prompt-engineering`

Feladata:

- tiszta, korlatos promptok tervezese cel, scope, bemenet es kimenet szerint
- system/developer instrukciok es user/retrieved tartalom szetvalasztasa
- prompt injection, tulengedelyezes es secret exposure kockazatok kiszurese
- hosszu eletu agent, workflow es termek promptok review-ja es standardizalasa

### `release-versioning`

Feladata:

- task ID, verzio bump, release manifest es workflow trigger konzisztencia
- deployhez szukseges image tag kovetes
- package verzio es release dokumentacio osszhangban tartasa
- stale vagy hianyzó workflow trigger logikak gyors felfedese

### `git-management`

Feladata:

- feladat-ID alapú branching és commit elnevezések kikényszerítése
- branch életciklus menedzsment (létrehozás, push, takarítás)
- PR létrehozás, frissítés, cím és törzs formázás automatizálása
- konzisztencia biztosítása a lokális branch és a remote PR állapot között

### `docs-sync`

Feladata:

- README, architecture, deployment es authentication dokumentacio frissen tartasa
- architekturadiagram frissitesi kotelezettseg kezelese
- “prepared/scaffolded” jellegu stale allitasok kiszurese
- runtime valtozasok visszairasa a dokumentacioba

## Groupok

### `app-surface`

Feladata:

- public UI, browser runtime es auth callback felulet kezelese
- frontend oldali request-trigger logika kezelese
- user oldali regressziok reprodukcioja

### `api-auth`

Feladata:

- backend API, JWT, claim es Keycloak contract kezelese
- `/api/v1/me` es kapcsolodo auth endpointok viselkedese
- frontend/backend auth szerzodes tisztan tartasa

### `edge-routing`

Feladata:

- Caddy, Spring Cloud Gateway es upstream routing kezelese
- public/private hatar felugyelete
- path, header es route config problemak kezelese

### `delivery-runtime`

Feladata:

- GitHub Actions, GHCR, OCI, Docker Compose es deploy pipeline felugyelete
- public/private host runtime allapot validalasa
- image tag drift, env drift es trigger drift kezelese

### `knowledge-governance`

Feladata:

- dokumentacio, diagram, release es task traceability karbantartasa
- architektura es deploy tudasbazis aktualis allapotban tartasa

## Hogyan használd (Üzletmeneti folyamat)

Az AI Tooling stack nem egy egyszerű eszközlista, hanem egy hierarchikus diagnosztikai rendszer. Az ügynöknek a következő sorrendben kell navigálnia:

1. **Group (Képességcsoport) kiválasztása**: Azonosítsd, hogy a hiba melyik doménbe tartozik (pl. ha a felhasználó nem tud belépni $\rightarrow$ `app-surface` vagy `api-auth`).
2. **Skill (Készség) aktiválása**: Határozd meg a konkrét célt a csoporton belül (pl. ha a callback nem működik $\rightarrow$ `auth-debug`).
3. **MCP (Eszköz) alkalmazása**: Gyűjts bizonyítékokat a skillhez tartozó eszközökkel (pl. `playwright` a böngészőhöz, `http-api` a tokenekhez).

### Gyakori scenario-k (Troubleshooting Matrix)

| Probléma | Group | Skill | MCP-k (Sorrendben) |
| :--- | :--- | :--- | :--- |
| **Login hiba / Redirect loop** | `app-surface` | `auth-debug` | `playwright` $\rightarrow$ `http-api` $\rightarrow$ `keycloak-admin` |
| **502 Bad Gateway / 404 Route** | `edge-routing` | `gateway-debug` | `http-api` $\rightarrow$ `ssh-remote-shell` $\rightarrow$ `docker-compose` |
| **Deploy nem történt / Rossz verzió** | `delivery-runtime` | `oci-deploy-debug` | `github` $\rightarrow$ `docker-compose` $\rightarrow$ `ssh-remote-shell` |
| **JWT validációs hiba (Backend)** | `api-auth` | `backend-auth-runtime` | `http-api` $\rightarrow$ `maven-java-deps` $\rightarrow$ `keycloak-admin` |
| **Biztonsági audit / Sebezhetőség** | `security-compliance` | `security-audit` | `sast-scanner` $\rightarrow$ `dependency-audit` $\rightarrow$ `secret-scanner` |
| **Prompt minőség / prompt injection** | `knowledge-governance` | `prompt-engineering` | `prompt-library` $\rightarrow$ `context7` $\rightarrow$ `secret-scanner` |
| **Dokumentáció és realidadegyezetlen** | `knowledge-governance` | `docs-sync` | `oci` $\rightarrow$ `github` $\rightarrow$ `diagram-architecture` |

## Ajanlott prioritasi sorrend

### Elso kor

- `playwright`
- `github`
- `ssh-remote-shell`
- `docker-compose`
- `http-api`

### Masodik kor

- `keycloak-admin`
- `oci`
- `vault-secrets`
- `cloudflare`

### Harmadik kor

- `maven-java-deps`
- `node-workspace`
- `diagram-architecture`
- `prompt-library`
- `observability`

## Miert ez a csomag jo az Autoforge stackhez

Ez a struktura direkt azokra a hibakra van szabva, amelyek a stackben tenylegesen jelentkeztek:

- auth callback/session problemak
- gateway route packaging hibak
- deploy trigger hianyok
- public/private runtime drift
- Spring dependency mismatch
- production-only JWT validation kulonbsegek
- stale dokumentacio es stale arch allitasok

Ezert a cel nem a minimum, hanem egy olyan eszkozkeszlet, amivel a kod, a deploy es az architektura is kovetkezetesen tisztan tarthato.
