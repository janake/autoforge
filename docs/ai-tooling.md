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
- `ops/mcp/jira-local.sh`: helyi, gitignored Jira MCP launcher a repohoz kotott stdio inditashoz

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

### `github-actions`

Feladata:

- workflow run, job, step es artifact allapotok celzott diagnosztikaja
- trigger, path filter, permission, secret/variable es runner kornyezet ellenorzese
- main merge utani deploy viselkedes bizonyitasa logok alapjan
- build, registry es deploy jobok kozti handoff hibak izolalasa

### `ghcr-registry`

Feladata:

- GHCR package, image tag, digest es visibility allapot ellenorzese
- deploy token scope, package permission es pull/push hiba szetvalasztasa
- verziozott image tag es `package.json` verzio osszevetese registry allapottal
- third-party image preload es cache viselkedes ellenorzese private host deploynal

### `jira`

Feladata:

- Jira epic, story, bug, task es spike olvasasa, letrehozasa es frissitese
- backlog grooming, statuszvaltas es JQL alapu kereses tamogatasa
- teljes Agile sprintkezeles: board es sprint listazas, sprint letrehozas, start/close/update, issue sprintbe mozgatas, backlogba mozgatas es rankeles
- teljes projekt export determinisztikus JSON-ba: projekt metadata, boardok, sprintek, epicek, standard issue-k, subtaskok, kommentek, linkek, parent kapcsolatok, labelek es custom fieldek
- Jira issue-k osszekotese branch, commit, PR, release es verifikacios adatokkal
- Jira-first source-of-truth mukodes tamogatasa a Markdown task migracio utan
- Helyi futtatasnal az `ops/ai/mcps.yaml` a `ops/mcp/jira-local.sh` gitignored launchert hasznalja.

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

### `docker-engine`

Feladata:

- Docker image, network, volume es daemon allapot ellenorzese
- rovid eletu utility kontenerek futtatasi mintainak validalasa host telepites helyett
- `DOCKER-USER` chain es metadata endpoint izolacio ellenorzese
- Docker permission, image preload es local cache problemak szetvalasztasa

### `firecracker`

Feladata:

- Firecracker microVM alapu izolacios dontesek elokeszitese
- jailer, tap network, kernel/rootfs es metadata izolacio vizsgalata
- kontener vs microVM trust boundary osszehasonlitasa AI/code execution munkaknal
- kesobbi OpenCode vagy worker sandboxolas infrastrukturajanak tervezese

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

### `oracle-cloud`

Feladata:

- OCI runtime, Always Free es host readiness dontesek ellenorzese
- Dynamic Group, policy, instance principal es metadata endpoint feltetelek validalasa
- VCN routing, security list, NSG, jump-host es private host eleres vizsgalata
- OCI control-plane es SSH/Docker runtime bizonyitekok osszekapcsolasa

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
- Java 21-25, Maven compiler/toolchain es Lombok annotation processor kompatibilitas ellenorzese

### `spring-framework`

Feladata:

- Spring Framework, Spring Boot, Spring Security, Spring Data es Spring Cloud diagnosztika
- auto-configuration, profile, property binding es actuator viselkedes ellenorzese
- datasource, JPA, transaction es repository problemak szetvalasztasa
- Spring Cloud Gateway es backend Spring verzioillesztes kovetese

### `java-platform`

Feladata:

- Java 21-25 nyelvi/runtime kompatibilitas ellenorzese
- Maven compiler, toolchain, bytecode target, CI JDK es Docker runtime JDK osszhang vizsgalata
- virtual threads, records, pattern matching, sealed types es uj JVM feature tradeoffok review-ja
- classfile version, module path, GC, memory es container JVM viselkedes diagnosztikaja

### `lombok`

Feladata:

- Lombok annotation processing es generalt kod ellenorzese
- constructor, builder, equals/hashCode, logging es nullability feltetelezesek review-ja
- Spring/JPA modellekben a Lombok mellekhatasok kiszurese
- minimalis, explicit es Java/Spring kompatibilis Lombok hasznalat tamogatasa

### `node-workspace`

Feladata:

- root es workspace dependency konzisztencia
- lockfile drift csokkentese
- frontend csomagok telepitett allapotanak ellenorzese
- verziozasi es package szintu meglepetesek csokkentese

### `react-web`

Feladata:

- React, Vite es TypeScript web UI viselkedes diagnosztikaja
- komponens, hook, form, routing, auth bootstrap es API integracio ellenorzese
- browser runtime, CORS es gateway request hibak szetvalasztasa
- modern React mintak hasznalata felesleges memoization nelkul, repo mintak szerint

### `react-native`

Feladata:

- React Native / mobile surface tervezesi es diagnosztikai dontesek tamogatasa
- shared React/domain kod es web-only Vite/browser feltetelezesek szetvalasztasa
- navigation, Metro, native module, platform permission es mobile auth feltetelek review-ja
- mobil API contract es auth flow elokeszitese explicit feladat eseten

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

### `github-actions-ghcr-debug`

Feladata:

- GitHub Actions run/job logok elso valodi hibajanak izolalasa
- GHCR image tag, digest, package visibility es deploy token scope ellenorzese
- build -> registry -> private/public host image handoff bizonyitasa
- workflow permission, secret, variable es path-filter drift kiszurese

### `oracle-cloud-runtime-debug`

Feladata:

- Oracle Cloud compute, VCN, Vault, Dynamic Group es policy allapot ellenorzese
- instance principal es metadata endpoint viselkedes validalasa deploy hibak elott
- public/private host elhelyezes, jump-host es security list/NSG problemak szetvalasztasa
- Always Free korlatok es reprodukalhato host readiness feltetelek figyelembe vetele

### `container-runtime-debug`

Feladata:

- Docker image, Compose stack, daemon es host permission hibak izolalasa
- ephemeral utility kontener mintak hasznalata host csomagtelepites helyett
- kontener metadata endpoint kitettség es `DOCKER-USER` chain ellenorzese
- image preload, local cache es tag drift verifikalasa deploy hibaknal

### `microvm-runtime-isolation`

Feladata:

- Firecracker microVM izolacio szuksegessegenek eldontese magas kockazatu AI/code execution munkaknal
- konteneres es microVM-es sandboxolas tradeoffjainak dokumentalasa
- jailer, halozat, rootfs/kernel es metadata eleres felteteleinek review-ja
- follow-up infra feladatok kijelolese, ha microVM iranyt valasztunk

### `backend-auth-runtime`

Feladata:

- Spring Security es JWT runtime validacio ellenorzese
- local vs production tokenelfogadas kulonbsegenek feltarasa
- issuer/JWKS/static key hibak szetvalasztasa
- backend auth config es Keycloak realm viselkedes osszehangolasa

### `spring-boot-java-runtime`

Feladata:

- Spring Boot profile, property, actuator, datasource es auto-config problemak vizsgalata
- Spring Boot, Spring Cloud, Maven BOM, Java 21-25 es Docker runtime JDK osszhang ellenorzese
- Java nyelvi/runtime verzio emeles hatasainak review-ja Java 25-ig
- Lombok annotation processing es generalt kod hatasanak ellenorzese Spring/JPA komponensekben

### `react-web-engineering`

Feladata:

- React web komponens, hook, form es routing viselkedes validalasa
- Vite, TypeScript, API base URL, CORS es gateway integracio osszhangban tartasa
- UI/auth hibaknal browser bizonyitek gyujtese kodolvasas helyett
- repo design system es React mintak megorzese explicit valtoztatasi igenyig

### `react-native-mobile-readiness`

Feladata:

- React Native bevezetes elotti web-only es reusable kodhatarok feltarasa
- mobile auth, deep link, API, storage es platform permission feltetelek tisztazasa
- Metro/native module es dependency dontesek explicit dokumentalasa
- mobil scaffold hozzaadasanak elkerulese konkret task nelkul

### `lombok-java-hygiene`

Feladata:

- Lombok Maven/CI annotation processor konfiguracio ellenorzese
- `@Data`, builder, constructor, equals/hashCode es logging annotaciok hatasainak review-ja
- explicit Java kod preferalasa, ha Lombok domain vagy persistence viselkedest takar el
- Lombok kompatibilitas kovetese aktiv Java es Spring/JPA modellek mellett

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

### `jira-management`

Feladata:

- Jira-first backlog, epic, story, bug, task es spike eletciklus kezelese
- Markdown -> Jira migracio tamogatasa external ID alapu idempotens frissitesekkel
- Jira statuszok osszehangolasa branch, PR, review, merge es verifikacios allapottal
- prompt-first job creation, majd a Jira altal generalt issue key tovabbi hasznalata branch, commit es PR szinten

### `docs-sync`

Feladata:

- README, architecture, deployment es authentication dokumentacio frissen tartasa
- architekturadiagram frissitesi kotelezettseg kezelese
- “prepared/scaffolded” jellegu stale allitasok kiszurese
- runtime valtozasok visszairasa a dokumentacioba

### `clean-code`

Feladata:

- egyszeru, olvashato, minimalis kodalak kialakitasa
- felesleges absztrakciok, duplikaciok es rejtett mellekhatasok kiszurese
- Lombok hasznalat csak ott, ahol a boilerplate csokkentese tenyleg olvashatosagot javit
- review elotti kodhigienia es elnevezesi konzisztencia ellenorzese

## Groupok

### `app-surface`

Feladata:

- public UI, browser runtime es auth callback felulet kezelese
- frontend oldali request-trigger logika kezelese
- user oldali regressziok reprodukcioja
- React web es kesobbi React Native/mobile surface dontesek kezelese

### `api-auth`

Feladata:

- backend API, JWT, claim es Keycloak contract kezelese
- `/api/v1/me` es kapcsolodo auth endpointok viselkedese
- frontend/backend auth szerzodes tisztan tartasa
- Spring Boot, Java platform es Lombok hatasok figyelembe vetele backend valtozasoknal

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
- Docker Engine, ephemeral utility kontener es Firecracker/microVM izolacios dontesek kezelese
- Oracle Cloud control-plane, GitHub Actions es GHCR registry bizonyitekok osszekapcsolasa deploy hibaknal

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
| **GitHub Actions / GHCR image hiba** | `delivery-runtime` | `github-actions-ghcr-debug` | `github-actions` $\rightarrow$ `ghcr-registry` $\rightarrow$ `github` |
| **Oracle Cloud runtime / instance principal hiba** | `delivery-runtime` | `oracle-cloud-runtime-debug` | `oracle-cloud` $\rightarrow$ `oci` $\rightarrow$ `vault-secrets` |
| **Docker runtime / utility kontener hiba** | `delivery-runtime` | `container-runtime-debug` | `docker-engine` $\rightarrow$ `docker-compose` $\rightarrow$ `ssh-remote-shell` |
| **AI sandbox / microVM izoláció döntés** | `delivery-runtime` | `microvm-runtime-isolation` | `firecracker` $\rightarrow$ `docker-engine` $\rightarrow$ `ssh-remote-shell` |
| **Jira backlog / story migráció** | `delivery-runtime` | `jira-management` | `jira` $\rightarrow$ `github` $\rightarrow$ `node-workspace` |
| **JWT validációs hiba (Backend)** | `api-auth` | `backend-auth-runtime` | `http-api` $\rightarrow$ `maven-java-deps` $\rightarrow$ `keycloak-admin` |
| **Spring Boot / Java runtime hiba** | `api-auth` | `spring-boot-java-runtime` | `spring-framework` $\rightarrow$ `maven-java-deps` $\rightarrow$ `java-platform` |
| **React web UI hiba** | `app-surface` | `react-web-engineering` | `react-web` $\rightarrow$ `playwright` $\rightarrow$ `http-api` |
| **React Native / mobile tervezés** | `app-surface` | `react-native-mobile-readiness` | `react-native` $\rightarrow$ `react-web` $\rightarrow$ `http-api` |
| **Lombok / annotation processing hiba** | `api-auth` | `lombok-java-hygiene` | `lombok` $\rightarrow$ `maven-java-deps` $\rightarrow$ `java-platform` |
| **Biztonsági audit / Sebezhetőség** | `security-compliance` | `security-audit` | `sast-scanner` $\rightarrow$ `dependency-audit` $\rightarrow$ `secret-scanner` |
| **Prompt minőség / prompt injection** | `knowledge-governance` | `prompt-engineering` | `prompt-library` $\rightarrow$ `context7` $\rightarrow$ `secret-scanner` |
| **Dokumentáció és realidadegyezetlen** | `knowledge-governance` | `docs-sync` | `oci` $\rightarrow$ `github` $\rightarrow$ `diagram-architecture` |

## Ajanlott prioritasi sorrend

### Elso kor

- `playwright`
- `github`
- `github-actions`
- `ghcr-registry`
- `jira`
- `ssh-remote-shell`
- `docker-engine`
- `docker-compose`
- `http-api`
- `react-web`
- `spring-framework`

### Masodik kor

- `keycloak-admin`
- `oracle-cloud`
- `oci`
- `vault-secrets`
- `cloudflare`
- `firecracker`
- `java-platform`
- `lombok`
- `react-native`

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
