# [AUTO-479] Persist AI-generated learning material content to database

Statusz: In Progress

Verzio: 0.1.82

Branch: `feature/AUTO-479-generated-learning-material`

PR: -

## Cel

A tanar tudjon tananyaghoz AI-generalt lesson tartalmat letrehozni, es az eredmeny a DB-ben tarolt generalt tartalomkent jelenjen meg a learning detail oldalon.

## Scope

- `LESSON` generation type
- `POST /api/v1/learning/materials/{materialId}/lesson` endpoint
- lesson fallback/AI prompt ág
- teacher UI generate gombok question/summary/lesson muveletekhez
- learning docs es release manifest frissites

## Lepesnaplo

- `jira_create_issue(AUTO-479)` - uj story letrehozva
- `jira_transition_issue(AUTO-479, In Progress)` - Jira statusz allitasa
- `git worktree add -b feature/AUTO-479-generated-learning-material ... origin/main` - dedikalt worktree/branch letrehozva
- `task(subagent_type=explore)` - meglevo learning generation/persistence flow feltarasa
- `mvn -q -f services/backend/pom.xml -Dtest=LearningContentGenerationControllerTest test` - RED: `LESSON` hianyzott
- `mvn -q -f services/backend/pom.xml -Dtest=LearningContentGenerationControllerTest test` - GREEN: lesson endpoint es DB persistence ok
- `npm ci` - worktree node dependencies telepitve lockfile alapjan
- `npm run typecheck:web` - ok
- `npm run test:backend` - ok
- `npm run build:web` - ok
- `git diff --check` - ok

## Megjegyzes

A raw source tovabbra sem DB-be kerul tervezetten; a generalt lesson tartalom a mar letezo `learning_generated_content` persistencia feluletet hasznalja.
