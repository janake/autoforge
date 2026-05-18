# [AUTO-366] Feltöltés utáni metadata mentés

Statusz: In Progress

Verzió: 0.1.53

Branch: `feature/AUTO-366-upload-metadata`

PR: pending

## Cél

A sikeres object write után mentsd a DB-be a storage referenciát és hash/meta adatokat.

## Scope

- object storage upload plan előkészítése
- Learning material metadata persistálása
- upload response és entitás mezők bővítése
- backend tesztek

## Lepesnaplo

- `git fetch origin main && git pull --ff-only origin main` - local `main` frissítése
- `jira_transition_issue(AUTO-366, In Progress)` - Jira státusz állítás
- `git worktree add -b feature/AUTO-366-upload-metadata ... origin/main` - dedikált worktree létrehozása

## Megjegyzés

A PR és a validáció még folyamatban van.
