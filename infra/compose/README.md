# compose

Itt vannak a ket OCI gephez tartozo Compose stackek.

- `docker-compose.public.yml`: frontend + API gateway + Caddy a publikus gepre
- `docker-compose.private.yml`: backend + OpenRouter provider proxy a privat gepre
- `Caddyfile`: publikus gateway szabalyok
- `openrouter-proxy`: a private compose stackben futo provider proxy, amely az OpenRouter API kulcsot izolalja a backendtol
- `.env.public.example`: publikus stack valtozoi
- `.env.private.example`: privat stack valtozoi es OCI Vault secret OCID placeholder-ek

A workflow-k ezeket a fajlokat masoljak ki a szerverekre:

- publikus host: `/opt/autoforge/public`
- privat host: `/opt/autoforge/private`
