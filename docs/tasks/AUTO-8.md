# AUTO-8: Spring Cloud API Gateway behind Caddy (make backend only reachable via gateway)
Feladat leírása
Hozzunk létre és deploy-oljunk egy Spring Cloud Gateway alapú `api` szolgáltatást, amely a publikus Caddy gateway mögött fut. A backend (Spring Boot) csak ezen az API gateway-en keresztül legyen elérhető — ne legyen közvetlen, publikus elérés a backend felé.
Acceptance criteria
- A `infra/compose/docker-compose.public.yml` tartalmaz `api` szolgáltatást és a `Caddyfile` a `/api` útvonalakat az `api` szolgáltatás felé irányítja.
- A `api` szolgáltatás továbbítja a kéréseket a privát backend `BACKEND_UPSTREAM` URL-re.
- A backend port nincs publikus hoston kitéve (host-port mapping NINCS), csak a belső hálózaton elérhető.
- Dokumentáció (`docs/` alatt) tartalmazza a telepítési lépéseket, a tesztelési lépéseket és a biztonsági megfontolásokat.
Munkalépések / tesztelés
1. A `Container Images` workflow buildelje és pusholja a gateway image-et (`ghcr.io/janake/autoforge/api-gateway`), vagy lokálisan buildeld az `infra/gateway` mappából.
2. Állítsd be a publikus host `.env`-jében a `BACKEND_UPSTREAM` változót a privát backend belső címére (pl. `10.42.0.91:8080`).
3. Futtasd a publikus stack-et (deploy script vagy `docker compose` a publikus hoston). Ellenőrizd, hogy a publikusan elérhető Caddy a `/api/v1/status` kérést továbbítja a backend-nek, és a backend válasza megjelenik.
4. Próbáld meg elérni a backend `8080` portját közvetlenül egy külső gépről: a közvetlen hozzáférésnek nem szabad működnie.
Dokumentumok és fájlok
- `infra/compose/docker-compose.public.yml` — publikus compose (Caddy + web + api)
- `infra/compose/Caddyfile` — Caddy konfiguráció, most a `/api` útvonalakat az `api` szolgáltatásra továbbítja
- `infra/gateway/` — Spring Cloud Gateway példa (`pom.xml`, `application.yml`, `Dockerfile`)
- `infra/compose/docker-compose.local.yml` — lokális teszt-kompozíció (api + backend + caddy), backend nem publikus
Biztonsági megfontolások
- Győződj meg róla, hogy a privát backend nem rendelkezik publikus port-nyitással a cloud/szerver konfigurációban.
- javasolt: mTLS vagy egyéb biztonságos csatorna a gateway és a backend között, illetve tűzfal szabály, amely csak a gateway-től enged be forgalmat a backend felé.
