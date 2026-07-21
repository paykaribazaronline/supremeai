import json
import os

import requests

RENDER_API_KEY = os.getenv("RENDER_API_KEY")
RENDER_API_KEY_BACKUP = os.getenv("RENDER_API_KEY_BACKUP")


def check_deploys(label, api_key, service_id):
    if not api_key:
        print(f"[{label}] No API key")
        return
    headers = {"Authorization": f"Bearer {api_key}", "Accept": "application/json"}
    url = f"https://api.render.com/v1/services/{service_id}/deploys?limit=3"
    resp = requests.get(url, headers=headers)
    if resp.status_code == 200:
        deploys = resp.json()
        print(f"\n--- Deploys for {label} ({service_id}) ---")
        for idx, item in enumerate(deploys):
            dep = item.get("deploy", {})
            commit = dep.get("commit", {})
            print(f"Deploy #{idx+1}:")
            print(f"  ID: {dep.get('id')}")
            print(f"  Status: {dep.get('status')}")
            print(f"  Created: {dep.get('createdAt')}")
            print(f"  Updated: {dep.get('updatedAt')}")
            if commit:
                print(f"  Commit SHA: {commit.get('id')}")
                print(f"  Commit Message: {commit.get('message')}")
    else:
        print(
            f"Failed to fetch deploys for {service_id}: {resp.status_code} {resp.text}"
        )


if __name__ == "__main__":
    check_deploys(
        "Primary - supremeai-backend", RENDER_API_KEY, "srv-d9d3n58js32c738n79k0"
    )
    check_deploys(
        "Backup - supremeai-admin", RENDER_API_KEY_BACKUP, "srv-d9e4q5rrjlhs73bnh71g"
    )
