#!/usr/bin/env python3
# scripts/verify_infisical_env.py
"""
বাংলা: Infisical Vault-এ সব প্রয়োজনীয় সিক্রেট আছে কিনা তা চেক করার স্ক্রিপ্ট।
Hybrid Migration-এর পর সব সিক্রেট Infisical-এ সেভ করা হয়েছে, তাই গিটহাবের বদলে আমরা
সরাসরি Infisical API কল করে চেক করবো। 
"""

import os
import sys
import json
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

sys.path.insert(0, os.path.dirname(__file__))
from parse_env_policy import parse_policy

def get_required_keys() -> set:
    categories = parse_policy(POLICY_PATH)
    return categories.get('infisical-vault', set())

def get_infisical_token(client_id: str, client_secret: str) -> str:
    url = "https://app.infisical.com/api/v1/auth/universal-auth/login"
    payload = json.dumps({"clientId": client_id, "clientSecret": client_secret}).encode("utf-8")
    req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = json.load(resp)
            return data.get("accessToken")
    except urllib.error.HTTPError as e:
        print(f"::error::Infisical Login HTTP {e.code}: {e.read().decode('utf-8', 'ignore')}")
        sys.exit(1)
    except Exception as e:
        print(f"::error::Infisical Login failed: {e}")
        sys.exit(1)

def fetch_infisical_secrets(project_id: str, token: str, env: str = "prod") -> set:
    url = f"https://app.infisical.com/api/v3/secrets/raw?workspaceId={project_id}&environment={env}"
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = json.load(resp)
            secrets = data.get("secrets", [])
            return {s.get("secretKey") for s in secrets if s.get("secretKey")}
    except Exception as e:
        print(f"::error::Failed to fetch secrets from Infisical API: {e}")
        sys.exit(1)

def main() -> int:
    client_id = os.environ.get("INFISICAL_CLIENT_ID")
    client_secret = os.environ.get("INFISICAL_CLIENT_SECRET")
    project_id = os.environ.get("INFISICAL_PROJECT_ID")
    env = os.environ.get("INFISICAL_ENV", "prod")

    if not client_id or not client_secret or not project_id:
        # Fallback to Service Token if Universal Auth is not available
        service_token = os.environ.get("INFISICAL_TOKEN")
        if not service_token:
            print("::error::INFISICAL_CLIENT_ID/SECRET or INFISICAL_TOKEN is missing!")
            sys.exit(1)
        print("[info] Using INFISICAL_TOKEN (Service Token) for authentication.")
        access_token = service_token
    else:
        print("[info] Using Universal Auth (Machine Identity) for authentication.")
        access_token = get_infisical_token(client_id, client_secret)
        
    required_keys = get_required_keys()
    present = fetch_infisical_secrets(project_id, access_token, env)
    
    print(f"=== Infisical Vault Health Check [{env}] ===")
    print(f"[info] Infisical-এ সর্বমোট সিক্রেট সংখ্যা: {len(present)}")
    print(f"[info] Policy-তে required সিক্রেট সংখ্যা: {len(required_keys)}")
    
    has_critical_failure = False
    
    for name in required_keys:
        if name not in present:
            print(f"::error::CRITICAL secret missing in Infisical: {name} (production boot will crash)")
            has_critical_failure = True
            
    if has_critical_failure:
        print(f"\n❌ FAIL: Infisical-এ এক বা একাধিক critical সিক্রেট মিসিং!")
        return 1
        
    print(f"\n✅ PASS: Infisical-এ সব critical সিক্রেট উপস্থিত আছে!")
    return 0

if __name__ == "__main__":
    sys.exit(main())
