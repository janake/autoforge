# [AUTO-391] Teacher tananyag hozzárendelés diákhoz és csoporthoz

Statusz: In Progress

Verzio: 0.1.62

Branch: `feature/AUTO-391-assignment-targets`

PR: pending

## Cél

A teacher/owner a Learning detail nézetben tudja szerkeszteni, mely student subjectek és group identifierek kapják meg a tananyagot.

## Scope

- teacher assignment editor a detail oldalon
- student subject és group target listák kezelése
- assignment save API hívás a meglévő backend endpointon
- UI state frissítés mentés után
- release és task traceability frissítés

## Lepesnaplo

- `jira_get_issue(AUTO-391)` - story scope lekérdezve Jira MCP-n keresztül
- `git worktree add -b feature/AUTO-391-assignment-targets ... origin/main` - dedikált worktree létrehozása

## Megjegyzés

A PR és a Jira lezárás még hátravan.
