# Deployment

## Celforma

Az Autoforge jelenlegi deploy modellje ket OCI geppel szamol:

- publikus host: React frontend + Caddy gateway
- privat host: Spring Boot backend

A GitHub Actions workflow-k GHCR image-eket hasznalnak, majd SSH-n keresztul frissitik a ket hostot.

## Kontenerkepek

A `Container Images` workflow ezeket az image-eket kezeli:

- `ghcr.io/janake/autoforge/web`
- `ghcr.io/janake/autoforge/backend`

A workflow a `main` es `sha-<commit>` tageket kesziti el.

## Compose stackek

Publikus host:

- fajl: `infra/compose/docker-compose.public.yml`
- szolgaltatasok: `web`, `gateway`
- gateway domain: `oci.prodet.org`, `api.oci.prodet.org`
- backend upstream: a privat host belso cime, jelenleg varhatoan `10.42.0.91:8080`

Privat host:

- fajl: `infra/compose/docker-compose.private.yml`
- szolgaltatasok: `backend`
- host port: `8080`

## Host konyvtarak

A workflow-k ide masoljak ki a futtatasra szant fajlokat:

- publikus host: `/opt/autoforge/public`
- privat host: `/opt/autoforge/private`

A tipikus tartalom:

- Compose fajl
- `.env`
- `.deploy.env`
- `deploy.sh`
- publikus hoston plusz `Caddyfile`

## Workflow-k

- `Frontend Build`: PR es main build a React apphoz
- `Backend Build`: PR es main test a Spring Boot apphoz
- `Container Images`: web es backend image build + push GHCR-be
- `Deploy Public Host`: manual workflow a publikus stack frissitesere
- `Deploy Private Host`: manual workflow a privat stack frissitesere ProxyJump-pal

## Szukseges GitHub secret-ek

- `OCI_SSH_PRIVATE_KEY`: a deploy SSH kulcs privat fele
- `OCI_PUBLIC_HOST`: a publikus host DNS neve vagy IP-je
- `OCI_PUBLIC_USER`: a publikus host SSH felhasznaloja
- `OCI_PRIVATE_HOST`: a privat host belso IP-je
- `OCI_PRIVATE_USER`: a privat host SSH felhasznaloja
- `OCI_GATEWAY_DOMAIN`: peldaul `oci.prodet.org, api.oci.prodet.org`
- `OCI_BACKEND_UPSTREAM`: peldaul `10.42.0.91:8080`
- `GHCR_DEPLOY_USERNAME`: GHCR olvasasi jogosultsagu usernev
- `GHCR_DEPLOY_TOKEN`: GHCR olvasasi jogu token

## Szerver bootstrap minimum

Mindket gepen kell:

- Docker Engine
- Docker Compose plugin vagy standalone `docker-compose`
- olyan SSH user, amelyik tud `docker compose` parancsot futtatni

Hasznos kezdo parancsok:

```bash
sudo mkdir -p /opt/autoforge/public /opt/autoforge/private
sudo chown -R "$USER:$USER" /opt/autoforge
```

Magyarazat:

- letrehozza a ket deploy konyvtarat
- az aktualis SSH user tulajdonaba adja oket

## Kezi frissitesi parancsok

Publikus host:

```bash
docker compose --env-file .env -f docker-compose.public.yml pull
docker compose --env-file .env -f docker-compose.public.yml up -d --remove-orphans
```

Privat host:

```bash
docker compose --env-file .env -f docker-compose.private.yml pull
docker compose --env-file .env -f docker-compose.private.yml up -d --remove-orphans
```

Ha a hoston nincs `docker compose` plugin, ugyanennek a standalone megfeleloje:

```bash
docker-compose --env-file .env -f docker-compose.public.yml pull
docker-compose --env-file .env -f docker-compose.public.yml up -d --remove-orphans
```
