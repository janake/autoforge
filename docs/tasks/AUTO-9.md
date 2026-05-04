# AUTO-9: Private OpenCode REST AI service
Feladat leírása
A hátsó OCI gépen egy Dockerben futó OpenCode REST AI szolgáltatást kell bevezetni, amelyet a backend belső hálózaton tud hívni. A szolgáltatás nem kap publikus host portot, csak a private compose stack részeként fut.

Statusz
- in-progress
Branch
- `AUTO-9-opencode-rest-ai`
PR
- pending

Acceptance criteria
- A `infra/compose/docker-compose.private.yml` tartalmaz `opencode` szolgáltatást.
- Az `opencode` szolgáltatás Docker image-ből fut, nem hoston telepített binárisként.
- Az `opencode` szolgáltatás csak a private Docker networkön érhető el.
- A backend megkapja az opencode REST endpoint eléréséhez szükséges környezeti változókat.
- A deploy workflow kimásolja a szükséges opencode config fájlt és environment változókat a private hostra.
- A dokumentáció leírja a private AI service célját, a REST hozzáférést és a szükséges secret-eket.

Munkalépések / tesztelés
1. Add hozzá az `opencode` compose service-t az `infra/compose/docker-compose.private.yml` fájlhoz.
2. Add hozzá a szükséges opencode config fájlt és private env mintát.
3. Frissítsd a private deploy workflow-t, hogy az új config és secret-ek is felkerüljenek a hostra.
4. Ellenőrizd, hogy a private hoston a backend és az opencode konténer is fut, és az opencode REST endpoint belső hálózatról elérhető.

Dokumentumok és fájlok
- `infra/compose/docker-compose.private.yml` — private stack backend + opencode szolgáltatásokkal
- `infra/compose/opencode.json` — OpenCode runtime config
- `infra/compose/.env.private.example` — private host változók és secret placeholder-ek
- `.github/workflows/deploy-private.yml` — private deploy workflow
- `docs/deployment.md` — deployment és secret leírás
- `docs/architecture.md` — rendszer topológia

Biztonsági megfontolások
- Az opencode szolgáltatás nem kap publikus portot.
- Az opencode HTTP felületét jelszó védi.
- Az AI provider API kulcsa nem kerül gitbe, csak GitHub secretből és runtime `.env` fájlból érkezik.
- A backend és az opencode közötti kommunikáció csak a private Docker networkön történik.

Lepesnaplo
1. Átnéztem a private Compose stack jelenlegi állapotát, hogy hova illeszkedik az új service.
2. Ellenőriztem az OpenCode hivatalos HTTP server dokumentációját, mert a service REST alapon fog futni.
3. Felmértem a private deploy workflow-t, hogy az opencode config és secret-ek is felkerülhessenek a hostra.

Eredmény
- pending
