# [AUTO-187] Job lekérdező API implementálása

## Feladat leírása

Implementálni kell a `GET /api/v1/jobs/{jobId}` endpointot, amely visszaadja egy job aktuális állapotát és eredményét.

## Statusz

completed

## Verzió

0.1.30

## Branch

auto-187-job-query

## PR

- https://github.com/janake/autoforge/pull/79

## Cél

A frontend és az automatizált folyamatok le tudják kérdezni egy job jelenlegi állapotát és metadatait.

## Scope

- `JobResponse` DTO a job részletes állapotával
- `JobController` `GET /api/v1/jobs/{jobId}` endpoint
- `JobService` lekérdező metódus
- 404 kezelés nem létező job ID-ra
- Controller teszt létező és hiányzó jobra

## Elfogadási kritériumok

- Létező job ID esetén `200` választ ad.
- A válasz tartalmazza: `jobId`, `jiraIssueKey`, `prompt`, `status`, `prUrl`, `errorMessage`, `createdAt`, `updatedAt`.
- Nem létező job ID esetén `404` választ ad.
- A response alkalmas frontend pollingra.

## Subtaskok

- `AUTO-188`: JobResponse DTO
- `AUTO-189`: GET /api/v1/jobs/{jobId}

## Lepesnaplo

- [x] Áttekintettem az `AUTO-187` Jira scope-ot és subtaskokat.
- [x] Bevezettem a GET endpointot és a 404 hibakezelést.
- [x] Kiegészítettem a controller teszteket létező és hiányzó job ID esetre.
- [x] `mvn test` sikeresen lefutott a backend modulban.

## Eredmény

A job lekérdező API implementálva van, és a job részletes állapotát visszaadja.
