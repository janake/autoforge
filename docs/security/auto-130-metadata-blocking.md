# AUTO-130 Metadata Blocking Runtime Change

## Goal

Block access to the metadata endpoint from the runtime that will run AI-driven or sandboxed workloads.

## Runtime change

- Add an iptables reject rule for `169.254.169.254` on the private host.
- Keep the rule explicit and easy to verify.
- Do not depend on manual checks after deploy.

## Suggested placement

- Apply the rule in the private host deploy path so it is recreated on every deploy.
- Keep it before the application containers start.

## Verification

- From the runtime namespace or container, `curl http://169.254.169.254` must fail.
- The host should show the rule in `iptables -S` or an equivalent persistent rule set.

## Weak AI rule

Use the simplest host-level iptables rule that makes the endpoint unreachable, then verify it with a curl test.
