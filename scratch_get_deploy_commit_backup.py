import json
import os

import requests

RENDER_API_KEY_BACKUP = os.getenv("RENDER_API_KEY_BACKUP")

headers = {
    "Authorization": f"Bearer {RENDER_API_KEY_BACKUP}",
    "Accept": "application/json",
}

service_id = "srv-d9e4q5rrjlhs73bnh71g"
deploy_id = "dep-d9fbt3btqb8s73covgcg"

url = f"https://api.render.com/v1/services/{service_id}/deploys/{deploy_id}"
resp = requests.get(url, headers=headers)
if resp.status_code == 200:
    print(json.dumps(resp.json(), indent=2))
else:
    print("Failed:", resp.status_code, resp.text)
