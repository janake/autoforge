# autoforge

AI-segitseggel sajat magat fejleszto alkalmazas/platform.

## Helyi projektfa

```text
autoforge/
  apps/
    web/
  services/
    backend/
    worker/
  infra/
    compose/
  docs/
  .github/
    workflows/
```

## Jelenlegi allapot

- A live frontend React + Vite + TypeScript alapon fut a public hoston.
- A public hoston Caddy -> Spring Cloud API gateway -> React frontend lánc szolgálja ki a külső forgalmat.
- A private hoston Spring Boot backend és OpenCode REST AI szolgáltatás fut Docker Compose stackben.
- A saját image-ek verziózott GHCR tagekkel (`<version>`) épülnek és deployolódnak.
- A `main` merge-ek automatikusan triggerelik a releváns build/deploy workflow-kat public és private hostra.
- A frontend Keycloak PKCE flow-val hitelesít, a backend JWT-alapú protected endpointokat szolgál ki.
- A backend jelenleg statikus Keycloak publikus kulccsal validálja a bearer tokeneket productionben.
- A private hosthoz 100 GB OCI Block Volume workspace tartozik `/mnt/autoforge-workspace` mounttal.
- A worker szolgáltatás helye továbbra is előkészített, de nem aktív része a jelenlegi OCI futásnak.

## Monorepo irany

- `apps/web`: React frontend helye
- `services/backend`: Spring Boot backend szolgaltatas helye
- `services/worker`: AI altal vezerelt hatterfolyamatok helye
- `infra/compose`: Docker Compose stackek es publikus/private host konfiguraciok
- `infra/deploy`: tavoli deploy scriptek
- `ops/ai`: repo-szintu MCP, skill es capability group manifestek
- `docs`: projekt dokumentacio

## Dokumentumok

- `docs/architecture.md`: OCI topologia es architektura
- `docs/deployment.md`: deploy modell es host elofeltetelek
- `docs/authentication.md`: OIDC / Keycloak integracio
- `docs/ai-tooling.md`: ajanlott MCP / skill / group stack az engineering munkahoz
- `docs/feature-flags.md`: feature flag, personalization es kesobbi OCI Always Free DB opciok
- `docs/branching.md`: feature/bug branch policy es CI/CD szabalyok

## Task tracking

- Minden feladatot a `docs/tasks/` alatt kovetunk.
- Minden feladat kulon Markdown fajlt kap egyedi azonosito alatt, peldaul `AUTO-1`.
- Az adott feladathoz tartozo commitok uzenetei az azonosito prefixszel kezdodjenek, peldaul: `[AUTO-1] Add ...`
- Az aktiv work branchek neve `feature/<azonosito>` vagy `bug/<azonosito>` formatumot kovessen.

## Indulasi megjegyzes

A jelenlegi fókusz a live web + gateway + backend + OpenCode lánc stabil működése. Az auth/deploy hibák után a fő dokumentáció most már a tényleges production állapotot követi; a következő nagyobb lépések a feature work és a worker runtime aktiválása lehetnek.

## Helyi MVP

A legkisebb helyi fejlesztői környezet a gyökérben lévő `docker-compose.yml` alapján indul.

1. Másold a példa környezeti változókat: `cp .env.example .env`
2. Indítsd el a stack-et: `docker compose up --build`
3. Nyisd meg a frontend felületet: `http://localhost:8081`
4. Ellenőrizd a backend health-et: `curl http://localhost:8080/actuator/health` vagy `curl http://localhost:8080/api/v1/health`
5. Nézd a logokat, ha kell: `docker compose logs -f backend web`
6. Állítsd le a stack-et: `docker compose down`

Ez a runbook csak helyi fejlesztésre vonatkozik; a szerveres adatkezelés az Oracle Always Free / Autonomous Database irányhoz igazodik.

## Git broker contract

- `POST /api/v1/git/pr`
- Request mezők: `repositoryUrl`, `baseBranch`, `branchName`, `commitMessage`, `pullRequestTitle`, `pullRequestBody`, `patch`
- Response mezők: `prUrl`, `branchName`, `commitSha`

## Prompt-first MVP flow

- A user promptot ad meg.
- A rendszer ebből Jira taskot hoz létre.
- A Jira automatikusan issue key-t ad a tasknak.
- Az Autoforge ezt az issue key-t használja a jobhoz, branchhez, commit message-hez és PR-hez.
