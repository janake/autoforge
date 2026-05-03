# AUTO-3 Docker deploy foundation

- Statusz: in progress
- Branch: `AUTO-3-docker-deploy-foundation`
- PR: `pending`

## Cel

Deployalhato Docker alapok letrehozasa a ket OCI gepes felallashoz, ugy hogy legyen web image, gateway config, publikus es privat Compose stack, valamint GitHub Actions deploy workflow.

## Scope

- web Docker image
- Caddy gateway config
- publikus es privat Compose stack
- deploy scriptek a ket hostra
- GHCR image build workflow
- public/private deploy workflow
- deployment dokumentacio

## Commit szabaly

Az ehhez a feladathoz tartozo commitok `AUTO-3` prefixet hasznalnak.

Pelda:

```text
[AUTO-3] Add Docker deployment foundation
```

## Validacio

- `npm run build:web`
- `mvn -q -f services/backend/pom.xml test`
- `docker-compose --env-file infra/compose/.env.public.example -f infra/compose/docker-compose.public.yml config`
- `docker-compose --env-file infra/compose/.env.private.example -f infra/compose/docker-compose.private.yml config`

## Megjegyzes

Az elso korben a deploy workflow-k manualis `workflow_dispatch` alapon mennek.
Az SSH es GHCR secret-eket GitHub oldalon kell majd beallitani.
