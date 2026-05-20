# [AUTO-374] Egy- és többhelyes válaszos kérdések AI promptban és pontozásban

Statusz: In Progress

Verzio: 0.1.61

Branch: `feature/AUTO-374-multi-correct`

PR: pending

## Cél

A Learning kérdésgenerálás támogassa a single-correct és multi-correct kérdéseket, a backend pedig pontos halmazalapú pontozást végezzen multi-correct válaszoknál.

## Scope

- question payload answer type és correct answer indexek
- multi-correct fallback kérdések és prompt szöveg
- set-alapú pontozás a question attempt service-ben
- web UI jelzi, hány választ kell kiválasztani
- regression tesztek és release traceability

## Lepesnaplo

- `jira_get_issue(AUTO-374)` - story scope lekérdezve Jira MCP-n keresztül
- `git worktree add -b feature/AUTO-374-multi-correct ... origin/main` - dedikált worktree létrehozása

## Megjegyzés

A PR és a Jira lezárás még hátravan.
