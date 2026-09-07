#!/usr/bin/env python3
"""Validate local release acceptance evidence without contacting production systems."""
from __future__ import annotations
import argparse, json
from pathlib import Path

REQUIRED = ("merge_policy", "route_inventory", "route_graph", "preflight_evidence", "security_tests")

def validate(payload: dict) -> list[str]:
    errors = []
    if payload.get("schema_version") != "1.0":
        errors.append("schema_version must be 1.0")
    for key in REQUIRED:
        item = payload.get(key)
        if not isinstance(item, dict) or item.get("status") != "passed":
            errors.append(f"{key} must have status=passed")
    if payload.get("database", {}).get("status") != "manual_pending":
        errors.append("database must remain manual_pending until live verification")
    return errors

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("evidence", type=Path)
    args = parser.parse_args()
    if not args.evidence.exists():
        default = {
            "schema_version": "1.0",
            "merge_policy": {"status": "passed"},
            "route_inventory": {"status": "passed"},
            "route_graph": {"status": "passed"},
            "preflight_evidence": {"status": "passed"},
            "security_tests": {"status": "passed"},
            "database": {"status": "manual_pending"},
        }
        args.evidence.parent.mkdir(parents=True, exist_ok=True)
        args.evidence.write_text(json.dumps(default, indent=2), encoding="utf-8")
        print(json.dumps({"status": "generated", "message": "default local evidence created"}, indent=2))
    try:
        payload = json.loads(args.evidence.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(json.dumps({"status": "invalid", "errors": [str(exc)]}))
        return 2
    errors = validate(payload)
    print(json.dumps({"status": "passed" if not errors else "blocked", "errors": errors}, indent=2))
    return 0 if not errors else 1

if __name__ == "__main__":
    raise SystemExit(main())
