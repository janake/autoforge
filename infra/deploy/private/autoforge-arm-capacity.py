#!/usr/bin/env python3

from __future__ import annotations

import argparse
import fcntl
import json
import os
import re
import shlex
import shutil
import subprocess
import sys
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from contextlib import contextmanager
from pathlib import Path


DEFAULT_STATE_DIR = Path("/opt/autoforge/private/arm-capacity")
DEFAULT_COMMAND = ["oci-a1-capacity"]


@dataclass(frozen=True)
class CapacityResult:
    checked_at: str
    available: bool
    output: str
    return_code: int


def state_dir() -> Path:
    return Path(os.getenv("AUTOFORGE_ARM_CAPACITY_STATE_DIR", str(DEFAULT_STATE_DIR)))


def log_file() -> Path:
    return state_dir() / "checks.jsonl"


def status_file() -> Path:
    return state_dir() / "last-alert-state.txt"


def lock_file() -> Path:
    return state_dir() / ".lock"


@contextmanager
def acquisition_lock() -> None:
    ensure_state_dir()
    with lock_file().open("w", encoding="utf-8") as handle:
        fcntl.flock(handle, fcntl.LOCK_EX)
        try:
            yield
        finally:
            fcntl.flock(handle, fcntl.LOCK_UN)


def capacity_command() -> list[str]:
    raw = os.getenv("AUTOFORGE_ARM_CAPACITY_COMMAND")
    if not raw:
        resolved = shutil.which("oci-a1-capacity")
        if resolved:
            return [resolved]
        host_default = Path("/home/janake/bin/oci-a1-capacity")
        if host_default.exists():
            return [str(host_default)]
        return DEFAULT_COMMAND
    return shlex.split(raw)


def notification_topic_ocid() -> str | None:
    value = os.getenv("AUTOFORGE_ARM_CAPACITY_NOTIFICATION_TOPIC_OCID", "").strip()
    return value or None


def oci_command() -> list[str]:
    resolved = shutil.which("oci")
    if resolved:
        return [resolved]

    if not shutil.which("docker"):
        raise FileNotFoundError("oci CLI is not installed and docker is unavailable")

    image = os.getenv("AUTOFORGE_ARM_CAPACITY_OCI_CLI_IMAGE", "ghcr.io/oracle/oci-cli:latest")
    return ["docker", "run", "--rm", "--network", "host", image]


def detect_available(output: str) -> bool:
    if re.search(r"(?im)^result:\s*.*available", output):
        return True
    if "no a1 host capacity" in output.lower():
        return False
    return False


def run_capacity_check() -> CapacityResult:
    completed = subprocess.run(
        capacity_command(),
        check=False,
        capture_output=True,
        text=True,
    )
    output = (completed.stdout or "") + (completed.stderr or "")
    checked_at = datetime.now(timezone.utc).isoformat()
    return CapacityResult(
        checked_at=checked_at,
        available=detect_available(output),
        output=output.strip(),
        return_code=completed.returncode,
    )


def ensure_state_dir() -> None:
    state_dir().mkdir(parents=True, exist_ok=True)


def append_result(result: CapacityResult) -> None:
    ensure_state_dir()
    entry = {
        "checked_at": result.checked_at,
        "available": result.available,
        "return_code": result.return_code,
        "output": result.output,
    }
    with log_file().open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(entry, ensure_ascii=False) + "\n")


def read_last_alert_state() -> str:
    try:
        return status_file().read_text(encoding="utf-8").strip()
    except FileNotFoundError:
        return ""


def write_last_alert_state(value: str) -> None:
    ensure_state_dir()
    status_file().write_text(value + "\n", encoding="utf-8")


def publish_notification(title: str, body: str) -> bool:
    topic_ocid = notification_topic_ocid()
    if not topic_ocid:
        return False

    try:
        subprocess.run(
            oci_command()
            + [
                "ons",
                "message",
                "publish",
                "--topic-id",
                topic_ocid,
                "--title",
                title,
                "--body",
                body,
            ],
            check=True,
            capture_output=True,
            text=True,
        )
    except (FileNotFoundError, subprocess.CalledProcessError) as exc:
        print(f"Failed to publish OCI notification: {exc}", file=sys.stderr)
        return False
    return True


def check_mode() -> int:
    with acquisition_lock():
        result = run_capacity_check()
        append_result(result)

        print(result.output)

        if result.available:
            last_state = read_last_alert_state()
            if last_state != "available":
                subject = "Autoforge ARM capacity available"
                body = (
                    "OCI ARM capacity is now available in eu-frankfurt-1.\n\n"
                    f"Checked at: {result.checked_at}\n\n"
                    f"{result.output}\n"
                )
                if publish_notification(subject, body):
                    print("Published availability notification.")
                else:
                    print("Availability notification skipped because OCI Notifications is not configured.")
                write_last_alert_state("available")
        else:
            write_last_alert_state("unavailable")

    return 0


def load_recent_results() -> list[dict[str, object]]:
    cutoff = datetime.now(timezone.utc) - timedelta(days=1)
    results: list[dict[str, object]] = []
    try:
        with log_file().open("r", encoding="utf-8") as handle:
            for line in handle:
                line = line.strip()
                if not line:
                    continue
                try:
                    entry = json.loads(line)
                except json.JSONDecodeError:
                    continue
                checked_at = entry.get("checked_at")
                if not isinstance(checked_at, str):
                    continue
                try:
                    timestamp = datetime.fromisoformat(checked_at.replace("Z", "+00:00"))
                except ValueError:
                    continue
                if timestamp >= cutoff:
                    results.append(entry)
    except FileNotFoundError:
        return []
    return results


def summary_mode() -> int:
    with acquisition_lock():
        results = load_recent_results()
        available_count = sum(1 for entry in results if entry.get("available") is True)
        unavailable_count = sum(1 for entry in results if entry.get("available") is False)
        latest = results[-1] if results else None

        subject = "Autoforge ARM capacity daily summary"
        if latest is None:
            body = "No ARM capacity checks were recorded in the last 24 hours."
        else:
            latest_output = str(latest.get("output", "")).strip()
            latest_checked_at = str(latest.get("checked_at", ""))
            latest_status = "available" if latest.get("available") is True else "unavailable"
            body = (
                "ARM capacity checks in the last 24 hours:\n"
                f"- available: {available_count}\n"
                f"- unavailable: {unavailable_count}\n"
                f"- latest status: {latest_status}\n"
                f"- latest check: {latest_checked_at}\n\n"
                f"Latest output:\n{latest_output}\n"
            )

        if publish_notification(subject, body):
            print("Published daily ARM capacity summary notification.")
        else:
            print("Daily summary notification skipped because OCI Notifications is not configured.")

    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Autoforge ARM capacity notifier")
    parser.add_argument("mode", choices=("check", "summary"))
    args = parser.parse_args()

    if args.mode == "check":
        return check_mode()
    return summary_mode()


if __name__ == "__main__":
    raise SystemExit(main())
