import urllib.request
import json
import os

api_key = os.environ.get("RENDER_API_KEY", "")
services = ["srv-d991umnaqgkc73fk89o0", "srv-d817sc7aqgkc73aocjlg"]
headers = {
    "Authorization": f"Bearer {api_key}",
    "Content-Type": "application/json",
    "Accept": "application/json"
}

data = {
    "serviceDetails": {
        "envSpecificDetails": {
            "dockerCommand": "uvicorn main:app --host 0.0.0.0 --port 10000 --workers 1"
        }
    }
}
json_data = json.dumps(data).encode('utf-8')

for sid in services:
    req = urllib.request.Request(f"https://api.render.com/v1/services/{sid}", data=json_data, headers=headers, method="PATCH")
    try:
        with urllib.request.urlopen(req) as response:
            print(f"Updated {sid}: {response.status}")
    except Exception as e:
        print(f"Failed {sid}: {e}")
