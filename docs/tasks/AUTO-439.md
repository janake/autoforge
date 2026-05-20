# [AUTO-439] Kérdéssor draft és publikálás student-visible állapotba

Statusz: Under Test

Verzió: 0.1.59

Branch: `feature/AUTO-439-question-draft-publish`

PR: #169

## Cél

A Learning modulban a kérdéssor generálás draft státuszban induljon, a teacher publikálhassa vagy archiválhassa a kérdéssort, a student pedig csak a publikált kérdéssorokat lássa és oldhassa meg.

## Scope

- question set publication status model
- teacher publish/archive endpoints
- student-facing filtering draft/archived question setsre
- web UI publish/archive akciók a question set kártyákon
- release és task traceability frissítés

## Lepesnaplo

- `jira_get_issue(AUTO-439)` - story scope lekérdezve Jira MCP-n keresztül
- `jira_transition_issue(AUTO-439, In Progress)` - Jira státusz állítás
- `git worktree add -b feature/AUTO-439-question-draft-publish ... origin/main` - dedikált worktree létrehozása
- `npm install` - web workspace függőségek telepítve az új worktree-ben
- `git merge origin/main` - branch frissítve a legutóbbi learning changes-re
- `mvn -q -f services/backend/pom.xml -Dtest=LearningContentGenerationControllerTest,LearningMaterialGenerationHistoryControllerTest test` - backend regression tesztek sikeresek
- `npm run build:web` - web build sikeres
- `gh pr create` - PR megnyitva

## Megjegyzés

A PR és a Jira lezárás még hátravan.
