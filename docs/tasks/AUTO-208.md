# [AUTO-208] Git repository clone és branch létrehozás

## Feladat leírása

A Git broker klónozza a repositoryt egy ideiglenes workspace-be, checkoutolja a base branchet, majd létrehozza a feature branchet.

## Statusz

in progress

## Verzió

0.1.34

## Branch

feature/auto-208-git-repository-clone-branch

## PR

- pending

## Cél

A későbbi patch alkalmazás és commit lépés már egy előkészített helyi repositoryra tudjon dolgozni.

## Scope

- temp workspace service
- `git clone` végrehajtás
- base branch checkout
- feature branch létrehozás
- workspace cleanup

## Elfogadási kritériumok

- A repository temp könyvtárba klónozódik.
- A temp könyvtár minden futás után törlődik.
- A baseBranch checkout megtörténik.
- A branchName alapján új branch jön létre.
- Clone vagy checkout hiba strukturált exceptiont eredményez.

## Lepesnaplo

- [x] Áttekintettem a backlog JSON és order manifest scope-ját.
- [x] Bevezettem a temp workspace, clone és branch creation szolgáltatásokat.
- [x] Hozzáadtam a git CLI alapú integrációs teszteket.

## Eredmény

A Git broker előkészítő lépése elkészült, és a későbbi patch/commit flow számára biztosít workspace-et.
