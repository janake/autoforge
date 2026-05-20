# [AUTO-474] Prompt draft flow cannot complete because AI clarification runtime is unavailable

Statusz: In progress

Verzio: 0.1.79

Branch: `bug/AUTO-474-enable-prompt-ai-runtime`

PR: pending

## Cel

A prompt draft flow vegig tudjon menni a draft/clarify lepestol az approval es Jira ticket letrehozas fele, ne alljon meg OpenCode session creation hibaval.

## Scope

- private deploy OpenCode/OpenRouter runtime engedelyezese dokumentalt Vault display-name defaultokkal
- OpenCode es OpenRouter image preload akkor is, ha GitHub var nincs explicit beallitva
- release manifest es task doc frissites
- private deploy utan prompt draft runtime verifikacio

## Lepesnaplo

- `jira_create_issue(AUTO-474)` - uj bug letrehozva a mukodesi hibara
- `jira_transition_issue(AUTO-474, In Progress)` - munka inditva
- `git worktree add bug/AUTO-474-enable-prompt-ai-runtime` - dedikalt worktree/branch letrehozva
- `bash -n infra/deploy/private/deploy.sh` - ok
- `git diff --check` - ok
