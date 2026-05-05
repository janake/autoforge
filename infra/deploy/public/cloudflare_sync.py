#!/usr/bin/env python3
"""Synchronize Autoforge Cloudflare DNS records for the public stack."""

from __future__ import annotations

import ipaddress
import json
import os
import socket
import sys
from dataclasses import dataclass
from urllib import error, parse, request


def env(name: str, default: str | None = None) -> str | None:
    value = os.environ.get(name, default)
    if value is None:
        return None
    value = value.strip()
    return value or None


def require(name: str) -> str:
    value = env(name)
    if not value:
        raise SystemExit(f"{name} is required")
    return value


def http_json(method: str, url: str, token: str, payload: dict | None = None) -> dict:
    data = None
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
    }
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")

    req = request.Request(url, data=data, headers=headers, method=method)
    try:
        with request.urlopen(req, timeout=30) as resp:
            body = resp.read().decode("utf-8")
    except error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise SystemExit(f"Cloudflare API request failed ({exc.code}): {body}") from exc
    except error.URLError as exc:
        raise SystemExit(f"Cloudflare API request failed: {exc}") from exc

    try:
        parsed = json.loads(body)
    except json.JSONDecodeError as exc:
        raise SystemExit(f"Cloudflare API returned non-JSON response: {body}") from exc

    if not parsed.get("success", False):
        errors = parsed.get("errors", [])
        raise SystemExit(f"Cloudflare API error: {errors or parsed}")

    return parsed


def resolve_ipv4(host: str) -> str:
    try:
        ipaddress.ip_address(host)
    except ValueError:
        infos = socket.getaddrinfo(host, None, family=socket.AF_INET, type=socket.SOCK_STREAM)
        if not infos:
            raise SystemExit(f"Could not resolve an IPv4 address for {host}")
        return infos[0][4][0]
    return host


@dataclass(frozen=True)
class RecordSpec:
    name: str
    content: str
    proxied: bool = True


def upsert_record(zone_id: str, token: str, record: RecordSpec) -> None:
    base = "https://api.cloudflare.com/client/v4"
    query = parse.urlencode({"type": "A", "name": record.name})
    list_url = f"{base}/zones/{zone_id}/dns_records?{query}"
    existing = http_json("GET", list_url, token)
    results = existing.get("result", [])
    payload = {
        "type": "A",
        "name": record.name,
        "content": record.content,
        "ttl": 1,
        "proxied": record.proxied,
        "comment": "Managed by Autoforge deploy",
    }
    if results:
        dns_id = results[0]["id"]
        http_json("PUT", f"{base}/zones/{zone_id}/dns_records/{dns_id}", token, payload)
        print(f"updated {record.name} -> {record.content}")
    else:
        http_json("POST", f"{base}/zones/{zone_id}/dns_records", token, payload)
        print(f"created {record.name} -> {record.content}")


def maybe_set_ssl_mode(zone_id: str, token: str, ssl_mode: str | None) -> None:
    if not ssl_mode:
        return
    ssl_mode = ssl_mode.lower()
    if ssl_mode not in {"off", "flexible", "full", "strict"}:
        raise SystemExit("CLOUDFLARE_SSL_MODE must be one of: off, flexible, full, strict")
    payload = {"value": ssl_mode}
    http_json("PATCH", f"https://api.cloudflare.com/client/v4/zones/{zone_id}/settings/ssl", token, payload)
    print(f"ssl mode set to {ssl_mode}")


def main() -> int:
    token = env("CLOUDFLARE_API_TOKEN")
    zone_id = env("CLOUDFLARE_ZONE_ID")
    if not token or not zone_id:
        print("Cloudflare sync skipped: missing CLOUDFLARE_API_TOKEN or CLOUDFLARE_ZONE_ID")
        return 0

    origin_host = require("CLOUDFLARE_ORIGIN_HOST")
    proxied = env("CLOUDFLARE_PROXIED", "true").lower() != "false"
    records = env("CLOUDFLARE_RECORDS", "")
    if not records:
        raise SystemExit("CLOUDFLARE_RECORDS is required")

    origin_ip = resolve_ipv4(origin_host)
    for name in [item.strip() for item in records.split(",") if item.strip()]:
        upsert_record(zone_id, token, RecordSpec(name=name, content=origin_ip, proxied=proxied))

    maybe_set_ssl_mode(zone_id, token, env("CLOUDFLARE_SSL_MODE"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
