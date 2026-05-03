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

- A frontend React lesz, de meg nincs scaffoldolva vagy telepitve.
- A backend es worker szolgaltatasok helye elokeszitve.
- A deployment Docker + GitHub Actions alapu lesz.
- A public/backend szerepkiosztas es az OCI runbook kulon dokumentumban van rogzitve.

## Monorepo irany

- `apps/web`: React frontend helye
- `services/backend`: privat gepen futo backend szolgaltatas helye
- `services/worker`: AI altal vezerelt hatterfolyamatok helye
- `infra/compose`: Docker Compose stackek
- `docs`: projekt dokumentacio

## Indulasi megjegyzes

Ebben a fazisban a strukturat keszitjuk elo. Dependency telepites, React scaffoldolas es backend runtime valasztas kesobbi lepes.
