# AUTO-15 Landing page cleanup

Feladat leírása
A publikus landing oldalról el kell távolítani a felesleges "Public entry" info panelt. A landing célja egy tiszta belépési felület: hero tartalom, Sign in gomb, és minimális identity provider információ.

Statusz
- in progress

Branch
- `feature/AUTO-15-landing-cleanup`

PR
- https://github.com/janake/autoforge/pull/26

Acceptance criteria
- A publikus landing oldal nem tartalmaz "Public entry" panelt.
- A landing oldal auth nélkül betöltődik.
- A Sign in gomb továbbra is Keycloak loginra visz.
- A frontend build sikeres.

Dokumentumok és fájlok
- `apps/web/src/App.tsx`
- `docs/tasks/AUTO-15.md`

Lepesnaplo
1. Azonosítottam, hogy a változtatás nem bug, hanem landing page UI cleanup.
2. A feladatot `AUTO-15` story-ként dokumentáltam a repo eddigi task naming konvenciója szerint.
3. A kapcsolódó branch és PR `AUTO-15` névre lesz átvezetve.

Eredmény
- A "Public entry" panel nincs a publikus landing oldalon.
