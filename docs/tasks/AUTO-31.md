# [AUTO-31] Add Jira migration dry-run generator

## Feladat leírása

Credential nélküli Jira migrációs dry-run generátor és idempotens Jira importáló készítése, amely a meglévő Markdown taskokat, bugokat, epiceket, story breakdownokat és release mappinget Jira import tervvé alakítja, majd jóváhagyás után Jira issue-kat tud létrehozni vagy frissíteni.

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
- Az importáló alapértelmezetten dry-run módban nem hív Jira API-t.
- Az importáló `--apply` kapcsolóval idempotensen create/update műveleteket tud végezni.
- Az importáló támogat közvetlen env, OCI secret OCID és OCI secret név alapú credential betöltést.

## Dokumentumok és fájlok

- `ops/jira/generate-import-plan.mjs`
- `ops/jira/import-plan.mjs`
- `ops/jira/README.md`
- `package.json`
- `docs/releases.md`

## Lepesnaplo

- [x] Dry-run generátor hozzáadva.
- [x] Idempotens Jira importáló hozzáadva.
- [x] `jira:dry-run` npm script hozzáadva.
- [x] `jira:import:*` npm scriptek hozzáadva.
- [x] Jira migrációs tooling dokumentálva.
- [x] `npm run jira:dry-run` sikeresen lefutott; `build/jira-import-plan.json` generálva.
- [x] `npm run jira:import:dry-run` sikeresen lefutott Jira API hívás nélkül.
- [x] `npm run jira:import:check -- --oci-lookup-by-name --limit 1` sikeresen ellenőrizte a Jira elérést írás nélkül.
- [x] Ellenőrizve, hogy a generált `build/jira-import-plan.json` gitignore alatt van.
- [x] Secret-pattern keresés nem talált token-szerű értéket.
- [x] PR megnyitva.

## Eredmény

A Jira import első, credential nélküli dry-run fázisa elkészült.
