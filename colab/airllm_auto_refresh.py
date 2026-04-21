#!/usr/bin/env python3
"""Automatically refresh SupremeAI AirLLM endpoint from Colab/ngrok runtime.

Usage:
   export SUPREMEAI_BASE_URL="https://supremeai-lhlwyikwlq-uc.a.run.app"
  export SUPREMEAI_SETUP_TOKEN="<your setup token>"
  export AIRLLM_PUBLIC_URL="https://xxxxx.ngrok-free.dev"
  python colab/airllm_auto_refresh.py

Optional:
  export AIRLLM_ACTOR="colab-airllm-bot"
  export AIRLLM_SOURCE="colab-cell4"
  export AIRLLM_VERIFY_HEALTH="true"
  export AIRLLM_ROLLBACK_ON_FAILURE="true"
"""

import json
import os
import sys
import urllib.error
import urllib.request


def read_required(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        print(f"Missing required environment variable: {name}")
        sys.exit(2)
    return value


def as_bool(name: str, default: bool) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "y"}


def main() -> int:
    base_url = read_required("SUPREMEAI_BASE_URL").rstrip("/")
    setup_token = read_required("SUPREMEAI_SETUP_TOKEN")
    public_url = read_required("AIRLLM_PUBLIC_URL").rstrip("/")

    endpoint = f"{public_url}/v1/chat/completions"
    actor = os.getenv("AIRLLM_ACTOR", "colab-airllm-bot").strip() or "colab-airllm-bot"
    source = os.getenv("AIRLLM_SOURCE", "colab").strip() or "colab"

    payload = {
        "endpoint": endpoint,
        "actor": actor,
        "source": source,
        "verifyHealth": as_bool("AIRLLM_VERIFY_HEALTH", True),
        "rollbackOnFailure": as_bool("AIRLLM_ROLLBACK_ON_FAILURE", True),
    }

    req = urllib.request.Request(
        url=f"{base_url}/api/providers/airllm/auto-refresh",
        method="POST",
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "X-Setup-Token": setup_token,
        },
    )

    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            body = resp.read().decode("utf-8")
            data = json.loads(body)
            print("Auto-refresh response:")
            print(json.dumps(data, indent=2, ensure_ascii=True))
            return 0 if data.get("success") else 1
    except urllib.error.HTTPError as exc:
        text = exc.read().decode("utf-8", errors="replace")
        print(f"HTTP error: {exc.code}")
        print(text)
        return 1
    except Exception as exc:
        print(f"Request failed: {exc}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
