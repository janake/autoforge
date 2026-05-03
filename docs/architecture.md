# Architecture

## Deployment shape

- Public OCI instance:
  - React frontend
  - reverse proxy / API gateway
- Private OCI instance:
  - backend service
  - worker service

## Delivery model

- Every component runs in Docker.
- Build and deployment flow will be driven by GitHub Actions.
- The public host serves as the external entry point.
- The private host serves internal application workloads.

## Current status

- React chosen for frontend.
- Frontend scaffolded with Vite + TypeScript.
- Backend runtime not selected yet.
- Worker runtime not selected yet.
