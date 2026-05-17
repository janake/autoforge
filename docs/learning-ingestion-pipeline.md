# Learning Ingestion Pipeline

`AUTO-331` backend slice for material ingestion.

## What it does

- Starts an ingestion job for an uploaded learning material.
- Splits content into deterministic chunks.
- Stores a placeholder embedding record through a provider boundary.
- Exposes job status and retry endpoints.

## Endpoints

- `GET /api/v1/learning/materials/{materialId}/ingestion`
- `POST /api/v1/learning/materials/{materialId}/ingestion`
- `POST /api/v1/learning/materials/{materialId}/ingestion/retry`

## Statuses

- `QUEUED`
- `PROCESSING`
- `COMPLETED`
- `FAILED`

## Notes

- The provider boundary is an internal service interface so a real embedding proxy can replace the default deterministic implementation later.
- Logging stays metadata-only; chunk content is persisted, not printed.
