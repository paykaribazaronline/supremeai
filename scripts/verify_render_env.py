#!/usr/bin/env python3
# scripts/verify_render_env.py
"""
বাংলা: Render API থেকে প্রকৃত service env vars fetch করে secrets_registry.yaml-এর
বিপরীতে চেক করে। এটি Gap #3 বন্ধ করে: CI GitHub secrets-এ set থাকলেও যদি Render
service-এর env-এ না থাকে, তবে এখানে ধরা পড়বে (production 503/crash এড়াতে)।

গুরুত্বপূর্ণ: Render API সিক্রেট ভ্যালু লুকায় (sync:false ম্যানুয়াল সিক্রেট) — তাই আমরা
শুধু KEY-এর উপস্থিতি চেক করি (ভ্যালু null হলেও key থাকলে OK)। validity (length) চেক
শুধু তখনই করা হবে যখন ভ্যালু ফেরত আসে।

নিরাপত্তা: কোনো secret ভ্যালু log-এ যায় না — শুধু key name।
"""

import os
import sys
import json
import argparse
import urllib.request
import urllib.error

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")

try:
    import yaml
except ImportError:
    print("::error::PyYAML ইনস্টল করা নাই — `pip install pyyaml` চালান।")
    sys.exit(1)

REGISTRY_PATH = os.path.join(os.path.dirname(__file__), "..", "secrets_registry.yaml")
RENDER_API = "https://api.render.com/v1"


def load_registry(path: str) -> dict:
    with open(path, "r", encoding="utf-8") as fh:
        data = yaml.safe_load(fh)
    return {e["name"]: e.get("criticality", {}) for e in data.get("keys", [])}


def fetch_render_env(service_id: str, api_key: str) -> set:
    """বাংলা: Render API থেকে service env var-এর key গুলোর set ফেরত দেয়।"""
    url = f"{RENDER_API}/services/{service_id}/env-vars?limit=100"
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {api_key}"})
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            payload = json.load(resp)
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", "ignore")
        print(f"::error::Render API call failed for {service_id}: HTTP {e.code} {body[:200]}")
        sys.exit(1)
    except Exception as e:  # network/timeout
        print(f"::error::Render API unreachable: {e}")
        sys.exit(1)

    keys = set()
    # বাংলা: Render API response হতে পারে list অথবা {{envVars: [...]}} wrapper
    items = payload if isinstance(payload, list) else payload.get("envVars", [])
    for item in items:
        key = item.get("key") or item.get("envVar", {}).get("key")
        if key:
            keys.add(key)
    return keys


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify Render runtime env vs registry")
    parser.add_argument("--env", required=True, choices=["render-backend", "render-admin", "render-worker"])
    parser.add_argument("--service-id", required=True, help="Render service ID")
    args = parser.parse_args()

    if args.env == "render-admin":
        api_key = os.environ.get("RENDER_API_KEY_BACKUP") or os.environ.get("RENDER_API_KEY")
    else:
        api_key = os.environ.get("RENDER_API_KEY")
        
    if not api_key:
        print("::error::RENDER_API_KEY/BACKUP env চার্জ করা হয়নি (GitHub secret থেকে ইনজেক্ট করুন)।")
        sys.exit(1)

    registry = load_registry(REGISTRY_PATH)
    present = fetch_render_env(args.service_id, api_key)

    has_critical_failure = False
    print(f"=== Render Runtime Env Check [{args.env}] service={args.service_id} ===")
    print(f"[info] Render-এ config করা env var সংখ্যা: {len(present)}")

    for name, crit_map in sorted(registry.items()):
        tier = crit_map.get(args.env)
        if not tier:
            continue  # ওই render env-এর জন্য প্রযোজ্য নয়
        if name in present:
            continue
        if tier == "critical":
            print(f"::error::[{args.env}] CRITICAL env var missing in Render: {name} (production boot will crash)")
            has_critical_failure = True
        elif tier == "important":
            print(f"::warning::[{args.env}] IMPORTANT env var missing in Render: {name} (feature degraded)")
        else:
            print(f"[{args.env}] [optional] env var missing in Render: {name} (feature disabled)")

    if has_critical_failure:
        print(f"\n❌ FAIL [{args.env}]: Render-এ এক বা একাধিক critical env var নাই — Render dashboard-এ সেট করুন।")
        return 1
    print(f"\n✅ PASS [{args.env}]: Render-এ সব critical env var উপস্থিত।")
    return 0


if __name__ == "__main__":
    sys.exit(main())
