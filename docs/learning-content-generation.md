# Learning Content Generation

`AUTO-332` and follow-up slices generate practice questions, summaries, and teacher-ready lesson content from uploaded learning materials.

## Endpoints

- `POST /api/v1/learning/materials/{materialId}/questions`
- `POST /api/v1/learning/materials/{materialId}/summary`
- `POST /api/v1/learning/materials/{materialId}/lesson`

## Behavior

- The material owner can generate content from the uploaded material.
- Generated output is stored with the owner subject, source chunk references, and a fallback flag.
- Question generation also stores a structured question-set payload with options, correct answer, explanation, and source chunk references.
- Lesson generation stores a reviewable lesson outline/content block in the same generated-content table with source references.
- The current implementation uses a deterministic fallback because no real AI learning provider is configured yet.

## Response shape

- generation type
- generated content
- source chunk references
- fallback status and fallback reason
- generation status and error message
- structured question-set payload for `/questions`
- null structured content for `/summary` and `/lesson`
