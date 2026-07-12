import urllib.request
import urllib.error
import json

api_key = "rnd_gEmpPduF8s6icBAWPAdt9fJMLwvf"
service_id = "srv-d991umnaqgkc73fk89o0"
url = f"https://api.render.com/v1/services/{service_id}"
headers = {
    "Authorization": f"Bearer {api_key}",
    "Content-Type": "application/json",
    "Accept": "application/json"
}

data = {
    "serviceDetails": {
        "env": "image",
        "envSpecificDetails": {
            "image": {
                "imagePath": "ghcr.io/paykaribazaronline/supremeai/supremeai-backend:latest"
            }
        }
    }
}

req = urllib.request.Request(url, data=json.dumps(data).encode('utf-8'), headers=headers, method='PATCH')
try:
    with urllib.request.urlopen(req) as response:
        service = json.loads(response.read().decode())
        print("Success:", service)
except urllib.error.HTTPError as e:
    print(f"HTTPError: {e.code}")
    print(e.read().decode())
except Exception as e:
    print(e)
