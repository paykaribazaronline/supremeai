# 📄 ফাইল: deploy_render.py

**প্রকার:** .py  
**সাইজ:** 633 বাইট  
**আপডেট:** 2026-07-11T19:26:12.017342

---

## কোড

```py
import urllib.request
import sys

api_key = "rnd_gEmpPduF8s6icBAWPAdt9fJMLwvf"
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

```