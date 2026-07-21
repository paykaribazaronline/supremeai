import json
import os
import urllib.request

api_key = os.environ.get("RENDER_API_KEY", "")
api_key_backup = os.environ.get("RENDER_API_KEY_BACKUP", "")

# বাংলা মন্তব্য: প্রতিটি সার্ভিস আইডিকে তার নিজ নিজ অ্যাকাউন্টের API Key-র সাথে ম্যাপ করে আপডেট করা হচ্ছে
services = [
    {"sid": "srv-d9d3n58js32c738n79k0", "key": api_key},
    {"sid": "srv-d9e4q5rrjlhs73bnh71g", "key": api_key_backup},
]

data = {
    "serviceDetails": {
        "envSpecificDetails": {
            "dockerCommand": "uvicorn main:app --host 0.0.0.0 --port 10000 --workers 1"
        }
    }
}
json_data = json.dumps(data).encode("utf-8")

for service in services:
    sid = service["sid"]
    key = service["key"]
    if not key:
        print(f"Skipping update for {sid}: API key not set")
        continue

    headers = {
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    }

    req = urllib.request.Request(
        f"https://api.render.com/v1/services/{sid}",
        data=json_data,
        headers=headers,
        method="PATCH",
    )
    try:
        with urllib.request.urlopen(req) as response:
            print(f"Updated {sid}: {response.status}")
    except Exception as e:
        print(f"Failed {sid}: {e}")
