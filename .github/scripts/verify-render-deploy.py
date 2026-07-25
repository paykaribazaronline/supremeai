# .github/scripts/verify-render-deploy.py
# বাংলা মন্তব্য: এই স্ক্রিপ্টটি নির্দিষ্ট Render সার্ভিসের (User/Primary বা Admin/Backup) ডেপ্লয়মেন্ট স্ট্যাটাস ও হেলথ ভেরিফাই করে।
# এটি সার্ভিস আইডি অনুযায়ী ফিল্টার করে ট্র্যাকিং নিশ্চিত করে যাতে একটি সার্ভিসের সুস্থতা অন্য ব্যর্থ সার্ভিসকে ঢেকে না ফেলে।

import argparse
import os
import sys
import time
from datetime import datetime, timedelta, timezone

import requests

try:
    from dotenv import load_dotenv

    load_dotenv(".env")
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
    "srv-d9fg48bh523c73f63bb0": {
        "name": "Admin Backend (Backup)",
        "service_id": "srv-d9fg48bh523c73f63bb0",
        "url": "https://supremeai-admin.onrender.com",
    },
}

POLL_INTERVAL = 30  # seconds
TIMEOUT_LIMIT = 900  # 15 minutes (900 seconds)


def check_http_health(url, label):
    # বাংলা মন্তব্য: শুধুমাত্র নির্দিষ্ট সার্ভিসের URL চেক হবে।
    # ভুল সার্ভিসের হেলথ দিয়ে False-positive তৈরি করা সম্পূর্ণ নিষিদ্ধ।
    health_url = f"{url.rstrip('/')}/api/v1/health"
    print(f"⏳ Verifying {label} HTTP health at {health_url}...")
    try:
        response = requests.get(health_url, timeout=10)
        if response.status_code == 200:
            print(f"✅ {label} HTTP check passed! Status: 200 OK")
            return True
        else:
            print(
                f"❌ {label} HTTP check returned non-200 status: {response.status_code}"
            )
            return False
    except Exception as e:
        print(f"❌ {label} HTTP check failed: {e}")
        return False


def monitor_service(service):
    name = service["name"]
    service_id = service["service_id"]

    primary_key = os.getenv("RENDER_API_KEY")
    backup_key = os.getenv("RENDER_API_KEY_BACKUP")
    candidate_keys = [k for k in [primary_key, backup_key] if k]

    if not candidate_keys:
        print(
            f"ℹ️ No API keys configured in environment. Checking HTTP health directly for {name}."
        )
        return check_http_health(service["url"], name)

    # বাংলা মন্তব্য: শুধুমাত্র নির্দিষ্ট service_id-র জন্য সঠিক API key খোঁজা হবে।
    # ৪০৪ হলে অন্য service-এ remap করা হবে না — এটা False-Positive তৈরি করে।
    # বরং সরাসরি HTTP health check-এ যাবে।
    headers = None
    for key in candidate_keys:
        test_headers = {"Authorization": f"Bearer {key}", "Accept": "application/json"}
        deploys_url = f"https://api.render.com/v1/services/{service_id}/deploys"
        try:
            res = requests.get(deploys_url, headers=test_headers, timeout=10)
            if res.status_code == 200:
                headers = test_headers
                print(
                    f"✅ Authenticated API key found for {name} (service {service_id})."
                )
                break
            elif res.status_code == 404:
                print(
                    f"⚠️ Service {service_id} returned 404 for this API key. Key does not own this service."
                )
                # বাংলা মন্তব্য: ৪০৪ মানে এই API key এই service-টির মালিক নয়।
                # অন্য service-এ remap করা false-positive তৈরি করবে — তাই skip করা হচ্ছে।
            elif res.status_code in (401, 403):
                print(
                    f"⚠️ API key unauthorized (HTTP {res.status_code}) for service {service_id}."
                )
        except Exception as e:
            print(f"⚠️ API connectivity error for {name}: {e}")

    if not headers:
        print(
            f"⚠️ No valid API key found for service {service_id} ({name}). Falling back to HTTP health check."
        )
        return check_http_health(service["url"], name)

    print(f"\n🔍 Tracking latest deploy for {name} (Service ID: {service_id})...")

    deploys_url = f"https://api.render.com/v1/services/{service_id}/deploys"
    try:
        res = requests.get(deploys_url, headers=headers, timeout=15)
        if res.status_code != 200:
            print(
                f"❌ Failed to fetch deploys for {name}: HTTP {res.status_code} - {res.text}"
            )
            return check_http_health(service["url"], name)

        deploys = res.json()
        if not deploys:
            print(f"⚠️ No deploys found for {name}. Checking HTTP health directly.")
            return check_http_health(service["url"], name)

        latest_deploy_item = deploys[0]
        latest_deploy = (
            latest_deploy_item.get("deploy", latest_deploy_item)
            if isinstance(latest_deploy_item, dict)
            else latest_deploy_item
        )

        deploy_id = latest_deploy.get("id")
        status = latest_deploy.get("status")
        created_at_str = latest_deploy.get("createdAt")

        print(
            f"📋 Latest Deploy details: ID={deploy_id}, Status={status}, CreatedAt={created_at_str}"
        )

        if not created_at_str:
            print("⚠️ createdAt timestamp is missing. Checking HTTP health directly.")
            return check_http_health(service["url"], name)

        # If latest deploy is already LIVE, proceed directly to health check
        status_str = (status or "").lower()
        if status_str == "live":
            print(f"🎉 Deploy {deploy_id} for {name} is already LIVE on Render!")
            return check_http_health(service["url"], name)

        created_at = datetime.fromisoformat(created_at_str.replace("Z", "+00:00"))
        now = datetime.now(timezone.utc)

        # Allow up to 10 minutes for deploy record initiation / polling queue on Render free tier
        if now - created_at > timedelta(minutes=10):
            print(
                f"❌ No new deploy record found for {name} within 10 minutes of triggering it. "
                f"The trigger call likely failed silently — latest deploy on file is {now - created_at} old."
            )
            return False

    except Exception as e:
        print(f"❌ Error communicating with Render API: {e}")
        return False

    start_time = time.time()
    deploy_url = f"https://api.render.com/v1/services/{service_id}/deploys/{deploy_id}"

    print(f"⏳ Polling status of deploy {deploy_id} for {name}...")

    while True:
        elapsed = time.time() - start_time
        if elapsed > TIMEOUT_LIMIT:
            print(
                f"❌ Timeout reached ({TIMEOUT_LIMIT}s) while waiting for deploy {deploy_id} to complete."
            )
            return False

        try:
            res = requests.get(deploy_url, headers=headers, timeout=15)
            if res.status_code == 200:
                deploy_info = res.json()
                deploy_data = (
                    deploy_info.get("deploy", deploy_info)
                    if isinstance(deploy_info, dict)
                    else deploy_info
                )
                status = deploy_data.get("status", "").lower()
                print(
                    f"  Deploy {deploy_id} status: {status} (elapsed: {int(elapsed)}s)"
                )

                if status == "live":
                    print(f"🎉 Deploy {deploy_id} is now LIVE on Render!")
                    return check_http_health(service["url"], name)
                elif status in ["update_failed", "build_failed", "canceled"]:
                    print(f"❌ Deploy {deploy_id} failed with status: {status}")
                    return False
            else:
                print(f"⚠️ Error fetching deploy details: HTTP {res.status_code}")
        except Exception as e:
            print(f"⚠️ Polling connection issue: {e}")

        time.sleep(POLL_INTERVAL)


def main():
    parser = argparse.ArgumentParser(description="Verify Render Deploy Status")
    parser.add_argument(
        "--service-id", type=str, help="Specific Render Service ID to verify"
    )
    parser.add_argument("--name", type=str, help="Custom Service Name label")
    args = parser.parse_args()

    # বাংলা মন্তব্য: যদি সার্ভিস আইডি নির্দিষ্ট করে দেওয়া হয় তবে শুধুমাত্র সেই নির্দিষ্ট ব্যাকএন্ডেরই হেলথ চেক হবে।
    if args.service_id:
        svc = SERVICES.get(
            args.service_id,
            {
                "name": args.name or f"Service ({args.service_id})",
                "service_id": args.service_id,
                "url": (
                    "https://supremeai-backend.onrender.com"
                    if "n58js" in args.service_id
                    else "https://supremeai-admin.onrender.com"
                ),
            },
        )
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

    # বাংলা মন্তব্য (জরুরি): আংশিক ডেপ্লয় ট্র্যাকিং বন্ধ। প্রতিটি উদ্দিষ্ট সার্ভিসকে ১০০% Healthy হতে হবে, নয়তো বিল্ড ফেল করবে।
    if all_ok:
        print(
            "\n🎉 Deployment verification PASSED! All targeted backend services are healthy and responding."
        )
        sys.exit(0)
    else:
        print(
            "\n❌ Deployment verification FAILED! One or more target services failed deployment verification."
        )
        sys.exit(1)


if __name__ == "__main__":
    main()
