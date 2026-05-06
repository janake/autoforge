# BUG-14 Keycloak callback init falls back to public state

Feladat leírása
A live rendszerben a gateway és backend már működik, token nélküli `/api/v1/me` hívás 401-et ad, de login után a frontend mégsem mutat bejelentkezett állapotot. A bootstrap logika minden esetben `check-sso`-val inicializálja a Keycloak klienst, így a login callback URL-en sem fut explicit callback-feldolgozás.

Statusz
- in progress

Verzió
- `0.1.9`

Branch
- `bug/BUG-14-keycloak-callback-init`

PR
- PR #43

Acceptance criteria
- Login callback URL esetén a Keycloak init nem `check-sso`-val fut.
- Sikeres login után a frontend `ready` állapotba kerül és megjelenik a private workspace.
- Nem callback URL-en marad a `check-sso` viselkedés a meglévő session ellenőrzésére.

Dokumentumok és fájlok
- `apps/web/src/App.tsx`
- `docs/releases.md`
- `docs/tasks/BUG-14.md`
- `package.json`
- `package-lock.json`

Lepesnaplo
1. Ellenőriztem, hogy a live gateway/backend már működik, token nélkül `/api/v1/me` 401-et ad.
2. Megállapítottam, hogy a frontend bootstrap mindig `check-sso`-t használ.
3. Visszahoztam a Keycloak callback felismerést, és callback esetén normál initet használok.
4. Emeltem a projektverziót `0.1.9`-re.

Eredmény
- A login callback útvonal explicit Keycloak callback-feldolgozással inicializálódik.
