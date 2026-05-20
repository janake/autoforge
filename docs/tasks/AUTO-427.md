# [AUTO-427] Learning assignment audit log

Statusz: In Progress

Verzio: 0.1.68

Branch: `feature/AUTO-427-assignment-audit-log`

PR: 

## Cél

A tananyag-hozzárendelések create/update/delete változásaihoz audit trail készüljön, és a tanár a saját tananyagán lássa ezeket az eseményeket.

## Scope

- assignment audit entitás és repository
- assignment create/update/delete audit mentés
- teacher-only audit lista endpoint
- teacher detail UI audit megjelenítés
- backend és frontend regression tesztek

## Lepesnaplo

- `jira_transition_issue(AUTO-427, In Progress)` - sprint ticket indítva
- `git worktree add -b feature/AUTO-427-assignment-audit-log ... origin/main` - dedikalt worktree letrehozva
- `services/backend/.../LearningAssignmentAudit*` - audit modell és service elkezdve

## Megjegyzes

A target version 0.1.68 a learning audit slice-hoz van rendelve.
