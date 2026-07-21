# check_logs.py
# বাংলা মন্তব্য: এই স্ক্রিপ্টটি Render-এর নির্দিষ্ট কোনো সার্ভিসের সাম্প্রতিক ডিপ্লয়মেন্ট স্ট্যাটাস চেক করতে সাহায্য করে।
# এখানে সার্ভিস আইডি এবং API কী এনভায়রনমেন্ট ভ্যারিয়েবল বা CLI আর্গুমেন্ট হিসেবে কনফিগারযোগ্য। সাইলেন্ট ফেইলর এড়াতে timeout এবং proper exception handling ব্যবহার করা হয়েছে।

import logging
import os
import sys

import requests

logging.basicConfig(
    level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s"
)
logger = logging.getLogger("check_logs")

API_KEY = os.getenv("RENDER_API_KEY")
SERVICE_ID = os.getenv("RENDER_SERVICE_ID") or (
    sys.argv[1] if len(sys.argv) > 1 else "srv-d995glt7vvec73f3jgo0"
)
REQUEST_TIMEOUT = int(os.getenv("HTTP_TIMEOUT_SECONDS", "15"))


def fetch_recent_deploys(service_id: str, api_key: str, limit: int = 5) -> list[dict]:
    # বাংলা মন্তব্য: API key এবং Service ID ভ্যালিডেট করা হচ্ছে।
    if not api_key:
        raise ValueError("RENDER_API_KEY সেট করা নেই — .env বা env var চেক করুন।")
    if not service_id:
        raise ValueError(
            "RENDER_SERVICE_ID সেট করা নেই — env var বা প্রথম CLI আর্গুমেন্ট হিসেবে দিন।"
        )

    headers = {"Authorization": f"Bearer {api_key}", "Accept": "application/json"}
    url = f"https://api.render.com/v1/services/{service_id}/deploys?limit={limit}"

    # বাংলা মন্তব্য: timeout সহ রিকোয়েস্ট পাঠানো হচ্ছে এবং raise_for_status() দিয়ে নন-২xx রেসপন্সে এরর রেইজ করা হচ্ছে।
    response = requests.get(url, headers=headers, timeout=REQUEST_TIMEOUT)
    response.raise_for_status()
    return response.json()


if __name__ == "__main__":
    try:
        logger.info("Fetching deploys for service %s...", SERVICE_ID)
        deploys = fetch_recent_deploys(SERVICE_ID, API_KEY)
        logger.info("Fetched %d deploys.", len(deploys))
        for d in deploys:
            dep = d.get("deploy", {})
            logger.info(
                "Deploy ID: %s | Status: %s | Created: %s",
                dep.get("id"),
                dep.get("status"),
                dep.get("createdAt"),
            )
    except (ValueError, requests.RequestException) as e:
        logger.error("Deploy fetch failed: %s", e)
        sys.exit(1)
