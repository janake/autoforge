# BUG-6 Workflow version quoting fix

Feladat leírása
A deploy es image workflow-kban a projektverzio beolvasasakor a shell quoting hibas volt. Emiatt a workflow elhasalt a `PROJECT_VERSION` betoltesenel. A verzio beolvasast stabil, multiline shell blokkra kell cserelni.

Statusz
- in progress

Verzió
- `0.1.1`

Branch
- `bug/BUG-6-version-quoting-fix`

PR
- pending

Acceptance criteria
- A `Read project version` step minden workflow-ban shell quoting hiba nelkul fut.
- A `container-images`, `deploy-public` es `deploy-private` workflow-k hasznaljak a root `package.json` verziojat.
- A workflow-k nem tornek a bash parancs parsolasan.

Dokumentumok és fájlok
- `.github/workflows/container-images.yml`
- `.github/workflows/deploy-public.yml`
- `.github/workflows/deploy-private.yml`
- `docs/releases.md`
- `docs/tasks/BUG-6.md`

Lepesnaplo
1. Azonosítottam, hogy a GitHub Actions hiba a `node -p` beágyazott quotingja miatt történt.
2. A problémás `Read project version` lépéseket multiline shell blokkra cseréltem.

Eredmény
- pending
