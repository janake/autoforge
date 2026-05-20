# [AUTO-472] Prompt draft clarification does not use configured AI

Statusz: Under test

Verzio: 0.1.77

Branch: `bug/AUTO-472-ai-prompt-draft-clarification`

PR: https://github.com/janake/autoforge/pull/188

## Cel

A prompt draft clarification ne hardcoded kerdeseket adjon vissza, hanem a konfiguralt OpenCode/OpenRouter AI futtatast hasznalja.

## Scope

- AI-alapu prompt draft clarification backend service
- explicit `503 Service Unavailable`, ha az AI nincs konfigurálva vagy nem elerheto
- frontend hibauzenet megjelenites a backend `message` mezobol
- controller es HTTP clarifier tesztek az AI-val es unavailable AI allapotra
- release manifest frissites

## Lepesnaplo

- `jira_transition_issue(AUTO-472, In Progress)` - munka inditva
- `git worktree add bug/AUTO-472-ai-prompt-draft-clarification` - dedikalt worktree/branch letrehozva
- `mvn test` - ok, 89 tests pass, 0 failures, 0 errors
- `npm run build:backend` - ok
- `gh pr create` - PR megnyitva
- `jira_transition_issue(AUTO-472, Under test)` - review/verifikacios statusz beallitva
- `npm ci` - frontend dependency install a worktree-ben verifikaciohoz
- `npm run typecheck:web` - ok
- `npm run test:backend` - ok
- `npm run build:web` - ok
