# AUTO-7 Security audit findings

- Statusz: in progress
- Branch: `AUTO-7-security-audit`
- PR: (to be opened)

## Cel

Repo attekintese biztonsagi szempontbol, talalatok dokumentalasa es minimalis javitasok bekuldese PR-ben a projekt szabalyai szerint.

## Scope

- Secrets/keys leak keresese
- Dependency CVE scan (frontend + backend)
- Dockerfile / docker-compose ellenorzes
- Konfig fajlok (nginx, Caddy, application.yml) ellenorzese
- Kis javitasok: Vite frissites, backend kontener ne root-kent fusson, security headers nginx-ben

## Lepesnaplo

- 2026-05-03: Repo-scan vegrehajtva. Talalatok memo: Vite (apps/web) versioban sok CVE, javasolt upgrade to ^8.0.5. Backend Dockerfile runtime runs as root — javasolt non-root. Nginx config hiányoznak security header-ek. Nem talaltam hard-coded secret-et vagy private key-et a repo forrasban.

## Javitasok (commit szabaly)

Ezt a feladatot jelolesere a commit uzenetek kezdodjenek `AUTO-7`-tel.

Pelda:

```
[AUTO-7] Upgrade vite to 8.0.5 to mitigate multiple dev-server CVEs
[AUTO-7] Add non-root USER 1000 to backend Dockerfile
[AUTO-7] Add security headers to nginx config for web
```

## Validacio

- Frontend build: `npm run build:web` (ajánlott a frissítés után lokálisan tesztelni)
- Backend image: `docker build -f services/backend/Dockerfile .` és konténer indítása, ellenőrizve, hogy a process UID 1000 alatt fut

## Megjegyzes

- Ha tovabbra is talalunk kritikus, jol dokumentalt secreteket a repo-ban, azt azonnal ki kell forgatni/rotalni es a PR-ben nem hagyunk plaintext kulcsokat.

