# BUG-15 Keycloak callback requires explicit login-required init

Feladat leírása
A login callback felismerése már visszakerült, de a frontend továbbra sem mutat bejelentkezett állapotot. A callback URL feldolgozásánál a Keycloak klienst explicit `login-required` módban kell inicializálni, hogy a visszaérkező auth response biztosan sessionné alakuljon át és a kliens ne essen vissza public állapotba.

Statusz
- in progress

Verzió
- `0.1.10`

Branch
- `bug/BUG-15-keycloak-login-required-callback`

PR
- PR #44

Acceptance criteria
- Login redirect után a frontend nem marad public állapotban.
- A Keycloak callback URL feldolgozása `login-required` initet használ.
- Sikeres login után megjelenik a private workspace.

Dokumentumok és fájlok
- `apps/web/src/App.tsx`
- `docs/releases.md`
- `docs/tasks/BUG-15.md`
- `package.json`
- `package-lock.json`

Lepesnaplo
1. Ellenőriztem, hogy a live gateway/backend lánc már működik.
2. Szűkítettem a hibát a frontend callback init viselkedésére.
3. A callback URL-es ágon `login-required` initre váltottam.
4. Emeltem a projektverziót `0.1.10`-re.

Eredmény
- A callback utáni auth init explicit login-required módban fut.
