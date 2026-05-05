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
- `OCI_PUBLIC_SSH_HOST`
- `OCI_PUBLIC_USER`
- `OCI_PRIVATE_HOST`
- `OCI_PRIVATE_SSH_HOST`
- `OCI_PRIVATE_USER`
- `OCI_BACKEND_UPSTREAM`

Megjegyzés: `OCI_GATEWAY_DOMAIN` nem szükséges - a domain be van égetve a docker-compose-ban.
- `GHCR_DEPLOY_USERNAME`
- `GHCR_DEPLOY_TOKEN`
- `CLOUDFLARE_API_TOKEN`
- `CLOUDFLARE_ZONE_ID`
- `CLOUDFLARE_SSL_MODE` is optional; if set, the deploy flow updates the zone SSL setting too.

Ha a Cloudflare secret-ek elérhetők, a public deploy flow a webes rekordokat is frissíti az origin IP-re.

## Verziózás

- Minden workflow vagy deploy szabaly valtozas celverziohoz kotott.
- A task fajlban kotelezo a `Verzió` mezo.
- A valtozast fel kell venni a `docs/releases.md` manifestbe.
- A `Container Images` workflow a sajat image-eket `main`, `<version>` es `sha-<commit>` tagekkel publikálja.
- A public/private deploy workflow-k a sajat image-ekhez a `<version>` taget hasznaljak.
