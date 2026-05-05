# MCP

## Cel

Az Autoforge agentek Playwright MCP-t hasznalhatnak bongeszos ellenorzesre, UI regresszio vizsgalatra es publikus oldal validalasara.

## Playwright MCP

A projekt OpenCode konfiguracioja `playwright` neven regisztralja a Microsoft Playwright MCP szervert.

Konfiguracio:

- repo-szintu OpenCode config: `opencode.json`
- private OpenCode service config: `infra/compose/opencode.json`
- MCP package: `@playwright/mcp@latest`
- futtatas: `npx -y @playwright/mcp@latest --headless --browser chromium --isolated`

## Hasznalat

Agent promptban hivatkozz a Playwright MCP-re neven:

```text
use playwright to open https://oci.prodet.org and verify the landing page
```

## Szabalyok

- Ne indits lokalis frontend vagy backend dev servert csak azert, hogy Playwright MCP-t hasznalj.
- Publikus OCI oldal ellenorzesehez hasznald a publikus URL-t.
- Lokalis URL ellenorzese csak akkor megengedett, ha a user explicit kert lokalis service inditast.
- Secretet, tokent vagy privat adatot ne irj be bongeszobe.
- Ha Playwrighttal verifikalsz egy PR-t, a task `Lepesnaplo` reszebe rogzitsd, mit ellenoriztel.
