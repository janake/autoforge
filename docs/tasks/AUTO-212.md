# [AUTO-212] Patch alkalmazása és commit létrehozása

## Feladat leírása

A Git broker alkalmazza a kapott unified diffet a klónozott repositoryra, majd commitot készít a megadott commit message-dzsel.

## Statusz

review

## Verzió

0.1.36

## Branch

feature/auto-212-patch-application

## PR

- pending

## Cél

A repo előkészítés után a patch ténylegesen érvényesüljenek, és visszakereshető commit SHA jöjjön létre.

## Scope

- patch fájl létrehozása temp workspace-ben
- `git apply` futtatása
- módosítás ellenőrzése commit előtt
- `git add` és `git commit`
- commit SHA visszaadása

## Elfogadási kritériumok

- A broker git apply segítségével alkalmazza a patch-et.
- Érvénytelen patch esetén strukturált hiba keletkezik.
- Ha nincs változás, a broker értelmes hibát ad.
- Siker esetén commit jön létre a megadott commit message-dzsel.
- A commit SHA visszakereshető.

## Lepesnaplo

- [x] Áttekintettem az AUTO-212 backlog scope-ját.
- [x] Létrehoztam a patch application és commit service-t.
- [x] Hozzáadtam a backend unit teszteket.

## Eredmény

A Git broker a patch-ből commitot tud készíteni, és a commit SHA visszakereshető.
