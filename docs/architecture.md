# Architecture

![Autoforge OCI architecture](assets/autoforge-oci-architecture.svg)

## Deployment shape

- Public OCI instance:
  - React frontend
  - Spring Cloud API gateway
  - reverse proxy / Caddy
- Private OCI instance:
  - Spring Boot backend service
  - OpenCode REST AI service
  - worker service
  - attached OCI Block Volume for the shared Autoforge workspace
- External identity provider:
  - Keycloak OIDC login with PKCE
  - public client for the web app

## Delivery model

- Every component runs in Docker.
- Build and deployment flow will be driven by GitHub Actions.
- The public host serves as the external entry point via Caddy, which routes `/api` to the API gateway and everything else to the web frontend.
- The web frontend delegates authentication to an external Keycloak OIDC provider and uses bearer tokens for API calls.
- The private host serves internal application workloads.
- The private host has a dedicated 100 GB OCI Block Volume mounted as an ext4 workspace at `/mnt/autoforge-workspace`.

## Current status

- React chosen for frontend.
- Frontend scaffolded with Vite + TypeScript.
- Frontend now has a deployable Docker image definition.
- Frontend build workflow prepared in GitHub Actions.
- Frontend authentication now uses an external Keycloak OIDC provider with PKCE.
- Backend scaffolded with Spring Boot + Maven.
- Backend deploys from a registry image in the private stack.
- Backend build workflow prepared in GitHub Actions.
- Docker Compose stacks are defined for the public and private OCI hosts.
- Manual GitHub Actions deploy workflows are prepared for both hosts.
- The private host now also runs an OpenCode REST AI service for backend-driven AI tasks.
- OpenCode runtime secrets are stored in OCI Vault and read by the private compute instance with instance principal authorization.
- A 100 GB OCI Block Volume is attached to the private host and mounted at `/mnt/autoforge-workspace`.
- Worker runtime not selected yet.
- Active work branch names include the task ID prefix without any extra namespace prefix.

## Diagram Rule

- If the architecture changes, update this diagram in the same change set.
