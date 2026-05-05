# Versioning

## Cel

Minden story, bugfix es dokumentacios feladat legyen hozzarendelve egy konkret projektverziohoz. Igy kesobb egyertelmuen latszik, hogy melyik valtozas melyik kiadashoz tartozik.

## Verzios formatum

Az Autoforge szemantikus verziozast hasznal:

```text
MAJOR.MINOR.PATCH
```

- `MAJOR`: inkompatibilis architektura-, API- vagy deploy-valtozas.
- `MINOR`: uj funkcio, nagyobb bovites, uj szolgaltatas vagy jelentosebb workflow kepesseg.
- `PATCH`: hibajavitas, kisebb UI cleanup, dokumentacio, workflow finomitas vagy nem breaking infra javitas.

## Kovetendo forras

- Az aktualis projektverzio a root `package.json` `version` mezoje.
- A story/bug -> verzio osszerendeles kotelezo forrasa: `docs/releases.md`.
- Minden task fajlban kotelezo a `Verzió` mezo.

## Uj feladat szabaly

Minden uj `AUTO-*` vagy `BUG-*` feladatnal meg kell adni a celverziot mar a task dokumentumban.

Pelda:

```text
Verzió
- `0.1.1`
```

Ezutan ugyanazt a feladatot fel kell venni a `docs/releases.md` megfelelo verzioja ala.

## Verzios besorolas

- Feature, scaffold, uj flow: kovetkezo `MINOR` vagy a mar nyitott kovetkezo release.
- Bugfix: kovetkezo `PATCH` vagy a mar nyitott patch release.
- Dokumentacio/processz valtozas: kovetkezo `PATCH`.
- Breaking valtozas: kovetkezo `MAJOR`, es az architektura/deploy doksit is frissiteni kell.

## Release manifest szabaly

A `docs/releases.md` minden verzional tartalmazza:

- verzioszam
- statusz (`planned`, `in progress`, `released`)
- task lista ID-val es rovid cimmel
- release megjegyzes

## PR kovetelmeny

Minden PR body tartalmazza a celverziot.

Pelda:

```markdown
## Version
- `0.1.1`
```

## Visszamenoleges kezeles

A korabbi feladatok eseten, ahol nincs task fajlban `Verzió` mezo, a `docs/releases.md` a forras. Uj vagy modositott task fajlbol mar nem hianyozhat a `Verzió` mezo.
