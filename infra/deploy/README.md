# deploy

Itt vannak a workflow-k altal hasznalt tavoli deploy scriptek.

- `public/deploy.sh`: publikus host frissitese
- `private/deploy.sh`: privat host frissitese

A workflow a kovetkezo fajlokat masolja ki a hostokra:

- Compose fajl
- `.env` fajl az aktualis image taggel es runtime valtozokkal
- `.deploy.env` fajl a GHCR beleptetesi adatokkal
- `deploy.sh`
