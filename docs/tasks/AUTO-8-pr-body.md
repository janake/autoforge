Feature: Add Spring Cloud API Gateway behind Caddy
This PR contains the infra changes to introduce a Spring Cloud API Gateway (`api` service) behind the public Caddy gateway. It includes:
- `infra/compose/docker-compose.public.yml`: added `api` service (Spring Cloud Gateway) and Caddy now proxies `/api` to it.
- `infra/gateway/`: example gateway Maven project, `application.yml`, and `Dockerfile` template.
- `infra/compose/docker-compose.local.yml`: local test compose (api + backend + caddy) ensuring backend is not published to host.
- `infra/compose/Caddyfile`: updated to route `/api` to `api` service.
- Docs: `docs/tasks/AUTO-8.md` describing the story, acceptance criteria and test steps.
Goal: ensure backend is reachable ONLY via the API gateway; backend port is not exposed to public host.
Testing: see `docs/tasks/AUTO-8.md` for build/run/test steps. In this branch I also built local images (`autoforge-api-gateway:local`, `autoforge-backend:local`) for manual testing.
Security notes: verify cloud security rules block direct access to the backend host/port; consider mTLS between gateway and backend.
