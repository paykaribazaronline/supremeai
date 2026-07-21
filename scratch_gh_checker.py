# scratch_gh_checker.py
# বাংলা মন্তব্য: এই স্ক্রিপ্টটি GitHub Actions-এর কোনো নির্দিষ্ট রান (Run ID) থেকে ফেইল হওয়া জব এবং স্টেপগুলো ট্র্যাক করে।
# ফাইল রিডিং ট্রাই-এক্সেপ্ট ব্লকে রাখা হয়েছে এবং রান আইডি ও টোকেন কনফিগারযোগ্য করা হয়েছে।

import json
import os
import sys
import urllib.request

token = os.getenv("GITHUB_TOKEN")

# বাংলা মন্তব্য: .env ফাইল থেকে টোকেন পড়ার চেষ্টা করা হচ্ছে এবং কোনো ফাইল না থাকলে ক্র্যাশ প্রতিরোধ করা হচ্ছে।
if not token:
    try:
        if os.path.exists(".env"):
            with open(".env", "r", encoding="utf-8") as f:
                for line in f:
                    if line.startswith("GITHUB_TOKEN="):
                        token = line.split("=", 1)[1].strip().strip('"').strip("'")
                        break
    except Exception as e:
        print(f"Warning: .env read failed: {e}")

# বাংলা মন্তব্য: GITHUB_TOKEN না পাওয়া গেলে GITHUB_API_TOKEN ট্রাই করা হচ্ছে।
if not token:
    token = os.getenv("GITHUB_API_TOKEN")

# CLI আর্গুমেন্ট বা এনভায়রনমেন্ট ভ্যারিয়েবল থেকে Run ID নেওয়া হচ্ছে।
run_id = os.getenv("GITHUB_RUN_ID") or (
    sys.argv[1] if len(sys.argv) > 1 else "29347144368"
)

headers = {
    "Authorization": f"Bearer {token}" if token else "",
    "Accept": "application/vnd.github.v3+json",
    "User-Agent": "SupremeAI-GitHub-Checker",
}

url = f"https://api.github.com/repos/paykaribazaronline/supremeai/actions/runs/{run_id}/jobs"
req = urllib.request.Request(url, headers=headers)

try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
        failed_jobs_found = False
        for job in data.get("jobs", []):
            if job.get("conclusion") == "failure":
                failed_jobs_found = True
                print(f"Failed Job: {job['name']}")
                for step in job.get("steps", []):
                    if step.get("conclusion") == "failure":
                        print(f"  Failed Step: {step['name']}")
        if not failed_jobs_found:
            print("No failed jobs found for this run.")
except Exception as e:
    print(f"Error fetching Github Actions run data: {e}")
    sys.exit(1)
