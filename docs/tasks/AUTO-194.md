# [AUTO-194] Mock AI patch generator

## Feladat leírása

Deterministic mock patch generator kell az MVP flow-hoz, hogy a backend AI provider nélkül is tudjon valid unified git diffet előállítani.

## Statusz

completed

## Verzió

0.1.32

## Branch

feature/auto-194-mock-ai-patch-generator

## PR

- pending

## Cél

A job processor és a későbbi Git broker integráció még valódi AI nélkül is tesztelhető legyen.

## Scope

- `AIPatchGenerator` interfész
- `GeneratedPatchResponse` DTO
- `MockAIPatchGenerator` implementáció
- unit teszt a determinisztikus patch-re

## Elfogadási kritériumok

- Van `AIPatchGenerator` interface `generatePatch(Job job)` metódussal.
- Van `GeneratedPatchResponse` DTO `patch`, `summary`, `changedFiles` mezőkkel.
- Van `MockAIPatchGenerator` implementáció.
- A mock implementáció valid unified diffet ad vissza.
- Unit test ellenőrzi, hogy nem üres patch keletkezik.

## Lepesnaplo

- [x] Felmértem, hogy a repo-ban még nincs AI patch generator contract.
- [x] Létrehoztam az interfészt, DTO-t és a determinisztikus mock implementációt.
- [x] Hozzáadtam a mock generator unit tesztjét.
- [x] Dokumentáltam a sample repository targetet.
- [x] `mvn test` sikeresen lefutott a backend modulban.

## Eredmény

A backend már tud determinisztikus patch-et előállítani az MVP flow következő lépéseihez.
