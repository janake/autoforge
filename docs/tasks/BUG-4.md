# BUG-4: Frontend azonnal redirectel Keycloak-ra

Feladat leírása
A frontend betöltésekor azonnal Keycloak login-ra redirectel, ahelyett hogy a publikus landing page-t mutatná. A várt viselkedés: publikus oldal betöltése, és csak a "Sign in" gombra kattintás után Keycloak redirect.

Statusz
- in progress

Branch
- `bug/BUG-4`

PR
- https://github.com/janake/autoforge/pull/24

Acceptance criteria
- A publikus landing page betöltődik auth nélkül.
- A "Sign in" gomb megnyomására történik Keycloak redirect.
- Inkognitoban is helyesen működik.

Dokumentumok és fájlok
- `apps/web/src/auth/keycloak.ts`
- `apps/web/src/App.tsx`

Lepesnaplo
1. Ellenőriztem a Keycloak init kódot - `onLoad: check-sso` van használva.
2. Megállapítottam, hogy a landing page bootstrapje is Keycloak `check-sso`-t futtatott.
3. A landing page most auth init nélkül public állapotba lép.
4. Keycloak init csak OAuth callback feldolgozásakor vagy explicit sign-in indításakor történik.

Eredmény
- A publikus landing nem kezdeményez Keycloak redirectet.
