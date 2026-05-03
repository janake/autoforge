# PR #10 — Build, npm audit és Trivy image-scan (runbook)

Cél: reprodukálható, lépésről-lépésre útmutató a PR #10 (vite frissítés) ellenőrzésére: frontend build, `npm audit`, Docker image build és Trivy scan. A lépések mind lokális, mind CI futtatásra használhatók. Minden parancs pontosan a repo struktúrájára (root `package-lock.json`, `apps/web/Dockerfile`, npm workspaces) van szabva.

---

Összefoglaló döntés: A — Indítsd el a build+audit+Trivy scanneket és csatolom az eredményt a PR #10-hez.

Preferencia (ajánlás): Lokális gyors előellenőrzés, majd CI-ben végleges futtatás és artifact feltöltés a PR-hez.

Kérdés a lockfile-ról: akarod-e, hogy frissítsem és commitoljam a `package-lock.json`-t a `vite` frissítéséhez? (igen/nem)

---

1) Előkészületek (repo gyökérből)

Parancsok copy-paste-hez (bash):

```bash
# lépj a repo gyökérbe
cd /path/to/autoforge

# hozz létre mappát az eredményeknek
mkdir -p security/audit/pr-10

# (opcionális) győződj meg róla, hogy nincs lokális node_modules aminek problémát okozhat
rm -rf node_modules
```

2) Lokális dependency telepítés és `npm audit`

```bash
# telepítés reproducible módon (workspaces miatt a root lockfile használatos)
npm ci

# npm audit JSON mentése
npm audit --json > security/audit/pr-10/npm-audit.json

# rövid, ember-olvasható kivonat (jq telepítve szükséges)
jq -r '"summary:\n" + (.metadata.vulnerabilities|to_entries|map("") )' security/audit/pr-10/npm-audit.json > security/audit/pr-10/npm-audit-summary.txt || true
```

Megjegyzés: a projekt Dockerfile-ja (`apps/web/Dockerfile`) a root `package-lock.json`-t másolja be a build lépéshez — ezért a lockfile szerepe döntő.

3) Docker image build (web)

Build a frontend image-hez (repo gyökérből):

```bash
# egyszerű tag
docker build -t autoforge-web:pr-10 -f apps/web/Dockerfile .

# ha architektúra miatt kell (pl. runner x86 helyett arm):
docker build --platform linux/amd64 -t autoforge-web:pr-10 -f apps/web/Dockerfile .
```

Időigény: néhány perc — függ a géptől és cache-től.

4) Trivy image scan

Telepítsd a Trivy-t (ha nincs): https://aquasecurity.github.io/trivy/v0.40.0/installation/

```bash
# alap Trivy scan JSON kimenettel
trivy image --format json -o security/audit/pr-10/trivy-image.json autoforge-web:pr-10

# rövid összegzés készítése (jq-val)
jq '.Results[] | {Target, Vulnerabilities: (.Vulnerabilities|length)}' security/audit/pr-10/trivy-image.json > security/audit/pr-10/trivy-summary.txt || true
```

Alternatíva: a Dockerfile és a fájlrendszer közvetlen vizsgálata:

```bash
trivy fs --format json -o security/audit/pr-10/trivy-fs.json apps/web
```

5) Eredmények csatolása a PR-hez

Választható módszerek:

- A) Commitoljuk az eredményeket a branch-re (ajánlott, ha elfogadott a repo policy):

  - Fájlok amiket érdemes hozzáadni: `security/audit/pr-10/npm-audit.json`, `security/audit/pr-10/npm-audit-summary.txt`, `security/audit/pr-10/trivy-image.json`, `security/audit/pr-10/trivy-summary.txt`.
  - Commit példa:

    ```bash
    git add security/audit/pr-10
    git commit -m "docs(security): add scan reports for PR #10 (npm audit, trivy)"
    git push origin HEAD
    ```

- B) CI artifact + PR comment (nem módosítja a branch-et):

  - Hozzunk létre vagy bővítsünk egy workflow-ot, amely artifact-ként feltölti a JSON-okat, majd a workflow létrehoz egy PR-commentet a rövid összegzéssel és az artifact linkjével.

- C) Manuális feltöltés / Gist + PR comment (ha nem akarjuk commitolni a fájlokat):

  - `gh` CLI: `gh pr comment 10 --body-file security/audit/pr-10/pr-comment.md`

PR comment sablon (másold be a PR megjegyzésbe):

```
Biztonsági scan eredmények — PR #10

- npm audit: lásd `security/audit/pr-10/npm-audit.json` (összegzés: lásd `npm-audit-summary.txt`)
- Trivy image scan: lásd `security/audit/pr-10/trivy-image.json` (összegzés: `trivy-summary.txt`)

Rövid javaslat: frissítsük a `vite`-et a PR-ben javasolt verzióra és döntsünk a lockfile (`package-lock.json`) commitolásáról. Ha szeretnéd, frissítem és commitolom a lockfile-t, majd futtatok új CI buildet.
```

6) Lockfile frissítése (ha a PR a `vite`-et frissítette)

Ha a PR frissíti a `vite` dependency-t, a root `package-lock.json` frissítése szükséges lehet (npm workspaces). Lépések:

```bash
# 1) Frissítsd az érintett package.json-okat (pl. apps/web/package.json)
# 2) A repo gyökérben futtasd:
npm install

# 3) Teszteld a buildet:
npm run typecheck:web
npm run build:web

# 4) Ha minden ok, commitold a változásokat (package-lock.json + apps/web/package.json)
git add package-lock.json apps/web/package.json
git commit -m "chore(deps): upgrade vite to <VERSION> and update package-lock.json"
git push origin HEAD
```

Megjegyzés: ha csak a lockfile-t akarjuk frissíteni anélkül, hogy ténylegesen új verziót telepítenénk, használhatod `npm install --package-lock-only`, de általában javasolt `npm install` + build teszt.

7) CI javaslat (automatizáció)

- Bővítsd a `.github/workflows/frontend-build.yml`-t vagy hozz létre egy `security-scan.yml` workflow-ot PR eseményre, amely:
  - `npm ci`
  - `npm audit --json` → upload artifact
  - `docker build` (a runneren) → `trivy image` → upload artifact
  - `actions/github-script` segítségével hozz létre PR commentet rövid összegzéssel és artifact linkkel

8) Ellenőrzési checklist (egyszerű, copy-paste)

- [ ] Lokálisan: `npm ci` lefut
- [ ] Locally: `npm audit --json > security/audit/pr-10/npm-audit.json`
- [ ] Docker build sikeres: `docker build -t autoforge-web:pr-10 -f apps/web/Dockerfile .`
- [ ] Trivy scan elkészült: `trivy image --format json -o security/audit/pr-10/trivy-image.json autoforge-web:pr-10`
- [ ] Rövid összegzés (`*_summary.txt`) kész
- [ ] Eredmények commitolva vagy CI artifact feltöltve
- [ ] PR comment elkészítve és társítva a PR-hez
- [ ] (Ha kell) Lockfile frissítése és új CI futtatás

Időbecslés összesen (lokális előellenőrzés + PR csatolás): 30–60 perc. Ha lockfile és CI-s automatizálás is kell: 1.5–3 óra.

---

Ha szeretnéd, elvégzem a lokális előellenőrzést itt (futtatom a `npm ci`, `npm audit`, `docker build`, `trivy image` lépéseket) és commitolom az eredményeket a PR-hez. Kérlek válaszolj egyszerűen: szeretnéd-e, hogy (1) lokálisan előszűrjek most, és (2) frissítsem-e és commitoljam a `package-lock.json`-t a `vite` frissítéshez.

