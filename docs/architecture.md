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
- OCI storage services:
  - OCI Object Storage private bucket for full AI prompt JSON files
  - Oracle Autonomous Database Job Store for job metadata, prompt hash, summary, and object URI
- External identity provider:
  - Keycloak OIDC login with PKCE
  - public client for the web app

## Request flow

- Browser -> `https://oci.prodet.org`
- Caddy routes `/api/**` to the Spring Cloud API gateway container and all other paths to the React frontend.
- The API gateway forwards requests to the private backend host on port `8080`.
- The frontend authenticates against Keycloak with Authorization Code + PKCE.
- Authenticated API calls carry a bearer token to `/api/v1/me` and other protected backend endpoints.
- The backend validates JWT signatures with the bundled Keycloak public signing key.

## Delivery model

- Every component runs in Docker.
- Build and deployment flow is driven by GitHub Actions.
- The public host serves as the external entry point via Caddy, which routes `/api` to the API gateway and everything else to the web frontend.
- The web frontend delegates authentication to an external Keycloak OIDC provider and uses bearer tokens for API calls.
- The private host serves internal application workloads.
- The private host has a dedicated 100 GB OCI Block Volume mounted as an ext4 workspace at `/mnt/autoforge-workspace`.
- The AI Proxy stores full generated prompts in an OCI Object Storage private bucket as native JSON objects.
- The Oracle Autonomous Database Job Store keeps `job_id`, `jira_id`, `status`, `prompt_hash`, secret-filtered `summary`, and `object_store_uri` for audit and debug workflows.
- Object Storage lifecycle management deletes full prompt JSON objects after 72 hours; the Job Store retains hash and summary for long-term audit history.

## Current status

- The public host runs versioned `web` and `api-gateway` containers behind Caddy.
- The private host runs versioned `backend` and optional `opencode` containers via Docker Compose.
- Container images are built in GitHub Actions and tagged with the root project version from `package.json`.
- `main` merges automatically trigger relevant public/private deploy workflows based on changed paths.
- The frontend uses Keycloak PKCE login and the backend serves `/api/v1/me` for authenticated profile bootstrapping.
- The backend validates JWTs with a bundled Keycloak public key to avoid production-only remote JWKS/issuer fetch failures.
- OCI Vault still stores OpenCode runtime secrets, which the private host reads at deploy time via instance principal.
- The 100 GB OCI Block Volume remains mounted at `/mnt/autoforge-workspace`.
- Prompt audit storage uses the hybrid OCI Object Storage plus Oracle Autonomous Database Job Store model decided in `AUTO-114`.
- The worker runtime is still not selected and is not shown as an active service in the current deploy chain.

## Diagram Rule

- If the architecture changes, update this diagram in the same change set.

## Versioning Rule

- Every architecture change must be assigned to a target version in the task file.
- Update `docs/releases.md` in the same change set so the architecture change is traceable to a release.
- Follow `docs/versioning.md` when deciding whether the change is `MAJOR`, `MINOR`, or `PATCH`.
