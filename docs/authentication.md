# Authentication

## Cél

Az Autoforge webes felülete külső OIDC szolgáltatóval hitelesít, a backend pedig JWT issuer-validációval fogadja az API-hívásokat.

## Jelenlegi modell

- a homepage publikus, nem kényszerít belépésre
- a sign-in gomb indítja a Keycloak login flow-t
- a frontend public clientként működik
- a login PKCE `S256` flow-val történik
- a backend resource serverként ellenőrzi az issuer URI-t
- külön client secret jelenleg nem szükséges
- ha később confidential client kell, annak secretje OCI Vaultba kerüljön

## Verziózás

- Minden auth flow, Keycloak, token vagy jogosultsag valtozasnak legyen celverzioja.
- A valtozast a task fajl `Verzió` mezojeben es a `docs/releases.md` manifestben is rogziteni kell.
- Breaking auth valtozasnal a `docs/versioning.md` szerint legalabb `MINOR`, inkompatibilis API/deploy hatasnal `MAJOR` verzio szukseges.

## Konfigurációs változók

- `KEYCLOAK_URL`: az OIDC szolgáltató base URL-je
- `KEYCLOAK_REALM`: deploy-time helykitöltő a realmhez
- `KEYCLOAK_CLIENT_ID`: a webes public client azonosítója
- `KEYCLOAK_ISSUER_URI`: a backend issuer URI-ja

## Beállítási elv

### Web client

- Standard flow legyen bekapcsolva
- PKCE legyen engedélyezve
- public client legyen használva
- client authentication legyen kikapcsolva
- implicit flow legyen kikapcsolva
- direct access grants legyen kikapcsolva a web clienten
- valid redirect URI-k:
  - `https://oci.prodet.org/*`
  - `http://localhost:5173/*`
  - `http://localhost:4173/*`
- web originök:
  - `https://oci.prodet.org`
  - `http://localhost:5173`
  - `http://localhost:4173`

### Konfigurált Autoforge web client

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
- Valid redirect URIs:
  - `https://oci.prodet.org/*`
  - `http://localhost:5173/*`
  - `http://localhost:4173/*`
- Valid post logout redirect URIs:
  - `https://oci.prodet.org/*`
  - `http://localhost:5173/*`
  - `http://localhost:4173/*`
- Web origins:
  - `https://oci.prodet.org`
  - `http://localhost:5173`
  - `http://localhost:4173`
- Proof Key for Code Exchange Code Challenge Method: `S256`

### Backend

- issuer URI-t környezeti változóból kap
- a JWT role claim-ekből realm és resource role-ok is olvasódnak
- a `/api/v1/me` végpont a tokenből visszaadja a felhasználói adatokat, hogy a frontend személyre szabható legyen

## Secret policy

- most nincs szükség Keycloak client secretre
- ha később mégis lesz auth secret, azt OCI Vaultban kell tárolni
- gitbe semmilyen token, jelszó, private key vagy személyes azonosító nem kerülhet
