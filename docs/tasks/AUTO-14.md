# AUTO-14: Auto deploy merge utani pushokra

Feladat leírása
A publikus és privát deploy workflow-kat úgy kell beállítani, hogy a `main` branch-re merge-elt releváns változások automatikusan deployoljanak, ne csak manuális indításra fussanak.

Statusz
- in progress

Branch
- `AUTO-14-auto-deploy-on-merge`

PR
- pending

Acceptance criteria
- A publikus deploy workflow `main` pushra is indul, ha releváns publikus fájlok változtak.
- A privát deploy workflow `main` pushra is indul, ha releváns privát fájlok változtak.
- A manual workflow dispatch megmarad.
- A deployment doksi leírja az automatikus merge utáni rolloutot.

Dokumentumok és fájlok
- `.github/workflows/deploy-public.yml`
- `.github/workflows/deploy-private.yml`
- `docs/deployment.md`
- `docs/tasks/AUTO-14.md`

Biztonsági megfontolások
- Nem szabad minden docs-only merge-re deployt indítani.
- Secret, token, OCID, private key vagy személyes azonosító továbbra sem kerülhet gitbe.

Lepesnaplo
1. Ellenőriztem, hogy a jelenlegi deploy workflow-k csak manuális indítást használtak.
2. Frissítettem a publikus és privát deploy workflow triggerét `push` alapú automatikus futásra.
3. Frissítettem a deployment runbookot az automatikus merge utáni deploy leírásával.

Eredmény
- Az új viselkedés a `main`-re merge-elt releváns változások után automatikus deployt indít.
