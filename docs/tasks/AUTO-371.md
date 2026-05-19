# [AUTO-371] Tananyag több source életciklus és ingestion update

Statusz: Under Test

Verzió: 0.1.57

Branch: `feature/AUTO-371-multi-source-lifecycle`

PR: pending

## Cél

A learning tananyagok több source rekordot kezeljenek, a tananyag detail oldalon az owner lássa és kezelje ezeket, az ingestion pedig az aktív source-okból dolgozzon.

## Scope

- learning material source entity és repository
- source add / soft delete API
- detail response source listával
- ingestion aktív source-alapú text boundary
- web detail oldali source upload és delete UI
- backend és web regression tesztek

## Lepesnaplo

- `jira_transition_issue(AUTO-371, In Progress)` - Jira státusz állítás a munka kezdetén
- `git worktree` a dedikált `AUTO-371` branchhez
- `mvn -q -f services/backend/pom.xml test` - backend test suite lefuttatva, sikeres
- `npm run build:web` - web build lefuttatva, sikeres

## Megjegyzés

A PR még nincs megnyitva; a végső commit/push/PR lépés hátravan.
