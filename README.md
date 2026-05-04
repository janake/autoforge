# autoforge

AI-segitseggel sajat magat fejleszto alkalmazas/platform.

## Helyi projektfa

```text
autoforge/
  apps/
    web/
  services/
    backend/
    worker/
  infra/
    compose/
  docs/
  .github/
    workflows/
```

## Jelenlegi allapot

- A frontend React + Vite + TypeScript alappal scaffoldolva van.
- A frontendhez deployolhato Docker image definicio is keszult.
- A frontendhez alap GitHub Actions build workflow is elokeszitve van.
- A publikus hoston a React frontend + Spring Cloud API gateway + Caddy routing lancolat elokeszitett.
- A backend Spring Boot + Maven alappal scaffoldolva van.
- A backendhez alap GitHub Actions build workflow is elokeszitve van.
- A ket OCI gephez tartozo Compose stackek es deploy workflow-k alapjai elokeszitve vannak.
- A worker szolgaltatas helye elokeszitve.
- A deployment Docker + GitHub Actions alapu lesz.
- A public/backend szerepkiosztas es az OCI runbook kulon dokumentumban van rogzitve.

## Monorepo irany

- `apps/web`: React frontend helye
- `services/backend`: Spring Boot backend szolgaltatas helye
- `services/worker`: AI altal vezerelt hatterfolyamatok helye
- `infra/compose`: Docker Compose stackek es publikus/private host konfiguraciok
- `infra/deploy`: tavoli deploy scriptek
- `docs`: projekt dokumentacio

## Task tracking

- Minden feladatot a `docs/tasks/` alatt kovetunk.
- Minden feladat kulon Markdown fajlt kap egyedi azonosito alatt, peldaul `AUTO-1`.
- Az adott feladathoz tartozo commitok uzenetei az azonosito prefixszel kezdodjenek, peldaul: `[AUTO-1] Add ...`
- Az aktiv work branchek neve is tartalmazza az azonositot, peldaul: `AUTO-1-react-base`.

## Indulasi megjegyzes

Ebben a fazisban a frontend es a backend alap mar letrejott, a ket hostos Docker deploy foundation elokeszitve van, a worker runtime valasztas meg kesobbi lepes.
