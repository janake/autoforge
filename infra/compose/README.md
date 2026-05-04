# compose

Itt vannak a ket OCI gephez tartozo Compose stackek.

- `docker-compose.public.yml`: frontend + API gateway + Caddy a publikus gepre
- `docker-compose.private.yml`: backend + opencode REST AI a privat gepre
- `Caddyfile`: publikus gateway szabalyok
- `opencode.json`: az opencode server alap konfiguracioja
- `.env.public.example`: publikus stack valtozoi
- `.env.private.example`: privat stack valtozoi

A workflow-k ezeket a fajlokat masoljak ki a szerverekre:

- publikus host: `/opt/autoforge/public`
- privat host: `/opt/autoforge/private`
