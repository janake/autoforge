# Deployment

## Celforma

Az Autoforge jelenlegi deploy modellje ket OCI geppel szamol:

- publikus host: React frontend + Spring Cloud API gateway + Caddy gateway
- privat host: Spring Boot backend

A GitHub Actions workflow-k GHCR image-eket hasznalnak, majd SSH-n keresztul frissitik a ket hostot.

## Kontenerkepek

A `Container Images` workflow ezeket az image-eket kezeli:

- `ghcr.io/<registry-owner>/autoforge/web`
- `ghcr.io/<registry-owner>/autoforge/api-gateway`
- `ghcr.io/<registry-owner>/autoforge/backend`

A workflow a `main` es `sha-<commit>` tageket kesziti el.

## Compose stackek

Publikus host:

- fajl: `infra/compose/docker-compose.public.yml`
- szolgaltatasok: `web`, `api`, `gateway`
- gateway domain: `oci.prodet.org`, `api.oci.prodet.org`
- backend upstream: a privat host belso cime, jelenleg varhatoan `10.42.0.91:8080`
- API image: `ghcr.io/<registry-owner>/autoforge/api-gateway`

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

## Erzekeny adatok kezelese

- Gitbe nem kerulhet privat kulcs, kulcsfajl-nev, abszolut lokalis path, szemelyes felhasznalonev, email, token vagy cloud credential.
- Deploy parancsokban szemelyes path helyett env valtozot kell hasznalni, peldaul `${AUTOFORGE_SSH_KEY}`.
- Registry owner, repo owner es account nev csak placeholderkent vagy GitHub Actions runtime valtozokent szerepelhet.
- Konkreten hasznalt secret ertekek csak GitHub Secretsben vagy a celgepek `.env` fajljaiban lehetnek, gitelt fajlban nem.

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

## Halozati elofeltetelek

A public host kulso HTTPS eleresehez ket retegen is nyitva kell lennie a portoknak:

1. OCI subnet security list:
- `22/tcp` (SSH)
- `80/tcp` (HTTP, ACME challenge)
- `443/tcp` (HTTPS)

2. Public host iptables:
- INPUT chainben engedni kell `80/tcp` es `443/tcp` bejovo kapcsolatokat.

Gyors ellenorzes:

```bash
sudo iptables -S | grep -E 'dport (22|80|443)'
```

Ha Oracle image default szabaly miatt csak `22` nyitott, akkor a `rules.v4`-et boviteni kell es persistalni:

```bash
sudo sed -i '/--dport 22 -j ACCEPT/a -A INPUT -p tcp -m state --state NEW -m tcp --dport 80 -j ACCEPT\n-A INPUT -p tcp -m state --state NEW -m tcp --dport 443 -j ACCEPT' /etc/iptables/rules.v4
sudo iptables-restore < /etc/iptables/rules.v4
sudo netfilter-persistent save
```

Megjegyzes:

- `iptables-restore` utan Docker chain-ek hianyozhatnak; ilyenkor `sudo systemctl restart docker`, majd compose `up -d --remove-orphans` kell.

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
