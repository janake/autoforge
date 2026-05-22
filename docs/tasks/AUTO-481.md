# [AUTO-481] Provider proxy smoke should tolerate upstream rate limits

Statusz: In Progress

Verzio: 0.1.84

Branch: `bug/AUTO-481-provider-proxy-smoke-rate-limit`

PR: TBD

## Cel

A private deploy ne bukjon el akkor, ha a provider-proxy health es model ellenorzes sikeres, de az OpenRouter upstream free modell transient `429` rate limitet ad a live completion smoke kozben.

## Scope

- `provider-proxy-smoke.sh` upstream `429` kezelesenek non-fatal agat hozzaadni
- runtime/deployment docs frissitese a rate-limit viselkedesrol
- release manifest frissitese

## Lepesnaplo

- `gh run view 26306576260 --log-failed` - main deploy failure oka: provider-proxy smoke prompt HTTP 429 upstream rate limit
- `jira_create_issue(AUTO-481)` - uj Bug letrehozva
- `jira_transition_issue(AUTO-481, In Progress)` - Jira statusz allitasa
- `git worktree add -b bug/AUTO-481-provider-proxy-smoke-rate-limit ... origin/main` - dedikalt worktree/branch letrehozva
- `PATH=/tmp/opencode/fake-docker-bin:$PATH AI_PROVIDER_MODEL=google/gemma-4-26b-a4b-it:free bash infra/deploy/private/provider-proxy-smoke.sh` - ok, szimulalt upstream HTTP 429 non-fatal ag
- `bash -n infra/deploy/private/provider-proxy-smoke.sh && git diff --check` - ok
- `docker-compose -f infra/compose/docker-compose.private.yml --profile ai config` - ok, placeholder env-ekkel validalt compose render

## Megjegyzes

A health/model ellenorzes tovabbra is kotelezo. Csak az upstream `429` live completion rate limit nem fatalis, mert az nem deployment wiring hiba.
