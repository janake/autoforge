# Agent Operating Rules

This file is mandatory for every AI agent working in this repository. Before starting any new task, read and follow these rules. If these rules conflict with a direct user instruction in the current conversation, ask for clarification before changing files.

## Core Principles

- Do not mix unrelated work in the same branch, commit, or PR.
- Do not start local servers, frontend dev servers, backend services, Docker stacks, or long-running local processes unless the user explicitly asks for it.
- Prefer small, correct changes over broad rewrites.
- Never revert, overwrite, or remove user changes unless the user explicitly asks for it.
- Do not commit secrets, private keys, tokens, local absolute paths, personal identifiers, cloud credentials, tenancy/user OCIDs, or machine-specific data.
- Use placeholders or configured secrets for sensitive values, for example `${AUTOFORGE_SSH_KEY}`, `<registry-owner>`, `<repo-url>`.

## Task Classification

Classify every new request before making changes.

- Use `AUTO-<number>` for features, enhancements, UI cleanup, documentation improvements, scaffolding, and planned work.
- Use `BUG-<number>` only for actual defects, regressions, failing workflows, broken deploys, or incorrect shipped behavior.
- A simple requested UI/content change is not a bug unless the user explicitly describes it as broken behavior.
- If the classification is unclear, ask one short clarification question before creating a branch or ticket.

## Required New Task Flow

For any non-trivial code, infra, workflow, or documentation task, do the following:

1. Pick the next unused task ID from `docs/tasks/`.
2. Create a task file under `docs/tasks/` named exactly `<ID>.md`.
3. Assign the task to a target version before editing code or docs.
4. Add the task to `docs/releases.md` under the target version.
5. Create a dedicated branch from the current `origin/main` unless the user explicitly asks to continue an existing branch.
6. Use branch names in this format:
   - `feature/AUTO-<number>-short-description`
   - `bug/BUG-<number>-short-description`
7. Make only changes that belong to that task.
8. Update the task file as work progresses.
9. Run the relevant verification commands.
10. Commit with a message that starts with the task ID.
11. Push the branch.
12. Open or update a PR with a title that starts with the task ID and body that includes the target version.

## Task File Minimum Content

Every task file must include:

- Title with ID and short name.
- `Feladat leírása`.
- `Statusz`.
- `Verzió`.
- `Branch`.
- `PR`.
- `Acceptance criteria`.
- `Dokumentumok és fájlok`.
- `Lepesnaplo`.
- `Eredmény` when the work is done.

Use internal references like `PR #26` in task files instead of full URLs when possible.

## Versioning

- Every `AUTO-*` and `BUG-*` task must have a target version.
- Automatic version bump policy:
  - For non-major tasks (AUTO-*, BUG-*), the version is automatically incremented according to the protocol (using MAJOR.MINOR.PATCH format). Typically, a PATCH or MINOR bump occurs, with the specific bump determined by the task type (see docs/versioning.md). For MAJOR changes, user confirmation is required for the version increment, and this is recorded in the PR/Release manifest.
  - The root package.json version and container image tags are updated accordingly during CI/CD.
  - Version changes are reflected in all related documents: `docs/releases.md`, `docs/versioning.md`, and the `Version` field in task files.
  - The `Version` field in task files remains mandatory and must appear in the release manifest.
  - Always display the appropriate version in the AUTO-/BUG banner in documentation so it can be easily traced during audits.
- For non-major tasks, choose the next version automatically; only ask the user before a `MAJOR` bump.
- The canonical task-to-version mapping is `docs/releases.md`.
 - Versioning rules are documented in `docs/versioning.md`.
- Project-owned Docker images must be tagged with the root `package.json` version by the container image workflow.
- Do not open a PR if the task file has no `Verzió` section.
- Do not merge task documentation that disagrees with `docs/releases.md`.

## Commit And PR Naming

- Commit messages must start with the task ID, for example `[AUTO-15] Remove public entry panel`.
- PR titles must start with the task ID, for example `[AUTO-15] Landing page cleanup`.
- PR bodies must include a `Version` section with the target version.
- For non-major tasks, the target version should be the next automatic `PATCH` or `MINOR` version.
- Do not use generic-only titles like `fix:` or `docs:` without the task ID.
- Keep commits focused and do not include unrelated cleanup.

## Branch And PR Hygiene

- Do not create a `BUG-*` branch for a user-requested feature or UI cleanup.
- Do not reuse an unrelated branch just because it is currently checked out.
- If a mistaken branch or PR was created, close it and create the correct one instead of continuing the mistake.
- If the target code change is already on `main`, do not fabricate a duplicate code diff. Document the situation honestly in the task and PR.

## Verification

Run only relevant checks. Common commands in this repo:

- Frontend: `npm run build:web`.
- Frontend typecheck only: `npm run typecheck:web`.
- Backend tests: `npm run test:backend`.
- Backend build: `npm run build:backend`.

Record the commands and outcomes in the task file `Lepesnaplo` section. Do not run long-lived local services unless explicitly requested.

## Deployment And OCI

- Do not assume local Docker state represents OCI state.
- For OCI checks, use the SSH target the user provides, for example `ssh ubuntu@oci.prodet.org "..."`.
- Do not start or stop OCI services unless the user explicitly asks for that action.
- Main branch merges may trigger deploy workflows; verify workflow triggers before claiming deploy behavior.

## Documentation Rules

- If architecture, deploy behavior, workflow behavior, auth flow, or public UI behavior changes, update the relevant docs in the same task.
- Keep task docs consistent with the actual branch and PR.
- Do not leave stale references to wrong IDs, wrong PRs, or closed/superseded branches.

## Review Mindset

When asked for a review, prioritize findings first: bugs, regressions, risks, missing tests, and security concerns. If no findings exist, say that explicitly and mention residual risks.

## Pre-PR Mandatory Protocol

Before opening a PR, perform the following:

1. Identify the relevant Group/Skill/MCP stack for the task (see `docs/ai-tooling.md`).
2. Run the necessary verification commands using the identified tooling.
3. Document the outcomes in the task file `Lepesnaplo`.

## Final Response Checklist

When finishing a task, report:

- Task ID.
- Branch.
- PR link or PR number.
- Key files changed.
- Verification command results.
- Anything not completed or blocked.
