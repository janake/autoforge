# AUTO-10: OpenCode Vault-backed deploy fix
Feladat leírása
A private OpenCode REST AI service deploy hibáit javítani kell úgy, hogy a secret értékek ne GitHubból és ne gitelt fájlból érkezzenek, hanem OCI Vaultból, a private compute instance instance principal jogosultságával.

Statusz
- review

Branch
- `AUTO-10-opencode-vault-fix`

PR
- PR #14

Acceptance criteria
- A private deploy workflow nem ír OpenCode jelszót vagy provider API kulcsot a host `.env` fájljába.
- A GitHub Actions csak OCI Vault secret OCID-ket továbbít a private host felé.
- A private host deploy scriptje OCI CLI-vel, instance principal auth-tal olvassa ki a Vault secret értékeket.
- Az ideiglenes runtime env fájl deploy után törlődik.
- Az OpenCode konténer ugyanazt a basic auth username/password konfigurációt kapja, mint amit a backend használ.
- A dokumentáció pontosan leírja a létrehozandó Vault secreteket és a szükséges OCI/GitHub beállításokat.

Munkalépések / tesztelés
1. Frissítsd a private Compose stack OpenCode environment változóit.
2. Frissítsd a private deploy scriptet OCI Vault secret lekérésre.
3. Frissítsd a GitHub Actions private deploy workflow-t, hogy secret érték helyett secret OCID-t küldjön.
4. Frissítsd a deployment dokumentációt és a task állapotokat.
5. Ellenőrizd a diffet, a tiltott sensitive mintákat és a workflow/compose szintaxist.

Dokumentumok és fájlok
- `infra/compose/docker-compose.private.yml`
- `infra/compose/.env.private.example`
- `infra/deploy/private/deploy.sh`
- `.github/workflows/deploy-private.yml`
- `docs/deployment.md`
- `docs/tasks/AUTO-10.md`

Biztonsági megfontolások
- Gitbe nem kerülhet OpenCode jelszó, provider API kulcs, token, lokális path vagy személyes azonosító.
- A Vault secret OCID-ket GitHub secretként kezeljük, hogy ne jelenjenek meg logban.
- A private hoston a secret értékek csak ideiglenes runtime env fájlban jelennek meg a Compose futtatásához.
- A runtime env fájlt a deploy script cleanup trap törli.

Lepesnaplo
1. Ellenőriztem a kiinduló branch állapotot:
   ```bash
   git status --short --branch
   gh pr view 13 --json mergeable,mergeStateStatus,state,title,headRefName,baseRefName,url
   ```
2. Frissítettem a lokális `main` ágat és új javító branchet nyitottam:
   ```bash
   git fetch origin
   git switch main
   git pull --ff-only origin main
   git switch -c AUTO-10-opencode-vault-fix
   ```
3. Ellenőriztem a hivatalos OpenCode dokumentációban, hogy a `ghcr.io/anomalyco/opencode` image és az `opencode serve` HTTP server mód támogatott.
4. Az OpenCode secret értékeket GitHub secret helyett OCI Vault secret OCID alapú átadásra állítottam át.
5. Lefuttattam a whitespace és shell szintaxis ellenőrzéseket:
   ```bash
   git diff --check
   bash -n infra/deploy/private/deploy.sh
   ```
6. Lefuttattam a private compose config validációt dummy secret értékekkel:
   ```bash
   env OPENCODE_SERVER_PASSWORD=dummy OPENAI_API_KEY=dummy docker-compose --env-file infra/compose/.env.private.example -f infra/compose/docker-compose.private.yml config
   ```
7. Ellenőriztem, hogy nem maradt gitelt lokális path, kulcsfájlnév vagy személyes azonosító:
   ```bash
   git grep -n "<sensitive-patterns>" -- .
   ```

Eredmény
- PR #14 megnyitva review-ra.
