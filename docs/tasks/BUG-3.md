# BUG-3: Caddy GATEWAY_DOMAIN hiányzik a docker-compose configban

Feladat leírása
A Caddy nem kap érvényes domain nevet a GATEWAY_DOMAIN environment variable-ból, emiatt nem tud SSL tanúsítványt szerezni a Let's Encrypt-től. A containerben `GATEWAY_DOMAIN=-` volt beállítva, ami érvénytelen domain.

Statusz
- in progress

Branch
- `bug/BUG-3`

PR
- https://github.com/janake/autoforge/pull/22

Acceptance criteria
- A docker-compose.public.yml tartalmazza a GATEWAY_DOMAIN értéket.
- A Caddy sikeresen szerzi meg az SSL tanúsítványt.
- A frontend és API elérhető HTTPS-en.

Dokumentumok és fájlok
- `infra/compose/docker-compose.public.yml`
- `docs/deployment.md`

Biztonsági megfontolások
- A domain név nem érzékeny adat, nyilvánosan is ismert.

Lepesnaplo
1. Ellenőriztem a futó container-eket SSH-n.
2. Megtaláltam a hibás GATEWAY_DOMAIN=- értéket a Caddy container env-jében.
3. Javítsam a docker-compose.public.yml-t a domain beépítésével.