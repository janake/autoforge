# [AUTO-221] React dashboard és navigációs shell

## Feladat leírása

A frontend kapjon egységes dashboard shellt és navigációs keretet, amely a public és authenticated állapotot is ugyanabban az alkalmazásvázban kezeli.

## Statusz

review

## Verzió

0.1.39

## Branch

feature/auto-221-react-dashboard-shell

## PR

- pending

## Cél

A felület legyen dashboard-szerű, és későbbi job/Jira/PR nézetekhez stabil navigációs alapot adjon.

## Scope

- top navigation shell
- section anchorok
- public/auth/error state egységes keretben
- mobilbarát layout

## Elfogadási kritériumok

- A frontend rendelkezik egységes dashboard shelllel.
- A public és authenticated nézet ugyanazon navigációs kereten jelenik meg.
- A layout mobilon is használható.
- Nincsenek route- vagy state-regressziók.

## Lepesnaplo

- [x] Áttekintettem az `AUTO-221` scope-ját az order manifest alapján.
- [x] Bevezettem a dashboard shellt és a navigációs anchorokat.
- [x] Frissítettem a responsive stílusokat.

## Eredmény

A web UI kapott egy egységes navigációs keretet, amelyre később a job és Jira nézetek épülhetnek.
