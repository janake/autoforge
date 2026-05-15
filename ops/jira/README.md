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

Ha az OCI Vaultban készen vannak a Jira credential referenciák, akkor az idempotens Jira importáló a dry-run terv alapján hoz létre vagy frissít issue-kat.

## Jira meglévő issue ellenőrzés

Közvetlen env változókkal:

```bash
JIRA_BASE_URL="https://<tenant>.atlassian.net" \
JIRA_PROJECT_KEY="<project-key>" \
JIRA_EMAIL="<email>" \
JIRA_API_TOKEN="<token>" \
npm run jira:import:check
```

OCI Vault OCID env változókkal:

```bash
OCI_JIRA_BASE_URL_SECRET_OCID="<vault-secret-ocid>" \
OCI_JIRA_PROJECT_KEY_SECRET_OCID="<vault-secret-ocid>" \
OCI_JIRA_EMAIL_SECRET_OCID="<vault-secret-ocid>" \
OCI_JIRA_API_TOKEN_SECRET_OCID="<vault-secret-ocid>" \
npm run jira:import:check
```

OCI Vault secret név alapú lookupkal:

```bash
npm run jira:import:check -- --oci-lookup-by-name
```

Elérhető Jira projektek ellenőrzése:

```bash
node ops/jira/import-plan.mjs --oci-lookup-by-name --list-projects
```

Ha a lista üres, akkor a tokenhez tartozó Jira felhasználó nem lát projektet, vagy nincs `Browse projects` jogosultsága az adott Jira site-on.

## Jira import futtatás

Alap create/update import:

```bash
npm run jira:import -- --oci-lookup-by-name
```

Egyetlen issue import teszthez:

```bash
npm run jira:import -- --oci-lookup-by-name --issue EPIC-3
```

Státusz transition próbával:

```bash
npm run jira:import -- --oci-lookup-by-name --apply-status
```

Fontos:

- Az import idempotens: external ID label alapján keres meglévő issue-t.
- A meglévő issue keresés a Jira Cloud új `/rest/api/3/search/jql` endpointját használja.
- A token értéke nem kerül kiírásra.
- Ha egy Jira issue type nem létezik, a create retry alapértelmezetten `Task` típussal történik.
- Első éles futás előtt mindig nézd meg a `build/jira-import-plan.json` tartalmát.

## Jira MCP sprintkezeles es export

Az `ops/mcp/jira-server.mjs` stdio MCP szerver kozvetlen Jira Agile REST toolokat ad az agenteknek.

Futtatas helyi MCP launcherrel:

```bash
ops/mcp/jira-local.sh
```

Credential feloldas sorrendje:

- kozvetlen env valtozo: `JIRA_BASE_URL`, `JIRA_PROJECT_KEY`, `JIRA_EMAIL`, `JIRA_API_TOKEN`
- explicit OCI Vault OCID env: `OCI_<SECRET_NAME>_SECRET_OCID`
- OCI Vault display name lookup azonos secret nevekkel

Sprint/backlog toolok:

- `jira_list_boards`
- `jira_list_sprints`
- `jira_create_sprint`
- `jira_update_sprint` state valtashoz is, peldaul `active` vagy `closed`
- `jira_get_sprint_issues`
- `jira_add_issues_to_sprint`
- `jira_move_issues_to_backlog`
- `jira_rank_issues`

Issue management toolok:

- `jira_create_issue`
- `jira_search`
- `jira_get_issue`
- `jira_update_issue`
- `jira_list_transitions`
- `jira_transition_issue`
- `jira_add_comment`

Export toolok:

- `jira_export_issues`: JQL vagy projekt szerinti issue export epicekre, standard issue-kra es subtaskokra bontva.
- `jira_export_project`: teljes projekt planning export boardokkal, sprintekkel, issue-kkal, kommentekkel, linkekkel, parent kapcsolatokkal, labelekkel es custom fieldekkel.

Pelda teljes projekt export MCP tool argumentumra:

```json
{
  "projectKey": "AUTO",
  "includeClosedSprints": true,
  "includeComments": true,
  "pageSize": 100
}
```
