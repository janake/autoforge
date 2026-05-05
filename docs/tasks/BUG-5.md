# BUG-5 Keycloak fragment callback handling

Feladat leírása
Login utan a Keycloak callback a URL fragmentben erkezik (`#state=...&code=...`), de a frontend csak a query stringet vizsgalta. Emiatt a callback nem kerult feldolgozasra, a felulet public allapotban maradt, a `Sign in` gomb tovabbra is latszott, es a logout csak a private workspace-ben megjeleno allapot helyett nem jelent meg.

Statusz
- in progress

Verzió
- `0.1.1`

Branch
- `bug/BUG-5-keycloak-fragment-callback`

PR
- pending

Acceptance criteria
- A frontend felismeri a Keycloak callbacket query stringbol es hash fragmentbol is.
- Login utan az app betolti a private workspace-et.
- Login utan a public `Sign in` gomb nem marad kint.
- A private workspace-ben elerheto marad a `Sign out` gomb.
- A callback URL-bol a Keycloak adapter eltavolitja az auth parametereket feldolgozas utan.
- A frontend build sikeres.

Dokumentumok és fájlok
- `apps/web/src/App.tsx`
- `docs/releases.md`
- `docs/tasks/BUG-5.md`

Lepesnaplo
1. Azonositottam, hogy a visszatero URL `#state=...&code=...` hash fragmentet hasznal.
2. Ellenoriztem, hogy a frontend csak `window.location.search` alapjan detektalta az auth callbacket.
3. Hozzaadtam a hash fragment callback felismereset.
4. Lefuttattam a frontend buildet: `npm run build:web` sikeres.

Eredmény
- A frontend mar query es hash callback formatumot is felismer.
