# AUTO-23 AI tooling stack integration

Feladat leírása
Az Autoforge stackhez egy erősebb, nem minimál MCP + skill + group struktúrát kell bekötni a repóba. A cél, hogy az auth, gateway, deploy, OCI runtime és dokumentációs munkák ne ad hoc módon történjenek, hanem egy világos capability-modellel. A konfigurációs rétegnek repo-szinten is léteznie kell, és a leírásban minden elemhez tételesen szerepelnie kell, mire való.

Statusz
- in progress

Verzió
- `0.1.14`

Branch
- `feature/AUTO-23-ai-ops-tooling`

PR
- PR #48

Acceptance criteria
- A repo tartalmaz külön MCP, skill és group manifestet.
- A dokumentáció tételesen leírja minden MCP szerepét.
- A dokumentáció tételesen leírja minden skill szerepét.
- A dokumentáció tételesen leírja minden group szerepét.
- A fő README-k hivatkoznak az AI tooling dokumentációra.

Dokumentumok és fájlok
- `ops/ai/mcps.yaml`
- `ops/ai/skills.yaml`
- `ops/ai/groups.yaml`
- `docs/ai-tooling.md`
- `docs/README.md`
- `README.md`
- `docs/releases.md`
- `docs/tasks/AUTO-23.md`
- `package.json`
- `package-lock.json`

Lepesnaplo
1. Felmértem, hogy a repóban nincs meglévő MCP/skill/group struktúra.
2. Létrehoztam a repo-s AI tooling manifesteket.
3. Elkészítettem a tételes dokumentációt minden MCP, skill és group céljáról.
4. Bekötöttem a hivatkozásokat a fő README-kbe.
5. Emeltem a projektverziót `0.1.14`-re.

Eredmény
- Az Autoforge stackhez dedikált AI tooling konfiguráció és leírás került a repóba.
