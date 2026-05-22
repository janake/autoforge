# [AUTO-474] Prompt draft flow cannot complete because AI clarification runtime is unavailable

Statusz: Under test

Verzio: 0.1.79

Branch: `bug/AUTO-474-enable-prompt-ai-runtime`, `bug/AUTO-474-openrouter-secret-name`, `bug/AUTO-474-opencode-smoke-env`, `bug/AUTO-474-ssh-keepalive`, `bug/AUTO-474-opencode-message-model`, `bug/AUTO-474-batch-private-upload`, `bug/AUTO-474-upload-retry`, `bug/AUTO-474-opencode-max-tokens`

PR: https://github.com/janake/autoforge/pull/190, https://github.com/janake/autoforge/pull/191, https://github.com/janake/autoforge/pull/192, https://github.com/janake/autoforge/pull/193, https://github.com/janake/autoforge/pull/197, https://github.com/janake/autoforge/pull/198

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
- `gh run view 26243386952` - private deploy SSH broken pipe: `client_loop: send disconnect: Broken pipe` (exit code 255)
- Root cause: SSH connection has no keepalive; OCI Vault lookups take 4+ minutes without output, connection drops
- Fix: add `ServerAliveInterval 60` and `ServerAliveCountMax 5` to SSH config for `autoforge-private` host
- `bash -n .github/workflows/deploy-private.yml` - ok
- `git diff --check` - ok
- `gh run view 26249778790 --job 77259980085 --log-failed` - private deploy starts containers, then `opencode-smoke.sh` fails on `/session/$session_id/message` with HTTP 400
- `context7 /anomalyco/opencode` - current `/session/:id/message` API expects `model` as `{ providerID, modelID }`
- Root cause: current OpenCode REST API expects `model` as `{ providerID, modelID }`, but the smoke script sent `OPENCODE_MODEL` as a single string
- Fix: split `OPENCODE_MODEL` at the first `/` for the smoke message payload and print the OpenCode HTTP response body on non-2xx message responses
- Direct OpenRouter probe: current `deepseek/deepseek-v4-flash:free` returned upstream 429 during investigation; if this remains, deploy will surface a provider rate-limit body instead of a bare curl 400
- `bash -n infra/deploy/private/opencode-smoke.sh` - ok
- `git diff --check` - ok
- `gh run view 26252435050 --job 77267401295 --log-failed` - private deploy failed before smoke in `Upload private stack files` with `Connection closed by UNKNOWN port 65535`
- Root cause: the upload step opened repeated SCP connections through the jump host, so transient SSH/proxy instability could fail before deploy reached the host
- Fix: use native `ProxyJump`, add keepalives to the jump host entry, and upload the private stack files as one tar stream over a single SSH connection
- `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/deploy-private.yml')); print('YAML OK')"` - ok
- `tar -czf - --transform='s|.*/||' ... | tar -tzf -` - ok, archive contains the expected flat deploy filenames
- `git diff --check` - ok
- `gh run view 26253049075 --job 77267401295 --log-failed` - even with ProxyJump + keepalives, upload still fails: `Connection closed by UNKNOWN port 65535` after ~2 min, exit code 255
- Root cause: transient SSH connection failure through ProxyJump; tar pipe is not retried
- Fix: wrap tar-over-SSH upload in bash retry loop (3 attempts, 10s delay), add explicit per-command `-o ServerAliveInterval=30 -o ServerAliveCountMax=3` on the upload ssh invocation, and tighten the SSH config keepalive interval from 60s to 30s
- `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/deploy-private.yml')); print('YAML OK')"` - ok
- `bash -n .github/workflows/deploy-private.yml` - ok
- `git diff --check` - ok
- `gh run view 26255891186 --job 77278347552 --log-failed` - upload succeeded, deploy reached OpenCode smoke, then OpenRouter rejected the smoke completion with `max_tokens exceeds configured limit of 2048`
- User clarified that the previous Qwen choice was wrong because the relevant path is still a free deployment path, not the local Zen login
- OpenRouter model catalog check: `google/gemma-4-26b-a4b-it:free` is `text+image+video->text` with prompt/completion pricing `0` and max completion tokens 32768; it is an actually free multimodal option
- Fix: make `google/gemma-4-26b-a4b-it:free` the OpenCode/OpenRouter default and allowed model; raise default proxy completion limit to 32768 and request body limit to 10485760 bytes for image-capable payloads
- `python3 -c "import json; json.load(open('infra/compose/opencode.json')); print('JSON OK')"` - ok
- `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/deploy-private.yml')); yaml.safe_load(open('infra/compose/docker-compose.private.yml')); print('YAML OK')"` - ok
- `bash -n infra/deploy/private/opencode-smoke.sh` - ok
- `npm run test:provider-proxy` - ok
- `mvn -q -f services/backend/pom.xml -Dtest=HttpPromptDraftClarifierTest,HttpOpenCodeAIPatchGeneratorTest,PromptDraftFallbackMessageTest test` - ok
- `npm run test:backend` - failed in existing unrelated `LearningContentGenerationControllerTest.questionSetSettingsPersistAndLimitAttempts` with expected 201 but got 403
- `git diff --check` - ok
