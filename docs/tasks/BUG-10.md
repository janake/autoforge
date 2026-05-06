# BUG-10 Public deploy trigger misses gateway image changes

Feladat leírása
BUG-9 javította a gateway route konfiguráció becsomagolását, de a merge után csak a container image workflow futott le. A `Deploy Public Host` workflow nem indult el, mert a path filter nem tartalmazta az `infra/gateway/**`, `package.json`, és `package-lock.json` változásokat. Emiatt az új gateway image elkészült, de nem került kideployolásra, így a publikus `/api/v1/me` továbbra is 404-et adott.

Statusz
- in progress

Verzió
- `0.1.5`

Branch
- `bug/BUG-10-public-deploy-trigger`

PR
- PR #39

Acceptance criteria
- A public deploy workflow lefut `infra/gateway/**` változásra.
- A public deploy workflow lefut root verzió (`package.json`, `package-lock.json`) változásra.
- Gateway image változás merge után automatikusan kikerül a public hostra.

Dokumentumok és fájlok
- `.github/workflows/deploy-public.yml`
- `docs/releases.md`
- `docs/tasks/BUG-10.md`
- `package.json`
- `package-lock.json`

Lepesnaplo
1. Ellenőriztem, hogy BUG-9 után a Container Images workflow zöld volt, de a Deploy Public Host nem futott.
2. Megtaláltam, hogy a deploy-public workflow path filtere nem tartalmaz gateway és verzió fájlokat.
3. Hozzáadtam az `infra/gateway/**`, `package.json`, és `package-lock.json` pathokat.
4. Emeltem a projektverziót `0.1.5`-re.

Eredmény
- A public deploy workflow gateway image és verzió változásra is elindul.
