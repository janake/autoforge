# Security audit report — Autoforge repository

Date: 2026-05-03

Summary:

- No plaintext secrets or private keys were found in the repository files scanned.
- The frontend (apps/web) depends on `vite@^5.4.10`, which has multiple CVEs affecting the development server (path traversal, fs.deny bypass, WebSocket origin validation). Upgrade to `vite@^8.0.5` is recommended.
- The backend Dockerfile runs the Java runtime as root by default. A minimal mitigation to reduce attack surface is to run the container as a non-root UID (e.g., USER 1000) or create a dedicated non-root user and set ownership appropriately.
- The static web server config (`apps/web/nginx.conf`) does not set common security headers (HSTS, X-Frame-Options, etc.). Added recommended headers to reduce client-side risks.
- Caddyfile reverse proxy and compose files expose typical environment-driven configuration; environment variables are used rather than hard-coding credentials which is good practice. Deployment docs reference SSH key paths and hosts — these are operational notes but contain host IPs and should be handled appropriately (do not commit private keys).

Findings and severity:

- HIGH / MEDIUM: `vite@5.4.10` — multiple vulnerabilities affecting dev server. Impact: local dev server can leak files or be abused if exposed. Remediation: upgrade to `vite@^8.0.5` and avoid exposing Vite dev server to the network; set `server.cors` to false or limited origins when running dev server in shared environments.

- MEDIUM: backend runtime image default root user — running as root increases risk if container is compromised. Remediation: run as non-root user in runtime image (added `USER 1000` as a minimal mitigation in this PR).

- LOW: missing security response headers in `apps/web/nginx.conf`. Remediation: add X-Content-Type-Options, X-Frame-Options, Referrer-Policy, HSTS and related headers. (Changes included in this PR.)

Recommended next steps:

1. Rotate any secrets that may have been exposed previously outside this repo scan (not detected here).
2. Run full dependency audits locally: `npm audit` in workspace and `mvn dependency:tree` + OWASP Dependency-Check for Java.
3. Run container image scanning (e.g., Trivy or Clair) on built images.
4. Consider adding automated scanning to CI: snyk/OWASP Dependency-Check, hadolint for Dockerfiles, semgrep for code patterns, and a secrets scanner (e.g., detect-secrets) as pre-commit hooks.

Artifacts produced with this PR:

- Updated `apps/web/package.json` bumping `vite` to `^8.0.5`.
- Updated `services/backend/Dockerfile` to set `USER 1000`.
- Updated `apps/web/nginx.conf` to add security response headers.
- This report and a short task file `docs/tasks/AUTO-7.md` describing the audit.

If you want, I can open a PR branch with these changes (branch name: `AUTO-7-security-audit`) and prepare the PR description following the task rules in `docs/tasks/README.md`.

