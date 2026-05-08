# EPIC-3: Jira alapú epic és story kezelés

## Cél

Az Autoforge jelenlegi Markdown alapú task/story nyilvántartását át kell vezetni Jira-ba, és a továbbiakban minden epicet, storyt, bugot, spike-ot és delivery státuszt Jira-ban kell elsődlegesen kezelni.

A repo-ban maradhat minimális, auditálható dokumentáció, de a munka operatív forrása Jira lesz.

## Motiváció

A jelenlegi működésben a feladatok a repo-ban, `docs/tasks/*.md` fájlokban, a release mapping pedig `docs/releases.md` alatt él. Ez jól működött induláskor, de a következő fázisban több párhuzamos epic, AI által készített PR, queue-zott munka, security audit és deployment flow lesz. Ehhez erősebb issue lifecycle, riportolás, keresés, státuszkezelés és backlog grooming kell.

Jira bevezetésével:

- minden epic/story/bug egy közös backlogban kezelhető;
- státuszok, assignee-k, priority-k és review állapotok konzisztensen követhetők;
- PR-ok, branch-ek és release verziók Jira issue-khoz köthetők;
- az AI ügynökök egyértelmű source-of-truth alapján dolgozhatnak;
- később az önfejlesztő webfelület közvetlenül Jira issue-ból vagy Jira issue-ba tud dolgozni.

## Scope

Részletes Jira-ready story bontás: `docs/epics/EPIC-3-jira-migration-stories.md`.

Fontos végállapot: sikeres Jira migráció és validált mapping után a story/task szintű Markdown fájlokat törölni kell a gitből. A repo nem marad párhuzamos story-kezelési hely; Jira lesz az egyetlen operatív backlog és story source of truth.

### Egyszeri migráció

Át kell emelni Jira-ba:

- minden `docs/tasks/AUTO-*.md` taskot;
- minden `docs/tasks/BUG-*.md` bugot;
- minden meglévő epic dokumentumot `docs/epics/` alól;
- a `docs/releases.md` task-to-version mappinget;
- PR számokat és branch neveket, ahol rendelkezésre állnak;
- acceptance criteria és lépésnapló releváns részeit;
- eredmény és lezárási információkat.

### Jövőbeli működés

Az import után:

- új epic csak Jira-ban jöhet létre;
- új story/bug/spike csak Jira-ban jöhet létre;
- branch és PR név Jira issue key alapján készüljön;
- commit üzenet Jira issue key-vel kezdődjön;
- PR title Jira issue key-vel kezdődjön;
- release dokumentáció Jira issue-kból származó mapping alapján frissüljön;
- repo-ban csak szükséges technikai ADR, runbook, architecture és release dokumentáció maradjon.

## Nem cél az első verzióban

- Nem cél a meglévő GitHub PR-ok automatikus módosítása.
- Nem cél a Jira workflow végleges enterprise szintű testreszabása.
- Nem cél a bidirectional full sync minden mezőre.
- Nem cél Jira token vagy bármilyen secret repo-ba írása.
- Nem cél fizetős Jira Marketplace app bevezetése, ha API-val megoldható.

## Jira projekt modell

Javasolt issue típusok:

- `Epic`: nagyobb üzleti vagy platform capability.
- `Story`: implementálható feature vagy dokumentációs/infra scope.
- `Bug`: regresszió, hibás shipped behavior, failing workflow, broken deploy.
- `Task`: technikai housekeeping, migráció, processz frissítés.
- `Spike`: kutatási vagy mérési feladat, ahol a kimenet döntés vagy javaslat.
- `Sub-task`: story-n belüli kisebb végrehajtási lépés, ha indokolt.

## Jira hierarchy

Javasolt mapping:

- `docs/epics/EPIC-*.md` -> Jira `Epic`.
- `AUTO-*` -> Jira `Story` vagy `Task`, a tartalomtól függően.
- `BUG-*` -> Jira `Bug`.
- Kutatási jellegű `AUTO-*` -> Jira `Spike`.
- Egy epic alá tartozó későbbi implementációk -> Jira `Story` az adott Epic Link alatt.

Kiemelt epic mapping:

- `EPIC-1`: Önfejlesztő Autoforge webfelület.
- `EPIC-2`: GraalVM / Spring Native erőforrás-optimalizálás, külön epicben kezelendő.
- `EPIC-3`: Jira alapú epic és story kezelés.

## Státusz workflow

Javasolt Jira státuszok:

- `Backlog`: rögzítve, még nem priorizált.
- `Ready`: tisztázott scope, indítható.
- `In Progress`: aktív branch vagy aktív munka.
- `In Review`: PR nyitva, review/check folyamatban.
- `Blocked`: külső döntés, secret, infrastruktúra vagy hozzáférés hiányzik.
- `Done`: merged, dokumentált, verifikált.
- `Cancelled`: nem valósul meg vagy kiváltotta másik issue.

Spike és döntési issue szabály:

- Spike vagy döntési issue nem kerülhet `Done` státuszba pusztán default javaslat, agent preferencia vagy feltételezett best practice alapján.
- Spike csak explicit user döntés, rögzített kutatási eredmény vagy közvetlen lezárási kérés után zárható.
- Ha csak ajánlott default van, de nincs megerősített döntés, kommentben `Recommended, not decided` jelölést kell használni, és az issue maradjon nyitva.
- Hibás lezárás esetén az issue-t vissza kell nyitni, korrekciós kommenttel és user felé történő egyértelmű jelzéssel.

## Markdown -> Jira státuszleképezés

Import során a jelenlegi Markdown adatokból kell best-effort státuszt képezni.

Javasolt szabályok:

- Ha a task `Statusz` mezője `completed`, akkor Jira státusz: `Done`.
- Ha van PR szám és a PR merged, akkor Jira státusz: `Done`.
- Ha van PR szám, de nincs merged állapot, akkor Jira státusz: `In Review`.
- Ha van branch és nincs PR, akkor Jira státusz: `In Progress`.
- Ha `Statusz` mező `in_progress`, de nincs branch/PR adat, akkor Jira státusz: `In Progress`.
- Ha a taskban `Blocked`, `blocked`, `függőben` vagy hasonló jelzés van, akkor Jira státusz: `Blocked`.
- Ha nincs egyértelmű jel, akkor Jira státusz: `Backlog` vagy `Ready`, emberi ellenőrzéssel.

## Jira mezők

Minimum mezők importnál:

- Summary: task/epic címe.
- Description: Markdown tartalom normalizált formában.
- Issue Type: Epic/Story/Bug/Task/Spike.
- Status: mapping alapján.
- Fix Version: `docs/releases.md` alapján.
- Labels: `autoforge`, `migration`, `auto`, `bug`, `docs`, `infra`, `security`, `deploy`, ahol releváns.
- External ID: eredeti azonosító, például `AUTO-28`, `BUG-17`, `EPIC-1`.
- Branch: eredeti branch név, ha ismert.
- PR: belső PR szám, ha ismert.
- Acceptance Criteria: külön blokkban.
- Verification / Lepesnaplo: külön blokkban vagy kommentként.

## Egyedi Jira mezők javaslat

Ha a Jira projektben lehet custom fieldet létrehozni, ezek hasznosak:

- `Autoforge ID`: eredeti repo task ID, például `AUTO-28`.
- `Target Version`: projekt verzió, például `0.1.19`.
- `Branch`: git branch név.
- `PR`: GitHub PR szám.
- `Source File`: eredeti Markdown fájl útvonala.
- `AI Generated`: boolean, ha AI készítette vagy AI jelentősen közreműködött.
- `Security Sensitive`: boolean, ha auth, secret, deploy vagy infra jogosultság érintett.

Ha custom field nincs első körben, ezek maradjanak a descriptionben strukturált Markdown blokkokként.

## Import működés

Javasolt import stratégia:

1. Jira projekt és workflow véglegesítése.
2. Jira token biztonságos tárolása OCI Vaultban vagy lokális környezeti változóban, de soha nem gitben.
3. Markdown task parser készítése.
4. `docs/releases.md` parser készítése version mappinghez.
5. GitHub PR státusz lekérdezése `gh`/GitHub API segítségével.
6. Dry-run import JSON generálása.
7. Emberi review a generált import tervre.
8. Jira issue-k létrehozása idempotens módon.
9. Létrejött Jira issue key-k visszamentése mapping fájlba.
10. AGENT.md és folyamatdokumentáció frissítése Jira-first működésre.

## Idempotencia és duplikációvédelem

Az importnak újrafuttathatónak kell lennie.

Kötelező szabályok:

- Minden importált issue kapjon `Autoforge ID` vagy descriptionben `External ID` mezőt.
- Új issue létrehozása előtt keresni kell meglévő Jira issue-t ugyanazzal az external ID-val.
- Ha létezik, frissítés történjen, ne duplikált issue létrehozás.
- Import után készüljön `docs/jira-mapping.md` vagy gépi `docs/jira-mapping.json` mapping.
- A mapping ne tartalmazzon secretet vagy tokent.

## Biztonsági követelmények

Jira token kezelése:

- Jira token nem kerülhet gitbe.
- Jira token nem kerülhet dokumentációba.
- Jira token nem kerülhet task fájlba.
- Jira token nem kerülhet shell historyba szándékosan.
- Jira token csak környezeti változóként vagy Vaultból olvasva használható.
- Logokban a token értékét maszkolni kell.

Javasolt változók:

- `JIRA_BASE_URL`
- `JIRA_EMAIL` vagy `JIRA_USERNAME`
- `JIRA_API_TOKEN`
- `JIRA_PROJECT_KEY`

Ha CI-ből fut az import:

- GitHub Secrets csak Jira token referenciát vagy közvetlen tokent tárolhat, de repo fájlba nem kerülhet.
- Preferált hosszabb távon: OCI Vault secret OCID -> deploy/runtime olvasás.

## Jira és AI ügynökök kapcsolata

Az AI ügynökök jövőbeli működése:

- Munka megkezdése előtt Jira issue-t kell olvasni.
- Branch név Jira issue key alapján készüljön.
- Commit Jira issue key-vel kezdődjön.
- PR Jira issue key-vel kezdődjön.
- PR body linkelje a Jira issue-t.
- Munka végén az AI kommentelje Jira-ba a változást, verifikációt és PR-t.
- A Jira státuszt csak kontrollált szabályok alapján módosítsa.

## Új branch/commit/PR naming Jira után

Javasolt forma:

- Branch: `feature/<JIRA-KEY>-short-description` vagy `bug/<JIRA-KEY>-short-description`.
- Commit: `[<JIRA-KEY>] Short description`.
- PR title: `[<JIRA-KEY>] Short description`.
- PR body: tartalmazza a Jira issue linket, verziót, summaryt és verification részt.

A régi `AUTO-*` és `BUG-*` azonosítók import után historical reference-ként maradnak meg.

## Repo dokumentáció jövőbeli szerepe

Jira-first működés után a repo dokumentáció szerepe:

- `docs/architecture.md`: aktuális architektúra.
- `docs/deployment.md`: deploy és runtime runbook.
- `docs/authentication.md`: auth folyamat.
- `docs/ai-tooling.md`: AI skillek és MCP-k.
- `docs/releases.md`: release manifest, de Jira issue key alapú.
- `docs/epics/*.md`: opcionális high-level ADR/vision dokumentum, nem napi státuszforrás; story bontásokat Jira migráció után törölni kell.
- `docs/tasks/*.md`: sikeres migráció és validált mapping után törlendő, nem read-only archive.
- `docs/jira-mapping.*`: megmaradó audit mapping a régi ID-k és Jira issue key-k között.

## Migrációs lépések taskokra bontva

Későbbi task bontási javaslat:

Megjegyzés: a részletes, Jira-ba emelhető story lista külön dokumentumban van: `docs/epics/EPIC-3-jira-migration-stories.md`.

1. Jira projekt, issue type-ok és workflow véglegesítése.
2. Jira access token biztonságos tárolási modell kialakítása.
3. Markdown task/release parser készítése.
4. Jira import dry-run JSON generátor.
5. PR státusz enrichment GitHub API-ból.
6. Emberi review az import tervre.
7. Jira issue import futtatása.
8. Jira mapping fájl generálása.
9. AGENT.md frissítése Jira-first szabályokra.
10. docs/tasks és story Markdown fájlok törlése a sikeres Jira migráció után.
11. CI/PR template frissítése Jira issue linkeléssel.
12. AI tooling bővítés `jira` MCP-vel és `jira-management` skillel.

## Jira MCP és skill igény

Az AI tooling stack Jira támogatással bővül, hogy a migráció és a későbbi Jira-first működés explicit ügynöki képesség legyen.

Javasolt MCP:

- `jira`: Jira issue olvasás, létrehozás, frissítés, kommentelés, státuszváltás, JQL keresés.

Javasolt skill:

- `jira-management`: backlog grooming, issue import, státusz szinkron, PR/Jira linkelés, release mapping.

Manifestek és dokumentáció:

- `ops/ai/mcps.yaml`: `jira` MCP.
- `ops/ai/skills.yaml`: `jira-management` skill.
- `ops/ai/groups.yaml`: delivery és knowledge capability group kapcsolódások.
- `ops/mcp/jira-local.sh`: helyi, gitignored launch point a Jira MCP stdio futtatásához.
- `docs/ai-tooling.md`: használati sorrend és troubleshooting mátrix.

## Acceptance criteria az epic lezárásához

- Minden meglévő `AUTO-*` task Jira issue-ként szerepel.
- Minden meglévő `BUG-*` bug Jira issue-ként szerepel.
- Minden meglévő epic Jira Epic-ként szerepel.
- Minden issue megfelelő státuszban van a mapping szabályok szerint.
- Minden issue tartalmazza az eredeti Autoforge ID-t.
- A release verzió mapping Jira-ban visszakereshető.
- Az import idempotens és újrafuttatható.
- A Jira token nem került gitbe vagy dokumentációba.
- Az AGENT.md Jira-first működést ír elő.
- Az AI tooling stack tartalmaz Jira MCP-t és Jira management skillt.

## Nyitott kérdések

- Mi legyen a Jira projekt kulcsa?
- Milyen Jira workflow státuszok vannak már létrehozva?
- Használhatunk custom fieldeket vagy csak description alapú strukturált mezőket?
- Pontosan mely dokumentumok maradjanak meg high-level ADR/architecture célra, ha a story/task fájlokat töröljük?
- A Jira import fusson lokálisan egyszeri admin műveletként vagy GitHub Actions manual workflow-ból?
- A Jira issue-kbe visszakerüljön-e a teljes lépésnapló, vagy csak summary + link a repo fájlra?
