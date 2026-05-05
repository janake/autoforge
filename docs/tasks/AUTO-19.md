# AUTO-19 Playwright MCP setup

Feladat leírása
Be kell allitani a Playwright MCP szervert az agenteknek, hogy bongeszos ellenorzeseket tudjunk vegezni OpenCode-bol. A konfiguracio legyen repo-szinten dokumentalt, es a private OpenCode service configban is legyen elerheto.

Statusz
- in progress

Verzió
- `0.1.1`

Branch
- `feature/AUTO-19-playwright-mcp`

PR
- pending

Acceptance criteria
- Van repo-szintu OpenCode MCP config Playwright szerverrel.
- A private OpenCode service config is tartalmazza a Playwright MCP szervert.
- Van MCP dokumentacio.
- Az agent szabalyok hivatkoznak a Playwright MCP hasznalatara.
- A release manifest tartalmazza az `AUTO-19` feladatot.

Dokumentumok és fájlok
- `opencode.json`
- `infra/compose/opencode.json`
- `docs/mcp.md`
- `AGENT.md`
- `docs/README.md`
- `docs/releases.md`
- `docs/tasks/AUTO-19.md`

Lepesnaplo
1. Ellenoriztem, hogy nincs meglevo MCP konfiguracio a repoban.
2. Elolvastam az OpenCode MCP es Playwright MCP konfiguracios dokumentaciot.
3. Hozzaadtam a Playwright MCP szervert a repo-szintu OpenCode configba.
4. Hozzaadtam a Playwright MCP szervert a private OpenCode service configba.
5. Letrehoztam az MCP hasznalati dokumentaciot.
6. Frissitettem az agent es release dokumentaciot.
7. Beallitottam a user-level OpenCode configot is ugyanarra a Playwright MCP szerverre.

Eredmény
- pending
