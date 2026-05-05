# BUG-3: Follow-up CI/CD deploy and security workflow fixes

Feladat leirasa
A `BUG-2` merge utan a public deploy tovabbra is hibasan futott, mert a workflow SSH/SCP hivasai torott opciofeladassal mentek, a public deploy nem toltotte fel a Cloudflare sync scriptet, es a security scan PR comment lepes hibas GitHub API objektumot hasznalt.

Statusz
- in progress

Branch
- `bug/BUG-3`

PR
- pending

Acceptance criteria
- A public deploy workflow ervenyes `ssh` es `scp` opciohivassal fut.
- A public deploy a `cloudflare_sync.py` scriptet is feltolti a hosztra.
- A security scan PR comment lepes nem bukik el a GitHub API hivason.
- A repo secret-ek tartalmazzak az SSH deploy hostokat a tenyleges OCI IP-kkel.

Dokumentumok es fajlok
- `.github/workflows/deploy-public.yml`
- `.github/workflows/security-scan.yml`
- `docs/tasks/BUG-3.md`

Biztonsagi megfontolasok
- Erzekeny host/IP/secret ertek nem kerul gitelt fajlba.
- A GitHub secret-ekben marad a tenyleges SSH deploy host.

Lepesnaplo
1. Megneztem a merge utani main workflow logokat.
2. Azonositottam a public deploy SSH/SCP hivasi hibat.
3. Javítottam a security scan commentelo lepes GitHub API hasznalatat.
4. Ellenoriztem az OCI instance IP-ket, hogy a GitHub secret-eket valos deploy celokra allitsam.

Eredmeny
- A koveto workflow-hibak kulon bug ticket ala kerultek, a javitasok e feladatban kovethetok.
