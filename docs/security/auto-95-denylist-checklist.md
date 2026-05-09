# AUTO-95 Implementation Checklist

1. Reject secret-bearing files by filename pattern.
2. Reject deployment and key material directories.
3. Reject policy and runtime control directories that the AI must not mutate.
4. Allow normal source files and build output only when the task requires them.
5. Return a short reason for every denied path.
