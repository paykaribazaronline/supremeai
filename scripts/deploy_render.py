import urllib.request
import os

api_key = os.environ.get("RENDER_API_KEY", "")
services = ["srv-d991umnaqgkc73fk89o0", "srv-d817sc7aqgkc73aocjlg"]
headers = {
    "Authorization": f"Bearer {api_key}",
    "Content-Type": "application/json",
    "Accept": "application/json"
}

for sid in services:
    req = urllib.request.Request(f"https://api.render.com/v1/services/{sid}/deploys", data=b'{}', headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as response:
            print(f"Triggered deploy for {sid}: {response.status}")
    except Exception as e:
        print(f"Failed deploy for {sid}: {e}")
