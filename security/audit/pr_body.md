Summary

This PR contains minimal security hardening based on a repository audit performed on 2026-05-03.

Changes:

- Upgrade `vite` in `apps/web/package.json` to `^8.0.5` to mitigate multiple dev-server CVEs.
- Set `USER 1000` in `services/backend/Dockerfile` to avoid running the runtime as root.
- Add several security response headers to `apps/web/nginx.conf` (HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy, X-XSS-Protection).

Notes

- No plaintext secrets were found in the repository files scanned.
- The Vite upgrade may require additional local testing; please run `npm ci` and `npm run build:web` locally or in CI.
- For production, ensure the Vite dev server is not exposed to the network. Consider adding automated dependency and secret scanning in CI.

See `security/audit/security-audit-report.md` for full findings and recommendations.

Testing performed

- Static scans and quick code inspection; no full CI runs were executed in this branch.

Suggested reviewers: @janake

