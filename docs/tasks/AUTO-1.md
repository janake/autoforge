# AUTO-1 React frontend base

- Statusz: done
- Branch: `AUTO-1-react-base`
- PR: `PR #3` (merged)

## Cel

React frontend base letrehozasa az `apps/web` alatt ugy, hogy legyen egy buildelheto kezdo alkalmazas, de deployment meg ne tortenjen.

## Scope

- React + Vite + TypeScript frontend alap
- alap app szerkezet
- workspace illesztes a monorepoba
- frontend GitHub Actions build workflow
- kezdo dokumentacio

## Commit szabaly

Ettol a ponttol az ehhez a feladathoz tartozo uj commitok `AUTO-1` prefixet hasznalnak.

Pelda:

```text
[AUTO-1] Add task tracking for React base work
```

## Megjegyzes

A task tracking szabaly bevezetese elott ezen a branchen mar keszultek commitok. Ezeket most nem irjuk at, hogy a branch tortenete stabil maradjon.

## Validacio

- `npm install`
- `npm run typecheck:web`
- `npm run build:web`
- GitHub Actions `Frontend Build` workflow sikeresen lefutott a PR-on
