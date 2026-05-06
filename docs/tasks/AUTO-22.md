# AUTO-22 Documentation and architecture refresh

Feladat leírása
A projekt dokumentációjának több része elavult maradt a BUG-9 .. BUG-17 hibajavítási kör után. A fő README, az architektúra leírás, a deploy dokumentáció, az auth dokumentáció és az SVG architektúra-ábra jelenleg részben még előkészített/scaffoldolt állapotot ír le, nem a mostani live OCI topológiát, verziózott image/deploy modellt és JWT-validációs működést.

Statusz
- in progress

Verzió
- `0.1.13`

Branch
- `feature/AUTO-22-docs-refresh`

PR
-

Acceptance criteria
- A `README.md` a jelenlegi live platformállapotot írja le, nem előkészített/scaffoldolt jövőidőben.
- A `docs/architecture.md` és az SVG ábra összhangban van a public/private OCI topológiával és a jelenlegi auth/deploy modellel.
- A `docs/deployment.md` leírja a verziózott image-eket, az auto deploy triggert és a jelenlegi secret/runtime modellt.
- A `docs/authentication.md` leírja a mostani frontend PKCE flow-t és a backend statikus publikus kulcsos JWT-validációját.

Dokumentumok és fájlok
- `README.md`
- `docs/README.md`
- `docs/architecture.md`
- `docs/assets/autoforge-oci-architecture.svg`
- `docs/deployment.md`
- `docs/authentication.md`
- `docs/releases.md`
- `docs/tasks/AUTO-22.md`
- `package.json`
- `package-lock.json`

Lepesnaplo
1. Átnéztem a fő dokumentációs fájlokat és az SVG ábrát a live OCI állapothoz képest.
2. Frissítettem a README és docs index leírásokat a mostani működésre.
3. Aktualizáltam az architektúra, deploy és auth dokumentációkat.
4. Javítottam az SVG ábra félrevezető címkéit.
5. Emeltem a projektverziót `0.1.13`-ra.

Eredmény
- A fő dokumentációs felület már a jelenlegi live rendszert írja le.
