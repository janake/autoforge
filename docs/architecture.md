# Architecture

## Deployment shape

- Public OCI instance:
  - React frontend
  - reverse proxy / API gateway
- Private OCI instance:
  - Spring Boot backend service
  - worker service

## Delivery model

- Every component runs in Docker.
- Build and deployment flow will be driven by GitHub Actions.
- The public host serves as the external entry point.
- The private host serves internal application workloads.

## Current status

- React chosen for frontend.
- Frontend scaffolded with Vite + TypeScript.
- Frontend build workflow prepared in GitHub Actions.
- Backend selected: Spring Boot + Maven.
- Worker runtime not selected yet.
- Active work branch names include the task ID prefix without any extra namespace prefix.
