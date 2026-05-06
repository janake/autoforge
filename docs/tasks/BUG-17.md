# BUG-17 Backend JWT validation depends on remote JWKS availability

Feladat leírása
A live rendszerben a frontend Bearer tokent küld a `/api/v1/me` kérésre, a token a Keycloak `userinfo` szerint érvényes, a lokális backend is elfogadja, a production private backend mégis 401-et ad. Ez production-only JWT validation különbségre utal. A legbiztosabb workaround, hogy a backend ne runtime issuer/JWKS fetch-re támaszkodjon, hanem a Keycloak aktuális publikus aláíró kulcsát használja helyi erőforrásként.

Statusz
- in progress

Verzió
- `0.1.12`

Branch
- `bug/BUG-17-static-jwt-public-key`

PR
-

Acceptance criteria
- A backend a Keycloak access tokeneket statikus publikus kulccsal validálja.
- A lokális backend ugyanazzal a valódi tokennel 200-at ad `/api/v1/me`-re.
- A production backend nem függ remote JWKS/issuer elérhetőségtől a token aláírás ellenőrzéséhez.

Dokumentumok és fájlok
- `services/backend/src/main/resources/application.yml`
- `services/backend/src/main/resources/keycloak-public.pem`
- `docs/releases.md`
- `docs/tasks/BUG-17.md`
- `package.json`
- `package-lock.json`

Lepesnaplo
1. Playwrighttal reprodukáltam a login flow-t valós userrel.
2. Igazoltam, hogy a frontend Bearer tokent küld a `/api/v1/me` kérésen.
3. Igazoltam, hogy a token a Keycloak `userinfo` szerint érvényes.
4. Igazoltam, hogy a lokális backend ugyanezt a tokent elfogadja, production viszont 401-et ad.
5. Átállítottam a backendet statikus publikus kulcs alapú JWT validálásra.
6. Emeltem a projektverziót `0.1.12`-re.

Eredmény
- A backend tokenvalidációja nem függ a production környezet runtime issuer/JWKS elérésétől.
