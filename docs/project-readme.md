# Autoforge Project Readme

## README use rule

`<!-- AI-SKIP-START -->`

The content between the markers below is for humans. An AI that is only starting a task should skip this section unless it needs a short human-language summary.

`<!-- AI-SKIP-END -->`

## What this project is

Autoforge is an AI-assisted application platform that is building a self-improving product on top of an OCI-based deployment stack.

## Core product shape

- Public OCI host serves the web entry point with React, the API gateway, and Caddy.
- Private OCI host runs the Spring Boot backend and the OpenCode runtime.
- The deployment model uses Docker, GitHub Actions, GHCR, and OCI-hosted secrets.
- The frontend authenticates through Keycloak using PKCE.
- The backend uses JWT-based authentication and a bundled public signing key.

## Current operating model

- Work is tracked in Jira and mirrored in repo docs only when needed.
- Architecture and workflow changes must be documented alongside the change.
- Confluence holds published diagrams when the repo should not duplicate them.

## Main concerns the project is solving

- Secure AI runtime and sandbox hardening.
- Reproducible OCI infrastructure and deploy flow.
- Controlled access to secrets, metadata endpoints, and host capabilities.
- Observable runtime behavior and traceable generated artifacts.

## How to think about tasks

- Treat Jira as the source of truth for task scope.
- Prefer dedicated branches per task.
- Keep implementation small, explicit, and reviewable.
- Do not add secrets, host-specific values, or manual-only steps unless the task explicitly requires them.

## Rövid magyar összefoglaló

Az Autoforge egy AI-segített alkalmazásplatform, ami egy önfejlesztő terméket épít OCI-alapú deploy stacken.

### Fő elemek

- A publikus OCI hoston fut a webes belépési pont, a gateway és a Caddy.
- A privát OCI hoston fut a Spring Boot backend és az OpenCode runtime.
- A deploy Dockerrel, GitHub Actions-szel, GHCR-rel és OCI secret-ekkel működik.
- A frontend Keycloak PKCE-vel hitelesít.
- A backend JWT alapú autentikációt használ, beágyazott publikus aláíró kulccsal.

### Mire fókuszál a projekt

- Biztonságos AI runtime és sandbox hardening.
- Reprodukálható OCI infrastruktúra és deploy folyamat.
- Titkok, metadata endpointok és host képességek kontrollált elérése.
- Megfigyelhetőség és nyomon követhető generált artefaktok.

### Munkastílus

- A Jira a task-scope forrása.
- Minden tasknak saját branch kell.
- A megoldás legyen kicsi, egyértelmű és reviewzható.
- Ne tegyél bele titkot, host-specifikus értéket vagy kézi-only lépést, ha nem muszáj.
