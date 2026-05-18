# Learning Material Detail

`AUTO-340` adds a dedicated learning material detail route.

## Route

- `/learning/materials/{materialId}`

## Behavior

- The list view links into the detail route for a single material.
- The student view shows the learning content, ingestion status, and generated content history.
- The owner view shows assignments, ingestion metadata, and generated content history.
- Unauthorized access is surfaced as an error state instead of leaking the material.

## Notes

- The detail page reuses the existing learning API endpoints for material, ingestion, and generation history.
- The UI remains responsive on desktop and mobile.
