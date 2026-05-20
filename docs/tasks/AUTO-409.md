# [AUTO-409] Határidő és maximális próbálkozásszám kérdéssorhoz

Statusz: In Progress

Verzio: 0.1.64

Branch: `feature/AUTO-409-deadline-attempt-limit`

PR: pending

## Cél

A Learning kérdéssorokhoz állítható legyen határidő és maximális próbálkozásszám, és a rendszer ezek alapján blokkolja a késői vagy túl sokadik beadást.

## Scope

- question set settings a generált kérdéssor szintjén
- deadline és max attempts backend persistence
- submit guard deadline és attempt limit alapján
- learning detail UI a limit és a blokk okának kijelzésével
- backend és frontend regression tesztek

## Döntés

- A limit a question set generációhoz tartozik, nem a material assignment sorhoz.
- A tanár a question set settings endpointon állítja a határidőt és a max attempts értéket.

## Lepesnaplo

- `jira_update_issue(AUTO-409, fixVersions=0.1.64)` - target version beállítás
- `jira_transition_issue(AUTO-409, In Progress)` - Jira státusz állítás
- `jira_add_comment(AUTO-409, deadline/max attempts slice indítva)` - indulás rögzítve Jira kommentben
- `git worktree add -b feature/AUTO-409-deadline-attempt-limit ... origin/main)` - dedikált worktree létrehozása

## Megjegyzes

A question set limitációk és a hozzájuk tartozó UI még kidolgozás alatt áll.
