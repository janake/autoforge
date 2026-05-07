# [AUTO-30] Add prompt engineering AI tooling

## Feladat leírása

Prompt engineering és prompt safety capability hozzáadása az AI Tooling stackhez, hogy az ügynökök egységes prompt mintákkal, guardrail szemlélettel és prompt injection elleni alapkontrollokkal dolgozzanak.

## Statusz
in_progress

## Verzió
0.1.21

## Branch
feature/AUTO-30-prompt-engineering-tooling

## PR
- (Nincs még)

## Acceptance criteria
- `prompt-library` MCP bekerül az `ops/ai/mcps.yaml` manifestbe.
- `prompt-engineering` skill bekerül az `ops/ai/skills.yaml` manifestbe.
- A skill kapcsolódik a megfelelő capability grouphoz.
- `docs/ai-tooling.md` dokumentálja a használatát.
- A dokumentáció nem tartalmaz secretet vagy tokent.

## Dokumentumok és fájlok
- `ops/ai/mcps.yaml`
- `ops/ai/skills.yaml`
- `ops/ai/groups.yaml`
- `docs/ai-tooling.md`
- `docs/releases.md`

## Lepesnaplo
- [x] Prompt engineering tooling beazonosítva hiányzó capabilityként.
- [x] `prompt-library` MCP hozzáadva.
- [x] `prompt-engineering` skill hozzáadva.
- [x] Dokumentáció frissítve.

## Eredmény
A prompt engineering és prompt safety capability beállítva az AI tooling stackben.
