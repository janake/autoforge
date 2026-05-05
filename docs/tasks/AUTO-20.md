# AUTO-20 Keycloak web client configuration

Feladat leírása
A Keycloak `autoforge` realmben be kell allitani az `autoforge-web` public OIDC clientet a frontendhez, majd a konkret konfiguralt parametereket dokumentalni kell.

Statusz
- in progress

Verzió
- `0.1.1`

Branch
- `feature/AUTO-20-keycloak-client-config`

PR
- PR #30

Acceptance criteria
- Letrejott vagy frissult az `autoforge-web` Keycloak client az `autoforge` realmben.
- A client public OIDC + PKCE `S256` modban mukodik.
- A redirect URI-k, logout redirect URI-k es web originok tartalmazzak a production es local frontend originokat.
- A konfiguralt parameterek szerepelnek a dokumentacioban.
- Nem kerul token, jelszo vagy admin credential gitbe.

Dokumentumok és fájlok
- `docs/authentication.md`
- `docs/releases.md`
- `docs/tasks/AUTO-20.md`

Konfigurált paraméterek
- Realm: `autoforge`
- Client ID: `autoforge-web`
- Client type: `OpenID Connect`
- Client authentication: `Off`
- Authorization: `Off`
- Standard flow: `On`
- Direct access grants: `Off`
- Implicit flow: `Off`
- Service accounts roles: `Off`
- OAuth 2.0 Device Authorization Grant: `Off`
- OIDC CIBA Grant: `Off`
- Root URL: `https://oci.prodet.org`
- Home URL: `/`
- Valid redirect URIs: `https://oci.prodet.org/*`, `http://localhost:5173/*`, `http://localhost:4173/*`
- Valid post logout redirect URIs: `https://oci.prodet.org/*`, `http://localhost:5173/*`, `http://localhost:4173/*`
- Web origins: `https://oci.prodet.org`, `http://localhost:5173`, `http://localhost:4173`
- Proof Key for Code Exchange Code Challenge Method: `S256`

Lepesnaplo
1. Letrehoztam az `AUTO-20` branchet `origin/main` alaprol.
2. Playwright/CDP hasznalattal megnyitottam a Keycloak admin konzolt a user altal biztositott bejelentkezett sessionben.
3. Ellenoriztem, hogy az `autoforge` realmben nem volt `autoforge-web` client.
4. Letrehoztam az `autoforge-web` OpenID Connect clientet.
5. Beallitottam a public client flow-kat es frontend URL-eket.
6. Beallitottam a PKCE code challenge methodot `S256`-ra.
7. Dokumentaltam a konfiguralt parametereket.
8. Megnyitottam a kapcsolodo PR-t.

Eredmény
- Az `autoforge-web` Keycloak client beallitva es dokumentalva lett.
