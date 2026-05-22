# [AUTO-478] Spike lightweight AI communication alternatives to OpenCode runtime

Statusz: Under Review

Verzio: 0.1.81

Branch: `feature/AUTO-478-ai-communication-spike`

PR: #202

## Cel

El kell donteni, mivel kommunikáljon az Autoforge az AI-val, ha a private szerveren az OpenCode runtime tul sok eroforrast fogyaszt.

## Scope

- jelenlegi OpenCode/OpenRouter/provider-proxy utvonal feltarasa
- alternativ AI kommunikacios mintak osszehasonlitasa
- ajanlott irany es kovetkezo implementacios taskok meghatarozasa

## Lepesnaplo

- `jira_create_issue(AUTO-478)` - uj Spike letrehozva
- `jira_update_issue(AUTO-478, fixVersion=0.1.81)` - MCP override hiba miatt kozvetlen Jira REST fallbackkel beallitva
- `jira_transition_issue(AUTO-478, In Progress)` - Jira statusz allitasa
- `git worktree add -b feature/AUTO-478-ai-communication-spike ... origin/main` - dedikalt worktree/branch letrehozva
- `task(subagent_type=explore)` - jelenlegi AI runtime es alternativak feltarasa
- `git diff --check` - ok
- `git commit -m "[AUTO-478] Spike AI communication alternatives"` - spike docs commitolva
- `git push -u origin feature/AUTO-478-ai-communication-spike` - branch feltolva
- `gh pr create` - PR #202 megnyitva
- `jira_transition_issue(AUTO-478, Under review)` - Jira review statusz beallitva
- `jira_add_comment(AUTO-478)` - PR, branch, verzio es verification komment felteve

## Eredmeny roviden

A javasolt irany: backend -> provider-proxy -> OpenRouter. Ez kiveszi az OpenCode kontenert a private hostrol, de megtartja az OpenRouter API key izolaciojat es a proxy guardrail-eket.

## Megjegyzes

Ez Spike, ezert a javaslat meg nem zar le implementacios dontest merge/review nelkul.
