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
- completed

Taskok
- `AUTO-15`: Landing page cleanup
- `AUTO-16`: Agent operating rules
- `AUTO-18`: Version tracking
- `AUTO-20`: Keycloak web client configuration

Megjegyzes
- Processz es UI tisztitas: agent szabalyok, release manifest, landing cleanup es deployment sorrend tanulsagok.

## 0.1.2

Statusz
- completed

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
- `AUTO-325`: Vault-backed OpenRouter runtime secret references
- `AUTO-326`: AI runtime cost and health guardrails

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
- `AUTO-242`: MVP prompt alapú job létrehozás - done

Megjegyzes
- A job létrehozó API prompt alapján hoz létre queued jobot; Jira key-t nem kér a usertől.

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
- A repo dokumentáció prompt-first modellre lett igazítva: a user promptot ad meg, a Jira issue key-t pedig Jira generálja, nem user input.

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
- `AUTO-211`: Job processor a teljes MVP backend flow összekötésére

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
- `AUTO-225`: Prompt beküldő felület

Megjegyzes
- A web UI authenticated prompt submit formot kap, amely queued jobot hoz létre a backendben.

## 0.1.41

Statusz
- in progress

Taskok
- `AUTO-270`: Jira-first docs cleanup

Megjegyzes
- A repo eltávolítja a helyi task és epic markdownokat, és a manifestekre/Jira state-re támaszkodik.

## 0.1.42

Statusz
- in progress

Taskok
- `AUTO-93`: Autonomous DB vector capability check

Megjegyzes
- A Learning RAG sprint Autonomous Database vector capability döntése dokumentálva van secret-mentes runtime smoke checkkel és Always Free korlátokkal.

## 0.1.43

Statusz
- in progress

Taskok
- `AUTO-341`: Tananyag hozzárendelés diákokhoz és tanulócsoportokhoz

Megjegyzes
- Az első Learning implementációs slice a hozzáférés- és hozzárendelés-kezelést vezeti be a tananyagokhoz.

## 0.1.44

Statusz
- in progress

Taskok
- `AUTO-333`: Tananyag feltöltés és userhez kötött dokumentum metaadat

Megjegyzes
- A Learning backend megkapja a multipart alapú feltöltési slice-ot, a userhez kötött metadatákkal, méretkorláttal és kezdeti formátumtámogatással.

## 0.1.45

Statusz
- in progress

Taskok
- `AUTO-338`: Learning API contract web és Android klienshez

Megjegyzes
- A Learning namespace kliensfüggetlen contractját rögzíti, beleértve a bearer authot, a mobilbarát hibaszabályokat és a planned endpoint bővítési pontokat.

## 0.1.46

Statusz
- in progress

Taskok
- `AUTO-331`: Tananyag ingestion, chunking és embedding pipeline

Megjegyzes
- A Learning backend ingestion job, chunk és embedding modellel, valamint status/retry endpointokkal kap determinisztikus pipeline alapot.

## 0.1.47

Statusz
- in progress

Taskok
- `AUTO-332`: Kérdésgenerálás és összefoglalók tananyagból

Megjegyzes
- A Learning backend megkapja a practice question és summary generálást, user-bound persistence-t és fallback üzenetet AI provider hiányában.

## 0.1.48

Statusz
- in progress

Taskok
- `AUTO-258`: RAG storage és document chunk modell tervezése

Megjegyzes
- A Learning RAG adattárolási boundary megkapja a chunk source offseteket és az adapter-független embedding rekordokat.

## 0.1.49

Statusz
- in progress

Taskok
- `AUTO-335`: Learning navigáció és tanulási workspace shell
- `AUTO-337`: Személyre szabott learner profile és retrieval context

Megjegyzes
- A Learning web shell saját anyagokat, generált kérdéseket és összefoglalókat mutat upload CTA-val és user-scoped listákkal.
- A Learning profilréteg user-scoped preferenciákat és prompt contextet ad a személyre szabott generáláshoz.

## 0.1.50

Statusz
- in progress

Taskok
- `AUTO-336`: Hibrid RAG adatmodell OCI Always Free célra

Megjegyzes
- A Learning RAG relációs metadatáját, chunk/embedding boundary-ját és user isolation mezőit rögzíti az OCI Always Free kompatibilis MVP-hez.

## 0.1.51

Statusz
- in progress

Taskok
- `AUTO-339`: AI kérdéssor generálás tananyagból és mentés DB-be

Megjegyzes
- A Learning kérdéssor generálás strukturált kérdés/opció/adatmodellje, fallback státusza és DB-s mentése kerül be.

## 0.1.52

Statusz
- in progress

Taskok
- `AUTO-340`: Tananyag részletező weboldal jogosultságfüggő mezőkkel

Megjegyzes
- A Learning tananyag detail route a role-alapú UI mezőket, ingestions státuszt, hozzárendeléseket és generált tartalmakat mutatja.

## 0.1.53

Statusz
- in progress

Taskok
- `AUTO-366`: Feltöltés utáni metadata mentés

Megjegyzes
- Az upload flow object storage reference-et, hash-t és metadata mezőket ment a Learning tananyag rekordba.
- PR #157 lezárta a slice-ot.

## 0.1.54

Statusz
- in progress

Taskok
- `AUTO-343`: Diák kérdéssor megoldás és tanulási eredmények mentése

Megjegyzes
- A Learning backend kérdéssor-listázást, diák válaszbeküldést, pontszámítást és owner/student scoped eredménylistát kap.

## 0.1.55

Statusz
- in progress

Taskok
- `AUTO-358`: Object Storage hiba tesztek

Megjegyzes
- Az eredeti fájl Object Storage előkészítési hibaágai célzott teszteket kapnak, hogy olvasási hiba esetén kontrollált exception szülessen.

## 0.1.56

Statusz
- in progress

Taskok
- `AUTO-397`: Teacher hibabejelentés review és pontszám felülbírálás

Megjegyzes
- A student saját question attempthez dispute-ot nyithat, a teacher review-zhatja és score override-ot menthet.

## 0.1.57

Statusz
- in progress

Taskok
- `AUTO-371`: Tananyag több source életciklus és ingestion update

Megjegyzes
- A Learning tananyagok több forrást kezelnek, a detail nézet source listát és owner műveleteket kap, az ingestion pedig az aktív source-okból dolgozik.

## 0.1.58

Statusz
- in progress

Taskok
- `AUTO-372`: Learning teacher CRUD és student megoldási jogosultságok

Megjegyzes
- A Learning felület és backend explicit teacher/admin guardot kap a tananyag létrehozásához, miközben a student csak a hozzárendelt tananyagok megoldási útvonalait látja.

## 0.1.59

Statusz
- in progress

Taskok
- `AUTO-439`: Kérdéssor draft és publikálás student-visible állapotba

Megjegyzes
- A Learning question set generálás draft státuszban indul, a teacher publish/archive műveletekkel kezeli a láthatóságot, a student pedig csak a publikált kérdéssorokat éri el.

## 0.1.60

Statusz
- in progress

Taskok
- `AUTO-433`: Tananyag forrásverziózás és generált tartalom eredete

Megjegyzes
- A generált tartalom most a forrás snapshotját is tárolja, így a régi generálások visszakövethetők maradnak még forrás törlése után is.

## 0.1.61

Statusz
- in progress

Taskok
- `AUTO-374`: Egy- és többhelyes válaszos kérdések AI promptban és pontozásban

Megjegyzes
- A learning kérdéspayload most single- és multi-correct módot is hordoz, a generálás kevert kérdéstípusokat ad, a pontozás pedig halmazegyezést vár multi-correct esetben.

## 0.1.62

Statusz
- in progress

Taskok
- `AUTO-391`: Teacher tananyag hozzárendelés diákhoz és csoporthoz

Megjegyzes
- A Learning detail oldalon az owner most közvetlenül szerkesztheti a student és group assignment listát, a backend pedig a meglévő assignment replace API-t használja.

## 0.1.63

Statusz
- in progress

Taskok
- `AUTO-403`: Assignment és próbálkozás státuszok

Megjegyzes
- A Learning modul külön progress entitással követi az assigned, started, submitted, reviewed és completed állapotokat, hogy később Duolingo/Anki-szerű ismétlési logika épülhessen rá.

## 0.1.64

Statusz
- in progress

Taskok
- `AUTO-409`: Határidő és maximális próbálkozásszám kérdéssorhoz

Megjegyzes
- A Learning question set settings most deadline és max attempts korlátot kap, a backend pedig ezek alapján blokkolja a túl késői vagy túl sokadik beadásokat.

## 0.1.65

Statusz
- in progress

Taskok
- `AUTO-415`: Tanulasi eredmeny dashboard tananyaghoz es csoporthoz

Megjegyzes
- A Learning modul teacher dashboard slice-a progress sorokhoz csoportmetadata-t kot, es a material detail oldalon aggregalt progress listat ad filterezessel.
