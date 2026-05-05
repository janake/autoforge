# BUG-8 Improve error message for 404 API route failures

Feladat leírása
A bejelentkezés után, amikor az alkalmazás a /api/v1/me endpointot hívja és az 404-et ad vissza, az UI félrevezető "Auth init failed" / "Unable to initialize Keycloak" hibaüzenetet mutat. A valódi probléma az, hogy a backend API route nem érhető el (deploy/routing hiba), nem a Keycloak inicializáció.

Statusz
- in progress

Verzió
- `0.1.3`

Branch
- `bug/BUG-8-api-route-error-message`

PR
- 

Acceptance criteria
- 404 esetén az UI "API route failed" és "Backend API route is not available." üzenetet mutat
- Egyéb auth hibáknál megmarad az eredeti "Auth init failed" üzenet

Dokumentumok és fájlok
- `apps/web/src/App.tsx`
- `package.json`

Lepesnaplo
1. Megtaláltam, hogy a hibaüzenet félrevezető
2. Javítottam az App.tsx fájlban a hiba megjelenítést

Eredmény
- Javítva az error üzenet megjelenítése