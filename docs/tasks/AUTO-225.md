# [AUTO-225] Jira kulcsos prompt beküldő felület

## Feladat leírása

A frontend kapjon olyan űrlapot, ahol a user a Jira issue key-t, a promptot, a target repositoryt és a base branch-et megadva létre tudja hozni a jobot.

## Statusz

review

## Verzió

0.1.40

## Branch

feature/auto-225-jira-prompt-submit

## PR

- PR #89

## Cél

A prompt-first flow legyen a webes felületen is használható: a user a Jira key-vel együtt beküldheti a feladatot a backendnek.

## Scope

- authenticated prompt submit form
- Jira issue key input
- backend job create API hívás
- success/error visszajelzés

## Elfogadási kritériumok

- A user be tud küldeni egy Jira kulcsos promptot.
- A backend job create API meghívódik.
- Siker esetén a létrejött job azonosítója megjelenik.
- Hiba esetén olvasható error jelenik meg.

## Lepesnaplo

- [x] Áttekintettem az `AUTO-225` scope-ját az order manifest alapján.
- [x] Bevezettem az authenticated prompt submit panelt.
- [x] Hozzáadtam az authed POST helper-t a frontendhez.
- [x] A PR megnyitva review-ra.

## Eredmény

A frontend alkalmas prompt-first job létrehozásra Jira issue key használatával.
