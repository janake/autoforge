# BUG-16 Keycloak callback detection is too narrow

Feladat leírása
A login után a frontend továbbra sem mutat bejelentkezett állapotot. A backend és gateway már működik, ezért a maradék hiba a frontend callback-felismerésben van. A jelenlegi logika csak `state` + `code/error` kombinációt tekint callbacknek, miközben a Keycloak redirect URL-ben ettől eltérő paraméterkészlet is megjelenhet. Emiatt a kliens callback helyett sima `check-sso` ágra esik vissza.

Statusz
- in progress

Verzió
- `0.1.11`

Branch
- `bug/BUG-16-keycloak-callback-detection`

PR
- PR #45

Acceptance criteria
- A frontend callbacknek tekinti a Keycloak redirect URL-eket akkor is, ha nincs együtt `state` és `code`.
- Login redirect után a callback ág fut le, nem a sima `check-sso` ág.
- Sikeres login után megjelenik a private workspace.

Dokumentumok és fájlok
- `apps/web/src/App.tsx`
- `docs/releases.md`
- `docs/tasks/BUG-16.md`
- `package.json`
- `package-lock.json`

Lepesnaplo
1. Ellenőriztem, hogy a live gateway/backend lánc már egészséges.
2. A frontend callback-felismerést túl szűknek találtam.
3. Kibővítettem a callback detektálást a gyakori Keycloak redirect paraméterekre.
4. Emeltem a projektverziót `0.1.11`-re.

Eredmény
- A Keycloak callback URL felismerése robusztusabb lett.
