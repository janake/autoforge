# AUTO-2 Spring backend base

- Statusz: in review
- Branch: `AUTO-2-spring-backend`
- PR: `https://github.com/janake/autoforge/pull/5`

## Cel

Spring Boot backend base letrehozasa a `services/backend` alatt ugy, hogy legyen egy tesztelheto es buildelheto kezdo alkalmazas.

## Scope

- Spring Boot + Maven backend scaffold
- alap status endpoint
- actuator health endpoint
- backend GitHub Actions build workflow
- backend Dockerfile
- dokumentacio frissites

## Commit szabaly

Az ehhez a feladathoz tartozo commitok `AUTO-2` prefixet hasznalnak.

Pelda:

```text
[AUTO-2] Add Spring backend scaffold
```

## Validacio

- `npm install`
- `mvn -q -f services/backend/pom.xml test`
- `mvn -q -DskipTests package -f services/backend/pom.xml`
- GitHub Actions `Backend Build` workflow sikeresen lefutott a PR-on
- GitHub Actions `Frontend Build` workflow sikeresen lefutott a stacked PR-on

## Megjegyzes

Az eredeti backend PR a frontend branchre ment, nem a `main` branchre.
Az eredeti stacked PR: `https://github.com/janake/autoforge/pull/4`
Az aktualis `main` elleni PR: `https://github.com/janake/autoforge/pull/5`
