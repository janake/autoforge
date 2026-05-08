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

Megjegyzes
- Az infrastruktúra-, host readiness-, OCI- és runtime provisioning jellegű változásokhoz Terraform script vagy modul kötelező, hogy a módosítás nulláról reprodukálható legyen.
