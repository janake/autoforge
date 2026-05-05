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

Nem-MAJOR tasknal a celverzio automatikusan a kovetkezo megfelelo `PATCH` vagy `MINOR` verzio.
MAJOR emelesnel a user jovahagyasa kotelezo.

Pelda:

```text
Verzió
- `0.1.2`
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

## Docker image verziozas

A sajat Docker image-ek kotelezoen kapjanak verzios taget az aktualis root `package.json` `version` mezoje alapjan.

A root verzio is automatikusan lep a kovetkezo megfelelo verziora nem-MAJOR tasknal; ez lesz az image tag alapja is.

A `Container Images` workflow minden sajat image-re legalabb ezeket a tageket kesziti:

- `main`, csak a default branch buildnel
- `<version>`, peldaul `0.1.1`
- `sha-<commit>`

Erintett image-ek:

- `ghcr.io/<registry-owner>/autoforge/web:<version>`
- `ghcr.io/<registry-owner>/autoforge/backend:<version>`
- `ghcr.io/<registry-owner>/autoforge/api-gateway:<version>`

Kulsos upstream image-eket, peldaul az OpenCode image-et, nem tagelunk at sajat projektverziora.

## Deploy verziozas

A deploy workflow-k a sajat image-ekbol a `<version>` taget hasznaljak, nem a mozgó `main` taget.

- Public deploy: `WEB_IMAGE_TAG=<version>` es `API_IMAGE_TAG=<version>`.
- Private deploy: `BACKEND_IMAGE_TAG=<version>`.

Ez biztosítja, hogy a futó Docker containerbol is latszik, melyik projektverzio van kint.

Deploy elott a workflow-knak ellenorizniuk kell, hogy a szukseges verzios image tag mar elerheto GHCR-ben. Ez megelozi, hogy a deploy a container builddel parhuzamosan, meg nem letezo vagy stale image-re fusson ra.

## PR kovetelmeny

Minden PR body tartalmazza a celverziot.

Pelda:

```markdown
## Version
- `0.1.2`
```

## Visszamenoleges kezeles

A korabbi feladatok eseten, ahol nincs task fajlban `Verzió` mezo, a `docs/releases.md` a forras. Uj vagy modositott task fajlbol mar nem hianyozhat a `Verzió` mezo.
