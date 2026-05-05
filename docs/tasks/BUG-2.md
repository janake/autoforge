# BUG-2: SSH deploy host split for public/private workflows

Feladat leírása
A deploy workflow-k nem a webes domainre, hanem a tényleges SSH-routolható hostra kell hogy csatlakozzanak. A publikus hostnál a Cloudflare-kezelt web hostname nem használható SSH célként.

Statusz
- in progress

Branch
- `bug/BUG-2`

PR
- pending

Acceptance criteria
- A public deploy workflow külön SSH host secretet használ.
- A private deploy workflow külön SSH host secretet használ a jump hosthoz, és ha kell, a private hosthoz is.
- A dokumentáció külön kezeli a web hostot és az SSH hostot.
- A public deploy workflow opcionálisan Cloudflare DNS rekordokat is frissít.
- A CI továbbra is fut `feature/*` és `bug/*` ágakon, a deploy pedig csak `main`-en.

Dokumentumok és fájlok
- `.github/workflows/deploy-public.yml`
- `.github/workflows/deploy-private.yml`
- `.github/workflows/README.md`
- `infra/deploy/public/cloudflare_sync.py`
- `docs/deployment.md`
- `docs/tasks/BUG-2.md`

Biztonsági megfontolások
- Nem írunk gitbe hostneveket, IP-ket, tokeneket vagy kulcsokat, ha azok érzékeny környezethez köthetők.
- A Cloudflare-kezelt web hostname nem tekinthető SSH deploy célként.

Lepesnaplo
1. Megnéztem a GitHub Actions logot és a hibás SSH hostname feloldást.
2. Szétválasztottam a web host és az SSH host használatát a workflow-kban.
3. Frissítettem a deploy dokumentációt a külön SSH host secret-ekkel.
4. Beépítettem a Cloudflare DNS rekord syncet a public deploy flow-ba.

Eredmény
- A deploy hostok már külön kezelhetők webes és SSH célokra.
