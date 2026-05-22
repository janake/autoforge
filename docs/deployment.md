# Deployment

## Celforma

Az Autoforge jelenlegi deploy modellje ket OCI geppel szamol:

- publikus host: React frontend + Spring Cloud API gateway + Caddy gateway
- privat host: Spring Boot backend + OpenRouter provider proxy

A GitHub Actions workflow-k GHCR image-eket hasznalnak, majd SSH-n keresztul frissitik a ket hostot.
A publikus host deployja opcionálisan Cloudflare DNS rekordokat is frissit a webes domainhez.

## Kontenerkepek

A `Container Images` workflow ezeket az image-eket kezeli:

- `ghcr.io/<registry-owner>/autoforge/web`
- `ghcr.io/<registry-owner>/autoforge/api-gateway`
- `ghcr.io/<registry-owner>/autoforge/backend`
- `ghcr.io/<registry-owner>/autoforge/openrouter-proxy`

A workflow a `main`, `<version>` es `sha-<commit>` tageket kesziti el. A `<version>` tag a root `package.json` `version` mezojebol jon, peldaul `0.1.13`.
Deploy soran a sajat image-eknel a `<version>` tag kerul a Compose env fajlba, nem a mozgó `main` tag.

## Verziózás

- Minden deploy vagy workflow valtozasnak legyen celverzioja a task fajlban.
- A valtozast fel kell venni a `docs/releases.md` manifestbe.
- A verzios besorolast a `docs/versioning.md` alapjan kell eldonteni.
- A sajat Docker image-eknek tartalmazniuk kell a projektverzio taget is.
- A deploy workflow-knak a projektverzio taget kell hasznalniuk a sajat image-ekhez.

## Compose stackek

Publikus host:

- fajl: `infra/compose/docker-compose.public.yml`
- szolgaltatasok: `web`, `api`, `gateway`
- gateway domain: `oci.prodet.org`, `api.oci.prodet.org`
- backend upstream: a privat host belso cime, peldaul `<private-backend-ip>:8080`
- API image: `ghcr.io/<registry-owner>/autoforge/api-gateway`
- a public deploy normalizalja a `BACKEND_UPSTREAM` erteket `host:port` formatumra

Privat host:

- fajl: `infra/compose/docker-compose.private.yml`
- szolgaltatasok: `backend`, `openrouter-proxy` (`ai` profillal, ha az OpenRouter kulcs feloldhato)
- host port: `8080`
- memoriakorlatozas: konzervativ `mem_limit` cap-ek a backendhez es az OpenRouter proxyhoz; a limitek `BACKEND_MEMORY_LIMIT` es `OPENROUTER_PROXY_MEMORY_LIMIT` env varokkal felulirhatok
- backend AI endpoint: `AI_PROVIDER_PROXY_BASE_URL`, alapertelmezett private compose ertek `http://openrouter-proxy:8080/v1`
- backend AI modell: `AI_PROVIDER_MODEL`, alapertelmezett `google/gemma-4-26b-a4b-it:free`
- openrouter-proxy image: `ghcr.io/<registry-owner>/autoforge/openrouter-proxy`
- OpenRouter secret ertek: OCI Vaultbol, instance principal-lal olvasva a private hoston, csak a provider proxy kapja meg runtime env-kent
- workspace storage: 100 GB OCI Block Volume, ext4, mount point: `/mnt/autoforge-workspace`
- ha nincs feloldhato OpenRouter API key, a deploy nem engedelyezi az `ai` profilt, es a backend AI provider base URL uresen marad

Keycloak / OIDC:

- a web frontend kulso OIDC providerhoz csatlakozik PKCE flow-val
- a public stack runtime envje tartalmazza a Keycloak URL-t, a realm helykitoltot es a public client azonositot
- a private stack runtime envje tovabbra is megkapja a Keycloak issuer URI-t, de a JWT signature validacio jelenleg a backendbe csomagolt publikus kulccsal tortenik
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
- cel: backend/worker altal hasznalt tartos workspace, nem kontener image vagy gitelt adat

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

## Workflow-k

- `Frontend Build`: PR es main build a React apphoz
- `Backend Build`: PR es main test a Spring Boot apphoz
- `Container Images`: web, backend, API gateway es OpenRouter proxy image build + push GHCR-be
- `Deploy Public Host`: public stack frissitese merge utan vagy manual dispatch-csel
- `Deploy Private Host`: private stack frissitese merge utan vagy manual dispatch-csel, beleertve a backendet es az OpenRouter provider proxyt
- a private hoston a deploy a `autoforge-arm-capacity-check.timer` systemd timert is telepiti, amely 3 percenkent futtatja az `oci-a1-capacity` ellenorzest a Frankfurt tenancy ARM kapacitasara
- a private hoston a deploy a `autoforge-arm-capacity-summary.timer` systemd timert is telepiti, amely minden nap 07:00-kor kuldi az elozo 24 ora osszegzeset
- a private hoston a deploy OCI Notifications topicot hoz letre vagy ujrahasznal, majd ehhez email subscriptiont regisztral `janak.endre@gmail.com` cimre
- a private deploy a sikeres provider proxy inditas utan egy REST smoke tesztet is futtat, amely ellenorzi a proxy health/model endpointjait es egy OpenAI-kompatibilis chat completiont
- ugyanazok a workflow-k `main`-re merge-elt, relevans fájlokat erinto pushokra is lefutnak, hogy a deploy automatikusan meginduljon

Fontos trigger-ek:

- public deploy lefut `apps/web/**`, `infra/gateway/**`, `infra/compose/**`, `infra/deploy/public/**`, `.github/workflows/deploy-public.yml`, `package.json`, `package-lock.json` valtozasra
- private deploy lefut `services/backend/**`, `services/provider-proxy/**`, `infra/compose/**`, `infra/deploy/private/**`, `.github/workflows/deploy-private.yml`, `package.json`, `package-lock.json` valtozasra

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

Megjegyzések:
- `OCI_GATEWAY_DOMAIN` nem szükséges - a domain (`oci.prodet.org`) be van égetve a `docker-compose.public.yml`-be (BUG-3).

Vault secret azonosito GitHub secret-ek:

- `OCI_OPENROUTER_API_KEY_SECRET_OCID`: az OCI Vaultban tarolt OpenRouter API key secret OCID-ja; ezt csak az OpenRouter proxy kapja meg, a backend nem
- `OCI_OPENROUTER_API_KEY_SECRET_NAME`: opcionális OCI Vault display name az OpenRouter API keyhez, ha OCID helyett név alapján oldjuk fel
- `AUTOFORGE_DB_URL`: opcionális direkt JDBC URL, ha nem walletes ADB kapcsolatot hasznalunk
- `AUTOFORGE_DB_URL_SECRET_NAME`: opcionális OCI Vault display name a direkt JDBC URL-hez, alapertelmezett: `autoforge-db-url`
- `AUTOFORGE_DB_WALLET_URL`: a private ADB wallet zip object storage URL-je
- `AUTOFORGE_DB_WALLET_URL_SECRET_NAME`: opcionális OCI Vault display name a wallet URL-hez, alapertelmezett: `autoforge-db-wallet-url`
- `AUTOFORGE_DB_WALLET_PASSWORD_SECRET_OCID`: a wallet zip jelszavát tarolo OCI Vault secret OCID-ja
- `AUTOFORGE_DB_WALLET_PASSWORD_SECRET_NAME`: opcionális OCI Vault display name a wallet zip jelszavahoz, alapertelmezett: `db-wallet-pwd`
- `AUTOFORGE_DB_SERVICE_ALIAS_SECRET_NAME`: opcionális OCI Vault display name az ADB service aliashoz, alapertelmezett: `autoforge-db-service-alias`
- `AUTOFORGE_DB_USERNAME`: az adatbazis felhasznalo, alapertelmezett: `ADMIN`
- `AUTOFORGE_DB_USERNAME_SECRET_NAME`: opcionális OCI Vault display name az adatbazis felhasznalohoz, alapertelmezett: `autoforge-db-username`
- `AUTOFORGE_DB_PASSWORD`: opcionális direkt adatbazis jelszo, ha nem Vault secret OCID-t hasznalunk
- `AUTOFORGE_DB_PASSWORD_SECRET_OCID`: opcionális DB password secret OCID, ha a DB jelszó is Vaultban van
- `AUTOFORGE_DB_PASSWORD_SECRET_NAME`: opcionális OCI Vault display name az adatbazis jelszohoz, alapertelmezett: `autoforge-db-password`
- `AUTOFORGE_ARM_CAPACITY_NOTIFICATION_COMPARTMENT_OCID`: opcionális OCI compartment OCID, ha a private instance metadata nem elerheto a topic letrehozasahoz

GitHub PR broker konfiguráció:

- `AUTOFORGE_GITHUB_OWNER`: a repository owner vagy org neve
- `AUTOFORGE_GITHUB_REPO`: a repository neve
- `AUTOFORGE_GITHUB_API_BASE_URL`: GitHub API base URL, GitHub.com esetén `https://api.github.com`, enterprise esetén a saját domain API endpointja
- `AUTOFORGE_GITHUB_TOKEN`: a PR nyitáshoz használt token

Fontos:

- A GitHub secret-ekben csak a Vault secret OCID-k szerepelnek, nem a provider API kulcs ertekei.
- A GitHub variable-okban opcionálisan Vault display name-ek is szerepelhetnek; konkrét OpenRouter secret érték nem kerülhet GitHubba vagy gitelt fájlba.
- A deploy workflow ezeket az OCID/name referenciákat masolja a private host `.env` fajljaba.
- A private host `deploy.sh` scriptje olvassa ki a konkret secret ertekeket OCI Vaultbol, `--auth instance_principal` hasznalataval.
- A kapacitasjelzes OCI Notifications topicra megy; a deploy script ezt a topicot kezeli, az email subscription pedig `janak.endre@gmail.com` cimre mutat.
- A provider API kulcsot csak az OpenRouter proxy kapja meg runtime env-kent; a backend csak a belso proxy URL-t es a modellazonositot latja.
- Az OpenRouter proxy alapertelmezett modellje `google/gemma-4-26b-a4b-it:free`, mert text, image es video inputot is tud kezelni, es az OpenRouter katalogusban free modellkent szerepel.
- Az OpenRouter proxy `OPENROUTER_ALLOWED_MODELS`, `OPENROUTER_MAX_COMPLETION_TOKENS` es `OPENROUTER_MAX_REQUEST_BYTES` guardrailekkel korlatozza az AI runtime koltseg- es payload-kockazatat, a default completion limit pedig a modell 32768-as plafonjahoz igazodik.
- A provider proxy logjai csak provider/model/status/duration metaadatot irhatnak; promptot, bearer tokent, API kulcsot vagy provider response bodyt nem.
- Az ADB wallet zipet a private host `deploy.sh` letolti object storage-bol, kicsomagolja az `APP_DIR/wallet` mappaba, majd a backend kontenernek `TNS_ADMIN`-nel atadja.
- A backend nem olvas Vaultot runtime alatt; csak runtime env valtozokat kap.
- A webes hostname es az SSH-cel nem ugyanaz: a Cloudflare-kezelt publikus domain nem alkalmas SSH deploy celra, ehhez kulon SSH host kell.
- Ha a Cloudflare token es zone id rendelkezésre all, a public deploy script automatikusan az `oci.prodet.org` es `api.oci.prodet.org` rekordokat a publikus origin IP-re allitja, proxied rekordokkal.
- Ha külön `CLOUDFLARE_SSL_MODE` is meg van adva, a zone SSL setting is frissul.

## Szükséges OCI Vault secret-ek

- `openrouter-api-key`: az OpenRouter API kulcs, amelyet csak az OpenRouter proxy hasznal. Newline nelkuli ertek legyen.
- `db-wallet-pwd`: az ADB wallet zip kicsomagolasi jelszava.

Megjegyzes:

- a mostani Keycloak integraciohoz nem kell tovabbi secret; a login public client + PKCE alapon mukodik
- ha kesobb confidential clientet vezetunk be, annak secretje is OCI Vaultba kerul

## Szükséges OCI jogosultság

A private compute instance-nek instance principalon keresztul kell tudnia olvasni a Vault secret bundle-ok tartalmat es kezelni az OCI Notifications topic/subscription eroforrasokat.

Minimum elofeltetelek:

- A private instance legyen benne egy OCI Dynamic Groupban.
- Legyen policy, amely engedi a Dynamic Groupnak a secret bundle olvasast abban a compartmentben vagy vaultban, ahol a ket secret van.
- Ha secret display name alapjan tortenik a feloldas, a Dynamic Groupnak resource search / secret metadata olvasasi jog is kell a secret OCID megtalalasahoz.
- Vault secret hasznalata eseten a private hoston legyen telepitve az OCI CLI, es az SSH-n futtatott non-interactive shell PATH-jaban is latszodjon.
- Ha az OCI CLI nincs telepitve, a private deploy script ideiglenes OCI CLI kontenert futtat (`ghcr.io/oracle/oci-cli:latest`) `docker run --rm --network host` modon, instance principal auth-tal.
- A Notifications topic es email subscription letrehozasahoz a Dynamic Groupnak `manage ons-topics` es `manage ons-subscriptions` jog kell abban a compartmentben, ahol a topic el.

Pelda policy minta:

```text
Allow dynamic-group <dynamic-group-name> to read secret-bundles in compartment prodet-new
```

## Erzekeny adatok kezelese

- Gitbe nem kerulhet privat kulcs, kulcsfajl-nev, abszolut lokalis path, szemelyes felhasznalonev, email, token vagy cloud credential.
- Deploy parancsokban szemelyes path helyett env valtozot kell hasznalni, peldaul `${AUTOFORGE_SSH_KEY}`.
- Registry owner, repo owner es account nev csak placeholderkent vagy GitHub Actions runtime valtozokent szerepelhet.
- Konkreten hasznalt secret ertekek gitelt fajlban nem lehetnek; provider runtime secret ertekek OCI Vaultban vannak, es csak deploy futaskor kerulnek at ideiglenes runtime env fajlba.

## Szerver bootstrap minimum

Mindket gepen kell:

- Docker Engine
- Docker Compose plugin vagy standalone `docker-compose`
- olyan SSH user, amelyik tud `docker compose` parancsot futtatni

A private gepen plusz:

- csatolt es mountolt workspace volume: `/mnt/autoforge-workspace`
- olyan SSH user, amelyik jelszo nelkuli `sudo iptables` joggal tudja tiltani a kontener metadata endpoint hozzaferest a Docker `DOCKER-USER` chainben

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

## Always Free NAT instance celallapot

Az Always Free tierben nincs hasznalhato managed Service Gateway vagy NAT Gateway limit, ezert a private backend egress celallapota self-managed NAT instance a public hoston.

Celhalozat:

- VCN: `vcn-prodet-new`, `10.42.0.0/16`
- Public subnet: `subnet-prodet-new-public`, `10.42.0.0/24`, `0.0.0.0/0 -> Internet Gateway`
- NAT host: `prodet-new-e2-public-01`, private IP `10.42.0.241`, public IP `144.24.176.5`
- Private subnet: `subnet-autoforge-private`, `10.42.1.0/24`, public IP tiltva
- Private host: `prodet-new-e2-private-03`, private IP `10.42.1.144`, public IP nelkul
- Private subnet route table: `0.0.0.0/0 -> 10.42.0.241` private IP route target
- Private security list: inbound csak `10.42.0.0/24` es `10.42.1.0/24`, outbound `0.0.0.0/0`

Public NAT host kovetelmenyek:

- a public host VNIC-en `skip_source_dest_check = true`
- Linux IP forwarding: `net.ipv4.ip_forward=1`
- reverse path filter tiltva a NAT interface-en
- iptables MASQUERADE a private subnetre: `10.42.1.0/24 -> ens3`
- iptables FORWARD szabalyok a private subnet kimenore es established/related visszaforgalomra

Aktualis allapot:

- A private subnet, private security list es private route table letrejott.
- A public host NAT service (`autoforge-nat.service`) beallitja az IP forwardingot es iptables NAT szabalyokat.
- A regi `prodet-new-e2-private-02` instance terminálva lett, mert a `VM.Standard.E2.1.Micro` shape csak egy VNIC-et enged.
- Az uj `prodet-new-e2-private-03` instance kozvetlenul a `10.42.1.0/24` private subnetben fut.
- A 100 GB-os `autoforge-private-workspace-100gb` block volume az uj private hoston `/mnt/autoforge-workspace` alatt mountolva van.
- A private deploy GitHub secretjei az uj private IP-re mutatnak: `OCI_PRIVATE_HOST`, `OCI_PRIVATE_SSH_HOST`, `OCI_BACKEND_UPSTREAM`.
- Az ADB ACL engedi a NAT public IP-t: `144.24.176.5/32`.
- Az `ociprodet-backend-dg` dynamic group compartment-alapu, igy uj private instance rebuild utan is lefedi a `prodet-new` compartment compute instance-eit.
- Az `autoforge-backend-vault-read` policy engedi a dynamic groupnak a Vault secret bundle olvasast.
- A DB jelszo Vault secretben van: `autoforge-db-password`; GitHubban csak a secret OCID es a nem erzekeny JDBC URL deploy input szerepel.
- A regi public subnet route table-t nem szabad `0.0.0.0/0 -> NAT instance` iranyba atallitani, mert ugyanazon a subneten van a public host is, es ez elvagna a public host sajat outbound forgalmat.

Terraform reprodukciohoz rogzitendo eroforrasok:

- `oci_core_vcn` a `10.42.0.0/16` VCN-hez
- `oci_core_internet_gateway` es public route table `0.0.0.0/0 -> Internet Gateway`
- `oci_core_route_table` a private subnethez `0.0.0.0/0 -> oci_core_private_ip.public_nat_primary.id`
- `oci_core_subnet` public subnet `10.42.0.0/24`, public IP engedelyezve
- `oci_core_subnet` private subnet `10.42.1.0/24`, `prohibit_public_ip_on_vnic = true`
- public compute VNIC `skip_source_dest_check = true`
- cloud-init vagy remote provisioner a NAT hoston az `autoforge-nat.service` letrehozasara
- private compute instance kozvetlenul a private subnetben, public IP nelkul
- dynamic group rule: `instance.compartment.id = <prodet-new compartment OCID>`
- IAM policy: dynamic group olvashat `secret-bundles` eroforrast a `prodet-new` compartmentben
- ADB ACL rule a NAT public IP-re
- Vault secret a DB jelszora, es deploy secret/variable a DB URL-re es password secret OCID-ra

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
