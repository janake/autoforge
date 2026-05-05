# Task tracking

Itt kovetunk minden fejlesztesi feladatot.

## Szabalyok

- Minden feladat kulon Markdown fajlt kap.
- Az azonosito formatuma: `AUTO-<szam>`.
- A fajl neve egyezzen az azonositoval, peldaul: `AUTO-1.md`.
- Bug ticketekhez `BUG-<szam>` formatumot hasznalunk, feature ticketekhez maradhat az `AUTO-<szam>`.
- Minden feladathoz tartozo commit uzenete kezdodjon az azonositoval.
- Minden feladatban legyen `Verzió` mezo.
- Minden feladat szerepeljen a `docs/releases.md` manifestben a celverzio alatt.
- Nem-MAJOR feladatnal a celverzio automatikusan a kovetkezo megfelelo `PATCH` vagy `MINOR` verzio.
- Az aktiv work branchek neve `feature/<azonosito>` vagy `bug/<azonosito>` formatumot kovessen, peldaul: `feature/AUTO-1` vagy `bug/BUG-2`.
- Minden task fajlban legyen `Lepesnaplo` szekcio, ahol a futtatott parancsok szerepelnek rovid leirassal.
- A parancsokban secret/token erteket nem irunk ki, helyette placeholdert hasznalunk.
- Gitbe nem irunk szemelyes vagy erzekeny adatot: privat kulcsot, kulcsfajl-nevet, abszolut lokalis pathot, felhasznalonevet, emailt, tokent, cloud credentialt, tenancy/user OCID-t.
- Szemelyes azonositok helyett placeholdert, GitHub secretet vagy env valtozot kell hasznalni, peldaul `${AUTOFORGE_SSH_KEY}`, `<registry-owner>`, `<repo-url>`.
- PR vagy issue hivatkozasnal teljes URL helyett belso azonositot hasznalunk, peldaul `PR #9`.
- Ha az architektura valtozik, a diagramot is ugyanabban a valtozasban frissiteni kell.
- Ha a verziozasi szabaly valtozik, frissiteni kell a `docs/versioning.md`, `docs/releases.md` es `AGENT.md` fajlokat is.

Pelda:

```text
[AUTO-1] Add base React frontend scaffold
```

## Minimum tartalom

- feladat azonosito
- cim
- statusz
- verzio
- branch
- PR
- rovid cel
- scope
- megjegyzesek
