# [AUTO-179] Job entity, repository és adatbázis séma

## Feladat leírása

Készítsd el a backend job persistence alapját Oracle Autonomous Database Always Free / OCI Free Tier irányra, úgy hogy a job állapot, repo adat és PR link perzisztálható legyen.

## Statusz

in_progress

## Verzió

0.1.27

## Branch

feature/AUTO-179-job-persistence

## PR

- (Nincs még)

## Cél

A backend tartós job modellt használjon JPA repository-val, és erre épülhessen a későbbi job létrehozó/lekérdező API.

## Scope

- `Job` entity
- `JobStatus` enum
- `JobRepository`
- Oracle ADB oriented datasource/JPA config
- repository query tesztek

## Elfogadási kritériumok

- A `Job` entity létrejön és perzisztálható.
- A job státusz enum a tervezett MVP állapotokat tartalmazza.
- A repository tud státusz szerint listázni és Jira kulcs alapján keresni.
- Az adatbázis irány Oracle Autonomous Database Always Free / OCI Free Tier.
- Nincs SQLite vagy local file-based fallback.

## Lepesnaplo

- [x] Áttekintettem az `AUTO-179` Jira scope-ot és a meglévő backend állapotát.
- [x] Bevezettem a job entity, enum és repository kódot.
- [x] Beállítottam az Oracle Autonomous Database / OCI Free Tier orientált datasource és JPA configot.
- [x] Hozzáadtam repository szintű teszteket H2 test környezetben.
- [x] A SecurityConfig saját JWT decoderrel indul a bundled public key alapján.
- [x] Backend build és teszt futtatás.
  ```bash
  npm run build:backend
  ```
  Leírás: a backend Maven build és a kapcsolódó Spring tesztek sikeresen lefutottak.

## Eredmény

A job persistence réteg készen áll a job API és processor következő lépéseihez.
