# AUTO-21 Automatic version bump rule

Feladat leírása
Be kell vezetni azt a szabályt, hogy minden nem-MAJOR feladatnál a projektverzió automatikusan a következő megfelelő `PATCH` vagy `MINOR` verzióra emelkedjen. Csak MAJOR emelésnél kérdezzünk rá a usernél. A szabály kerüljön be az agent, task, branch, release és versioning doksikba.

Statusz
- in progress

Verzió
- `0.1.2`

Branch
- `feature/AUTO-21-auto-version-bump`

PR
- pending

Acceptance criteria
- A versioning doksi kimondja, hogy nem-MAJOR tasknál a bump automatikus.
- Az AGENT kimondja, hogy csak MAJOR emelésnél kell rákérdezni.
- A task tracking és branch doksik is követik a szabályváltozást.
- A release manifest tartalmazza az AUTO-21-et a 0.1.2 verzió alatt.
- A root package verzió 0.1.2-re van emelve.

Dokumentumok és fájlok
- `AGENT.md`
- `docs/README.md`
- `docs/branching.md`
- `docs/versioning.md`
- `docs/releases.md`
- `docs/tasks/README.md`
- `docs/tasks/AUTO-21.md`
- `package.json`
- `package-lock.json`

Lepesnaplo
1. Új `AUTO-21` branch-et hoztam létre `origin/main` alapból.
2. Átírtam a versioning szabályokat, hogy nem-MAJOR tasknál automatikus legyen a bump.
3. Frissítettem az agent, branching, task, docs és release manifest dokumentációt.
4. A root projektverziót `0.1.2`-re emeltem.

Eredmény
- pending
