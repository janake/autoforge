# AUTO-96 Implementation Checklist

1. Add one explicit block for `169.254.169.254`.
2. Prefer the runtime layer; use host fallback only if needed.
3. Verify with a curl test from inside the sandbox.
4. Keep the rule short and deterministic.
5. Record the chosen implementation layer in Jira.
