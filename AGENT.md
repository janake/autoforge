# Agent Operating Rules

This file is mandatory for every AI agent working in this repository. Before starting any new task, read and follow these rules. If these rules conflict with a direct user instruction in the current conversation, ask for clarification before changing files.

## Core Principles

- Do not mix unrelated work in the same branch, commit, or PR.
- Do not start local servers, frontend dev servers, backend services, Docker stacks, or long-running local processes unless the user explicitly asks for it.
- Prefer small, correct changes over broad rewrites.
- Never revert, overwrite, or remove user changes unless the user explicitly asks for it.
- Do not commit secrets, private keys, tokens, local absolute paths, personal identifiers, cloud credentials, tenancy/user OCIDs, or machine-specific data.
- Use placeholders or configured secrets for sensitive values, for example `${AUTOFORGE_SSH_KEY}`, `<registry-owner>`, `<repo-url>`.
- Before starting any task, check whether a relevant skill exists; if one does, use it.

## MCP And Skill Usage Protocol

Every agent must use the available MCP, skill, and capability-group guidance before asking the user for information that tools can retrieve.

1. Always read `docs/ai-tooling.md` and `ops/ai/mcps.yaml` when a task needs external state, Jira, GitHub, runtime, OCI, browser, frontend, backend, or dependency context.
2. Identify the relevant group/skill/MCP stack from `docs/ai-tooling.md` before implementation and before opening a PR.
3. If the platform exposes a native MCP/tool namespace, use that native tool first.
4. If no native MCP namespace is exposed, use the documented local launcher or repo script for the MCP.
5. Do not claim an MCP is unavailable only because direct environment variables are not visible. Many local MCP launchers resolve credentials from OCI Vault display-name lookup or configured secret OCIDs.
6. Never print secret values, tokens, credentials, OCIDs, or raw Vault payloads. It is acceptable to report that lookup succeeded or failed without showing values.
7. If an MCP lookup fails, report the exact non-secret failure mode and the exact launcher/tool that failed.
8. Do not ask the user for a Jira issue title, sprint order, status, target version, or scope until Jira MCP lookup has been attempted.
9. If a skill exists in the runtime skill registry, load it with the skill tool. If no runtime skill exists, follow the corresponding guidance in `docs/ai-tooling.md` and document which group/skill/MCP stack was used.

### Jira MCP Required Usage

Jira is the single source of truth for task scope, status, and execution order. For Jira work, use the `jira` MCP from `ops/ai/mcps.yaml`.

- Native Jira MCP tools, when exposed, include `jira_search`, `jira_get_issue`, `jira_list_transitions`, `jira_transition_issue`, `jira_add_comment`, `jira_list_boards`, `jira_list_sprints`, and `jira_get_sprint_issues`.
- Local launcher: `ops/mcp/jira-local.sh`.
- Local MCP server: `ops/mcp/jira-server.mjs`.
- Credential resolution order is direct env (`JIRA_BASE_URL`, `JIRA_PROJECT_KEY`, `JIRA_EMAIL`, `JIRA_API_TOKEN`), explicit OCI secret OCID env (`OCI_<NAME>_SECRET_OCID`), then OCI Vault display-name lookup for the same names.
- To identify the next task, query the active sprint or JQL before guessing. Examples: `jira_search` with `project = AUTO AND status != Done ORDER BY Rank ASC`; or list boards, list active sprints, then call `jira_get_sprint_issues`.
- Before starting a Jira story, transition it to `In Progress`.
- After opening a PR for a Jira story, transition it to `Under Test` and add a comment with branch, PR, version, and verification summary.

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
- For every Jira task that changes tracked files, use `git worktree` under `/home/janake/IdeaProjects/autoforge-worktrees/<TASK-ID>-<slug>` instead of working in the main workspace branch.
- Keep `/home/janake/IdeaProjects/autoforge` on the user's current branch so IntelliJ IDEA does not get forced onto task branches.
- Do not create a `BUG-*` branch for a user-requested feature or UI cleanup.
- Do not reuse an unrelated branch just because it is currently checked out.
- If the current branch belongs to a different Jira task, stop and create a dedicated branch for the new task before editing files.
- If a mistaken branch or PR was created, close it and create the correct one instead of continuing the mistake.
- If the target code change is already on `main`, do not fabricate a duplicate code diff. Document the situation honestly in the task and PR.
- If any tracked repository file is changed, the task must end with a commit, branch push, and open PR. Do not wait for the user to ask for the PR separately.
- When a task is finished, always open or update the PR before handing it back.

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
- For Oracle Cloud runtime, instance principal, Vault, VCN, security list, NSG, jump-host, or host readiness work, use `oracle-cloud-runtime-debug` with `oracle-cloud`, `oci`, `vault-secrets`, and `ssh-remote-shell` from `docs/ai-tooling.md`.
- For GitHub Actions, GHCR image tags/digests, package permissions, deploy token, or build-to-registry-to-host handoff work, use `github-actions-ghcr-debug` with `github-actions`, `ghcr-registry`, and `github` from `docs/ai-tooling.md`.
- For Spring, Spring Boot, Spring Cloud, Spring Security, Java 21-25, Maven toolchain, or Lombok work, use `spring-boot-java-runtime` and `lombok-java-hygiene` with `spring-framework`, `maven-java-deps`, `java-platform`, and `lombok` from `docs/ai-tooling.md`.
- For React web work, use `react-web-engineering` with `react-web`, `node-workspace`, `playwright`, and `http-api` from `docs/ai-tooling.md`; for React Native or mobile planning, use `react-native-mobile-readiness` with `react-native` before adding mobile scaffolding.
- For Docker, Compose, image preload, one-shot utility container, or container metadata isolation work, use the `delivery-runtime` group with `container-runtime-debug`, `docker-engine`, `docker-compose`, and `ssh-remote-shell` from `docs/ai-tooling.md`.
- For high-risk AI/code-execution sandboxing or stronger isolation decisions, use `microvm-runtime-isolation` with the `firecracker` MCP from `docs/ai-tooling.md` before choosing container-only isolation.

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

- Mindig mozgasd a Jira ticketet a megfelelő státuszba a munka aktuális állapotának megfelelően.
- Ha bármilyen Jira **story**-t kezdesz el dolgozni, akkor előtte (vagy a munka legelső lépésében) a Jira státuszt állítsd át **In Progress**-ra.
- Ha a storyhoz PR nyílik, a Jira státuszt állítsd át **Under Test**-re, és tartsd ott a review/verification ideje alatt.
- Ha a munka befejeződött és a PR merge-elve van, csak akkor állítsd **Done**-ra, ha az összes szükséges verifikáció is kész.

## Task Completion Rule

- A nem-spike Jira taskokat csak akkor szabad `Done` státuszba tenni, ha az implementáció merge-elve van, és a PR review/verification lezárult.

## Epic Closure Rule

- Jira epicet csak akkor szabad `Done` státuszba tenni, ha az epic alatti összes story is `Done` státuszban van.

## Jira Archive Rule

- Ha az összes Jira issue, epic és story exportálása vagy archiválása a cél, akkor a teljes exportot lokálisan kell letölteni, és egyetlen zip archívumban kell összecsomagolni.
