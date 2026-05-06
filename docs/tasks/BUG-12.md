# BUG-12 Normalize public backend upstream

Feladat leírása
A publikus gateway 502-t ad `/api/**` hívásokra, miközben a public és private deploy is lefut. A legvalószínűbb ok, hogy a `BACKEND_UPSTREAM` secret rossz formátumban kerül a gateway környezetébe: lehet benne `http://` vagy `https://` előtag, illetve hiányozhat a `:8080` port. A gateway konfiguráció `http://${BACKEND_UPSTREAM}` formát vár, ezért a deploynak `host:port` alakra kell normalizálnia az értéket.

Statusz
- in progress

Verzió
- `0.1.7`

Branch
- `bug/BUG-12-normalize-backend-upstream`

PR
-

Acceptance criteria
- A public deploy eltávolítja a `http://` és `https://` előtagot a backend upstreamből.
- A public deploy `:8080` portot ad hozzá, ha az upstreamből hiányzik a port.
- A public deploy fallbackként használja az `OCI_PRIVATE_SSH_HOST` vagy `OCI_PRIVATE_HOST` értéket, ha `OCI_BACKEND_UPSTREAM` nincs megadva.
- A gateway `BACKEND_UPSTREAM` env értéke `host:port` formátumú.

Dokumentumok és fájlok
- `.github/workflows/deploy-public.yml`
- `docs/releases.md`
- `docs/tasks/BUG-12.md`
- `package.json`
- `package-lock.json`

Lepesnaplo
1. Ellenőriztem, hogy BUG-11 után a public és private deploy is sikeresen lefutott.
2. Megállapítottam, hogy a 502 továbbra is gateway-to-backend upstream probléma.
3. Hozzáadtam a backend upstream deploy idejű normalizálását.
4. Emeltem a projektverziót `0.1.7`-re.

Eredmény
- A public gateway egységes, `host:port` formátumú backend upstream értéket kap.
