# Branching

## Cél

Az Autoforge fejlesztési ágai két csoportra oszlanak:

- `feature/<id>`: új funkciók, scaffoldok, bővítések
- `bug/<id>`: hibajavítások, workflow-fixek, regressziók

## Szabályok

- A branch név tartalmazza az azonosítót.
- A commit üzenet kezdődjön az azonosítóval.
- A task dokumentum a `docs/tasks/` alatt él.
- A bug ticketek neve `BUG-<szám>`, a feature ticketek neve `AUTO-<szám>`.

## CI/CD viselkedés

- `feature/*` és `bug/*` ágakon a CI fut.
- `main` ágra merge után a CD/deploy fut.
- Deploy workflow csak `main` pushra fusson.
- Ha workflowt módosítunk, a branch policy dokumentációt is frissíteni kell.

## Megjegyzés

- Ha egy hiba a saját munkám vagy a workflow miatt jön elő, arra külön `BUG-<szám>` ticketet kell felvenni.
