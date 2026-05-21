# [AUTO-474] Prompt draft flow cannot complete because AI clarification runtime is unavailable

Statusz: In progress

Verzio: 0.1.79

Branch: `bug/AUTO-474-enable-prompt-ai-runtime`, `bug/AUTO-474-openrouter-secret-name`, `bug/AUTO-474-opencode-smoke-env`

PR: https://github.com/janake/autoforge/pull/190, https://github.com/janake/autoforge/pull/191, pending follow-up

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
- `gh run view 26194334297` - private deploy failed: `Expected one ACTIVE OCI Vault secret named autoforge-opencode-server-password, found none.`
- `oci search resource free-text-search --text openrouter` - found ACTIVE Vault secret `openrouter-api-key`
- `oci vault secret create-base64 --secret-name autoforge-opencode-server-password` - OpenCode REST password secret created
- `oci vault secret get --secret-id <autoforge-opencode-server-password>` - ACTIVE
- `bash -n infra/deploy/private/deploy.sh` - ok
- `git diff --check` - ok
- `gh run view 26242615861` - private deploy containers started but smoke test failed: `OPENCODE_SERVER_USERNAME: OPENCODE_SERVER_USERNAME is required`
- Root cause: `deploy.sh` writes secrets to `RUNTIME_ENV` temp file (used as compose `--env-file`), but smoke test runs without sourcing that file, so shell env vars are missing
- Fix: source `RUNTIME_ENV` with `set -a` before running `opencode-smoke.sh`
- `bash -n infra/deploy/private/deploy.sh` - ok
- `git diff --check` - ok
