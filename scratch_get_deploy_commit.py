import json
import os

import requests

RENDER_API_KEY = os.getenv("RENDER_API_KEY")
RENDER_API_KEY_BACKUP = os.getenv("RENDER_API_KEY_BACKUP")


def check_deploy(label, api_key, service_id, deploy_id):
    headers = {"Authorization": f"Bearer {api_key}", "Accept": "application/json"}
    url = f"https://api.render.com/v1/services/{service_id}/deploys/{deploy_id}"
    resp = requests.get(url, headers=headers)
    if resp.status_code == 200:
        print(f"\n--- Deploy Details for {label} ({deploy_id}) ---")
        print(json.dumps(resp.json(), indent=2))
    else:
        print(f"Failed to fetch {label} deploy details: {resp.status_code} {resp.text}")


if __name__ == "__main__":
    check_deploy(
        "Primary - supremeai-backend",
        RENDER_API_KEY,
        "srv-d9d3n58js32c738n79k0",
        "dep-d9fcjn9hefhs73b5g5e0",
    )
    check_deploy(
        "Backup - supremeai-admin",
        RENDER_API_KEY_BACKUP,
        "srv-d9e4q5rrjlhs73bnh71g",
        "dep-d9fckc1kh4rs73c7c0h0",
    )
