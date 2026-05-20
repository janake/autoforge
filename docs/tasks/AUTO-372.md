# [AUTO-372] Learning teacher CRUD és student megoldási jogosultságok

Statusz: Under Test

Verzió: 0.1.58

Branch: `feature/AUTO-372-learning-permissions`

PR: https://github.com/janake/autoforge/pull/162

## Cél

A Learning modulban a teacher/admin szerepkör kezelje a tananyag létrehozási útvonalakat, a student pedig csak a hozzárendelt tananyagokat és megoldási/hibajelentési műveleteket érje el.

## Scope

- Jira MCP alapú story scope lekérés
- teacher/admin guard tananyag uploadra backend oldalon
- student upload tiltás regression teszttel
- frontend upload action elrejtése nem teacher/admin szerepkörben
- release és task traceability frissítés

## Lepesnaplo

- `jira_get_issue(AUTO-372)` - story scope lekérdezve Jira MCP-n keresztül
- `jira_transition_issue(AUTO-372, In Progress)` - Jira státusz állítás
- `jira_update_issue(AUTO-372, fixVersions=0.1.58)` - Jira célverzió beállítva
- `git worktree add -b feature/AUTO-372-learning-permissions ... origin/main` - dedikált worktree létrehozása
- `git merge --ff-only FETCH_HEAD` - worktree frissítése a legfrissebb `origin/main` állapotra
- `mvn -q -f services/backend/pom.xml -Dtest=LearningMaterialControllerTest test` - célzott backend regression teszt sikeres
- `npm install` - web workspace függőségek telepítve az új worktree-ben
- `npm run build:web` - web build sikeres
- `gh pr create` - PR megnyitva: https://github.com/janake/autoforge/pull/162

## Megjegyzés

A PR megnyílt, a story Under Test státuszba kerül.
