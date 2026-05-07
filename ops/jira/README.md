# Jira Migration Tools

Ez a mappa a Jira migrációhoz tartozó, credential nélküli előkészítő eszközöket tartalmazza.

## Dry-run import terv

Futtatás:

```bash
npm run jira:dry-run
```

Alapértelmezett kimenet:

```text
build/jira-import-plan.json
```

A `build/` mappa gitből kizárt, ezért a generált terv nem kerül véletlenül commitba.

Egyedi kimenet:

```bash
node ops/jira/generate-import-plan.mjs --output /tmp/jira-import-plan.json
```

## Mit csinál?

- Beolvassa a `docs/tasks/AUTO-*.md` és `docs/tasks/BUG-*.md` fájlokat.
- Beolvassa a `docs/epics/EPIC-*.md` epic dokumentumokat.
- Beolvassa az `EPIC-*-stories.md` story breakdown fájlokat.
- Beolvassa a `docs/releases.md` task-to-version mappinget.
- Jira API hívás nélkül előállít egy review-zhető JSON import tervet.

## Mit nem csinál?

- Nem hív Jira API-t.
- Nem használ Jira tokent.
- Nem ír secretet fájlba.
- Nem hoz létre Jira issue-t.

## Következő fázis

Ha az OCI Vaultban készen vannak a Jira credential referenciák, akkor külön lépésben jöhet az idempotens Jira importáló, amely a dry-run terv alapján hoz létre vagy frissít issue-kat.
