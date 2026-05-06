# EPIC-1: Önfejlesztő Autoforge webfelület

## Cél

Az Autoforge első nagy epicje egy olyan belső, autentikált webes felület létrehozása, ahol a felhasználó természetes nyelven megadhat egy fejlesztési kérést. A kérés a meglévő publikus belépési útvonalon keresztül halad:

- React frontend
- Spring Cloud Gateway
- Spring Boot backend
- belső OpenCode runtime
- GitHub branch + PR
- merge után GitHub Actions build/deploy

A végcél egy kontrollált, auditálható, PR-alapú önfejlesztő rendszer. Az AI nem közvetlenül módosítja az éles rendszert, hanem branch-et, commitot és PR-t készít. Az élesítés továbbra is emberi merge után, a meglévő CI/CD folyamaton keresztül történik.

## Tervezett folyamatábra

Forrás: `docs/assets/self-improving-web-flow.mmd`

```mermaid
flowchart TB
  user["Authenticated user<br/>admin/operator"]
  keycloak["Keycloak OIDC<br/>PKCE login<br/>JWT roles"]

  subgraph public_host["Public OCI host - Always Free"]
    caddy["Caddy<br/>HTTPS ingress<br/>oci.prodet.org"]
    web["React frontend<br/>Prompt panel<br/>Live output<br/>Job history"]
    gateway["Spring Cloud Gateway<br/>/api/** routing<br/>SSE pass-through"]
  end

  subgraph private_host["Private OCI host - Always Free"]
    backend["Spring Boot backend<br/>AI job API<br/>AuthZ + audit<br/>Rate limit"]
    sse["SSE event stream<br/>job.created<br/>job.queued<br/>ai.output<br/>tool.output<br/>pr.created"]
    scheduler["Job scheduler<br/>queued<br/>waiting_for_capacity<br/>running<br/>validating<br/>failed/cancelled"]
    queue[("Persistent job store<br/>Autonomous DB Always Free<br/>or SQLite on block volume")]
    audit[("Audit log<br/>user subject<br/>prompt hash<br/>provider/model<br/>branch/commit/PR<br/>checks")]
    orchestrator["OpenCode orchestrator<br/>private network only<br/>no public endpoint"]
    workspace["Isolated workspace<br/>per-job clone/worktree<br/>no .env<br/>no deploy secrets<br/>path allow/deny list"]
    opencode["OpenCode runtime<br/>source edit<br/>test/build commands<br/>patch/diff output"]
    model_proxy["AI provider proxy / router<br/>rate limit<br/>quota/backoff<br/>provider fallback"]
    github_broker["Git/PR broker<br/>minimal GitHub App/token<br/>branch<br/>commit<br/>push<br/>open PR"]
    security["Security gate<br/>secret scan<br/>dependency audit<br/>SAST<br/>forbidden paths"]
    validation["Validation gate<br/>build<br/>test<br/>typecheck<br/>release/version docs"]
    rag[("Future RAG index<br/>allowlisted docs/code<br/>Oracle AI Vector Search<br/>or local embedded store")]
    block_volume[("OCI Block Volume<br/>/mnt/autoforge-workspace<br/>workspace + queue + logs")]
  end

  subgraph providers["External AI providers"]
    openai["OpenAI / ChatGPT API"]
    gemini["Google Gemini API"]
  end

  subgraph github["GitHub"]
    repo["autoforge repository"]
    pr["Pull request<br/>AI-authored<br/>human review"]
    actions["GitHub Actions<br/>Frontend Build<br/>Backend Build<br/>Container Images<br/>Deploy Public/Private"]
    ghcr["GHCR images<br/>web<br/>gateway<br/>backend"]
  end

  subgraph oci_managed["OCI Always Free / managed options"]
    adb["Autonomous Database<br/>job/audit store candidate<br/>future vector search candidate"]
    nosql["OCI NoSQL<br/>regional availability spike"]
    vault["OCI Vault<br/>provider keys<br/>server password<br/>not exposed to AI runtime"]
    monitoring["OCI Monitoring/Logging<br/>free-tier check before use"]
  end

  subgraph hardening["Mandatory isolation controls"]
    no_secrets["No secrets inside AI workspace<br/>no GitHub token<br/>no OCI CLI creds<br/>no SSH keys"]
    no_metadata["Block metadata endpoint<br/>169.254.169.254"]
    no_docker["No Docker socket<br/>no privileged container<br/>non-root runtime"]
    allow_commands["Command allowlist<br/>build/test/lint/typecheck/git diff"]
  end

  user -->|Sign in| keycloak
  keycloak -->|JWT roles| web
  user -->|Prompt: what to build| web
  web -->|POST /api/v1/ai/jobs<br/>Bearer token| caddy
  caddy --> gateway
  gateway --> backend
  backend -->|create job ID| queue
  backend --> audit
  backend --> scheduler
  backend -->|SSE /api/v1/ai/jobs/{id}/events| sse
  sse -->|live progress| gateway
  gateway --> caddy
  caddy --> web
  web -->|append-only job log| user

  scheduler -->|capacity available| orchestrator
  scheduler -->|rate limit / quota| queue
  queue -->|not_before retry| scheduler
  scheduler -->|provider fallback decision| model_proxy

  orchestrator --> workspace
  workspace --> opencode
  opencode -->|needs model call| model_proxy
  model_proxy -->|primary/fallback| openai
  model_proxy -->|primary/fallback| gemini
  vault -.->|provider keys only to proxy/deploy runtime| model_proxy

  rag -.->|future contextual retrieval| orchestrator
  block_volume --> workspace
  block_volume --> queue
  block_volume --> audit
  adb -.-> queue
  adb -.-> audit
  adb -.-> rag
  nosql -.-> queue

  opencode -->|diff/patch output| security
  security --> validation
  validation -->|approved artifacts| github_broker
  github_broker -->|branch + commit + push| repo
  repo --> pr
  pr -->|human review + merge| actions
  actions --> ghcr
  actions -->|deploy public host| caddy
  actions -->|deploy private host| backend

  hardening -.-> orchestrator
  hardening -.-> workspace
  hardening -.-> opencode
  no_secrets -.-> workspace
  no_metadata -.-> opencode
  no_docker -.-> opencode
  allow_commands -.-> opencode

  monitoring -.-> backend
  monitoring -.-> scheduler
  monitoring -.-> actions
```

## Első verziós scope

Az első implementáció célja nem egy teljes autonóm fejlesztő platform, hanem egy biztonságos end-to-end alapfolyamat:

- A frontend authenticated workspace részében legyen egy input mező vagy prompt panel.
- A felhasználó be tudja írni, mit szeretne megvalósítani.
- A kérés a gatewayen keresztül jusson el a backendhez.
- A backend hozzon létre egy AI jobot és adjon vissza egy job ID-t.
- A backend továbbítsa a feladatot a private hoston futó OpenCode orchestration rétegnek.
- Az OpenCode egy izolált workspace-ben hozzáférjen a teljes forráskódhoz.
- Az AI dolgozzon a feladaton, módosítson fájlokat, futtasson releváns ellenőrzéseket.
- A rendszer készítsen branch-et, commitot és PR-t.
- A frontend folyamatosan lássa a folyamat állapotát és kimenetét.
- Merge után a meglévő GitHub Actions workflow-k automatikusan frissítsék a rendszert.

## Nem cél az első verzióban

- Nincs automatikus merge.
- Nincs automatikus production módosítás emberi jóváhagyás nélkül.
- Nincs közvetlen secret-hozzáférés az AI runtime számára.
- Nincs publikus OpenCode endpoint.
- Nincs fizetős OCI szolgáltatás bevezetése.
- Nincs multi-user projektmenedzsment vagy komplex ticketing rendszer.
- Nincs teljes agent marketplace vagy plugin rendszer.

## Fő felhasználói folyamat

1. A felhasználó bejelentkezik Keycloakkal.
2. A frontend betölti a protected workspace-et.
3. A felhasználó beír egy fejlesztési utasítást.
4. A frontend elküldi a kérést a backendnek `POST /api/v1/ai/jobs` jellegű endpointon.
5. A backend validálja a jogosultságot, létrehoz egy job rekordot, és queue-ba teszi.
6. A frontend feliratkozik a job eseményeire `GET /api/v1/ai/jobs/{jobId}/events` jellegű SSE endpointon.
7. A worker/orchestrator felveszi a jobot, előkészít egy izolált git workspace-et.
8. Az OpenCode végrehajtja a feladatot a workspace-ben.
9. A rendszer folyamatos logot, státuszt és részlépéseket streamel vissza a frontendnek.
10. Sikeres futás után a git/PR broker branch-et, commitot és PR-t készít.
11. A frontend megjeleníti a PR számát, státuszát és a további emberi teendőt.
12. A felhasználó review-zza és merge-eli a PR-t GitHubon.
13. A meglévő GitHub Actions workflow-k lefutnak, image-et építenek és deployolnak.

## Javasolt architektúra

### Frontend

- Új authenticated workspace komponens: AI prompt panel.
- Input mező vagy több soros textarea fejlesztési utasításhoz.
- Submit gomb, állapotjelző és élő output panel.
- Job history vagy legalább aktuális job állapot.
- Streaming megjelenítés időrendben: queued, running, tool output, validation, PR created, failed.
- Cancel gomb későbbi fázisban.

### Gateway

- A meglévő `/api/**` route marad az egyetlen publikus API belépési pont.
- A streaming endpointnak támogatnia kell hosszabb ideig nyitott HTTP kapcsolatot.
- Első verzióban Server-Sent Events javasolt WebSocket helyett, mert egyszerűbb, HTTP-kompatibilis és könnyebben proxyzható.

### Backend

- Új AI job API-k:
- `POST /api/v1/ai/jobs`: új feladat létrehozása.
- `GET /api/v1/ai/jobs/{jobId}`: státusz lekérdezése.
- `GET /api/v1/ai/jobs/{jobId}/events`: SSE stream.
- `POST /api/v1/ai/jobs/{jobId}/cancel`: későbbi cancel endpoint.
- Jogosultságkezelés: csak authenticated user, első körben lehetőleg admin/operator szerepkör.
- Audit mezők: user subject, username, prompt hash, létrehozás ideje, státusz, PR szám.
- Rate limit és max futási idő felhasználónként.
- Queue és job state kezelés.
- OpenCode orchestration kliens a private Docker networkön.

### Queue és ütemezés

Az ütemezésnek akkor is működnie kell, ha az AI provider átmenetileg nem elérhető, rate limitbe fut, vagy nincs szabad kapacitás.

Javasolt első verziós megoldás:

- Backend által kezelt persisted queue.
- Tárolás kezdetben SQLite vagy fájl-alapú job store a private hoston lévő `/mnt/autoforge-workspace` alatt.
- Nincs fizetős OCI Queue használat, amíg nem bizonyított, hogy Always Free keretben marad.
- Job állapotok: `queued`, `waiting_for_capacity`, `running`, `streaming`, `validating`, `pr_opened`, `failed`, `cancelled`.
- Exponenciális backoff provider rate limit esetén.
- `not_before` idő mező, hogy egy job későbbi időpontban fusson tovább.
- Provider fallback: ha az elsődleges modell nem elérhető, próbálja a másodlagos providert.
- A fallback csak addig aktív, amíg két előfizetés rendelkezésre áll; ezt konfigurációval kell kapcsolni, nem kódban fixen.

### OpenCode runtime

- Az OpenCode private Docker networkön fusson, publikus port nélkül.
- A frontend soha nem beszél közvetlenül az OpenCode konténerrel.
- A backend/orchestrator közvetíti a feladatot.
- Az OpenCode izolált workspace-ben dolgozzon, nem a deploy könyvtárban.
- Minden job külön worktree-t vagy klónt kapjon.
- A workspace törölhető és újraépíthető legyen.
- A futás közbeni outputot az orchestrator továbbítsa a backend SSE stream felé.

### Git és PR broker

Az AI nem kaphat GitHub tokent, deploy kulcsot vagy más secretet.

Javasolt bontás:

- OpenCode: csak a forráskódon dolgozik és patch/diff/commit intentet állít elő.
- Git broker: külön backend/worker komponens, amely rendelkezik minimális GitHub App vagy machine-user jogosultsággal.
- Git broker feladata: branch létrehozása, commit, push, PR nyitás.
- Git broker csak allowlistelt repohoz férhet hozzá.
- PR cím és body a meglévő `AGENT.md` szabályokat kövesse.
- PR előtt kötelező security és build verifikáció.

## Streaming kimenet

Első verzióban SSE javasolt.

Stream eseménytípusok:

- `job.created`
- `job.queued`
- `job.waiting_for_capacity`
- `job.started`
- `ai.output`
- `tool.output`
- `file.changed`
- `validation.started`
- `validation.output`
- `pr.created`
- `job.completed`
- `job.failed`

Frontend oldalon az output legyen append-only napló, időbélyeggel és státuszcímkével. A felhasználó lássa, hogy a rendszer éppen várakozik, dolgozik, validál, PR-t nyit vagy hibára futott.

## Biztonsági alapelvek

Ez az epic csak akkor élesíthető, ha az alábbi korlátok teljesülnek.

### Secret isolation

- Az AI runtime nem férhet hozzá semmilyen deploy, OCI, GitHub, SSH, registry vagy Keycloak secrethez.
- Az AI runtime ne kapjon GitHub tokent.
- Az AI runtime ne kapjon OCI CLI credentialt.
- Az AI runtime ne kapjon SSH kulcsot.
- Az AI runtime ne lássa a deploy `.env`, `.deploy.env`, Vault OCID vagy runtime secret fájlokat.
- Az OpenCode konténerből ki kell venni minden olyan secretet, ami nem közvetlenül szükséges a modellhíváshoz.

### AI provider kulcsok kezelése

A hosszú távú cél az, hogy az OpenCode se kapjon nyers OpenAI/Gemini API kulcsot.

Javasolt minta:

- Külön belső AI provider proxy vagy model router kezeli a provider kulcsokat.
- Az OpenCode csak a belső proxyhoz beszél.
- A proxy injektálja a provider API kulcsot szerveroldalon.
- A proxy kezeli a rate limitet, fallbacket és provider választást.
- A proxy nem rendelkezik GitHub, OCI vagy deploy jogosultsággal.

Megjegyzés: a jelenlegi OpenCode deploy támogat provider API kulcsokat OCI Vaultból. Az önfejlesztő rendszer élesítése előtt ezt át kell alakítani úgy, hogy a belső AI runtime ne kapjon széles körű secret hozzáférést.

### Workspace izoláció

- Minden job külön workspace-ben fusson.
- A workspace ne mountolja a host teljes `/opt/autoforge` vagy `/home` könyvtárát.
- Csak a szükséges repo checkout legyen elérhető.
- A workspace ne tartalmazzon `.env`, kulcsfájl vagy runtime secret állományt.
- A job végén a workspace archiválható vagy törölhető legyen policy alapján.

### Hálózati korlátozások

- Az OpenCode ne legyen publikus hálózatról elérhető.
- A konténer ne érje el az OCI instance metadata endpointot.
- Blokkolni kell például a `169.254.169.254` metadata címet az AI runtime felől.
- Egress lehetőleg csak a szükséges AI provider/proxy és GitHub irányába történjen.
- Ha GitHub műveletet külön broker végez, akkor az OpenCode-nak GitHub egress sem feltétlenül kell.

### Parancsfuttatás korlátai

- Allowlistelt parancsok: build, test, lint, typecheck, git diff.
- Tiltott műveletek: secret olvasás, SSH, OCI CLI, docker socket, host mount feltérképezés, force push, destructive git parancsok.
- Nincs Docker socket mount az AI konténerbe.
- Nincs privileged konténer.
- Lehetőleg non-root userrel fusson.

### PR előtti kötelező ellenőrzések

- `security-audit` skill: secret scan, dependency audit, SAST jellegű ellenőrzés.
- `git-management` skill: branch, commit és PR szabályok.
- `release-versioning` skill: task ID, verzió, release manifest konzisztencia.
- Releváns build/test parancsok a változás típusától függően.
- A PR body tartalmazza, hogy AI által készített változásról van szó.

## Jogosultsági modell

Első verzióban javasolt:

- Csak authenticated user tud jobot létrehozni.
- Csak `admin` vagy `operator` szerepkör indíthat önfejlesztő jobot.
- Minden job auditált felhasználóhoz kötött.
- Egy felhasználónak egyszerre limitált számú futó jobja lehet.
- Admin látja az összes jobot, normál jogosultságú user csak a sajátját.

## OCI Always Free követelmény

Az epic megvalósításához csak olyan OCI erőforrás használható, amely nem igényel előfizetésváltást és nem okoz folyamatos fizetős költséget.

Engedélyezett irányok:

- meglévő Always Free compute instance-ek használata;
- meglévő private host és public host használata;
- meglévő 100 GB Block Volume használata a 200 GB Always Free block storage kereten belül;
- konténeres komponensek a meglévő Docker Compose stackben;
- lokális SQLite/fájl-alapú queue a meglévő volume-on;
- GitHub Actions a jelenlegi használati keretek figyelembevételével.

Kerülendő vagy külön ellenőrzendő:

- OCI Queue, Streaming, Functions, API Gateway, Load Balancer vagy fizetős managed szolgáltatás;
- új compute instance;
- extra block volume vagy storage növelés;
- fizetős observability stack;
- provider API használat kontroll nélküli tokenlimittel.

Minden új OCI erőforrás előtt kötelező ellenőrizni és dokumentálni, hogy Always Free kereten belül marad.

## Adatbázis és tartós állapot

Az első verzióban szükség lesz tartós állapotra az AI jobokhoz, queue-hoz, audit loghoz, PR metadata adatokhoz és később a RAG indexekhez. A választott adatbázis nem férhet hozzá secret értékekhez, és nem lehet fizetős OCI szolgáltatás.

Javasolt elsődleges irány:

- OCI Autonomous Database Always Free, ha a tenancy home régiójában elérhető és a quota engedi.
- Első használati cél: job store, audit log, queue metadata, PR metadata, provider retry állapot.
- Előny: managed adatbázis, backup/üzemeltetés egyszerűbb, később Oracle AI Vector Search irányba bővíthető.
- Kockázat: csatlakozási wallet/credential kezelést nagyon szigorúan kell izolálni, az AI runtime nem kaphat közvetlen DB credentialt.

Alternatívák Always Free kereten belül:

- Lokális SQLite a private hoston lévő `/mnt/autoforge-workspace` volume-on.
- Oracle NoSQL Database Always Free, ha az adott régióban ténylegesen elérhető és a quota megfelel.
- Saját konténeres adatbázis csak akkor, ha belefér a private host RAM/CPU keretébe és nem veszélyezteti a backend/OpenCode futását.

Első verziós döntési javaslat:

- Ha Autonomous Database Always Free elérhető: használjuk elsődleges tartós job/audit store-ként.
- Ha nem elérhető vagy túl sok üzemeltetési/credential komplexitást okoz: kezdjünk SQLite-tal a private block volume-on.
- NoSQL-t csak akkor válasszunk, ha a job/event modellhez ténylegesen jobb, mint a relációs séma.

## RAG és vektoros keresés előkészítése

Későbbi fázisban szükség lehet RAG adatbázisra, hogy az AI jobok a repo, dokumentáció, release manifestek, architektúra és korábbi döntések alapján pontosabb kontextust kapjanak.

Lehetséges Always Free-kompatibilis irányok:

- Oracle Autonomous Database + Oracle AI Vector Search, ha az elérhető Always Free Autonomous Database verzió és kompatibilitás támogatja a `VECTOR` típust és vektoros indexelést.
- Lokális fájl-alapú vagy embedded vector store a private block volume-on, ha nem akarunk managed DB credentialt adni a rendszernek.
- Saját konténeres vector DB csak külön erőforrásmérés után, mert a private host CPU/RAM szűkös.

RAG biztonsági alapelvek:

- Secret, `.env`, deploy credential, Vault OCID és runtime config nem kerülhet embeddingbe.
- A RAG index csak repo dokumentációt, forráskódot és explicit allowlistelt fájlokat dolgozhat fel.
- Path denylist kötelező: `.env*`, secret fájlok, SSH kulcsok, deploy runtime fájlok, személyes vagy gépspecifikus állományok.
- Az embedding provider költségét rate limitelni kell.
- A RAG frissítés legyen batch/ütemezett, ne minden requestnél teljes újraindexelés.

RAG első task csak spike legyen:

- Autonomous Database vector capability ellenőrzése a tényleges OCI tenancyben.
- Always Free quota ellenőrzése.
- Minta séma és 100-500 dokumentumos próbaindex.
- Költség és memóriahasználat mérése.
- Döntés: Autonomous DB vector store, lokális embedded index, vagy későbbre halasztás.

## GraalVM és Spring Native erőforrás-optimalizálás

A private/public OCI erőforrások szűkösek, ezért érdemes külön spike-ban megvizsgálni, hogy a Spring Boot backend és a Spring Cloud Gateway profitál-e GraalVM Native Image használatából.

Várható előnyök:

- alacsonyabb memóriahasználat;
- gyorsabb startup;
- kisebb runtime overhead;
- jobb illeszkedés Always Free compute korlátokhoz.

Várható kockázatok:

- hosszabb és nehezebb CI build;
- nagyobb Docker image build komplexitás;
- reflection/resource hint problémák Spring Security, JWT, Gateway vagy Actuator körül;
- nehezebb diagnosztika production hibánál;
- Spring Cloud Gateway native kompatibilitását külön ellenőrizni kell.

Javasolt döntési folyamat:

1. Mérjük le a jelenlegi JVM alapú backend és gateway memóriahasználatát, startup idejét és image méretét.
2. Készítsünk külön GraalVM Native spike branch-et csak backenddel.
3. Ha backendnél érdemi nyereség látszik, vizsgáljuk meg a gatewayt is.
4. Futtassuk a meglévő backend/gateway teszteket és egy production-szerű smoke tesztet.
5. Csak akkor vezessük be, ha a memória/startup nyereség meghaladja a build és üzemeltetési komplexitás költségét.

Első döntési javaslat:

- Ne vezessük be azonnal alapértelmezettként.
- Legyen külön `GraalVM resource spike` task.
- Addig maradjon a jelenlegi JVM image, `JAVA_TOOL_OPTIONS` memóriahangolással és célzott konténer limitekkel.

## Kapacitás és provider fallback

A rendszernek akkor is fogadnia kell feladatot, ha az AI provider pillanatnyilag nem tudja kiszolgálni.

Követelmények:

- A frontend submit után azonnal job ID-t kapjon.
- Ha nincs kapacitás, a job `waiting_for_capacity` állapotba kerüljön.
- A job automatikusan újrapróbálkozzon később.
- Provider rate limit vagy quota hiba esetén legyen backoff.
- Ha több provider aktív, a rendszer próbálja a másodlagos providert.
- A provider fallback legyen konfigurálható.
- Ha később csak egy előfizetés marad, a queue továbbra is működjön, csak fallback nélkül.
- A felhasználó lássa, hogy a job várakozik és várhatóan később indul.

## Audit és traceability

Minden AI jobhoz legyen rögzítve:

- job ID;
- létrehozó user subject;
- prompt rövidített vagy hash-elt formája;
- teljes prompt tárolási policy szerint;
- kiválasztott provider/model;
- létrehozás, indulás, befejezés ideje;
- branch név;
- commit SHA;
- PR szám;
- lefuttatott ellenőrzések;
- hiba oka, ha sikertelen.

Az audit log ne tartalmazzon secretet vagy nyers credentialt.

## Későbbi task bontási javaslat

Ez csak előzetes bontás, a konkrét taskokat később kell létrehozni.

1. AI job API backendben.
2. SSE streaming backend és frontend között.
3. Frontend prompt panel és live output UI.
4. Persisted queue és job state machine.
5. OpenCode orchestration kliens.
6. Izolált git workspace kezelés.
7. Git/PR broker bevezetése secret izolációval.
8. AI provider proxy/model router provider kulcsok elrejtésére.
9. Provider fallback és capacity scheduling.
10. Security sandbox hardening.
11. PR előtti build/security verifikációs pipeline.
12. Audit log és job history.
13. Autonomous Database / SQLite job store spike.
14. RAG és vektoros keresés spike.
15. GraalVM/Spring Native erőforrás spike.
16. Deployment/architecture dokumentáció és diagram frissítése.

## Nyitott kérdések

- Pontosan melyik Keycloak role indíthat AI jobot: `admin`, `operator`, vagy külön `autoforge-ai-runner` role?
- A teljes promptot tároljuk-e audit célból, vagy csak hash + rövid összefoglaló legyen?
- Az első verzióban elég-e SSE, vagy kell WebSocket?
- A Git broker GitHub App legyen vagy machine user PAT minimális jogosultsággal?
- A PR-t mindig az AI nyissa, vagy legyen előtte emberi jóváhagyási pont a branch létrehozása után?
- A queue perzisztenciája SQLite legyen-e a private volume-on?
- Autonomous Database Always Free legyen-e az elsődleges job/audit store, vagy induljunk SQLite-tal?
- A későbbi RAG index Autonomous Database vector searchre épüljön, vagy lokális embedded vector store-ra?
- A tényleges OCI home régióban elérhető-e az Always Free Autonomous Database és/vagy NoSQL opció?
- A GraalVM Native Image nyeresége elég nagy-e a Spring Boot backendnél és gatewaynél?
- A provider fallback pontos sorrendje OpenAI -> Gemini vagy Gemini -> OpenAI legyen?
- Az AI által módosítható fájlokra kell-e path allowlist/denylist az első verzióban?

## Elsődleges kockázatok

- Secret exfiltration, ha az AI runtime túl sok környezeti változót vagy host mountot kap.
- Instance metadata elérés, ha nincs hálózati tiltás.
- Végtelen vagy túl drága provider tokenhasználat.
- Rossz PR vagy workflow módosítás, amely deploy hibát okoz.
- Hosszú futású jobok frontend timeouttal vagy gateway proxy timeouttal.
- Queue elvesztése, ha nem perzisztens tárolón van.
- GitHub token túl széles jogosultsággal.
- Managed DB credential kiszivárgása, ha az AI runtime közvetlen adatbázis credentialt kap.
- RAG indexbe kerülő érzékeny adat, ha nincs szigorú path allowlist/denylist.
- GraalVM native image kompatibilitási hiba, amely csak production-szerű futásnál jelentkezik.

## Elfogadási feltételek az epic lezárásához

- A felhasználó authenticated UI-ból AI jobot tud indítani.
- A job gatewayen és backend API-n keresztül jut a private OpenCode runtime-ig.
- A frontend élőben látja a job státuszát és kimenetét.
- A job izolált workspace-ben dolgozik.
- Az AI nem fér hozzá deploy, OCI, GitHub, SSH, registry vagy Keycloak secrethez.
- A rendszer PR-t nyit, nem közvetlenül deployol.
- PR merge után a meglévő GitHub Actions workflow-k frissítik a rendszert.
- Ha nincs AI kapacitás, a job queue-ban marad és később újrapróbálkozik.
- Provider fallback működik, ha több provider aktív.
- Minden használt OCI erőforrás Always Free kompatibilis.
- A folyamat auditálható job ID, user, branch, commit és PR alapján.
