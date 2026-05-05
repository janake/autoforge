# AUTO-12: Feature flag es personalization kutatas

Feladat leírása
Ossze kell foglalni, milyen teljesen ingyenes, self-hostolhato feature flag es personalization megoldasok illeszthetok az Autoforge stackbe, es hogyan hasznalhatok kesobb OCI Always Free adatbazisok.

Statusz
- in-progress

Branch
- `AUTO-12-feature-flags-research`

PR
- pending

Acceptance criteria
- A dokumentacio tartalmazza az ingyenes self-hosted feature flag opciokat.
- A dokumentacio kulon kezeli a szemelyre szabasi use case-eket.
- A dokumentacio kimondja, hogy a feature flag nem helyettesiti a backend jogosultsagellenorzest.
- A dokumentacio tartalmazza az OCI Always Free DB opciokat kesobbi felhasznalasra.
- A javasolt kezdo irany illeszkedik a jelenlegi Docker + React + Spring Boot + OCI stackhez.

Dokumentumok és fájlok
- `docs/feature-flags.md`
- `docs/tasks/AUTO-12.md`
- `README.md`

Biztonsági megfontolások
- Gitbe nem kerülhet credential, token, OCID, lokalis path vagy szemelyes azonosito.
- Frontend feature flag csak UI szemelyre szabast adhat; backend oldalon a valodi jogosultsagot ujra kell ellenorizni.
- A flag contextbe csak szukseges attributumok keruljenek.

Lepesnaplo
1. Ellenoriztem a hivatalos OpenFeature, flagd, Flipt, Unleash, GrowthBook es Flagsmith dokumentaciot.
2. Ellenoriztem a hivatalos OCI Always Free adatbazis opciokat.
3. Letrehoztam a feature flag dontesi dokumentumot:
   ```bash
   docs/feature-flags.md
   ```
4. Lefuttattam a dokumentacio ellenorzeseit:
   ```bash
   git diff --check
   rg -n "<sensitive-patterns>" README.md docs infra .github
   ```

Eredmény
- pending
