import os

import requests

RENDER_API_KEY = os.getenv("RENDER_API_KEY")
headers = {"Authorization": f"Bearer {RENDER_API_KEY}", "Accept": "application/json"}

resp = requests.get("https://api.render.com/v1/services?limit=1", headers=headers)
if resp.status_code == 200:
    services = resp.json()
    if services:
        srv = services[0].get("service", {})
        print("Service keys:", srv.keys())
        print("Owner ID:", srv.get("ownerId"))
        print("Service Detail keys:", srv.get("serviceDetails", {}).keys())
        print("Full first service object:", srv)
else:
    print("Failed:", resp.status_code, resp.text)
