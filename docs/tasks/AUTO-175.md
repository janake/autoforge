# [AUTO-175] Spring Boot backend alap és API konvenciók

## Feladat leírása

Rendezd a Spring Boot backend alapját úgy, hogy egységes `/api/v1` konvenciót, health endpointot és JSON-alapú hibaválaszokat adjon, miközben a későbbi job store/config réteghez is előkészíti a package struktúrát.

## Statusz

completed

## Verzió

0.1.26

## Branch

feature/order-mvp-implementation

## PR

- PR #72
- PR #73

## Cél

A backend legyen buildelhető, tesztelhető és API-konvencióban egységes, hogy a későbbi `job` entity, repository és API réteg erre tudjon épülni.

## Scope

- `/api/v1/health` endpoint
- `/api/v1/status` és `/api/v1/me` controller konvenció
- központi JSON hibaválasz formátum
- `controller`, `service`, `repository`, `domain`, `dto`, `config` package struktúra
- MVP GitHub konfigurációs property-k

## Elfogadási kritériumok

- A backend Maven buildje hibamentesen lefut.
- A backend ad `GET /api/v1/health` választ.
- Minden MVP endpoint `/api/v1` alatt van.
- A hibák JSON envelope formában térnek vissza.
- A package struktúra előkészíti a későbbi repository/domain réteget.

## Lepesnaplo

- [x] Áttekintettem az MVP backend scope-ot és a meglévő backend állapotát.
- [x] Bevezettem a controller/service/dto/config struktúrát.
- [x] Hozzáadtam az `/api/v1/health` endpointot.
- [x] Hozzáadtam a globális JSON exception handlert.
- [x] Frissítettem a backend teszteket.
- [x] Backend build és teszt futtatás.
  ```bash
  npm run build:backend
  ```
  Leírás: a backend Maven build és a kapcsolódó Spring tesztek sikeresen lefutottak.
- [x] Whitespace ellenőrzés.
  ```bash
  git diff --check
  ```
  Leírás: a módosításokban nem maradt whitespace vagy patch-formázási hiba.
- [x] GitHub PR létrehozás.
  Leírás: a branchhez elkészült a PR #72.
- [x] Jira MCP frissítés és státuszkezelés.
  Leírás: az MCP Jira API v3 searchre váltott, támogatja a transition/comment műveleteket, az `AUTO-175` Jira story `Under test` státuszba került.
- [x] Jira MCP export támogatás.
  Leírás: az MCP `jira_export_issues` toolt kapott, amely project key vagy JQL alapján JSON-ba exportálja az epiceket, normál issue-kat és subtaskokat.

## Eredmény

A backend alapja készen áll a job entity, repository és API bővítésre.
