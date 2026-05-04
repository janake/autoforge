# AUTO-5 Private deploy jump fix

- Statusz: done-with-followup
- Branch: `AUTO-5-deploy-private-jump-fix`
- PR: `PR #8` (merged)

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

- `Deploy Private Host` workflow futasa tovabbra is hibazott a runner oldali SSH/SCP opcio parse miatt
- fix merge-olve, de production deploy workaround kulon taskban dokumentalva
- backend kontener fut a private hoston
- web + gateway kontenerek futnak a public hoston

## Megjegyzes

- A workflow maradek hibajat es a manualis deploy/network javitasi lepeseit az `AUTO-6` task koveti.
