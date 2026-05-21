# [AUTO-475] Replace SCP image preload with direct docker pull on private host

Statusz: Under test

Verzio: 0.1.80

Branch: `feature/AUTO-475-direct-pull`

PR: https://github.com/janake/autoforge/pull/195

## Cel

A private host kozvetlenul pullolja a Docker imageket a GHCR-bol `docker compose pull` segitsegevel, ahelyett hogy a GitHub runner toltené le, tar.gz-be mentené, SCP-vel feltöltené, majd a private host docker load-olná.

## Scope

- `deploy-private.yml`: remove `Prepare private images` and `Upload private images` steps
- `deploy-private.yml`: change `Upload registry credentials` to write `GHCR_USERNAME`/`GHCR_TOKEN`
- `deploy.sh`: add safe `export_env_file()` helper for `RUNTIME_ENV` values
- `deploy.sh`: export runtime env safely before compose/pull/smoke test
- release manifest es task doc frissites

## Lepesnaplo

- `jira_create_issue(AUTO-475)` - uj task letrehozva
- `jira_transition_issue(AUTO-475, In Progress)` - munka inditva
- `jira_add_issues_to_sprint(sprint=102)` - Learning RAG Foundation sprinthez adva
- `git worktree add feature/AUTO-475-direct-pull` - dedikalt worktree/branch letrehozva
- `oci network route-table get` - private subnet route OK, NAT instance `prodet-new-e2-public-01` (10.42.0.241)
- `oci compute instance list` - private host `prodet-new-e2-private-03` (10.42.1.144) eleri az internetet (apt update mukodik)
- `deploy-private.yml` - removed `Prepare private images` step (55 lines)
- `deploy-private.yml` - removed `Upload private images` step (SCP of tar.gz)
- `deploy-private.yml` - changed `Upload registry credentials` to write GHCR_USERNAME/GHCR_TOKEN
- `deploy.sh` - added safe `export_env_file()` helper for env-file parsing
- `deploy.sh` - exported runtime env safely before compose/pull/smoke test
- `deploy.sh` - removed unsafe `source` of `RUNTIME_ENV` (broke on `JAVA_TOOL_OPTIONS` with spaces)
- `python3 -c "import yaml; yaml.safe_load(...)"` - valid YAML
- `bash -n infra/deploy/private/deploy.sh` - ok
- `git diff --check` - ok
- `git commit` - `[AUTO-475] Replace SCP image preload with direct docker pull on private host`
- `gh pr create #195` - https://github.com/janake/autoforge/pull/195
