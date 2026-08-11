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

POLICY_PATH = os.path.join(os.path.dirname(__file__), "..", "docs", "env_maintenance_policy.md")

# Add scripts directory to path to import local module
sys.path.insert(0, os.path.dirname(__file__))
from parse_env_policy import parse_policy

def get_required_keys(env_name: str) -> set:
    categories = parse_policy(POLICY_PATH)
    return categories.get(env_name, set())


def fetch_render_env(service_id: str, api_key: str) -> dict[str, str | None]:
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

    # বাংলা: Render API response হতে পারে list অথবা {envVars: [...]} wrapper
    # key → value (Render লুকানো secrets-এ value=None দেয়, কিন্তু key থাকে)
    env_data: dict[str, str | None] = {}
    items = payload if isinstance(payload, list) else payload.get("envVars", [])
    for item in items:
        ev = item.get("envVar", item)
        key = ev.get("key")
        val = ev.get("value")  # manual sync secrets-এ None আসে
        if key:
            env_data[key] = val
    return env_data


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify Render runtime env vs registry")
    parser.add_argument("--env", required=True, choices=["render-backend", "render-admin", "render-worker"])
    parser.add_argument("--service-id", required=True, help="Render service ID")
    args = parser.parse_args()

    # বাংলা: admin env-এর জন্য backup key ব্যবহার, অন্যথায় primary key
    if args.env == "render-admin":
        api_key = os.environ.get("RENDER_API_KEY_BACKUP") or os.environ.get("RENDER_API_KEY")
    else:
        api_key = os.environ.get("RENDER_API_KEY")

    if not api_key:
        print("::error::RENDER_API_KEY/BACKUP env চার্জ করা হয়নি (GitHub secret থেকে ইনজেক্ট করুন)।")
        sys.exit(1)

    # বাংলা: env_maintenance_policy.md থেকে এই env-এর জন্য required keys লোড করো
    required_keys = get_required_keys(args.env)
    
    if not required_keys:
        print(f"::error::কোনো required keys পাওয়া যায়নি {args.env} এর জন্য env_maintenance_policy.md ফাইলে।")
        sys.exit(1)

    # বাংলা: Render API থেকে service-এর actual env var keys fetch করো
    present_keys = fetch_render_env(args.service_id, api_key)
    print(f"\n[info] Render [{args.env}] service-এ পাওয়া env var সংখ্যা: {len(present_keys)}")
    print(f"[info] Registry-তে required keys: {len(required_keys)}")

    missing_critical = []
    missing_important = []

    for name in required_keys:
        if name not in present_keys:
            missing_critical.append(name)
            print(f"::error::CRITICAL key missing in Render [{args.env}]: {name} — সার্ভার boot crash হবে!")

    if missing_critical:
        print(f"\n❌ FAIL [{args.env}]: {len(missing_critical)}টি critical key missing! Render deploy crash করবে।")
        return 1

    print(f"\n✅ PASS [{args.env}]: সব required key Render-এ উপস্থিত।")

    return 0


if __name__ == "__main__":
    sys.exit(main())
