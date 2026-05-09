# AUTO-95 Workspace Denylist

## Goal

Define the file paths and directories the AI runtime must never read or write inside the workspace.

## Denied paths

- `.env`
- `.env.*`
- `**/*.pem`
- `**/*.key`
- `**/*secret*`
- `**/*token*`
- `**/*vault*`
- `**/*password*`
- `ops/mcp/**`
- `ops/ai/**`
- `docs/security/**`
- `deploy/**`
- `scripts/deploy/**`
- `**/id_rsa*`
- `**/known_hosts`
- any path containing `ocid`, `secret`, `token`, or `password`

## Rule

If a path matches the denylist, the runtime must refuse access by default and explain that the path is outside the allowed workspace surface.

## Allowed exceptions

- Repository source code required for the current task.
- Generated build artifacts in standard output directories.
- Read-only documentation files that do not contain secrets.

## Weak AI rule

The denylist must be simple enough for a weak AI to apply without asking follow-up questions.
