# [AUTO-473] Public frontend deploy did not include prompt draft error message fix

Statusz: In progress

Verzio: 0.1.78

Branch: `bug/AUTO-473-prompt-draft-error-message`

PR: pending

## Cel

A prompt draft flow ne generikus `Request failed with 503` hibauzenetet mutasson, hanem a backend `ApiErrorResponse.message` mezobe irt konkret AI konfiguracios vagy runtime okot.

## Scope

- frontend API helper hibauzenet-parsing nem-2xx valaszokra
- release manifest es task doc frissites
- public deploy utan ellenorizheto frontend bundle

## Lepesnaplo

- `jira_create_issue(AUTO-473)` - uj bug letrehozva a merge utan kimaradt frontend javitasra
- `jira_transition_issue(AUTO-473, In Progress)` - munka inditva
- `git worktree add bug/AUTO-473-prompt-draft-error-message` - dedikalt worktree/branch letrehozva
- `npm ci` - ok
- `npm run typecheck:web` - ok
- `npm run build:web` - ok
