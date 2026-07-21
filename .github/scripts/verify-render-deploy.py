# .github/scripts/verify-render-deploy.py
# বাংলা মন্তব্য: এই স্ক্রিপ্টটি Render এপিআই ব্যবহার করে ডেপ্লয়মেন্টের অবস্থা (status) ট্র্যাক করে এবং লাইভ হওয়া পর্যন্ত অপেক্ষা করে।
# এটি ৩ মিনিটের নাভল পার্লিং এড়ানো নিশ্চিত করে এবং রেন্ডারের কিউয়িং টাইমআউট সঠিকভাবে হ্যান্ডেল করে।

import os
import sys
import time
from datetime import datetime, timedelta, timezone

import requests

# Force stdout/stderr to use UTF-8 to prevent UnicodeEncodeError on Windows terminals when printing emojis
# বাংলা মন্তব্য: উইন্ডোজ টার্মিনালে ইমোজি প্রিন্ট করার সময় UnicodeEncodeError এড়াতে stdout এবং stderr-কে UTF-8 এ কনফিগার করা হলো।
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")

# Services to monitor
SERVICES = [
    {
        "name": "Primary backend",
        "service_id": "srv-d9d3n58js32c738n79k0",
        "url": "https://supremeai-backend.onrender.com",
        "api_key_env": "RENDER_API_KEY",
    },
    {
        "name": "Backup backend",
        "service_id": "srv-d9fg48bh523c73f63bb0",
        "url": "https://supremeai-admin.onrender.com",
        "api_key_env": "RENDER_API_KEY_BACKUP",
    },
]

POLL_INTERVAL = 30  # seconds
TIMEOUT_LIMIT = 900  # 15 minutes (900 seconds)


def check_http_health(url, label):
    """
    বাংলা মন্তব্য: সার্ভিসের হেলথ চেক এন্ডপয়েন্ট সরাসরি ভেরিফাই করে।
    """
    health_url = f"{url.rstrip('/')}/api/v1/health"
    print(f"⏳ Verifying {label} HTTP health at {health_url}...")
    try:
        response = requests.get(health_url, timeout=10)
        if response.status_code == 200:
            print(f"✅ {label} HTTP check passed! Status: 200 OK")
            return True
        else:
            print(f"⚠️ {label} HTTP check returned status: {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ {label} HTTP check failed: {e}")
        return False


def monitor_service(service):
    name = service["name"]
    service_id = service["service_id"]
    api_key = os.getenv(service["api_key_env"])

    if not api_key:
        print(
            f"ℹ️ API key for {name} ({service['api_key_env']}) is not configured. Skipping deploy tracking."
        )
        # বাংলা মন্তব্য: যদি কোন এপিআই কি না থাকে, তবে আমরা সরাসরি HTTP হেলথ চেক করতে পারি
        # কারণ ডেপ্লয়মেন্ট হয়ত অন্য কোনোভাবে ট্রিগার হয়েছে বা এপিআই কি বাদে চালানো হচ্ছে।
        return check_http_health(service["url"], name)

    headers = {"Authorization": f"Bearer {api_key}", "Accept": "application/json"}

    print(f"\n🔍 Tracking latest deploy for {name} (Service ID: {service_id})...")

    # 1. Get the list of deploys
    deploys_url = f"https://api.render.com/v1/services/{service_id}/deploys"
    try:
        res = requests.get(deploys_url, headers=headers, timeout=15)
        if res.status_code != 200:
            print(
                f"❌ Failed to fetch deploys for {name}: HTTP {res.status_code} - {res.text}"
            )
            return False

        deploys = res.json()
        if not deploys:
            print(f"⚠️ No deploys found for {name}. Checking HTTP health directly.")
            return check_http_health(service["url"], name)

        # বাংলা মন্তব্য: Render API-র রেসপন্স লিস্টে প্রতিটি ডেপ্লয়মেন্ট অবজেক্ট "deploy" কী-এর অধীনে র‍্যাপ করা থাকে।
        # তাই প্রথমে সেটি ডিকশনারি থেকে এক্সট্র্যাক্ট করে নিচ্ছি।
        latest_deploy_item = deploys[0]
        if isinstance(latest_deploy_item, dict) and "deploy" in latest_deploy_item:
            latest_deploy = latest_deploy_item["deploy"]
        else:
            latest_deploy = latest_deploy_item

        deploy_id = latest_deploy.get("id")
        status = latest_deploy.get("status")
        created_at_str = latest_deploy.get("createdAt")

        print(
            f"📋 Latest Deploy details: ID={deploy_id}, Status={status}, CreatedAt={created_at_str}"
        )

        if not created_at_str:
            print(f"⚠️ createdAt timestamp is missing. Checking HTTP health directly.")
            return check_http_health(service["url"], name)

        # 2. Check if this deploy was triggered recently (within the last 15 minutes)
        # Parse ISO timestamp
        # বাংলা মন্তব্য: ISO টাইমস্ট্যাম্প পার্স করার সময় safe-replace ব্যবহার করা হচ্ছে যাতে NoneType এরর না হয়।
        created_at = datetime.fromisoformat(created_at_str.replace("Z", "+00:00"))
        now = datetime.now(timezone.utc)

        if now - created_at > timedelta(minutes=15):
            print(
                f"ℹ️ Latest deploy is older than 15 minutes (created {now - created_at} ago). No recent deploy is active."
            )
            # Check if the service is currently healthy
            return check_http_health(service["url"], name)

    except Exception as e:
        print(f"❌ Error communicating with Render API: {e}")
        return False

    # 3. Poll deploy status
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
                # বাংলা মন্তব্য: পোলিং রেসপন্স যদি "deploy" কী দ্বারা আবৃত থাকে তবে সেটি আনর‍্যাপ করার জন্য চেক করছি।
                if isinstance(deploy_info, dict) and "deploy" in deploy_info:
                    deploy_data = deploy_info["deploy"]
                else:
                    deploy_data = deploy_info
                status = deploy_data.get("status", "").lower()
                print(
                    f"  Deploy {deploy_id} status: {status} (elapsed: {int(elapsed)}s)"
                )

                if status == "live":
                    print(f"🎉 Deploy {deploy_id} is now LIVE on Render!")
                    # Perform final HTTP health verification
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
    success = False
    results = {}

    for service in SERVICES:
        results[service["name"]] = monitor_service(service)

    print("\n================ DEPLOY SUMMARY ================")
    for name, ok in results.items():
        status_text = "✅ SUCCESS / HEALTHY" if ok else "❌ FAILED / UNHEALTHY"
        print(f"- {name}: {status_text}")
        if ok:
            success = True

    if success:
        print(
            "\n🎉 Deployment verification passed! At least one backend is healthy and responding."
        )
        sys.exit(0)
    else:
        print(
            "\n❌ Deployment verification FAILED! No backend instances responded successfully."
        )
        sys.exit(1)


if __name__ == "__main__":
    main()
