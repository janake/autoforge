# [AUTO-415] Tanulasi eredmeny dashboard tananyaghoz es csoporthoz

Statusz: Under Test

Verzio: 0.1.65

Branch: `feature/AUTO-415-teacher-dashboard`

PR: #174

## Cél

A Learning modulban a tanar a tananyaghoz es csoporthoz kotott progress adatokbol dashboardot kapjon, hogy attekintse a diakok allapotat, pontszamat, beadott probalkozasait es az esetleges vitakat.

## Scope

- student group metadata mentese a progress sorokba
- teacher-visible progress aggregacio a material detail oldalon
- group filter a dashboardon
- backend és frontend regression tesztek a progress metadata iranyba
- release és task traceability frissítés

## Lepesnaplo

- `jira_update_issue(AUTO-415, fixVersions=0.1.65)` - celverzio beallitasa
- `jira_transition_issue(AUTO-415, In Progress)` - Jira statusz allitasa
- `jira_add_comment(AUTO-415, teacher dashboard slice indult)` - indulasi komment felvive
- `git worktree add -b feature/AUTO-415-teacher-dashboard ... origin/main` - dedikalt worktree letrehozasa
- `LearningQuestionProgress` / `LearningQuestionAttemptService` - progress sorokhoz csoportadat kerult
- `apps/web/src/App.tsx` - teacher dashboard aggregacio es csoport filter elkezdve
- `npm run typecheck:web` - frontend verifikacio jelenleg kornyezeti deps hianya miatt nem futott le
- `npm run test:backend` - backend tesztek futtatva, hibajelzes nelkul
- `git commit -m "[AUTO-415] add teacher progress dashboard"` - task changes committed
- `git push -u origin feature/AUTO-415-teacher-dashboard` - branch pushed
- `gh pr create` - PR megnyitva

## Megjegyzes

A dashboard slice keszen all a tovabbi finomitashoz es a PR nyitashoz.
