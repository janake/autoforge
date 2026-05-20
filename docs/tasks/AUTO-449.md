# [AUTO-449] Allow teacher group access to AI prompt tools

Statusz: Under test

Verzio: 0.1.70

Branch: `feature/AUTO-449-teacher-ai-access`

PR: https://github.com/janake/autoforge/pull/180

## Cél

A prompt draft flow, job creation endpoint, and AI menu legyen elerheto Keycloak `teacher` csoport tagoknak is, ne csak `developer` tagoknak.

## Scope

- backend security gate a prompt-draft es job endpointokra
- frontend AI menu gate es copy
- célzott backend tesztek
- release manifest frissites

## Lepesnaplo

- `jira_create_issue(AUTO-449)` - uj task letrehozva
- `git worktree add feature/AUTO-449-teacher-ai-access` - dedikalt worktree/branch letrehozva
- `npm run typecheck:web` - ok
- `mvn -q -f services/backend/pom.xml -Dtest=JobControllerTest,PromptDraftControllerTest test` - ok
- `gh pr create` - PR megnyitva
