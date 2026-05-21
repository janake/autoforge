# [AUTO-476] Limit memory usage of private compose services

Statusz: In progress

Verzio: 0.1.80

Branch: `feature/AUTO-476-compose-memory-limits`

PR: pending

## Cel

A private compose stack minden service-e kapjon memory cap-et, hogy a 1 GB-os private hoston egyik kontener se tudjon elszabadulni startup vagy runtime kozben.

## Scope

- `infra/compose/docker-compose.private.yml`: per-service memory limitek
- `docs/deployment.md`: private stack memory cap dokumentacio
- `docs/releases.md`: release manifest frissites

## Lepesnaplo

- `jira_create_issue(AUTO-476)` - uj task letrehozva
- `jira_transition_issue(AUTO-476, In Progress)` - munka inditva
- `jira_add_issues_to_sprint(102)` - Learning RAG Foundation sprinthez adva
- `git worktree add feature/AUTO-476-compose-memory-limits` - dedikalt worktree/branch letrehozva
- `oci compute instance get` - private host: `VM.Standard.E2.1.Micro`, 1.0 OCPU, 1.0 GB RAM
- `infra/compose/docker-compose.private.yml` - backend/opencode/openrouter-proxy memory cap-ek hozzaadva, env override lehetoseggel
- `docs/deployment.md` - memory cap dokumentacio frissitve
- `docs/releases.md` - AUTO-476 felvetele a 0.1.80 release manifestbe
