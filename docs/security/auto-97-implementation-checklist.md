# AUTO-97 Implementation Checklist

1. Confirm the command name is in the allowlist.
2. Reject commands that are destructive, secret-reading, or long-running.
3. Return a single-line allow/deny result.
4. Keep the policy easy to extend with new allow entries.
5. Validate the policy against the repository's common build and verification commands.
