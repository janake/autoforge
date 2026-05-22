# [AUTO-477] Token economy guidance for global agent rules

Statusz: Under Review

Verzio: 0.1.16

Branch: `feature/AUTO-477-token-economy`

PR: #201

## Cél

Az AGENT.md-ben legyenek rovid, token-takarekos munkastilus iranyelvek, hogy minden AI session kevesebb tokent hasznaljon.

## Scope

- AGENT.md token economy szekcio
- release manifest frissites
- task dokumentacio letrehozas

## Lepesnaplo

- `jira_create_issue(AUTO-477)` - uj task letrehozva
- `git worktree add -b feature/AUTO-477-token-economy ... origin/main` - dedikalt worktree/branch letrehozva
- `jira_transition_issue(AUTO-477, In Progress)` - Jira statusz allitasa
- `AGENT.md` - token economy szekcio hozzaadasa
- `docs/releases.md` - 0.1.16 release entry frissitese
- `git diff --check` - ok
- `git commit -m "[AUTO-477] Add token economy guidance to agent rules"` - elso commit kesz
- `git push -u origin feature/AUTO-477-token-economy` - branch feltolva
- `gh pr create` - PR megnyitva

## Megjegyzes

A valtozas repo-szintu instrukciokat erint, kodot nem.
