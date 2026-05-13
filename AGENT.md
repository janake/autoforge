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

## Jira-First Task Flow

For any non-trivial code, infra, workflow, or documentation task, do the following:

1. Identify or create a Jira issue.
2. Assign the task to a target version in Jira.
3. Create a dedicated branch from the current `origin/main` unless the user explicitly asks to continue an existing branch.
4. Use branch names in this format:
   - `feature/AUTO-<number>-short-description`
   - `bug/BUG-<number>-short-description`
5. Make only changes that belong to that Jira task.
6. Run the relevant verification commands.
7. Commit with a message that starts with the Jira task ID.
8. Push the branch.
9. Open or update a PR with a title that starts with the Jira task ID and body that includes the target version.
10.Do not continue unrelated work on a branch that was created for a different Jira task; move the task to its own dedicated branch before finishing it.
11.The task is not completed until all of the subtasks are completed.

## Versioning

- Every `AUTO-*` and `BUG-*` Jira issue must have a target version assigned.
- Version changes are reflected in `docs/releases.md` and the Jira issue.
- The target version in Jira is mandatory.
- Always display the appropriate version in documentation banner so it can be easily traced during audits.
- For non-major tasks, choose the next version automatically; only ask the user before a `MAJOR` bump.
- The canonical task-to-version mapping is `docs/releases.md`.
- Versioning rules are documented in `docs/versioning.md`.
- Project-owned Docker images must be tagged with the root `package.json` version by the container image workflow.
- Do not open a PR if the Jira issue has no target version.
- Do not merge PR documentation that disagrees with `docs/releases.md`.

## Commit And PR Naming

- Commit messages must start with the task ID, for example `[AUTO-15] Remove public entry panel`.
- PR titles must start with the task ID, for example `[AUTO-15] Landing page cleanup`.
- PR bodies must include a `Version` section with the target version.
- For non-major tasks, the target version should be the next automatic `PATCH` or `MINOR` version.
- Do not use generic-only titles like `fix:` or `docs:` without the task ID.
- Keep commits focused and do not include unrelated cleanup.

## Branch And PR Hygiene

- Before making changes, verify that the checked-out branch matches the active Jira task.
- Do not create a `BUG-*` branch for a user-requested feature or UI cleanup.
- Do not reuse an unrelated branch just because it is currently checked out.
- If the current branch belongs to a different Jira task, stop and create a dedicated branch for the new task before editing files.
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
- Before starting work, every AI must read `docs/project-readme.md` to understand what Autoforge is and how to approach the task.
- Keep task docs consistent with the actual branch and PR.
- Do not leave stale references to wrong IDs, wrong PRs, or closed/superseded branches.
- For any infrastructure, host readiness, OCI, runtime provisioning, or similar repeatable environment change, also create a Terraform script or Terraform module that can reproduce the change from scratch.
- Architecture diagrams that are published in Confluence should not remain duplicated in git; update the Confluence page instead and keep only the summary/reference links in repo docs.
- If a task changes the filesystem, repository files, or any tracked artifact, finish the task with a PR so the change is reviewable and traceable.
- Treat "done" work on a Jira story as incomplete until the branch has a PR and the Jira story is moved to `Under Test` while the PR is open.

## Jira Decision And Spike Rules

- Never transition a Spike, decision issue, or user-choice issue to `Done` based only on a default recommendation, agent preference, or inferred best practice.
- A Spike can be moved to `Done` only when the user explicitly confirms the decision, the requested research output is completed and recorded, or the user directly asks to close it.
- If a Spike contains a recommended default but no confirmed decision, leave it open and comment with `Recommended, not decided` instead of closing it.
- When recording a decision in Jira, include the selected option, the reason, follow-up issue keys, and whether the decision came from explicit user confirmation.
- If an issue was closed incorrectly, immediately reopen it, add a correction comment, and tell the user what was corrected.

## Review Mindset

When asked for a review, prioritize findings first: bugs, regressions, risks, missing tests, and security concerns. If no findings exist, say that explicitly and mention residual risks.

## Pre-PR Mandatory Protocol

Before opening a PR, perform the following:

1. Identify the relevant Group/Skill/MCP stack for the task (see `docs/ai-tooling.md`).
2. Run the necessary verification commands using the identified tooling.
3. Document the outcomes in the task file `Lepesnaplo`.

## Final Response Checklist

When finishing a task, report:

- Jira issue ID.
- Branch.
- PR link.
- Key files changed.
- Verification command results.
- Anything not completed or blocked.

## Jira Status Rule

Ha bármilyen Jira **story**-t kezdessz el dolgozni, akkor előtte (vagy a munka legelső lépésében) a Jira státuszt állítsd át **In Progress**-ra.
- Ha a storyhoz PR nyílik, a Jira státuszt állítsd át **Under Test**-re, és tartsd ott a review/verification ideje alatt.

## Task Completion Rule

- A nem-spike Jira taskokat csak akkor szabad `Done` státuszba tenni, ha az implementáció merge-elve van, és a PR review/verification lezárult.

## Epic Closure Rule

- Jira epicet csak akkor szabad `Done` státuszba tenni, ha az epic alatti összes story is `Done` státuszban van.

## Jira Archive Rule

- Ha az összes Jira issue, epic és story exportálása vagy archiválása a cél, akkor a teljes exportot lokálisan kell letölteni, és egyetlen zip archívumban kell összecsomagolni.
