# [AUTO-270] Jira-first docs cleanup

## Feladat leírása

Távolítsuk el a helyi task- és epic dokumentumokat, és tartsuk meg csak az order/releases manifesteket, hogy a Jira legyen a forrás of truth.

## Statusz

review

## Verzió

0.1.41

## Branch

feature/auto-270-jira-docs-cleanup

## PR

- pending

## Cél

A repo docs oldala a Jira-first működéshez igazodjon: a részletes lokális task/epic fájlok eltűnjenek, a manifestek pedig maradjanak karbantartva.

## Scope

- local task markdownok törlése
- local epic markdownok törlése
- order manifest tisztítása
- release manifest duplikátumok eltávolítása

## Elfogadási kritériumok

- A `docs/tasks/*.md` részletes task fájlok eltűnnek.
- A `docs/epics/*.md` fájlok eltűnnek.
- `docs/order.md` konzisztens státuszokat mutat.
- `docs/releases.md` nem tartalmaz duplikált blokkot.

## Lepesnaplo

- [x] Áttekintettem a Jira MCP-n keresztül a merged PR-ek státuszát.
- [x] A lokális task/epic artifactok törlésre kerültek a worktree-ben.
- [x] Az order és release manifestet a jelenlegi Jira state-hez igazítottam.

## Eredmény

A repo már nem támaszkodik a régi lokális task/epic markdownokra, csak a manifestekre és a Jira állapotra.
