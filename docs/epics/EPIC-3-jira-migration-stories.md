# EPIC-3 Story Breakdown: Jira alapú epic és story kezelés

Ez a dokumentum az EPIC-3 Jira-ready story bontása. A storyk célja, hogy a Markdown-alapú Autoforge task/bug/epic nyilvántartás biztonságosan, idempotensen és auditálhatóan Jira-ba kerüljön, majd a jövőben Jira legyen az elsődleges source of truth.

## Story státuszok induláskor

Javasolt induló Jira státusz minden storyra: `Backlog`, kivéve ha a Jira projekt előkészítése már megtörtént.

## EPIC3-STORY-1: Jira projektmodell és workflow véglegesítése

Issue type: `Story`

Priority: `Highest`

Goal: A Jira projektben legyenek meg azok az issue type-ok, státuszok és workflow átmenetek, amelyekre az Autoforge migráció és a későbbi Jira-first működés épül.

Acceptance criteria:

- Jira projekt kulcs kiválasztva és dokumentálva.
- Issue type-ok rögzítve: Epic, Story, Bug, Task, Spike, Sub-task.
- Workflow státuszok rögzítve: Backlog, Ready, In Progress, In Review, Blocked, Done, Cancelled.
- Megengedett státuszátmenetek dokumentálva.
- Döntés készült arról, hogy használhatók-e custom fieldek.
- Nincs Jira token vagy secret dokumentációban.

Dependencies: nincs.

Output: Jira projektmodell döntés és workflow mapping.

## EPIC3-STORY-2: Jira secret kezelés és hozzáférési modell

Issue type: `Story`

Priority: `Highest`

Goal: A Jira API hozzáférés biztonságos kezelése úgy, hogy token ne kerüljön repo-ba, logba vagy AI workspace-be.

Acceptance criteria:

- Jira API token tárolási mód kiválasztva: lokális env, GitHub Secret vagy OCI Vault.
- Javasolt env változók rögzítve: `JIRA_BASE_URL`, `JIRA_EMAIL`, `JIRA_API_TOKEN`, `JIRA_PROJECT_KEY`.
- Log masking szabályok dokumentálva.
- Import és AI agent működés token-hozzáférése különválasztva.
- AI runtime nem kap közvetlen Jira tokent, ha később broker/proxy bevezethető.
- Token rotation runbook vázlat elkészült.

Dependencies: EPIC3-STORY-1.

Output: Jira credential handling runbook.

## EPIC3-STORY-3: Markdown task parser elkészítése

Issue type: `Story`

Priority: `High`

Goal: A `docs/tasks/*.md` fájlok gépileg feldolgozható, strukturált import reprezentációvá alakítása.

Acceptance criteria:

- Parser beolvassa az `AUTO-*` és `BUG-*` fájlokat.
- Parser kinyeri a címet, státuszt, verziót, branch-et, PR-t, acceptance criteria-t, dokumentumokat, lépésnaplót és eredményt.
- Hiányzó mezőket validációs hibaként vagy warningként jelzi.
- Parser nem olvas secret értékeket és nem írja ki őket logba.
- Kimenet determinisztikus JSON.

Dependencies: nincs.

Output: Markdown task import JSON.

## EPIC3-STORY-4: Release manifest parser és verzió mapping

Issue type: `Story`

Priority: `High`

Goal: A `docs/releases.md` alapján minden meglévő task Jira Fix Version vagy Target Version mezőhöz rendelhető legyen.

Acceptance criteria:

- Parser beolvassa a release verzió szekciókat.
- Parser összerendeli az `AUTO-*` és `BUG-*` azonosítókat verzióval.
- Hiányzó vagy duplikált verzió mapping warningként jelenik meg.
- Kimenet összefésülhető a task parser JSON-jával.
- A release manifest továbbra sem tartalmaz secretet.

Dependencies: EPIC3-STORY-3.

Output: Task-to-version mapping JSON.

## EPIC3-STORY-5: Epic dokumentum parser és Jira Epic mapping

Issue type: `Story`

Priority: `High`

Goal: A `docs/epics/EPIC-*.md` dokumentumok Jira Epic issue-ként importálhatók legyenek.

Acceptance criteria:

- Parser beolvassa az epic dokumentumokat.
- Jira Epic summary, description és labels generálódik.
- Epic external ID megmarad, például `EPIC-1`, `EPIC-2`, `EPIC-3`.
- Storyk később Epic Link vagy Parent mezővel kapcsolhatók hozzá.
- Az epic import nem duplikál meglévő Jira epiceket.

Dependencies: EPIC3-STORY-1.

Output: Epic import JSON.

## EPIC3-STORY-6: GitHub PR státusz enrichment

Issue type: `Story`

Priority: `Medium`

Goal: A meglévő taskok PR állapotát GitHubból ellenőrizni kell, hogy a Jira státusz mapping pontosabb legyen.

Acceptance criteria:

- A script kiolvassa a task fájlokban szereplő PR számokat.
- GitHub API vagy `gh` alapján lekéri, hogy a PR open, closed vagy merged.
- A merged PR-hez commit SHA vagy merge commit is kapcsolható, ha elérhető.
- API hiba esetén a migráció nem áll le, hanem warningot ad.
- GitHub token nem kerül logba vagy kimeneti fájlba.

Dependencies: EPIC3-STORY-3.

Output: PR-enriched import JSON.

## EPIC3-STORY-7: Jira státuszleképező implementálása

Issue type: `Story`

Priority: `High`

Goal: A Markdown státusz, branch, PR és GitHub állapot alapján automatikus Jira státusz javaslat készüljön.

Acceptance criteria:

- `completed` és merged PR -> `Done`.
- Open PR -> `In Review`.
- Branch PR nélkül -> `In Progress`.
- Explicit blocked jelzés -> `Blocked`.
- Ismeretlen állapot -> `Backlog` vagy `Ready` warninggal.
- Minden státuszdöntés auditálható okkal szerepel a dry-run outputban.

Dependencies: EPIC3-STORY-3, EPIC3-STORY-6.

Output: Status-mapped import plan.

## EPIC3-STORY-8: Jira import dry-run generátor

Issue type: `Story`

Priority: `Highest`

Goal: Éles Jira írás előtt készüljön teljes, ember által review-zható import terv.

Acceptance criteria:

- Dry-run JSON tartalmaz minden létrehozandó vagy frissítendő Jira issue-t.
- Dry-run tartalmazza az issue type-ot, summaryt, descriptiont, státuszt, verziót, labels-t és external ID-t.
- Dry-run jelzi a hiányzó adatokat és mapping konfliktusokat.
- Dry-run nem tartalmaz tokeneket vagy secret értékeket.
- Dry-run fájl commitolható, ha nem tartalmaz érzékeny adatot.

Dependencies: EPIC3-STORY-3, EPIC3-STORY-4, EPIC3-STORY-5, EPIC3-STORY-7.

Output: Review-zott Jira import plan.

## EPIC3-STORY-9: Jira issue import futtatása idempotensen

Issue type: `Story`

Priority: `Highest`

Goal: A jóváhagyott dry-run terv alapján a Jira issue-k létrejöjjenek vagy frissüljenek, duplikáció nélkül.

Acceptance criteria:

- Import előtt external ID alapján keres Jira-ban meglévő issue-t.
- Létező issue esetén frissít, nem duplikál.
- Új issue esetén létrehoz megfelelő issue type-pal.
- Import után minden issue key bekerül a mappingbe.
- Részleges hiba esetén újrafuttatható marad.
- Import log nem tartalmaz tokeneket.

Dependencies: EPIC3-STORY-8.

Output: Jira-ban létrehozott/frissített issue-k.

## EPIC3-STORY-10: Jira mapping fájl generálása

Issue type: `Story`

Priority: `High`

Goal: A régi Autoforge ID-k és az új Jira issue key-k között legyen auditálható mapping.

Acceptance criteria:

- Mapping tartalmazza az eredeti ID-t, Jira key-t, issue type-ot és source file-t.
- Mapping nem tartalmaz secretet vagy tokent.
- Mapping JSON vagy Markdown formában elérhető a repo-ban.
- Mapping alapján a régi task ID-k visszakereshetők.
- Mapping frissíthető új import futás után.

Dependencies: EPIC3-STORY-9.

Output: `docs/jira-mapping.json` vagy `docs/jira-mapping.md`.

## EPIC3-STORY-11: AGENT.md Jira-first folyamatra frissítése

Issue type: `Story`

Priority: `Highest`

Goal: Az AI ügynökök kötelező működési szabályai Jira-first modellre álljanak át.

Acceptance criteria:

- Új feladat indításakor Jira issue kötelező.
- Branch, commit és PR név Jira issue key alapján készül.
- PR body kötelezően linkeli a Jira issue-t.
- Munka végén Jira komment és státuszfrissítés szabályai rögzítve.
- Spike/döntési issue csak explicit user döntés vagy rögzített kutatási eredmény után zárható; default javaslat önmagában nem elég.
- Legacy `AUTO-*` és `BUG-*` használat historical reference-ként marad.
- Secret kezelés továbbra is tiltja tokenek fájlba írását.

Dependencies: EPIC3-STORY-9, EPIC3-STORY-10.

Output: Jira-first `AGENT.md`.

## EPIC3-STORY-12: AI tooling bővítése Jira MCP-vel és skill-lel

Issue type: `Story`

Priority: `High`

Goal: Az AI tooling stack explicit módon tartalmazza a Jira műveletekhez szükséges MCP-t és skillt.

Acceptance criteria:

- `ops/ai/mcps.yaml` tartalmaz `jira` MCP-t.
- `ops/ai/skills.yaml` tartalmaz `jira-management` skillt.
- `ops/ai/groups.yaml` a megfelelő delivery/knowledge csoporthoz kapcsolja.
- `ops/mcp/jira-local.sh` biztosít helyi, gitignored launch pointot a Jira MCP-hez.
- `docs/ai-tooling.md` dokumentálja a Jira MCP és skill használati sorrendjét.
- Pre-PR protokoll kiterjed Jira issue ellenőrzésre.

Dependencies: EPIC3-STORY-1.

Output: Jira-aware AI tooling manifestek.

## EPIC3-STORY-13: PR template és release dokumentáció Jira linkeléssel

Issue type: `Story`

Priority: `Medium`

Goal: A GitHub PR folyamat és release dokumentáció Jira issue key-t használjon elsődleges referenciaként.

Acceptance criteria:

- PR template tartalmaz Jira issue mezőt.
- Release manifest Jira issue key-ket használ új bejegyzéseknél.
- Régi `AUTO-*`/`BUG-*` referenciák historical mappingként megmaradnak.
- Dokumentált, hogyan kell PR-t írni Jira-first módban.

Dependencies: EPIC3-STORY-11.

Output: Jira-linked PR/release folyamat.

## EPIC3-STORY-14: Story/task Markdown fájlok törlése a migráció után

Issue type: `Story`

Priority: `Medium`

Goal: A sikeres Jira import és validált mapping után eltávolítani a story/task szintű Markdown nyilvántartást a gitből, hogy Jira legyen az egyetlen operatív backlog source of truth.

Acceptance criteria:

- A törlés előfeltétele dokumentált: minden érintett story/task/bug Jira-ban van és szerepel a mappingben.
- `docs/tasks/*.md` fájlok törlésre kerülnek a migráció lezáró PR-jében.
- Story breakdown jellegű átmeneti dokumentumok törlésre kerülnek, ha Jira issue-ként már létrejöttek.
- Megmarad egy minimális `docs/jira-mapping.*` audit mapping.
- README vagy index jelzi, hogy a napi source of truth Jira.
- Új `docs/tasks/*.md` létrehozása tiltottként dokumentált.
- Régi task linkek helyett Jira issue key és mapping alapján történik a visszakeresés.

Dependencies: EPIC3-STORY-10, EPIC3-STORY-11.

Output: Lezáró cleanup PR, amely eltávolítja a story/task Markdown nyilvántartást és meghagyja a Jira mappinget.

## EPIC3-STORY-15: Jira import validáció és lezárási riport

Issue type: `Story`

Priority: `High`

Goal: A migráció után legyen bizonyíték arra, hogy minden elvárt task, bug és epic átkerült Jira-ba megfelelő státusszal.

Acceptance criteria:

- Riport tartalmazza az összes importált `AUTO-*`, `BUG-*`, `EPIC-*` elemet.
- Riport jelzi a kimaradt vagy hibás elemeket.
- Riport összeveti a `docs/releases.md` mappinget Jira issue-kal.
- Riport ellenőrzi, hogy nincs duplikált external ID.
- Riport igazolja, hogy token/secret nem került import outputba.

Dependencies: EPIC3-STORY-9, EPIC3-STORY-10.

Output: Jira migration validation report.

## Ajánlott végrehajtási sorrend

1. EPIC3-STORY-1
2. EPIC3-STORY-2
3. EPIC3-STORY-3
4. EPIC3-STORY-4
5. EPIC3-STORY-5
6. EPIC3-STORY-6
7. EPIC3-STORY-7
8. EPIC3-STORY-8
9. EPIC3-STORY-9
10. EPIC3-STORY-10
11. EPIC3-STORY-11
12. EPIC3-STORY-12
13. EPIC3-STORY-13
14. EPIC3-STORY-14
15. EPIC3-STORY-15

## Első milestone javaslat

Milestone: `Jira migration dry-run ready`

Tartalom:

- EPIC3-STORY-1
- EPIC3-STORY-2
- EPIC3-STORY-3
- EPIC3-STORY-4
- EPIC3-STORY-5
- EPIC3-STORY-6
- EPIC3-STORY-7
- EPIC3-STORY-8

Eredmény: ember által review-zható, secretmentes Jira import terv.

## Második milestone javaslat

Milestone: `Jira source of truth enabled`

Tartalom:

- EPIC3-STORY-9
- EPIC3-STORY-10
- EPIC3-STORY-11
- EPIC3-STORY-12
- EPIC3-STORY-13
- EPIC3-STORY-14
- EPIC3-STORY-15

Eredmény: Jira-ban élő backlog, Jira-first agent workflow, validált migráció.
