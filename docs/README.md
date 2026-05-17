# docs

Projekt dokumentációk helye.

Ez a könyvtár tartalmazza a jelenlegi Autoforge OCI topológia, deploy modell, auth működés és task tracking leírásait.

Fő dokumentumok:

- `docs/architecture.md`: public/private OCI felépítés, gateway lánc, OpenCode és workspace storage, a részletes ábra Confluence-ban él
- `docs/ai-runtime-openrouter.md`: OpenRouter + OpenCode runtime döntés, secret izoláció és REST validációs terv
- `docs/learning-rag-adb-vector.md`: Learning RAG Autonomous Database vector capability döntés
- `docs/learning-api-contract.md`: Learning API contract web és Android klienshez
- `docs/learning-content-generation.md`: Learning kérdésgenerálás és összefoglaló slice
- `docs/deployment.md`: verziózott image-ek, auto deploy workflow-k, host könyvtárak és secret-ek
- `docs/authentication.md`: Keycloak PKCE login, frontend callback flow, backend JWT-validáció
- `docs/ai-tooling.md`: ajánlott MCP-k, skillek és capability groupok az Autoforge stackhez
- `docs/releases.md`: task -> verzió megfeleltetés
- `docs/tasks/`: feladatszintű nyilvántartás

Verziozasi szabalyok:

- `docs/versioning.md`: hogyan valasztunk celverziot uj feladathoz
- `docs/releases.md`: melyik `AUTO-*` vagy `BUG-*` melyik verziohoz tartozik
- Nem-MAJOR feladatnal a version bump automatikus; MAJOR elott jovahagyast kerunk.

Biztonsági vizsgálatok és PR-specifikus runbookok:

- `docs/security/` alatt tároljuk a PR-enkénti audit és image-scan jelentéseket és útmutatókat (például: `docs/security/pr-10-audit-and-scan.md`).

## Task tracking szabaly

- Minden feladatot a `docs/tasks/` alatt kovetunk.
- Minden feladat kulon fajlt kap egyedi azonosito alatt.
- Az adott feladathoz tartozo commitok uzenetei az azonositoval kezdodjenek.
- Minden feladatnak kotelezo `Verzió` mezot adni, es szerepelnie kell a `docs/releases.md` manifestben.
