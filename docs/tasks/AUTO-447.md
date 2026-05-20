# [AUTO-447] Jira MCP times out on repeated secret resolution

Statusz: Under Test

Verzio: 0.1.69

Branch: `bug/AUTO-447-jira-mcp-secret-cache`

PR: https://github.com/janake/autoforge/pull/179

## Cél

A Jira MCP ne timeoutoljon több Jira REST hívást végző műveleteknél azért, mert minden request újra feloldja az OCI Vault-backed Jira credentialöket.

## Scope

- Jira secret cache processzen belül
- Jira auth/config cache processzen belül
- MCP szintaktikai validáció
- sprint státusz-rendezés folytatása a javított MCP-vel

## Lepesnaplo

- `jira_create_issue(AUTO-447)` - bug létrehozva
- `jira_transition_issue(AUTO-447, In Progress)` - munka indítva
- `ops/mcp/jira-server.mjs` - secret/config cache hozzáadva
- `node --check ops/mcp/jira-server.mjs` - szintaktikai ellenőrzés ok
- `node ops/mcp/call-jira-tool.mjs jira_list_transitions ...` - Jira MCP validáció ok
- sprint státusz-rendezés sikeresen lefutott a javított MCP-vel
- `gh pr create --title "[AUTO-447] Cache Jira MCP secrets"` - PR megnyitva

## Megjegyzes

A hiba a Jira workflow transition batch közben jelentkezett: az ismételt secret feloldás túl lassúvá tette a Jira MCP tool hívásokat.
