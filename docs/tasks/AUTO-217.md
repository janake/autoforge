# [AUTO-217] Branch push és GitHub PR nyitás

## Feladat leírása

A Git broker pusholja a feature branchet a remote repositoryba, majd GitHub PR-t nyit a base branch ellen.

## Statusz

review

## Verzió

0.1.37

## Branch

feature/auto-217-branch-push-pr

## PR

- PR #86

## Cél

A patchből és commitból ténylegesen megnyitható, linkelhető PR készüljön.

## Scope

- GitHub token konfiguráció
- `git push` implementálása
- GitHub pull request API hívás
- PR URL visszaadása

## Elfogadási kritériumok

- A branch push megtörténik a remote repositoryba.
- A GitHub PR létrejön a baseBranch ellen.
- A válasz tartalmazza a PR URL-t.
- GitHub token nem kerül logba.
- API hiba esetén strukturált hibaüzenet jön létre.

## Lepesnaplo

- [x] Áttekintettem az AUTO-217 backlog scope-ját.
- [x] Létrehoztam a branch push és PR publish szolgáltatást.
- [x] Hozzáadtam a GitHub client és a publish service unit tesztjét.
- [x] A PR megnyitva review-ra.

## Eredmény

A Git broker a commitból remote branchet és GitHub PR-t tud létrehozni.
