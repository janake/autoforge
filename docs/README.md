# docs

Projekt dokumentaciok helye.

Az OCI infrastrukturahoz kapcsolodo aktualis runbook jelenleg a kovetkezo fajlban van:

- `./oci-prodet-new-runbook.md`

Itt lesznek majd:

- architektura leiras
- deploy folyamat
- local dev megjegyzesek
- task tracking
- verziozas es release manifest

Verziozasi szabalyok:

- `docs/versioning.md`: hogyan valasztunk celverziot uj feladathoz
- `docs/releases.md`: melyik `AUTO-*` vagy `BUG-*` melyik verziohoz tartozik

Biztonsági vizsgálatok és PR-specifikus runbookok:

- `docs/security/` alatt tároljuk a PR-enkénti audit és image-scan jelentéseket és útmutatókat (például: `docs/security/pr-10-audit-and-scan.md`).

## Task tracking szabaly

- Minden feladatot a `docs/tasks/` alatt kovetunk.
- Minden feladat kulon fajlt kap egyedi azonosito alatt.
- Az adott feladathoz tartozo commitok uzenetei az azonositoval kezdodjenek.
- Minden feladatnak kotelezo `Verzió` mezot adni, es szerepelnie kell a `docs/releases.md` manifestben.
