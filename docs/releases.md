# Releases

Ez a manifest mutatja, hogy melyik story vagy bugfix melyik projektverziohoz tartozik. Uj feladatot csak akkor szabad elkezdeni, ha itt is szerepel a celverzio alatt.

## 0.1.0

Statusz
- released baseline

Taskok
- `AUTO-1`: React frontend base
- `AUTO-2`: Spring backend base
- `AUTO-3`: Docker deploy foundation
- `AUTO-4`: Deploy workflow SSH fix
- `AUTO-5`: Private deploy jump fix
- `AUTO-6`: Manual deploy network fix
- `AUTO-7`: Security audit
- `AUTO-8`: API gateway behind Caddy
- `AUTO-9`: OpenCode REST AI
- `AUTO-10`: OpenCode Vault fix
- `AUTO-11`: Private workspace volume
- `AUTO-12`: Feature flags research
- `AUTO-13`: Keycloak OIDC
- `AUTO-14`: Auto deploy on merge
- `BUG-1`: Branch policy and CI triggers
- `BUG-2`: SSH deploy host split
- `BUG-3`: Caddy gateway domain config
- `BUG-4`: Frontend Keycloak redirect issue
- `BUG-5`: Keycloak fragment callback handling
- `BUG-6`: Workflow version quoting fix

Megjegyzes
- Elso mukodo platform baseline: frontend, backend, gateway, OCI deploy es auth alapok.

## 0.1.1

Statusz
- in progress

Taskok
- `AUTO-15`: Landing page cleanup
- `AUTO-16`: Agent operating rules
- `AUTO-18`: Version tracking
- `AUTO-20`: Keycloak web client configuration

Megjegyzes
- Processz es UI tisztitas: agent szabalyok, release manifest, landing cleanup es deployment sorrend tanulsagok.

## 0.1.2

Statusz
- in progress

Taskok
- `AUTO-21`: Automatic version bump rule

Megjegyzes
- Nem-MAJOR taskoknal a verzio bump automatikus; csak MAJOR emelesnel kerunk kulon jovahagyast.

## 0.1.3

Statusz
- in progress

Taskok
- `BUG-7`: Sign-in button shown to authenticated users
- `BUG-8`: Improve error message for 404 API route failures

Megjegyzes
- Auth flow es API route hiba megjelenites javitasok.

## 0.1.4

Statusz
- in progress

Taskok
- `BUG-9`: Gateway route configuration missing from image

Megjegyzes
- A gateway route konfiguracio bekerul a kontener image-be, hogy a publikus `/api/**` endpointok a backendhez route-oljanak.

## 0.1.5

Statusz
- in progress

Taskok
- `BUG-10`: Public deploy trigger misses gateway image changes

Megjegyzes
- A public deploy workflow gateway es verzio valtozasokra is lefut, hogy az uj gateway image kikeruljon OCI-ra.

## 0.1.6

Statusz
- in progress

Taskok
- `BUG-11`: Private deploy trigger misses version changes

Megjegyzes
- A private deploy workflow root verzio valtozasokra is lefut, hogy a backend image tag frissulese kikeruljon a private hostra.

## 0.1.7

Statusz
- in progress

Taskok
- `BUG-12`: Normalize public backend upstream

Megjegyzes
- A public deploy normalizalja a backend upstream erteket, hogy a gateway mindig host:port formatumot kapjon.

## 0.1.8

Statusz
- in progress

Taskok
- `BUG-13`: Gateway Spring dependency mismatch

Megjegyzes
- A gateway Spring Boot parent patch verzioja illeszkedik a Spring Cloud Gateway runtime elvarasaihoz.

## 0.1.9

Statusz
- in progress

Taskok
- `BUG-14`: Keycloak callback init falls back to public state

Megjegyzes
- A frontend a login callback URL-en nem `check-sso`, hanem normal callback-feldolgozassal inicializalja a Keycloak klienst.

## 0.1.10

Statusz
- in progress

Taskok
- `BUG-15`: Keycloak callback requires explicit login-required init

Megjegyzes
- A login callback URL-en a Keycloak init `login-required` modban fut, hogy a session biztosan `ready` allapotba keruljon.

## 0.1.11

Statusz
- in progress

Taskok
- `BUG-16`: Keycloak callback detection is too narrow

Megjegyzes
- A frontend a Keycloak redirect URL-t robusztusabban ismeri fel, nem csak `state+code` eseten.

## 0.1.12

Statusz
- in progress

Taskok
- `BUG-17`: Backend JWT validation depends on remote JWKS availability

Megjegyzes
- A backend statikus Keycloak publikus kulccsal validalja a JWT-t, hogy productionben ne bukjon el remote JWKS/issuer eleresen.

## 0.1.13

Statusz
- in progress

Taskok
- `AUTO-22`: Documentation and architecture refresh

Megjegyzes
- A README, az architektura, a deploy es az auth dokumentacio mostani live OCI allapothoz es workflow viselkedeshez igazodik.

## 0.1.14

Statusz
- in progress

Taskok
- `AUTO-23`: AI tooling stack integration

Megjegyzes
- A repo sajat MCP, skill es group manifesteket kap az auth/gateway/deploy/OCI munkafolyamatok tamogatasara.

## 0.1.15

Statusz
- completed

Taskok
- `AUTO-24`: Finalize AI Tooling integration and PR protocol

Megjegyzes
- Az AI Tooling stack (MCP, Skill, Group) integracioja es dokumentacioja (Context7) veglegesitese, valamint a pre-PR protokoll bevezetese.

## 0.1.16

Statusz
- in progress

Taskok
- `AUTO-25`: Integrate Gemini and ChatGPT API keys into OpenCode runtime

Megjegyzes
- A Gemini és ChatGPT API kulcsok kezelése az OCI Vault-ból a belső OpenCode konténer számára.

## 0.1.17

Statusz
- completed

Taskok
- `AUTO-26`: Add Gemini API key placeholder to .env.private.example

Megjegyzes
- Dokumentációs frissítés: Gemini helyőrző felvétele a példa környezeti változók közé.

## 0.1.18

Statusz
- in progress

Taskok
- `AUTO-27`: Integrate security auditing tools and skills

Megjegyzes
- Biztonsági eszközök (SAST, titokkeresés, függőség-audit) és egy átfogó `security-audit` skill integrálása.

## 0.1.20

Statusz
- in progress

Taskok
- `AUTO-29`: Define Jira migration epic

Megjegyzes
- EPIC-3 kidolgozása a meglévő Markdown taskok, bugok és epicek Jira-ba migrálására és a Jira-first működés bevezetésére.

## 0.1.21

Statusz
- in progress

Taskok
- `AUTO-31`: Add Jira migration dry-run generator

Megjegyzes
- Credential nélküli Jira migrációs dry-run generátor és idempotens Jira importáló a Markdown taskok, epicek, story breakdownok és release mapping alapján.

## 0.1.22

Statusz
- in progress

Taskok
- `AUTO-136`: Enforce Terraform scripts for reproducible infrastructure changes
- `AUTO-137`: OCI always-free observability strategy

Megjegyzes
- Az infrastruktúra-, host readiness-, OCI- és runtime provisioning jellegű változásokhoz Terraform script vagy modul kötelező, hogy a módosítás nulláról reprodukálható legyen.
- Az observability stratégia az OCI Always Free korlátaihoz igazodik, és self-hosted vagy könnyű komponensekre épít.

## 0.1.24

Statusz
- in progress

Taskok
- `AUTO-163`: Core application feature epic

Megjegyzes
- A core application feature epic a user-facing termékfunkciókat csoportosítja külön a platform hardening epicektől.

## 0.1.24

Statusz
- in progress

Taskok
- `AUTO-163`: Core application feature epic
- `AUTO-164`: Project dashboard and navigation shell
- `AUTO-165`: Task workspace list and detail view
- `AUTO-166`: Jira-linked feature flow
- `AUTO-167`: User session and profile surface
- `AUTO-168`: Command runner and execution status view
- `AUTO-169`: Prompt/history workspace view

Megjegyzes
- A core application feature epic a user-facing termékfunkciókat csoportosítja külön a platform hardening epicektől.

## 0.1.23

Statusz
- in progress

Taskok
- `AUTO-162`: Task changes require merged PR before Done

Megjegyzes
- A filesystemet vagy tracked artifactot változtató nem-spike taskok csak merge és review után markolhatók Done-nak.

## 0.1.26

Statusz
- completed

Taskok
- `AUTO-175`: Spring Boot backend alap és API konvenciók

Megjegyzes
- A backend controller/service/dto/config struktúrája és a `/api/v1/health` konvenció itt kerül nyilvántartásra.

## 0.1.27

Statusz
- in progress

Taskok
- `AUTO-179`: Job entity, repository és adatbázis séma

Megjegyzes
- A job persistence réteg és az Oracle Autonomous Database / OCI Free Tier irány ebben a verzióban kerül bevezetésre.

## 0.1.28

Statusz
- completed

Taskok
- `AUTO-242`: MVP Jira issue key validáció és tárolás - done

Megjegyzes
- A job létrehozó API Jira key validációval és tárolással ebben a verzióban bevezetésre került.

## 0.1.29

Statusz
- completed

Taskok
- `AUTO-183`: Job létrehozó API implementálása - done

Megjegyzes
- A `POST /api/v1/jobs` endpoint valid requestből perzisztált, `QUEUED` státuszú jobot hoz létre, és explicit `jobId` mezőt ad vissza.

## 0.1.30

Statusz
- completed

Taskok
- `AUTO-187`: Job lekérdező API implementálása - done

Megjegyzes
- A `GET /api/v1/jobs/{jobId}` endpoint visszaadja a job részletes állapotát, és 404-et ad nem létező job ID-ra.

## 0.1.31

Statusz
- completed

Taskok
- `AUTO-190`: Minimális audit log implementálása SQLite alapon - done

Megjegyzes
- Bevezetésre került az audit log entitás, repository és service, valamint a Lombok használata az új audit rétegben.

## 0.1.32

Statusz
- completed

Taskok
- `AUTO-194`: Mock AI patch generator - done

Megjegyzes
- Elkészült a determinisztikus mock patch generator, amely valid unified diffet ad vissza a backend flow teszteléséhez.

## 0.1.33

Statusz
- completed

Taskok
- `AUTO-204`: Git broker API contract - done

Megjegyzes
- Definiálásra került a Git broker create PR request/response contractja és a hozzá tartozó validation boundary.

## 0.1.34

Statusz
- in progress

Taskok
- `AUTO-208`: Git repository clone és branch létrehozás

Megjegyzes
- A Git broker a repository klónozását, base branch checkoutot és feature branch létrehozást végzi temp workspace-ben.

## 0.1.35

Statusz
- in progress

Taskok
- `BUG-18`: Prompt-first MVP flow documentation mismatch

Megjegyzes
- A repo dokumentáció prompt-first modellre lett igazítva: a user promptot ad meg, ebből Jira task jön létre, és a Jira issue key megy tovább a flow-ban.

## 0.1.36

Statusz
- in progress

Taskok
- `AUTO-212`: Patch alkalmazása és commit létrehozása

Megjegyzes
- A Git broker a klónozott repositoryban unified diffből commitot készít, és a commit SHA-t visszaadja a további PR flow-hoz.

## 0.1.37

Statusz
- in progress

Taskok
- `AUTO-217`: Branch push és GitHub PR nyitás

Megjegyzes
- A Git broker a remote branchet pusholja és GitHub PR-t nyit a megadott title/body értékekkel.

## 0.1.38

Statusz
- in progress

Taskok
- `AUTO-198`: Job processor a teljes MVP backend flow összekötésére

Megjegyzes
- A job processor a queued jobokat futtatott státuszokon, patch generáláson és Git broker híváson keresztül PR_OPENED vagy FAILED állapotba viszi.

## 0.1.39

Statusz
- in progress

Taskok
- `AUTO-221`: React dashboard és navigációs shell

Megjegyzes
- A frontend egységes dashboard keretet és navigációt kap a későbbi job, Jira és PR nézetekhez.

## 0.1.40

Statusz
- in progress

Taskok
- `AUTO-225`: Jira kulcsos prompt beküldő felület

Megjegyzes
- A web UI authenticated prompt submit formot kap, amely Jira issue key-vel hoz létre queued jobot a backendben.

## 0.1.41

Statusz
- in progress

Taskok
- `AUTO-270`: Jira-first docs cleanup

Megjegyzes
- A repo eltávolítja a helyi task és epic markdownokat, és a manifestekre/Jira state-re támaszkodik.
