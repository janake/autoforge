# [AUTO-29] Define Jira migration epic

## Feladat leírása

Az EPIC-3 kidolgozása arra, hogy a meglévő Markdown-alapú Autoforge taskokat, bugokat és epiceket Jira-ba migráljuk, majd a továbbiakban Jira legyen az epicek és storyk elsődleges kezelési helye.

## Statusz
in_progress

## Verzió
0.1.20

## Branch
feature/AUTO-29-jira-migration-epic

## PR
- PR #53

## Acceptance criteria
- EPIC-3 dokumentum létrejön a `docs/epics/` alatt.
- Az epic tartalmazza a Markdown -> Jira migrációs stratégiát.
- Az epic tartalmazza a státuszleképezést.
- Az epic tartalmazza a Jira-first jövőbeli működési szabályokat.
- Az epic tartalmazza a Jira token és secret kezelés biztonsági szabályait.
- Az epic tartalmazza a Jira MCP és skill igényt.
- Az epichez elkészül egy Jira-ready story bontás.
- Az epic rögzíti, hogy sikeres migráció után a story/task Markdown fájlokat töröljük a gitből.
- A Jira MCP és `jira-management` skill bekerül az AI tooling stackbe.

## Dokumentumok és fájlok
- `docs/epics/EPIC-3-jira-migration.md`
- `docs/epics/EPIC-3-jira-migration-stories.md`
- `docs/ai-tooling.md`
- `ops/ai/mcps.yaml`
- `ops/ai/skills.yaml`
- `ops/ai/groups.yaml`
- `docs/releases.md`

## Lepesnaplo
- [x] Jira migrációs epic létrehozva.
- [x] Státuszleképezés kidolgozva.
- [x] Jira-first jövőbeli működés rögzítve.
- [x] Secret kezelés szabályai rögzítve.
- [x] PR megnyitva.
- [x] Jira-ready story bontás elkészítve.
- [x] Migráció utáni story/task Markdown törlési szabály rögzítve.
- [x] Jira MCP és `jira-management` skill beállítva.

## Eredmény
Az EPIC-3 dokumentáció elkészült a későbbi taskokra bontáshoz.
