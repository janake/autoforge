# AUTO-5 Private deploy jump fix

- Statusz: in progress
- Branch: `AUTO-5-deploy-private-jump-fix`
- PR: `pending`

## Cel

Megjavitani a private deploy workflow SSH ugrast ugy, hogy a GitHub runnerrol biztosan menjen a public jump hoston at a private hostra.

## Scope

- `Deploy Private Host` workflow javitasa `ProxyCommand` helyett `-J` (ProxyJump) hasznalatra
- workflow ujrafuttatasa
- private es public stack deploy vegigfuttatasa

## Commit szabaly

Az ehhez a feladathoz tartozo commitok `AUTO-5` prefixet hasznalnak.

Pelda:

```text
[AUTO-5] Fix private deploy jump transport
```

## Validacio

- `Deploy Private Host` workflow successful
- `Deploy Public Host` workflow successful
- backend kontener fut a private hoston
- web + gateway kontenerek futnak a public hoston
