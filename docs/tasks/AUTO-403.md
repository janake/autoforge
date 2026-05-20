# [AUTO-403] Assignment és próbálkozás státuszok

Statusz: In Progress

Verzio: 0.1.63

Branch: `feature/AUTO-403-assignment-statuses`

PR: pending

## Cél

A Learning modulban a tananyaghoz és kérdéssorhoz tartozó haladás legyen követhető assigned, started, submitted, reviewed és completed állapotokkal.

## Scope

- dedikált `LearningQuestionProgress` entity és repository
- státusz frissítés question-set megnyitás, beadás és review után
- progress állapotok megjelenítése a material detail nézetben
- backend és frontend regression tesztek
- release manifest és task traceability frissítés

## Döntés

- Progress entitást vezetünk be külön az assignment és az attempt mellett.
- Az assignment marad a hozzárendelés, az attempt az auditált beadás, a progress pedig az állapotgép és a későbbi spaced-repetition alap.

## Lepesnaplo

- `jira_transition_issue(AUTO-403, In Progress)` - Jira státusz állítás
- `jira_add_comment(AUTO-403, progress entity döntés)` - döntés rögzítve Jira kommentben
- `git worktree add -b feature/AUTO-403-assignment-statuses ... origin/main` - dedikált worktree létrehozása

## Megjegyzés

A progress modell és a kapcsolódó UI/API frissítés folyamatban van.
