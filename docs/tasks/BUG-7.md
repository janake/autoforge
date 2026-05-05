# BUG-7 Sign-in button shown to authenticated users

Feladat leírása
A bejelentkezett felhasználók számára is megjelenik a bejelentkezés gomb a főoldalon, mert az alkalmazás csak a Keycloak callback jelenlétét ellenőrzi, és ha nincs callback, akkor "public" status-t állít be anélkül, hogy ellenőrizné a meglévő session-t.

Statusz
- in progress

Verzió
- `0.1.2`

Branch
- `bug/BUG-7-signin-button-visible`

PR
- 

Acceptance criteria
- A bejelentkezett felhasználók számára NEM jelenik meg a sign-in gomb
- A bejelentkezett felhasználók a PrivateWorkspace komponenst látják
- A nem bejelentkezett felhasználók számára megjelenik a sign-in gomb

Dokumentumok és fájlok
- `apps/web/src/App.tsx`

Lepesnaplo
1. Megtaláltam a hibát - a kód csak callback esetén ellenőrzi a Keycloak session-t
2. Javítottam a logikát - most mindig inicializálja a Keycloak-ot és ellenőrzi az authenticated státuszt

Eredmény
- Javítva az App.tsx bootstrap logikája