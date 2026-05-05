# BUG-9 Gateway route configuration missing from image

Feladat leírása
A publikus API gateway `/api/v1/me` és `/api/v1/status` hívásokra 404-et ad, mert a gateway route konfigurációja az `infra/gateway/application.yml` fájlban van, de a Docker build csak a `src` könyvtárat másolja be. Emiatt a konténer route-ok nélkül indul.

Statusz
- in progress

Verzió
- `0.1.4`

Branch
- `bug/BUG-9-gateway-route-config`

PR
-

Acceptance criteria
- A gateway image tartalmazza az application.yml route konfigurációt.
- A `/api/**` útvonal továbbmegy a `BACKEND_UPSTREAM` felé.
- A `/api/v1/status` és `/api/v1/me` nem gateway 404-et ad; auth nélküli `/api/v1/me` esetén auth hiba várható, nem 404.

Dokumentumok és fájlok
- `infra/gateway/src/main/resources/application.yml`
- `infra/gateway/application.yml`
- `docs/releases.md`
- `package.json`
- `package-lock.json`

Lepesnaplo
1. Ellenőriztem a publikus endpointokat: `/actuator/health` 200, `/api/v1/status` és `/api/v1/me` 404.
2. Megtaláltam, hogy a gateway `application.yml` nincs a Docker image-be csomagolva.
3. Áthelyeztem a konfigurációt `src/main/resources/application.yml` alá.
4. Javítottam a Spring property default szintaxist `GATEWAY_CONTEXT` esetén.
5. Emeltem a projektverziót `0.1.4`-re.

Eredmény
- A gateway route konfiguráció bekerül az image-be, így az `/api/**` route-ok elérhetőek lesznek deploy után.
