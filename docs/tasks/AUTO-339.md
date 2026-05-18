# [AUTO-339] AI kérdéssor generálás tananyagból és mentés DB-be

## Feladat leírása

Az AI kérdéssor generálásnak strukturáltan kell eltárolnia a kérdéseket, válaszopciókat, helyes választ, magyarázatot és a forrás chunk referencia adatokat.

## Statusz

under test

## Verzió

0.1.51

## Branch

feature/AUTO-339-ai-question-generation

## PR

- PR #153

## Cél

A Learning `/questions` flow ne csak szöveget adjon vissza, hanem visszakereshető, DB-ben tárolt, strukturált kérdéssort is.

## Scope

- question set payload és persistence
- answer option, correct answer és explanation model
- source chunk references a kérdésekhez
- fallback és error state a generálásnál

## Elfogadási kritériumok

- A kérdéssor strukturált adatként is el van mentve.
- A kérdéshez opciók és helyes válasz tartozik.
- A forrás chunk hivatkozások megmaradnak.
- A response tartalmazza a generálás státuszát és hiba mezőit.

## Lepesnaplo

- [x] A Jira issue-t `In Progress`-ra állítottam.
- [x] A release manifestben rögzítettem a `0.1.51` célt.
- [x] A PR megnyílt review/verification alatt.

## Eredmény

A story a backend worktree-ben megkezdve, a meglévő learning content generation flow bővítésével.
