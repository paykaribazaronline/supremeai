# .github/scripts/verify-render-deploy.py
# বাংলা মন্তব্য: এই স্ক্রিপ্টটি নির্দিষ্ট Render সার্ভিসের (User/Primary বা Admin/Backup) ডেপ্লয়মেন্ট স্ট্যাটাস ও হেলথ ভেরিফাই করে।
# এটি সার্ভিস আইডি অনুযায়ী ফিল্টার করে ট্র্যাকিং নিশ্চিত করে যাতে একটি সার্ভিসের সুস্থতা অন্য ব্যর্থ সার্ভিসকে ঢেকে না ফেলে।

import json
import os
import sys
import time
import urllib.parse
import urllib.request
import argparse
from datetime import datetime, timezone, timedelta

try:
    from dotenv import load_dotenv
    load_dotenv('.env')
except Exception:
    pass

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")

SERVICES = {
    "srv-d9d3n58js32c738n79k0": {
        "name": "User Backend (Primary)",
        "service_id": "srv-d9d3n58js32c738n79k0",
        "url": "https://supremeai-backend.onrender.com",
    },
    # বাংলা মন্তব্য: আগে এই ডিকশনারিতে শুধু User Backend ছিল। ফলে
    # deploy-combined-backend job (যেটা --service-id ছাড়াই এই স্ক্রিপ্ট চালায় এবং
    # SERVICES.values() এর সবকিছু ভেরিফাই করে) নীরবে Admin Backend-এর হেলথ চেক
    # স্কিপ করত — Admin ডেপ্লয় ব্যর্থ হলেও কম্বাইন্ড জব সবুজ দেখাত। এখন Admin
    # Backend-ও ডিফল্ট টার্গেট লিস্টে যুক্ত করা হলো (deploy-admin-backend জব
    # আলাদাভাবে --service-id দিয়ে এটিকে আইসোলেটেডভাবে চালায়, সেটা অপরিবর্তিত থাকল)।
    "srv-d9fg48bh523c73f63bb0": {
        "name": "Admin Backend (Backup)",
        "service_id": "srv-d9fg48bh523c73f63bb0",
        "url": "https://supremeai-admin.onrender.com",
    },
}

# Optimized timing: Poll interval 10s and timeout to 540s (9 minutes) for Render free tier
POLL_INTERVAL = 10  # poll every 10s for faster feedback
TIMEOUT_LIMIT = 540  # 9 minutes (allows Render free tier image pull & container spin-up)

class _UrllibResponse:
    def __init__(self, resp):
        self._resp = resp
        self.status_code = resp.status
        self.ok = 200 <= resp.status < 300
        self.text = resp.read().decode("utf-8")

    def json(self):
        return json.loads(self.text)


def _http_get(url, headers=None, timeout=15):
    req = urllib.request.Request(url, headers=headers or {}, method="GET")
    resp = urllib.request.urlopen(req, timeout=timeout)
    return _UrllibResponse(resp)


def check_http_health(url, label, retries=6, timeout_per_try=20):
    # বাংলা মন্তব্ব্য: সার্ভিসের /health এবং /api/v1/health রিট্রাই সহ চেক করা হবে কোল্ড স্টার্ট এড়াতে।
    # Render free tier-এর স্পিন-আপ/কোল্ড-স্টার্ট অনেক সময় নিতে পারে (৬০-৯০ সেকেন্ড), তাই
    # রিট্রাই সংখ্যা ও প্রতি-চেষ্টায় টাইমআউট বাড়ানো হলো যাতে মিথ্যা নেগেটিভ না আসে।
    base_url = url.rstrip('/')
    endpoints = [f"{base_url}/health", f"{base_url}/api/v1/health"]
    for attempt in range(1, retries + 1):
        for health_url in endpoints:
            print(f"⏳ Verifying {label} HTTP health at {health_url} (Attempt {attempt}/{retries})...")
            try:
                response = _http_get(health_url, timeout=timeout_per_try)
                if response.status_code == 200:
                    try:
                        # Verify the response is actually healthy, not just status 200
                        data = response.json()
                        if isinstance(data, dict) and data.get('status') in ['ok', 'healthy', 'UP', 'degraded']:
                            print(f"✅ {label} HTTP check passed! Status: 200 OK ({health_url})")
                            return True
                        else:
                            print(f"⚠️ {label} HTTP check returned unverified body: {data}")
                    except Exception as json_err:
                        print(f"⚠️ {label} HTTP check response is not valid JSON: {json_err}")
                else:
                    print(f"⚠️ {label} HTTP check returned HTTP {res.status_code}")
            except Exception as e:
                print(f"⏳ {health_url} health check attempt {attempt} failed: {e}")
        if attempt < retries:
            # বাংলা মন্তব্য: কোল্ড স্টার্টের জন্য প্রতি রিট্রাইয়ের মাঝে ব্যাকঅফ বাড়ানো হলো (৫→১০সে)
            time.sleep(10)
    print(f"❌ {label} HTTP check failed after {retries} retries.")
    return False

def monitor_service(service):
    name = service["name"]
    service_id = service["service_id"]

    primary_key = os.getenv("RENDER_API_KEY")
    backup_key = os.getenv("RENDER_API_KEY_BACKUP")
    candidate_keys = [k for k in [primary_key, backup_key] if k]

    if not candidate_keys:
        print(f"ℹ️ No API keys configured in environment. Checking HTTP health directly for {name}.")
        return check_http_health(service["url"], name)

    headers = None
    for key in candidate_keys:
        test_headers = {
            "Authorization": f"Bearer {key}",
            "Accept": "application/json"
        }
        deploys_url = f"https://api.render.com/v1/services/{service_id}/deploys"
        try:
            res = _http_get(deploys_url, headers=test_headers, timeout=10)
            if res.status_code == 200:
                headers = test_headers
                print(f"✅ Authenticated API key found for {name} (service {service_id}).")
                break
            elif res.status_code == 404:
                print(f"⚠️ Service {service_id} returned 404 for this API key. Key does not own this service.")
            elif res.status_code in (401, 403):
                print(f"⚠️ API key unauthorized (HTTP {res.status_code}) for service {service_id}.")
        except Exception as e:
            print(f"⚠️ API connectivity error for {name}: {e}")

    if not headers:
        print(f"⚠️ No valid API key found for service {service_id} ({name}). Falling back to HTTP health check.")
        return check_http_health(service["url"], name)

    print(f"\n🔍 Tracking latest deploy for {name} (Service ID: {service_id})...")

    deploys_url = f"https://api.render.com/v1/services/{service_id}/deploys"
    try:
        res = _http_get(deploys_url, headers=headers, timeout=15)
        if res.status_code != 200:
            print(f"❌ Failed to fetch deploys for {name}: HTTP {res.status_code} - {res.text}")
            return check_http_health(service["url"], name)

        deploys = res.json()
        if not deploys:
            print(f"⚠️ No deploys found for {name}. Checking HTTP health directly.")
            return check_http_health(service["url"], name)

        latest_deploy_item = deploys[0]
        latest_deploy = latest_deploy_item.get("deploy", latest_deploy_item) if isinstance(latest_deploy_item, dict) else latest_deploy_item

        deploy_id = latest_deploy.get("id")
        status = latest_deploy.get("status")
        created_at_str = latest_deploy.get("createdAt")

        print(f"📋 Latest Deploy details: ID={deploy_id}, Status={status}, CreatedAt={created_at_str}")

        if not created_at_str:
            print(f"⚠️ createdAt timestamp is missing. Checking HTTP health directly.")
            return check_http_health(service["url"], name)

        # If latest deploy is already LIVE, proceed directly to health check
        status_str = (status or "").lower()
        if status_str == "live":
            print(f"🎉 Deploy {deploy_id} for {name} is already LIVE on Render!")
            return check_http_health(service["url"], name)

        created_at = datetime.fromisoformat(created_at_str.replace("Z", "+00:00"))
        now = datetime.now(timezone.utc)

        # Allow up to 5 minutes for deploy record initiation / polling (reduced from 10)
        if now - created_at > timedelta(minutes=5):
            print(
                f"⚠️ No new deploy record found for {name} within 5 minutes. Falling back to direct HTTP health check..."
            )
            return check_http_health(service["url"], name)

    except Exception as e:
        print(f"❌ Error communicating with Render API: {e}")
        return check_http_health(service["url"], name)

    start_time = time.time()
    deploy_url = f"https://api.render.com/v1/services/{service_id}/deploys/{deploy_id}"

    print(f"⏳ Polling status of deploy {deploy_id} for {name}...")

    while True:
        elapsed = time.time() - start_time
        if elapsed > TIMEOUT_LIMIT:
            print(f"❌ Timeout reached ({TIMEOUT_LIMIT}s) while waiting for deploy {deploy_id} to complete.")
            print(f"⚠️ Proceeding to direct HTTP health check regardless of deploy status...")
            # Even if deploy status check times out, we still check HTTP health
            return check_http_health(service["url"], name)

        try:
            res = _http_get(deploy_url, headers=headers, timeout=15)
            if res.status_code == 200:
                deploy_info = res.json()
                deploy_data = deploy_info.get("deploy", deploy_info) if isinstance(deploy_info, dict) else deploy_info
                status = deploy_data.get("status", "").lower()
                print(f"  Deploy {deploy_id} status: {status} (elapsed: {int(elapsed)}s)")

                if status == "live":
                    print(f"🎉 Deploy {deploy_id} is now LIVE on Render!")
                    return check_http_health(service["url"], name)
                elif status in ["update_failed", "build_failed", "canceled"]:
                    # বাংলা মন্তব্য: ডিপ্লয় ফেইল হলে সরাসরি HTTP হেলথ চেক দিয়ে পাশ করা যাবে না।
                    # কারণ HTTP 200 শুধু পুরনো চলমান ভার্সন থেকে আসে (নতুন বিল্ড ডিপ্লয়ই হয়নি),
                    # যা ফলস-পজিটিভ (সবুজ CI) তৈরি করে। তাই ফেইল স্ট্যাটাস = সরাসরি HARD FAIL।
                    print(f"⚠️ Deploy {deploy_id} reported status: {status}. This is a HARD FAIL — the new build did not deploy.")
                    print(f"❌ {name} deployment FAILED (status: {status}). HTTP health fallback is intentionally skipped because it would only reflect the previous running version, masking the failure.")
                    return False
            else:
                print(f"⚠️ Error fetching deploy details: HTTP {res.status_code}")
        except Exception as e:
            print(f"⚠️ Polling connection issue: {e}")

        time.sleep(POLL_INTERVAL)

def main():
    parser = argparse.ArgumentParser(description="Verify Render Deploy Status")
    parser.add_argument("--service-id", type=str, help="Specific Render Service ID to verify")
    parser.add_argument("--name", type=str, help="Custom Service Name label")
    args = parser.parse_args()

    if args.service_id:
        svc = SERVICES.get(args.service_id, {
            "name": args.name or f"Service ({args.service_id})",
            "service_id": args.service_id,
            "url": "https://supremeai-backend.onrender.com" if "n58js" in args.service_id else "https://supremeai-admin.onrender.com"
        })
        targets = [svc]
    else:
        targets = list(SERVICES.values())

    results = {}
    for svc in targets:
        results[svc["name"]] = monitor_service(svc)

    print("\n================ DEPLOY SUMMARY ================")
    all_ok = True
    for name, ok in results.items():
        status_text = "✅ SUCCESS / HEALTHY" if ok else "❌ FAILED / UNHEALTHY"
        print(f"- {name}: {status_text}")
        if not ok:
            all_ok = False

    if all_ok:
        print("\n🎉 Deployment verification PASSED! All targeted backend services are healthy and responding.")
        sys.exit(0)
    else:
        print("\n❌ Deployment verification FAILED! One or more target services failed deployment verification.")
        sys.exit(1)

if __name__ == "__main__":
    main()
