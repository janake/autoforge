# [AUTO-446] Jira MCP fixVersion support

Statusz: Under Review

Verzio: 0.1.67

Branch: `feature/AUTO-446-jira-fixversion-support`

PR: #177

## Cél

A Jira MCP szerver kezelje a `fixVersion`/`fixVersions` shorthandot, es a hianyzo Jira verziokat automatikusan hozza letre.

## Scope

- Jira issue create/update fixVersion normalizalas
- Jira projekt verzio feloldas és on-demand version create
- MCP schema dokumentacio frissites
- release manifest traceability frissites

## Lepesnaplo

- `jira_create_issue(AUTO-446)` - uj task letrehozva
- `git worktree add -b feature/AUTO-446-jira-fixversion-support ... origin/main` - dedikalt worktree letrehozva
- `ops/mcp/jira-server.mjs` - fixVersion feloldas es version auto-create bevezetve
- `node --check ops/mcp/jira-server.mjs` - szintaktikai ellenorzes ok
- `node ops/mcp/call-jira-tool.mjs jira_update_issue ...` - local MCP teszt, Jira screen security miatt existing issue update blokkolva

## Megjegyzes

A `fixVersion` shorthand create oldalon mar normalizalodik; existing issue update csak addig mukodik, ameddig a Jira edit screen engedi a fieldet.
