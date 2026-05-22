# [AUTO-480] Replace OpenCode AI runtime with lightweight provider-proxy client

Statusz: Under Test

Verzio: 0.1.83

Branch: `feature/AUTO-480-provider-proxy-ai-client`

PR: #204

## Cel

Az Autoforge backend kozvetlenul a konnyu OpenAI-kompatibilis provider proxyt hasznalja AI valaszokhoz, hogy a private hoston ne kelljen OpenCode runtime-ot futtatni.

## Scope

- backend AI kliens atallitasa `AI_PROVIDER_PROXY_BASE_URL` es `AI_PROVIDER_MODEL` konfiguraciora
- patch generation, prompt clarification es learning generation provider-proxy chat completions utvonalra terelese
- private compose/deploy workflow OpenCode service, config es smoke eltavolitasa
- provider-proxy smoke teszt bevezetese
- runtime, deployment es release dokumentacio frissitese

## Lepesnaplo

- `jira_create_issue(AUTO-480)` - uj story letrehozva
- `jira_transition_issue(AUTO-480, In Progress)` - Jira statusz allitasa
- `git worktree add -b feature/AUTO-480-provider-proxy-ai-client ... origin/main` - dedikalt worktree/branch letrehozva
- `task(subagent_type=explore)` - AI runtime erintett backend/deploy pontok feltarasa
- `mvn -q -f services/backend/pom.xml -Dtest=HttpProviderProxyAIPatchGeneratorTest,HttpPromptDraftClarifierTest test` - GREEN: backend provider-proxy chat utvonal
- `mvn -q -f services/backend/pom.xml -Dtest=HttpProviderProxyAIPatchGeneratorTest,HttpPromptDraftClarifierTest,PromptDraftFallbackMessageTest test` - ok
- `npm run test:provider-proxy` - ok
- `bash -n infra/deploy/private/deploy.sh && bash -n infra/deploy/private/provider-proxy-smoke.sh` - ok
- `npm run test:backend` - ok
- `git diff --check` - ok
- `docker-compose -f infra/compose/docker-compose.private.yml --profile ai config` - ok, placeholder env-ekkel es `AI_PROVIDER_PROXY_BASE_URL=http://openrouter-proxy:8080/v1`-gyel validalt compose render
- `git commit -m "[AUTO-480] Replace OpenCode runtime with provider proxy client"` - implementacio commitolva
- `git push -u origin feature/AUTO-480-provider-proxy-ai-client` - branch feltolva
- `gh pr create` - PR #204 megnyitva

## Megjegyzes

A provider API kulcs tovabbra sem kerul a backendbe; az OCI Vaultbol feloldott OpenRouter kulcsot csak a private provider proxy kapja meg runtime env-kent.
