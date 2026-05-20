# [AUTO-433] Tananyag forrásverziózás és generált tartalom eredete

Statusz: Under Test

Verzió: 0.1.60

Branch: `feature/AUTO-433-source-versioning`

PR: #170

## Cél

A Learning modulban a generált kérdéssor és összefoglaló tárolja, hogy melyik aktív source verziókból készült, hogy a régi generálások később is visszakövethetők legyenek.

## Scope

- source snapshot mentése a generált tartalomhoz
- question set és summary source version hivatkozások
- web felületen source version összegzés megjelenítése
- deleted source utáni traceability regression teszt
- release és task traceability frissítés

## Lepesnaplo

- `jira_get_issue(AUTO-433)` - story scope lekérdezve Jira MCP-n keresztül
- `jira_transition_issue(AUTO-433, In Progress)` - Jira státusz állítás
- `git worktree add -b feature/AUTO-433-source-versioning ... origin/main` - dedikált worktree létrehozása
- `npm install` - web workspace függőségek telepítve az új worktree-ben
- `git merge origin/main` - branch frissítve a legutóbbi learning changes-re
- `mvn -q -f services/backend/pom.xml -Dtest=LearningContentGenerationControllerTest,LearningMaterialGenerationHistoryControllerTest test` - backend regression tesztek sikeresek
- `npm run build:web` - web build sikeres
- `gh pr create` - PR megnyitva

## Megjegyzés

A PR és a Jira lezárás még hátravan.
