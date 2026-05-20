# [AUTO-452] Menu-driven workspace navigation with separate feature pages

Statusz: Under test

Verzio: 0.1.73

Branch: `feature/AUTO-452-menu-driven-workspace`

PR: https://github.com/janake/autoforge/pull/183

## Cél

Az authenticated workspace ne egyetlen hosszu, telezsufolt oldalon mutassa az osszes funkciot, hanem menubol elerheto kulon destination page-ekre bontva.

## Scope

- fokuszalt dashboard fooldal
- kulon `Learning`, `AI`, `Jobs`, `Info` destinationok
- meglevo auth es visibility gate-ek megtartasa
- release manifest es task doc frissites

## Lepesnaplo

- `jira_transition_issue(AUTO-452, In Progress)` - munka inditva
- `git worktree add feature/AUTO-452-menu-driven-workspace` - dedikalt worktree/branch letrehozva
- `npm run typecheck:web` - ok
- `git diff --check` - ok
- `gh pr create` - PR megnyitva
- `jira_transition_issue(AUTO-452, Under test)` - review/verifikacios statusz beallitva
