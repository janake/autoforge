# [AUTO-397] Teacher hibabejelentés review és pontszám felülbírálás

Statusz: In Progress

Verzió: 0.1.56

Branch: `feature/AUTO-397-dispute-review`

PR: #168

## Cél

A student saját question attemptjéhez hibabejelentést nyithasson, a teacher pedig review-zza és szükség esetén felülbírálhassa a pontszámot.

## Scope

- dispute entity és status model
- student dispute submit endpoint
- teacher review és score override endpoint
- owner/student scoped dispute listázás
- backend regression tesztek

## Lepesnaplo

- `jira_transition_issue(AUTO-397, In Progress)` - Jira státusz állítás
- `git worktree add -b feature/AUTO-397-dispute-review ... origin/main` - dedikált worktree létrehozása
- `mvn -q -f services/backend/pom.xml -Dtest=LearningQuestionDisputeControllerTest test` - backend regression ellenőrzés
- `npm run build:web` - frontend build ellenőrzés

## Megjegyzés

A PR és validáció folyamatban van.
