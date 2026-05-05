# AUTO-18 Version tracking

Feladat leírása
Be kell vezetni a projektverziozast ugy, hogy minden story es bugfix egyertelmuen hozzarendelheto legyen egy konkret verziohoz. A szabaly keruljon be a kozos folyamatdokumentacioba, hogy kesobb ne lehessen elfelejteni.

Statusz
- in progress

Verzió
- `0.1.1`

Branch
- `feature/AUTO-18-version-tracking`

PR
- PR #28

Acceptance criteria
- Van `docs/versioning.md` verziozasi szabalyzat.
- Van `docs/releases.md` story/bug -> verzio manifest.
- A task tracking szabalyok kotelezove teszik a `Verzió` mezot.
- Az agent szabalyok kotelezove teszik a verzio megadasat uj feladatnal.
- A branching, docs overview, architecture, deploy, auth, feature flag es workflow doksik hivatkoznak a verziozasi szabalyra.
- A sajat Docker image-ek verzios taget kapnak a root `package.json` `version` mezoje alapjan.

Dokumentumok és fájlok
- `AGENT.md`
- `docs/README.md`
- `docs/branching.md`
- `docs/architecture.md`
- `docs/deployment.md`
- `docs/authentication.md`
- `docs/feature-flags.md`
- `.github/workflows/README.md`
- `docs/tasks/README.md`
- `docs/versioning.md`
- `docs/releases.md`
- `.github/workflows/container-images.yml`
- `.github/workflows/deploy-public.yml`
- `.github/workflows/deploy-private.yml`
- `package.json`
- `package-lock.json`
- `docs/tasks/AUTO-18.md`

Lepesnaplo
1. Uj `AUTO-18` feature branchet hoztam letre `origin/main` alaprol.
2. Letrehoztam a verziozasi szabalyzatot.
3. Letrehoztam a release manifestet.
4. Frissitettem a kozos folyamatdokumentaciot, hogy a verzio kotelezo legyen.
5. Frissitettem a top-level projekt- es workflow-doksikat verziozasi hivatkozassal.
6. Megnyitottam a kapcsolodo PR-t.
7. Hozzaadtam a projektverzio Docker image taget a container image workflow-hoz.
8. A root projektverziot `0.1.1`-re emeltem, hogy az image tag egyezzen a release manifesttel.
9. Atallitottam a public/private deploy workflow-kat, hogy a sajat image-ekhez a verzios taget hasznaljak.
10. A deploy workflow-kba verzios image elerhetosegi ellenorzest tettem a build/deploy race elkerulesehez.

Eredmény
- A verziozasi szabalyzat es release manifest dokumentalva lett.
