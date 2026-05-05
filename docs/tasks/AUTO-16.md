# AUTO-16 Agent operating rules

Feladat leírása
Repo-szintű agent szabályfájlt kell létrehozni, amely minden AI számára egyértelműen leírja az új feladatok kötelező folyamatát: task azonosító, branch naming, dokumentáció, commit/PR konvenció, validáció, deploy és tiltott műveletek.

Statusz
- in progress

Verzió
- `0.1.1`

Branch
- `feature/AUTO-16-agent-guidelines`

PR
- PR #27

Acceptance criteria
- Van repo gyökérben agent szabályfájl.
- A fájl egyértelműen különválasztja az `AUTO-*` és `BUG-*` feladatokat.
- Leírja az új feladatok kötelező branch, task doc, commit és PR folyamatát.
- Tiltja az unrelated work keverését és a lokális szolgáltatásindítást explicit kérés nélkül.
- A task dokumentáció követi a repo meglévő `docs/tasks/README.md` és `docs/branching.md` szabályait.

Dokumentumok és fájlok
- `AGENT.md`
- `docs/tasks/AUTO-16.md`

Lepesnaplo
1. Ellenőriztem, hogy nincs meglévő agent szabályfájl a repóban.
2. Elolvastam a meglévő task és branching szabályokat.
3. Létrehoztam az `AGENT.md` repo-szintű operating rules fájlt.
4. Létrehoztam az `AUTO-16` task dokumentációt.
5. Megnyitottam a kapcsolódó PR-t.

Eredmény
- Az AI agentek számára kötelező munkafolyamat dokumentálva lett.
