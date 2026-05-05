# BUG-1: CI/CD branch policy es workflow stabilitas

Feladat leírása
A branch naming konvenciót feature/bug alapu rendszerré kell alakítani, és a CI/CD workflow-kat úgy kell beállítani, hogy a `feature/*` és `bug/*` ágakon a CI fusson, a deploy pedig továbbra is csak `main` merge után induljon.

Statusz
- in progress

Branch
- `bug/BUG-1`

PR
- pending

Acceptance criteria
- A dokumentáció leírja a `feature/<id>` és `bug/<id>` branch szabályt.
- A CI workflow-k `feature/*` és `bug/*` ágakon is futnak.
- A deploy workflow-k csak `main` pushra futnak.
- A bug ticketek külön `BUG-<szám>` azonosítót használnak.

Dokumentumok és fájlok
- `docs/branching.md`
- `docs/tasks/README.md`
- `docs/tasks/BUG-1.md`
- `README.md`
- `.github/workflows/*.yml`

Biztonsági megfontolások
- Nincs szabad helye tokennek, jelszónak, private key-nek vagy személyes azonosítónak.
- A branch név és a ticket név csak azonosítót tartalmazhat, érzékeny adatot nem.

Lepesnaplo
1. Ellenőriztem a jelenlegi workflow trigger szabályokat.
2. Rögzítettem a feature/bug branch policy-t a dokumentációban.
3. Beállítottam a CI workflow-kat, hogy feature és bug ágakon is fussanak.

Eredmény
- A branch policy és a CI/CD viselkedés dokumentált és a workflow-khoz igazított.
