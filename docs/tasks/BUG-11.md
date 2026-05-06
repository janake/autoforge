# BUG-11 Private deploy trigger misses version changes

Feladat leírása
A publikus gateway 502-t ad `/api/**` hívásokra, mert BUG-10 után a public deploy már lefutott, de a private backend deploy nem követte a verzióemelést. A `Deploy Private Host` workflow path filtere nem tartalmazta a root `package.json` és `package-lock.json` fájlokat, ezért verzió/tag változásra nem indult el.

Statusz
- in progress

Verzió
- `0.1.6`

Branch
- `bug/BUG-11-private-deploy-version-trigger`

PR
-

Acceptance criteria
- A private deploy workflow lefut `package.json` változásra.
- A private deploy workflow lefut `package-lock.json` változásra.
- Verzióemelés után a backend image tag deployolódik a private hostra.
- A public gateway `/api/**` útvonala nem 502-t ad backend elérhetetlenség miatt.

Dokumentumok és fájlok
- `.github/workflows/deploy-private.yml`
- `docs/releases.md`
- `docs/tasks/BUG-11.md`
- `package.json`
- `package-lock.json`

Lepesnaplo
1. Ellenőriztem, hogy BUG-10 után a Deploy Public Host lefutott, de Deploy Private Host nem.
2. Megtaláltam, hogy a private deploy path filterből hiányzik a root verzió fájlok listája.
3. Hozzáadtam a `package.json` és `package-lock.json` pathokat.
4. Emeltem a projektverziót `0.1.6`-ra.

Eredmény
- A private deploy workflow verzió/tag változásra is elindul.
