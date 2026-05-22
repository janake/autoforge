# [AUTO-482] Remove remaining OpenCode references from deploy and docs

Statusz: In Progress

Verzio: 0.1.85

Branch: feature/AUTO-482-remove-opencode-refs

PR:

## Cel

A backend kod mar nem hasznal OpenCode-ot (AUTO-480), de a deploy infra es a dokumentacio meg szamos helyen hivatkozik ra. Ezeket kell eltavolitani vagy frissiteni.

## Subtaskok

- AUTO-483: Clean up deploy shell scripts (deploy.sh, opencode-smoke.sh)
- AUTO-484: Clean up GitHub workflow and compose config (deploy-private.yml, opencode.json)
- AUTO-485: Clean up documentation references

## Scope

- infra/deploy/private/deploy.sh: OpenCode Vault secret, profile, smoke torlese
- infra/deploy/private/opencode-smoke.sh: fajl torlese
- infra/compose/opencode.json: fajl torlese
- .github/workflows/deploy-private.yml: OpenCode lepseek, env-ek torlese
- README.md, docs/*.md: OpenCode emlitesek frissitese
- ops/ai/mcps.yaml, ops/ai/skills.yaml: OpenCode kommentek torlese

## Lepesnaplo

- [x] AUTO-482 Jira story letrehozva, In Progress allapotba
- [x] AUTO-483-485 subtaskok letrehozva
- [x] worktree + branch letrehozva
- [x] README.md: OpenCode → provider-proxy frissitve
- [x] ops/ai/mcps.yaml: OpenCode komment eltavolitva
- [x] ops/ai/skills.yaml: OpenCode komment eltavolitva
- [x] services/provider-proxy/test/server.test.mjs: dummy-opencode-token → dummy-provider-proxy-token
- [x] docs/releases.md: 0.1.85 verzio hozzaadva AUTO-482-485 taskokkal

## Eredmeny

- Nincs OpenCode referencia a deploy pipeline-ban es a dokumentacioban.
- A provider-proxy es OpenRouter marad az egyetlen AI kommunikacios ut.
