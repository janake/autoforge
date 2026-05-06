# [AUTO-25] Integrate Gemini and ChatGPT API keys into OpenCode runtime

## Feladat leírása
A belső szerveren futó `opencode` konténer számára a Gemini és ChatGPT API kulcsok lekérése az OCI Vault-ból és elérhetővé tétele a runtime környezetben.

## Statusz
in_progress

## Verzió
0.1.16

## Branch
feature/AUTO-25-gemini-chatgpt-integration

## PR
- (Nincs még)

## Acceptance criteria
- Gemini és ChatGPT kulcsok lekérése OCI Vault-ból a `deploy.sh` segítségével.
- `opencode` konténer konfigurálása mindkét API kulccsal.
- GitHub workflow frissítése az új titkok kezelésére.

## Dokumentumok és fájlok
- infra/deploy/private/deploy.sh
- .github/workflows/deploy-private.yml
- infra/compose/docker-compose.private.yml

## Lepesnaplo
- [x] Task file létrehozva.
- [x] deploy.sh módosítása Gemini támogatással.
- [x] GitHub workflow módosítása az OCID-k átadására.
- [x] docker-compose módosítása a Gemini kulcsok fogadására.
- [x] Dokumentáció frissítve (deployment.md).

## Eredmény
A Gemini és ChatGPT API kulcsok integrálva lettek az OCI Vault-ból történő olvasással. A rendszer készen áll az új API kulcsok használatára a `feature/AUTO-25-gemini-chatgpt-integration` branch-en.
