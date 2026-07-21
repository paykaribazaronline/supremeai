# check_render.py
# বাংলা মন্তব্য: এই স্ক্রিপ্টটি Render-এর মাল্টিপল অ্যাকাউন্টের সার্ভিস ও ডিপ্লয়মেন্ট স্ট্যাটাস চেক করে।
# পারফরম্যান্সের উন্নয়নের জন্য ThreadPoolExecutor ব্যবহার করে N+1 সিকোয়েন্সিয়াল কুয়েরির সমস্যা সমাধান করা হয়েছে এবং কনকারেন্টলি স্ট্যাটাস আনা হচ্ছে।

import logging
import os
from concurrent.futures import ThreadPoolExecutor, as_completed

import requests

logging.basicConfig(
    level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s"
)
logger = logging.getLogger("check_render")

REQUEST_TIMEOUT = int(os.getenv("HTTP_TIMEOUT_SECONDS", "15"))
MAX_WORKERS = int(os.getenv("RENDER_CHECK_CONCURRENCY", "5"))

ACCOUNTS = {
    "Primary": os.getenv("RENDER_API_KEY"),
    "Backup": os.getenv("RENDER_API_KEY_BACKUP"),
}


def get_latest_deploy_status(service_id: str, headers: dict) -> str:
    # বাংলা মন্তব্য: প্রতিটি নির্দিষ্ট সার্ভিসের ডিপ্লয়মেন্ট স্ট্যাটাস আনতে timeout ব্যবহার করা হচ্ছে।
    resp = requests.get(
        f"https://api.render.com/v1/services/{service_id}/deploys?limit=1",
        headers=headers,
        timeout=REQUEST_TIMEOUT,
    )
    resp.raise_for_status()
    deploys = resp.json()
    return (
        deploys[0].get("deploy", {}).get("status", "Unknown")
        if deploys
        else "No deploys"
    )


def check_account(name: str, api_key: str) -> None:
    if not api_key:
        logger.warning("%s: API key সেট করা নেই, স্কিপ করা হচ্ছে।", name)
        return

    headers = {"Authorization": f"Bearer {api_key}", "Accept": "application/json"}
    try:
        resp = requests.get(
            "https://api.render.com/v1/services?limit=10",
            headers=headers,
            timeout=REQUEST_TIMEOUT,
        )
        resp.raise_for_status()
    except requests.RequestException as e:
        logger.error("%s: সার্ভিস তালিকা আনতে ব্যর্থ — %s", name, e)
        return

    services = resp.json()
    if not services:
        logger.info("%s: কোনো সার্ভিস পাওয়া যায়নি।", name)
        return

    # বাংলা মন্তব্য: N+1 সিকোয়েন্সিয়াল রিকোয়েস্ট দূর করতে ThreadPoolExecutor দিয়ে সমান্তরালভাবে (concurrently) ডিপ্লয়মেন্ট স্ট্যাটাস আনা হচ্ছে।
    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as pool:
        futures = {}
        for srv in services:
            service = srv.get("service", {})
            srv_id = service.get("id")
            if srv_id:
                futures[pool.submit(get_latest_deploy_status, srv_id, headers)] = (
                    service
                )

        for future in as_completed(futures):
            service = futures[future]
            status_str = (
                "Suspended" if service.get("suspended") == "suspended" else "Active"
            )
            url = service.get("serviceDetails", {}).get("url", "No URL")
            try:
                deploy_status = future.result()
            except requests.RequestException as e:
                deploy_status = f"Error: {e}"
            logger.info(
                "[%s] %s | State: %s | URL: %s | Last Deploy: %s",
                name,
                service.get("name", "Unknown"),
                status_str,
                url,
                deploy_status,
            )


if __name__ == "__main__":
    for account_name, key in ACCOUNTS.items():
        check_account(account_name, key)
