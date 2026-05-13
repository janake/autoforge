# BUG-18 Prompt-first MVP flow documentation mismatch

## Feladat leírása

A repo dokumentáció több helyen még azt sugallja, hogy a user Jira issue key-t ad meg induláskor, miközben a helyes flow az, hogy a user promptot ad meg, ebből Jira task jön létre, és a Jira által kiosztott issue key megy tovább a jobhoz, branchhez és PR-hez.

## Statusz

review

## Verzió

0.1.35

## Branch

bug/BUG-18-prompt-first-flow-doc-fix

## PR

- pending

## Cél

A dokumentáció és a backlog leírása legyen összhangban a tényleges prompt-first MVP modellel.

## Scope

- README prompt-first flow leírás
- AI tooling Jira-first irányának pontosítása
- order manifest szövegének frissítése
- backlog nyitó leírásának prompt-first modellre igazítása

## Elfogadási kritériumok

- A README egyértelműen prompt-first flow-t ír le.
- A tooling dokumentáció promptból létrejövő Jira taskról beszél.
- Az order manifest nem tartalmaz régi Jira-key-first megfogalmazást.
- A backlog nyitó leírása prompt -> Jira task -> issue key sort használ.

## Lepesnaplo

- [x] Azonosítottam a régi Jira-key-first megfogalmazásokat.
- [x] Átírtam a README, tooling és order dokumentációt prompt-first modellre.
- [x] Frissítettem a backlog nyitó leírását is.

## Eredmény

A repo dokumentáció a helyes prompt-first MVP flow-t tükrözi.
