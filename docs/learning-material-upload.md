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
- original filename, content type, file size, object key, object URI, content hash, and etag are persisted with the material
- the upload flow prepares an object-storage plan before the DB write so the object reference is stable

## Supported initial formats

- `application/pdf` / `.pdf`
- `text/plain` / `.txt`
- `text/markdown` / `.md`
- `application/markdown` / `.markdown`

## Limits

- maximum upload size: 10 MB
