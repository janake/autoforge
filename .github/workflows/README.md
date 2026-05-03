# workflows

Ide kerulnek a GitHub Actions workflow-k:

- frontend build
- backend build
- container image build + push GHCR-be
- manual deploy public gepre
- manual deploy private gepre jump hoston at

Szukseges secret-ek:

- `OCI_SSH_PRIVATE_KEY`
- `OCI_PUBLIC_HOST`
- `OCI_PUBLIC_USER`
- `OCI_PRIVATE_HOST`
- `OCI_PRIVATE_USER`
- `OCI_GATEWAY_DOMAIN`
- `OCI_BACKEND_UPSTREAM`
- `GHCR_DEPLOY_USERNAME`
- `GHCR_DEPLOY_TOKEN`
