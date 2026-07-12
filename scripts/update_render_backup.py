import urllib.request
import json
import os

api_key = os.environ.get("RENDER_API_KEY", "")
service = "srv-d995glt7vvec73f3jgo0"
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

req = urllib.request.Request(f"https://api.render.com/v1/services/{service}", data=json_data, headers=headers, method="PATCH")
try:
    with urllib.request.urlopen(req) as response:
        print(f"Updated {service}: {response.status}")
except Exception as e:
    print(f"Failed {service}: {e}")
