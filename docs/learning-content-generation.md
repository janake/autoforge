# Learning Content Generation

`AUTO-332` backend slice for generating practice questions and summaries from uploaded learning materials.

## Endpoints

- `POST /api/v1/learning/materials/{materialId}/questions`
- `POST /api/v1/learning/materials/{materialId}/summary`

## Behavior

- The material owner can generate content from the uploaded material.
- Generated output is stored with the owner subject, source chunk references, and a fallback flag.
- Question generation also stores a structured question-set payload with options, correct answer, explanation, and source chunk references.
- The current implementation uses a deterministic fallback because no real AI learning provider is configured yet.

## Response shape

- generation type
- generated content
- source chunk references
- fallback status and fallback reason
- generation status and error message
- structured question-set payload for `/questions`
