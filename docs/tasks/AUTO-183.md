# [AUTO-183] Job létrehozó API implementálása

## Feladat leírása

Implementálni kell a `POST /api/v1/jobs` endpointot, amely valid requestből perzisztált, `QUEUED` státuszú jobot hoz létre.

## Statusz

completed

## Verzió

0.1.29

## Branch

auto-183-create-job-api

## PR

- (Nyitás alatt)

## Cél

A UI és az automatizált tesztek megbízhatóan tudjanak új jobot létrehozni a backend API-n keresztül, validált Jira issue kulccsal.

## Scope

- `CreateJobRequest` DTO validációval
- `CreateJobResponse` DTO `jobId`, `jiraIssueKey`, `status` mezőkkel
- `JobController` `POST /api/v1/jobs` endpoint
- `JobService` perzisztált `QUEUED` job létrehozással
- Controller tesztek valid requestre, üres prompt hibára, invalid Jira key hibára és DB mentésre

## Elfogadási kritériumok

- Valid requestre `201 Created` válasz érkezik.
- A válasz tartalmazza a `jobId` mezőt és a `QUEUED` státuszt.
- Üres prompt esetén `400` hiba érkezik.
- Érvénytelen Jira issue key esetén `400` hiba érkezik.
- A létrehozott job adatbázisba kerül.

## Subtaskok

- `AUTO-184`: CreateJobRequest DTO
- `AUTO-185`: CreateJobResponse DTO
- `AUTO-186`: JobController POST endpoint

## Lepesnaplo

- [x] Áttekintettem az `AUTO-183` Jira scope-ot és subtaskokat.
- [x] Bevezettem a `CreateJobResponse` DTO-t explicit `jobId` válasszal.
- [x] Kiegészítettem a controller teszteket üres prompt és perzisztálás ellenőrzéssel.
- [x] `mvn test` sikeresen lefutott a backend modulban.

## Eredmény

A job létrehozó API implementálva van, és a létrehozott jobot `QUEUED` státusszal adatbázisba menti.
