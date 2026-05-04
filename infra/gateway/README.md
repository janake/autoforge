Spring Cloud API Gateway (example)
---------------------------------

This directory contains example configuration and a Dockerfile template for running a Spring Cloud Gateway container as the API gateway behind Caddy.

Design notes:
- Caddy (public) -> Spring Cloud Gateway (`api` service) -> backend (private host)
- The gateway is expected to accept requests under a base context (e.g. `/api`) and forward them to the backend upstream configured via `BACKEND_UPSTREAM`.

Files:
- `application.yml` — Spring Cloud Gateway configuration forwarding `/api/**` and `/actuator/**` to the backend upstream.
- `Dockerfile` — multi-stage image build that compiles the gateway app and runs the resulting Spring Boot jar.

Usage:
1. Build and push the Docker image through the `Container Images` workflow, or build locally from this directory.
2. Set `API_IMAGE`/`API_IMAGE_TAG` if you want to deploy a non-default registry image.
3. Set `BACKEND_UPSTREAM` in the public host `.env` to the private backend address (e.g. `10.42.0.91:8080`).
