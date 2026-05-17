# Learning Material Upload

Backend slice for `AUTO-333`.

## Endpoint

- `POST /api/v1/learning/materials`
- `multipart/form-data`
- parts:
  - `file` required
  - `title` optional
  - `description` optional

## Storage

- owner subject is taken from the JWT subject
- original filename, content type, file size, and binary payload are persisted with the material

## Supported initial formats

- `application/pdf` / `.pdf`
- `text/plain` / `.txt`
- `text/markdown` / `.md`
- `application/markdown` / `.markdown`

## Limits

- maximum upload size: 10 MB
