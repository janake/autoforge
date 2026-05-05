# AUTO-13: Keycloak OIDC integráció

Feladat leírása
A projektet be kell kötni a meglévő Keycloak szolgáltatásba úgy, hogy a frontend loginja OIDC/PKCE alapú legyen, a backend JWT resource serverként validáljon, és a személyre szabáshoz legyen egy `/api/v1/me` jellegű endpoint.

Statusz
- review

Branch
- `AUTO-13-keycloak-oidc`

PR
- pending

Acceptance criteria
- A frontend publikus homepage-et ad és külön sign-in felületet biztosít.
- A frontend Keycloak loginra vált a protected workspace alatt.
- A backend issuer alapján validálja a JWT-t.
- Van user-profile endpoint a frontend személyre szabásához.
- A runtime konfiguráció Dockerből is felülírható.
- A dokumentáció nem tartalmazza a realm nevét.
- Az esetleges jövőbeli secret-ekhez a Vault használati irányelve rögzítve van.

Dokumentumok és fájlok
- `docs/authentication.md`
- `docs/deployment.md`
- `docs/architecture.md`
- `docs/assets/autoforge-oci-architecture.svg`
- `docs/tasks/AUTO-13.md`
- `README.md`
- `apps/web/*`
- `services/backend/*`

Biztonsági megfontolások
- Gitbe nem kerülhet token, jelszó, private key, OCID, lokális path vagy személyes azonosító.
- A realm neve nem jelenhet meg dokumentációban.
- Ha később secret kell, azt OCI Vaultban kell tárolni.

Lepesnaplo
1. Elolvastam a jelenlegi frontend, backend, deploy és architektúra állapotot.
2. Bekötöttem a frontend runtime Keycloak konfigurációját és a bearer tokenes API hívást.
3. Bekötöttem a Spring Boot backend JWT resource server ellenőrzését.
4. Elkészítettem az auth dokumentációt és frissítettem az architektúra leírást.

Eredmény
- A Keycloak integráció kódszinten elkészült és validált.
