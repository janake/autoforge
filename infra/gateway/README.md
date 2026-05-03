Spring Cloud API Gateway (example)
---------------------------------

This directory contains example configuration and a Dockerfile template for running a Spring Cloud Gateway container as the API gateway behind Caddy.

Design notes:
- Caddy (public) -> Spring Cloud Gateway (`api` service) -> backend (private host)
- The gateway is expected to accept requests under a base context (e.g. `/api`) and forward them to the backend upstream configured via `BACKEND_UPSTREAM`.

Files:
- `application.yml` — example Spring Cloud Gateway configuration showing a route that forwards `/api/**` to the backend upstream and strips the prefix.
- `Dockerfile` — template to build the gateway image from a Spring Boot fat jar.

Usage:
1. Build your Spring Cloud Gateway app into a fat jar (e.g. `gateway.jar`).
2. Build the Docker image and push to your registry, or adjust `docker-compose.public.yml` to `build:` this directory.
3. Set `BACKEND_UPSTREAM` in the public host `.env` to the private backend address (e.g. `10.42.0.91:8080`).

