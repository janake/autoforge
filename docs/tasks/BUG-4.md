# BUG-4: Frontend azonnal redirectel Keycloak-ra

Feladat leírása
A frontend betöltésekor azonnal Keycloak login-ra redirectel, ahelyett hogy a publikus landing page-t mutatná. A várt viselkedés: publikus oldal betöltése, és csak a "Sign in" gombra kattintás után Keycloak redirect.

Statusz
- in progress

Branch
- `bug/BUG-4`

PR
- pending

Acceptance criteria
- A publikus landing page betöltődik auth nélkül.
- A "Sign in" gomb megnyomására történik Keycloak redirect.
- Inkognitoban is helyesen működik.

Dokumentumok és fájlok
- `apps/web/src/auth/keycloak.ts`
- `apps/web/src/App.tsx`

Lepesnaplo
1. Ellenőriztem a Keycloak init kódot - `onLoad: check-sso` van használva.
2. Hozzáadtam debug console log-okat.
3. PR létrehozva BUG-3 alatt - de ez BUG-4, új branch és PR kell.