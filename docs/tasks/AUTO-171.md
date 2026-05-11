# [AUTO-171] Lokális MVP docker-compose és konfiguráció

## Feladat leírása

Hozz létre egy helyben futtatható MVP compose stack-et a frontend és backend számára, SQLite volume-mal és példa `.env` fájllal, hogy egy új fejlesztő egyetlen README alapján el tudja indítani a rendszert.

## Statusz

in_progress

## Verzió

0.1.25

## Branch

feature/order-mvp-implementation

## PR

- (Nincs még)

## Cél

A frontend és a backend lokálisan indítható legyen dokumentált portokon, miközben a backend már megkapja a későbbi MVP job-folyamathoz szükséges konfigurációs környezeti változókat.

## Scope

- `docker-compose.yml` a repo gyökerében
- `.env.example` placeholder értékekkel
- backend konfigurációs környezeti változók
- README indítási és leállítási útmutató

## Elfogadási kritériumok

- A `docker compose up --build` elindítja a backend és frontend service-eket.
- A backend elérhető a dokumentált porton, és a health endpoint válaszol.
- A frontend böngészőből elérhető a dokumentált porton.
- A konfiguráció nem tartalmaz beégetett secretet.
- Az SQLite tároló külön, perzisztens volume-on van.

## Lepesnaplo

- [x] A lokális MVP compose igényét és a repo konvenciókat áttekintettem.
- [x] Létrehoztam a gyökérszintű `docker-compose.yml` fájlt.
- [x] Hozzáadtam a `.env.example` mintát a szükséges placeholder értékekkel.
- [x] Frissítettem a backend konfigurációt a lokális MVP környezethez.
- [x] Frontend build ellenőrzés.
  ```bash
  npm run build:web
  ```
- [x] Backend build ellenőrzés.
  ```bash
  npm run build:backend
  ```
- [x] YAML szintaxis ellenőrzés.
  ```bash
  python3 -c 'import yaml; yaml.safe_load(open("docker-compose.yml")); yaml.safe_load(open("services/backend/src/main/resources/application.yml")); print("yaml-ok")'
  ```
- [x] Compose CLI ellenőrzés.
  ```bash
  docker compose --env-file .env.example config
  ```
  Leírás: a környezetben nem volt használható Compose plugin/CLI, ezért a parancs nem futott le sikeresen.

## Eredmény

A lokális MVP compose és konfiguráció alapjai elkészültek, a README runbook frissítve lett, a compose CLI ellenőrzés viszont a környezeti korlát miatt nem futtatható ezen a gépen.
