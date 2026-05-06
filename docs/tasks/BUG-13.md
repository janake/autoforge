# BUG-13 Gateway Spring dependency mismatch

Feladat leírása
A publikus gateway `/api/**` hívásokra 502-t ad. A public host runtime logja szerint a hiba nem upstream kapcsolat, hanem dependency inkompatibilitás: `NoSuchMethodError: java.util.Set org.springframework.http.HttpHeaders.headerSet()`. A gateway `spring-cloud.version` `2023.0.6`, de a Boot parent `3.2.2`, ami régebbi Spring Framework verziót húz be.

Statusz
- in progress

Verzió
- `0.1.8`

Branch
- `bug/BUG-13-gateway-spring-version`

PR
-

Acceptance criteria
- A gateway runtime nem dob `HttpHeaders.headerSet()` `NoSuchMethodError` hibát.
- A gateway build sikeresen lefut.
- A `/api/**` route-ok nem 502-vel esnek el dependency mismatch miatt.

Dokumentumok és fájlok
- `infra/gateway/pom.xml`
- `docs/releases.md`
- `docs/tasks/BUG-13.md`
- `package.json`
- `package-lock.json`

Lepesnaplo
1. Ellenőriztem a public host API gateway logját.
2. Megtaláltam a runtime hibát: `HttpHeaders.headerSet()` `NoSuchMethodError`.
3. Frissítettem a gateway Spring Boot parent verzióját `3.2.12`-re.
4. Emeltem a projektverziót `0.1.8`-ra.

Eredmény
- A gateway Spring Framework runtime verziója kompatibilis a használt Spring Cloud Gateway verzióval.
