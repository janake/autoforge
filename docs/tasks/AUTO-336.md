# [AUTO-336] Hibrid RAG adatmodell OCI Always Free célra

## Feladat leírása

A Learning modul relációs adatmodelljét úgy kell rögzíteni, hogy az OCI Always Free környezetben is működjön, és külön boundary maradjon a chunk, embedding és user-szintű tanulási állapot számára.

## Statusz

in progress

## Verzió

0.1.50

## Branch

feature/AUTO-336-hybrid-rag-model

## PR

- TBD

## Cél

A Learning adattárolási réteg legyen jól elkülönítve: relációs metadata, chunk offsetek, adapter-független embedding payload és user isolation mezők.

## Scope

- learning material, chunk, embedding és ingestion job boundary ellenőrzése
- owner subject alapú izoláció a releváns entitásokon
- hybrid vector storage fallback dokumentálása
- Always Free méret- és retention limitek rögzítése

## Elfogadási kritériumok

- A relációs boundary dokumentált és a kódban is követhető.
- A user isolation mezők minden releváns Learning entitáson jelen vannak.
- A vector payload fallback nem követel natív ADB vector supportot.
- A méret- és retention szabályok dokumentálva vannak.

## Lepesnaplo

- [x] A Jira issue-t `In Progress`-ra állítottam.
- [x] A release manifestben rögzítettem a `0.1.50` célt.

## Eredmény

A feladat worktree-ben elő van készítve; a további verifikáció és esetleges finomítás a branchen történik.
