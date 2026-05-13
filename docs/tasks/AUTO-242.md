# [AUTO-242] MVP Jira issue key validáció és tárolás - done

## Feladat leírása

A user által megadott Jira issue key legyen formailag validálva, és a jobhoz tárolódjon, hogy a későbbi branch/PR konvenciók és job lifecycle lépések erre épülhessenek.

## Statusz

completed

## Verzió

0.1.28

## Branch

auto-242-job-intake

## PR

- https://github.com/janake/autoforge/pull/75

## Cél

A backend job létrehozó folyamata megbízható Jira kulcsot fogadjon, és azt a perzisztált job rekordban tárolja.

## Scope

- Jira key regex validáció backend oldalon
- Jira key regex validáció frontend oldalon
- `jiraIssueKey` mező a Job entityben
- Jira kulcs beépítése branch/PR névbe

## Elfogadási kritériumok

- Hibás Jira kulcs 400 választ ad.
- A backend és frontend ugyanazt a mintát használja.
- A Job entity tartalmazza a `jiraIssueKey` mezőt.
- A branch név és PR cím tartalmazza a Jira kulcsot.

## Lepesnaplo

- [x] Áttekintettem az `AUTO-242` Jira scope-ot és a kapcsolódó subtaskokat.

## Eredmény

A job intake útvonal merge-elve van, és a Jira key alapú munkafolyamatra készen áll.
