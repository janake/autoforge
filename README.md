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
- A frontendhez alap GitHub Actions build workflow is elokeszitve van.
- A backend es worker szolgaltatasok helye elokeszitve.
- A deployment Docker + GitHub Actions alapu lesz.
- A public/backend szerepkiosztas es az OCI runbook kulon dokumentumban van rogzitve.

## Monorepo irany

- `apps/web`: React frontend helye
- `services/backend`: privat gepen futo backend szolgaltatas helye
- `services/worker`: AI altal vezerelt hatterfolyamatok helye
- `infra/compose`: Docker Compose stackek
- `docs`: projekt dokumentacio

## Task tracking

- Minden feladatot a `docs/tasks/` alatt kovetunk.
- Minden feladat kulon Markdown fajlt kap egyedi azonosito alatt, peldaul `AUTO-1`.
- Az adott feladathoz tartozo commitok uzenetei az azonosito prefixszel kezdodjenek, peldaul: `[AUTO-1] Add ...`

## Indulasi megjegyzes

Ebben a fazisban a frontend alap mar letrejott, a backend es worker runtime valasztas meg kesobbi lepes.
