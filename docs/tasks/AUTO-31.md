# [AUTO-31] Add Jira migration dry-run generator

## Feladat leírása

Credential nélküli Jira migrációs dry-run generátor készítése, amely a meglévő Markdown taskokat, bugokat, epiceket, story breakdownokat és release mappinget Jira import tervvé alakítja.

## Statusz
in_progress

## Verzió
0.1.21

## Branch
feature/AUTO-31-jira-dry-run-import

## PR
- PR #55

## Acceptance criteria

- A dry-run generátor Jira token nélkül fut.
- A generátor nem hív Jira API-t.
- A generátor beolvassa a taskokat, bugokat, epiceket, story breakdownokat és release mappinget.
- A kimenet determinisztikus JSON import terv.
- A generált kimenet alapértelmezetten gitből kizárt helyre kerül.

## Dokumentumok és fájlok

- `ops/jira/generate-import-plan.mjs`
- `ops/jira/README.md`
- `package.json`
- `docs/releases.md`

## Lepesnaplo

- [x] Dry-run generátor hozzáadva.
- [x] `jira:dry-run` npm script hozzáadva.
- [x] Jira migrációs tooling dokumentálva.
- [x] `npm run jira:dry-run` sikeresen lefutott; `build/jira-import-plan.json` generálva.
- [x] Ellenőrizve, hogy a generált `build/jira-import-plan.json` gitignore alatt van.
- [x] Secret-pattern keresés nem talált token-szerű értéket.
- [x] PR megnyitva.

## Eredmény

A Jira import első, credential nélküli dry-run fázisa elkészült.
