import json
import os
import urllib.request

# বাংলা মন্তব্য: ব্যাকআপ অ্যাকাউন্টের API Key এবং সঠিক সার্ভিস আইডি সেট করা হলো
api_key = os.environ.get("RENDER_API_KEY_BACKUP", "")
service = "srv-d9e4q5rrjlhs73bnh71g"
headers = {
    "Authorization": f"Bearer {api_key}",
    "Content-Type": "application/json",
    "Accept": "application/json",
}

data = {
    "serviceDetails": {
        "envSpecificDetails": {
            "dockerCommand": "uvicorn main:app --host 0.0.0.0 --port 10000 --workers 1"
        }
    }
}
json_data = json.dumps(data).encode("utf-8")

req = urllib.request.Request(
    f"https://api.render.com/v1/services/{service}",
    data=json_data,
    headers=headers,
    method="PATCH",
)
try:
    with urllib.request.urlopen(req) as response:
        print(f"Updated {service}: {response.status}")
except Exception as e:
    print(f"Failed {service}: {e}")
