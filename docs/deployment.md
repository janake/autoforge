# Deployment

## Celforma

Az Autoforge jelenlegi deploy modellje ket OCI geppel szamol:

- publikus host: React frontend + Spring Cloud API gateway + Caddy gateway
- privat host: Spring Boot backend

A GitHub Actions workflow-k GHCR image-eket hasznalnak, majd SSH-n keresztul frissitik a ket hostot.
A publikus host deployja opcionálisan Cloudflare DNS rekordokat is frissit a webes domainhez.

## Kontenerkepek

A `Container Images` workflow ezeket az image-eket kezeli:

- `ghcr.io/<registry-owner>/autoforge/web`
- `ghcr.io/<registry-owner>/autoforge/api-gateway`
- `ghcr.io/<registry-owner>/autoforge/backend`
- `ghcr.io/anomalyco/opencode` hivatalos OpenCode image-kent fut a private stackben, ezt nem ez a workflow epiti

A workflow a `main` es `sha-<commit>` tageket kesziti el.

## Compose stackek

Publikus host:

- fajl: `infra/compose/docker-compose.public.yml`
- szolgaltatasok: `web`, `api`, `gateway`
- gateway domain: `oci.prodet.org`, `api.oci.prodet.org`
- backend upstream: a privat host belso cime, peldaul `<private-backend-ip>:8080`
- API image: `ghcr.io/<registry-owner>/autoforge/api-gateway`

Privat host:

- fajl: `infra/compose/docker-compose.private.yml`
- szolgaltatasok: `backend`, `opencode`
- host port: `8080`
- plusz config: `infra/compose/opencode.json`
- opencode server: belso REST endpoint a backendhez, auth-vedett
- opencode image: `ghcr.io/anomalyco/opencode`
- opencode secret ertekek: OCI Vaultbol, instance principal-lal olvasva a private hoston
- workspace storage: 100 GB OCI Block Volume, ext4, mount point: `/mnt/autoforge-workspace`

Keycloak / OIDC:

- a web frontend kulso OIDC providerhoz csatlakozik PKCE flow-val
- a public stack runtime envje tartalmazza a Keycloak URL-t, a realm helykitoltot es a public client azonositot
- a private stack runtime envje az issuer URI-t kapja, amely a provider URL-bol es a realm helykitoltobol epul fel
- jelenlegi modellben public clientet hasznalunk, ezert nincs külön client secret
- ha kesobb confidential client vagy tovabbi auth secret kell, azt OCI Vaultban kell tarolni

## Private workspace storage

A private hosthoz egy kulon 100 GB-os OCI Block Volume van csatolva az Autoforge workspace celjara.

- volume nev: `autoforge-private-workspace-100gb`
- attach tipus: paravirtualized
- filesystem: ext4
- mount point: `/mnt/autoforge-workspace`
- fstab: UUID alapu mount `defaults,nofail,_netdev` opciokkal
- tulajdonos a hoston: deploy SSH user
- cel: OpenCode/backend/worker altal hasznalt tartos workspace, nem kontener image vagy gitelt adat

Megjegyzes:

- Az OCI Always Free storage keret a Block Volume storage-ra vonatkozik, nem az OCI File Storage Service NFS-re.
- A jelenlegi ket boot volume mellett a 100 GB-os extra block volume a 200 GB-os Always Free block storage kereten belul marad.
- Ujabb volume vagy boot volume meretnoveles elott ellenorizni kell a teljes Block Volume storage hasznalatot.

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
- privat hoston plusz `opencode.json`

## Workflow-k

- `Frontend Build`: PR es main build a React apphoz
- `Backend Build`: PR es main test a Spring Boot apphoz
- `Container Images`: web es backend image build + push GHCR-be
- `Deploy Public Host`: manual workflow a publikus stack frissitesere
- `Deploy Private Host`: manual workflow a privat stack frissitesere ProxyJump-pal, beleertve a backendet es az opencode REST AI service-et
- ugyanazok a workflow-k `main`-re merge-elt, relevans fájlokat erinto pushokra is lefutnak, hogy a deploy automatikusan meginduljon

## Szukseges GitHub secret-ek

Kapcsolati es registry secret-ek:

- `OCI_SSH_PRIVATE_KEY`: a deploy SSH kulcs privat fele
- `OCI_PUBLIC_HOST`: a publikus host webes DNS neve, peldaul `oci.prodet.org`
- `OCI_PUBLIC_SSH_HOST`: a publikus host SSH-celja, azaz a valos routolhato IP vagy SSH DNS nev; nem a Cloudflare-kezelt web hostname
- `OCI_PUBLIC_USER`: a publikus host SSH felhasznaloja
- `OCI_PRIVATE_HOST`: a privat host belso IP-je
- `OCI_PRIVATE_SSH_HOST`: a privat host SSH-celja, ha nem egyezik az `OCI_PRIVATE_HOST` ertekevel
- `OCI_PRIVATE_USER`: a privat host SSH felhasznaloja
- `OCI_GATEWAY_DOMAIN`: peldaul `oci.prodet.org, api.oci.prodet.org`
- `OCI_BACKEND_UPSTREAM`: peldaul `<private-backend-ip>:8080`
- `GHCR_DEPLOY_USERNAME`: GHCR olvasasi jogosultsagu usernev
- `GHCR_DEPLOY_TOKEN`: GHCR olvasasi jogu token
- `CLOUDFLARE_API_TOKEN`: Cloudflare API token DNS write joggal
- `CLOUDFLARE_ZONE_ID`: a `prodet.org` zone id-ja
- `CLOUDFLARE_SSL_MODE`: optional, ha be van allitva, a zone SSL setting is frissul
- `OCI_KEYCLOAK_URL`: Keycloak base URL-je, peldaul `https://kc.prodet.org`
- `OCI_KEYCLOAK_REALM`: a realm neve helyett hasznalt deploy-time helykitolto, amelybol az issuer URI epul
- `OCI_KEYCLOAK_CLIENT_ID`: a public web client azonositoja

Vault secret azonosito GitHub secret-ek:

- `OCI_OPENCODE_SERVER_PASSWORD_SECRET_OCID`: az OCI Vaultban tarolt `autoforge-opencode-server-password` secret OCID-ja
- `OCI_OPENAI_API_KEY_SECRET_OCID`: az OCI Vaultban tarolt `autoforge-openai-api-key` secret OCID-ja

Fontos:

- A GitHub secret-ekben csak a Vault secret OCID-k szerepelnek, nem az OpenCode jelszo vagy provider API kulcs ertekei.
- A deploy workflow ezeket az OCID-ket masolja a private host `.env` fajljaba.
- A private host `deploy.sh` scriptje olvassa ki a konkret secret ertekeket OCI Vaultbol, `--auth instance_principal` hasznalataval.
- A webes hostname es az SSH-cel nem ugyanaz: a Cloudflare-kezelt publikus domain nem alkalmas SSH deploy celra, ehhez kulon SSH host kell.
- Ha a Cloudflare token es zone id rendelkezésre all, a public deploy script automatikusan az `oci.prodet.org` es `api.oci.prodet.org` rekordokat a publikus origin IP-re allitja, proxied rekordokkal.
- Ha külön `CLOUDFLARE_SSL_MODE` is meg van adva, a zone SSL setting is frissul.

## Szükséges OCI Vault secret-ek

- `autoforge-opencode-server-password`: az OpenCode REST szerver HTTP basic auth jelszava. Legalabb 32 karakteres, veletlen, newline nelkuli ertek legyen.
- `autoforge-openai-api-key`: az OpenAI API kulcs, amelyet az OpenCode provider hasznal. Newline nelkuli ertek legyen.

Megjegyzes:

- a mostani Keycloak integraciohoz nem kell tovabbi secret; a login public client + PKCE alapon mukodik
- ha kesobb confidential clientet vezetunk be, annak secretje is OCI Vaultba kerul

## Szükséges OCI jogosultság

A private compute instance-nek instance principalon keresztul kell tudnia olvasni a Vault secret bundle-ok tartalmat.

Minimum elofeltetelek:

- A private instance legyen benne egy OCI Dynamic Groupban.
- Legyen policy, amely engedi a Dynamic Groupnak a secret bundle olvasast abban a compartmentben vagy vaultban, ahol a ket secret van.
- A private hoston legyen telepitve az OCI CLI.

Pelda policy minta:

```text
Allow dynamic-group <dynamic-group-name> to read secret-bundles in compartment prodet-new
```

## Erzekeny adatok kezelese

- Gitbe nem kerulhet privat kulcs, kulcsfajl-nev, abszolut lokalis path, szemelyes felhasznalonev, email, token vagy cloud credential.
- Deploy parancsokban szemelyes path helyett env valtozot kell hasznalni, peldaul `${AUTOFORGE_SSH_KEY}`.
- Registry owner, repo owner es account nev csak placeholderkent vagy GitHub Actions runtime valtozokent szerepelhet.
- Konkreten hasznalt secret ertekek gitelt fajlban nem lehetnek; OpenCode runtime secret ertekek OCI Vaultban vannak, es csak deploy futaskor kerulnek at ideiglenes runtime env fajlba.

## Szerver bootstrap minimum

Mindket gepen kell:

- Docker Engine
- Docker Compose plugin vagy standalone `docker-compose`
- olyan SSH user, amelyik tud `docker compose` parancsot futtatni

A private gepen plusz:

- csatolt es mountolt workspace volume: `/mnt/autoforge-workspace`

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
APP_DIR=/opt/autoforge/private bash /opt/autoforge/private/deploy.sh
```

Magyarazat:

- a private deploy script beolvassa a Vault secret ertekeket;
- letrehoz egy ideiglenes runtime env fajlt;
- ezzel futtatja a private Docker Compose stack frissiteset.

Ha a hoston nincs `docker compose` plugin, a script automatikusan standalone `docker-compose` parancsra valt.

Publikus host standalone compose pelda:

```bash
docker-compose --env-file .env -f docker-compose.public.yml pull
docker-compose --env-file .env -f docker-compose.public.yml up -d --remove-orphans
```
