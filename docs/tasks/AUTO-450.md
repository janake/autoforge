# [AUTO-450] Document PR body format in global agent rules

Statusz: Under test

Verzio: 0.1.71

Branch: `feature/AUTO-450-pr-body-format`

PR: https://github.com/janake/autoforge/pull/181

## Cél

Az `AGENT.md` tartalmazzon kötelezo PR body sablont, hogy minden sessionben latszodjon a helyes forma.

## Scope

- global agent rules PR body template
- project readme emlekezteto
- release manifest frissites

## Lepesnaplo

- `jira_create_issue(AUTO-450)` - uj task letrehozva
- `git worktree add feature/AUTO-450-pr-body-format` - dedikalt worktree/branch letrehozva
- `git diff --check` - ok
- `gh pr create` - PR megnyitva
- `gh api ... body="$body"` - PR body ujrairva valodi multiline Markdown tartalommal
- globalis `/home/janake/.config/opencode/AGENT.md` - minden uj opencode sessionre kiterjesztve
