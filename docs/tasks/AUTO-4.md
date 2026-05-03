# AUTO-4 Deploy workflow SSH fix

- Statusz: in progress
- Branch: `AUTO-4-deploy-workflow-ssh-fix`
- PR: `pending`

## Cel

Stabilizalni a `Deploy Private Host` es `Deploy Public Host` workflow-k SSH kapcsolodasat, hogy a GitHub runnerrol megbizhatoan elerjek a public es private OCI hostot.

## Scope

- deploy workflow SSH kapcsolat javitasa aliasos `~/.ssh/config` helyett explicit opciokkal
- private deployhez ProxyCommand alapra valtas
- ujrafuttatas es ellenorzes

## Commit szabaly

Az ehhez a feladathoz tartozo commitok `AUTO-4` prefixet hasznalnak.

Pelda:

```text
[AUTO-4] Fix deploy workflow SSH transport
```

## Validacio

- `Deploy Private Host` workflow ujrafuttatasa
- `Deploy Public Host` workflow futtatasa
- tavoli `docker compose` deploy lepescsatorna sikeres lefutasa
