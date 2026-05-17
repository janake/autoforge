# Learning API Contract

`AUTO-338` célja, hogy a Learning API stabil maradjon web és későbbi Android kliens számára.

## Alapelv

- Minden endpoint bearer JWT-vel hitelesített.
- A contract kliensfüggetlen, cookie/session logikára nem épít.
- A válaszok JSON alapúak, verziózott namespace alatt élnek: `/api/v1/learning/**`.
- A hibaválaszok közös `ApiErrorResponse` sémát használnak.

## Jelenlegi tananyag endpointok

### `POST /api/v1/learning/materials`

- `multipart/form-data`
- `file`: kötelező
- `title`: opcionális
- `description`: opcionális
- A backend az owner subjectet a JWT subjectből veszi.
- A fájlmetaadatok: eredeti fájlnév, content type, méret, bináris payload.

### `GET /api/v1/learning/materials`

- A bejelentkezett user számára elérhető tananyagokat listázza.
- Owner, direct assignment vagy group assignment alapján szűr.

### `GET /api/v1/learning/materials/{materialId}`

- Egy tananyag részletei.
- Jogosulatlan hozzáférés esetén `403`.
- Nem létező tananyag esetén `404`.

### `PUT /api/v1/learning/materials/{materialId}/assignments`

- Csak owner módosíthatja.
- `studentSubjects`: direct assignment lista
- `groupNames`: group assignment lista

## Learning generation endpoints

Az API ugyanebben a namespace-ben már támogatja az alábbi generálási műveleteket is:

- `POST /api/v1/learning/materials/{materialId}/questions`
- `POST /api/v1/learning/materials/{materialId}/summary`

Az API ugyanebben a namespace-ben tovább bővíthető ingestion státusz, tananyag chunk lista és learner profile endpointokkal, anélkül hogy a kliens oldali auth vagy base URL modell változna.

## Error contract

- `400`: validation / unsupported upload format / size limit
- `403`: access denied
- `404`: resource not found
- `409`: konfliktus csak akkor, ha későbbi workflow ezt igényli

## Mobile considerations

- Nincs frontend-specifikus session dependency.
- A válaszok self-contained JSON objektumok.
- A size limit és upload validation külön hibát kap.
