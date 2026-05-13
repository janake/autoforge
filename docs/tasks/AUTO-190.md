# [AUTO-190] Minimális audit log implementálása

## Feladat leírása

Audit eseményeket kell rögzíteni a job lifecycle fontos pontjain, úgy, hogy a details mező JSON-ként tárolható legyen.

## Statusz

completed

## Verzió

0.1.31

## Branch

auto-190-audit-log-lombok

## PR

- (Nyitás alatt)

## Cél

A job események később visszakereshetőek legyenek, és az audit réteg minimális boilerplate-tel, Lombokkal legyen felépítve.

## Scope

- `AuditEventType` enum létrehozása
- `AuditLog` entity és repository
- `AuditService.logEvent(...)` és lifecycle helper metódusok
- Lombok bekötése a backend modulba
- Clean code skill dokumentálása az AI tooling listában

## Elfogadási kritériumok

- AuditLog entity mezők: `eventId`, `jobId`, `userSubject`, `timestamp`, `eventType`, `detailsJson`.
- `AuditService.logEvent` JSON stringként menti a details objektumot.
- Serialization hiba esetén a job feldolgozás nem dől el.
- A job létrehozás audit eseményt hoz létre.
- A status change audit helper elérhető a későbbi lifecycle lépésekhez.

## Subtaskok

- `AUTO-191`: AuditEventType enum létrehozása
- `AUTO-192`: AuditLog entity és repository
- `AUTO-193`: AuditService implementálása

## Lombok használat

- `JobController` és `JobService` `@RequiredArgsConstructor`-t használ.
- `AuditLog` `@Getter` és `@NoArgsConstructor` annotációkat használ a JPA boilerplate csökkentésére.
- `AuditService` `@RequiredArgsConstructor` és `@Slf4j` annotációkat használ.
- Lombok csak ott került be, ahol olvashatóbbá teszi a kódot, üzleti logikát nem rejt el.

## Lepesnaplo

- [x] Áttekintettem az `AUTO-190` Jira scope-ot és a kapcsolódó subtaskokat.
- [x] Felvettem a `clean-code` skillt az AI tooling manifestbe.
- [x] Bevezettem a Lombokot az új audit rétegben.
- [x] Elkészítettem az audit entity/repository/service réteget és a hozzájuk tartozó teszteket.

## Eredmény

A job audit log réteg elkészült, és a backend modulban az audit események visszakereshetően tárolódnak.
