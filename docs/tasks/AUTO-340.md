# [AUTO-340] Tananyag részletező weboldal jogosultságfüggő mezőkkel

## Feladat leírása

A Learning tananyag részletező oldalának role-alapú nézetet kell adnia: a diák csak a saját tanulási tartalmát és állapotát látja, az owner pedig az admin és hozzárendelési adatokat is.

## Statusz

in progress

## Verzió

0.1.52

## Branch

feature/AUTO-340-learning-detail

## PR

- TBD

## Cél

A learning listából egy külön detail route nyíljon meg, és azon a user szerepének megfelelő mezők jelenjenek meg.

## Scope

- learning material detail route
- role-alapú UI mezők
- ingestion és generation státusz megjelenítése
- question set és summary history megjelenítése

## Elfogadási kritériumok

- A detail route megnyitható a learning listából.
- A diák nézet nem mutat owner/admin mezőket.
- Az owner nézet mutatja a hozzárendeléseket és ingest/generation metaadatokat.
- A generált kérdéssorok és összefoglalók olvashatók a detail oldalon.

## Lepesnaplo

- [x] A Jira issue-t `In Progress`-ra állítottam.
- [x] A release manifestben rögzítettem a `0.1.52` célt.

## Eredmény

A route és a részletes tananyagnézet a worktree-ben implementálás alatt áll.
