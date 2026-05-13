# [AUTO-204] Git broker API contract

## Feladat leírása

Definiálni kell a Git broker create PR contractját, DTO-it és validációját, hogy a broker később külön service-ként is stabilan cserélhető maradjon.

## Statusz

review

## Verzió

0.1.33

## Branch

feature/auto-204-git-broker-contract

## PR

- PR #82

## Cél

A job processor később egy stabil belső contracton keresztül tudjon PR létrehozást kérni a Git brokertől.

## Scope

- `CreatePullRequestRequest` DTO
- `CreatePullRequestResponse` DTO
- `GitBrokerClient` interface
- request validáció
- rövid contract dokumentáció

## Elfogadási kritériumok

- Van `CreatePullRequestRequest` DTO.
- Van `CreatePullRequestResponse` DTO.
- Van `GitBrokerClient` interface `createPullRequest` metódussal.
- A request validálja a kötelező mezőket.
- A contract dokumentálva van a README-ben.

## Lepesnaplo

- [x] Áttekintettem a backlog JSON contract mintáját.
- [x] Létrehoztam a request/response DTO-kat és a client interface-t.
- [x] Hozzáadtam a validation unit tesztet.
- [x] Frissítettem a README contract összefoglalóját.
- [x] A PR megnyitva review-ra.

## Eredmény

A Git broker PR contract készen áll a későbbi implementációra.
