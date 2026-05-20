# [AUTO-451] Teacher group cannot see learning material creation UI

Statusz: Under test

Verzio: 0.1.72

Branch: `bug/AUTO-451-learning-upload-teacher-group`

PR: https://github.com/janake/autoforge/pull/182

## Cél

A Keycloak `teacher` csoport tagjai lathassak a tananyag feltolto/keszito UI-t, ne csak azok, akiknel a jogosultsag role-kent erkezik.

## Scope

- frontend learning upload gate javitasa role es group alapra
- release manifest frissites
- frontend typecheck

## Lepesnaplo

- `jira_create_issue(AUTO-451)` - bug letrehozva
- `jira_transition_issue(AUTO-451, In Progress)` - munka inditva
- `git worktree add bug/AUTO-451-learning-upload-teacher-group` - dedikalt worktree/branch letrehozva
- `npm run typecheck:web` - ok
- `git diff --check` - ok
- `gh pr create` - PR megnyitva
- `jira_transition_issue(AUTO-451, Under test)` - review/verifikacios statusz beallitva
