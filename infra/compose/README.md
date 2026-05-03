# compose

Itt vannak a ket OCI gephez tartozo Compose stackek.

- `docker-compose.public.yml`: frontend + gateway a publikus gepre
- `docker-compose.private.yml`: backend a privat gepre
- `Caddyfile`: publikus gateway szabalyok
- `.env.public.example`: publikus stack valtozoi
- `.env.private.example`: privat stack valtozoi

A workflow-k ezeket a fajlokat masoljak ki a szerverekre:

- publikus host: `/opt/autoforge/public`
- privat host: `/opt/autoforge/private`
