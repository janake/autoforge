# AUTO-97 Command Allowlist

## Goal

Define the minimal command policy the sandbox may execute without asking for clarification.

## Allowed commands

- `git status`
- `git diff`
- `git log`
- `git branch`
- `git show`
- `git rev-parse`
- `npm run build:web`
- `npm run typecheck:web`
- `npm run test:backend`
- `npm run build:backend`
- `mvn -q -f services/backend/pom.xml test`
- `mvn -q -f services/backend/pom.xml package`
- `curl` for read-only health checks
- `ssh` for read-only OCI host checks when explicitly provided by the user

## Disallowed commands

- Any destructive git command such as `git reset --hard`, `git checkout --`, or force push.
- Any command that writes secrets, tokens, credentials, or private keys to disk.
- Any command that starts long-running services unless the user explicitly asks for it.
- Any command that escalates privileges or changes system-wide configuration without an explicit task.
- Any command that reads or prints secret values.

## Policy Rule

If a command is not in the allowed list, the sandbox must reject it by default and return a short reason.

## Weak AI Rule

The policy must be deterministic, explicit, and safe enough for a low-capability agent to follow without asking questions.
