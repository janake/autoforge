# AUTO-96 Metadata Endpoint Blocking

## Goal

Block access to the cloud metadata endpoint from the AI runtime.

## Blocked target

- `169.254.169.254`

## Preferred implementation order

1. Block at the application or runtime network layer first.
2. Add a container or host network rule if runtime blocking is not enough.
3. Add an iptables fallback on the host if the runtime layer cannot guarantee isolation.

## Practical options

- `iptables` reject rule for `169.254.169.254`
- Docker network isolation with no route to the metadata IP
- Container runtime network policy that denies link-local access

## Verification

- `curl http://169.254.169.254` must fail from inside the sandbox.
- The failure must be deterministic and not depend on manual intervention.

## Weak AI rule

Use the simplest blocking method available in the current runtime, then verify the endpoint is unreachable.
