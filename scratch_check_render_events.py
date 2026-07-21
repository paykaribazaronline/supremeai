import json
import os

import requests

RENDER_API_KEY = os.getenv("RENDER_API_KEY")
RENDER_API_KEY_BACKUP = os.getenv("RENDER_API_KEY_BACKUP")


def check_events(label, api_key, service_id):
    headers = {"Authorization": f"Bearer {api_key}", "Accept": "application/json"}
    url = f"https://api.render.com/v1/services/{service_id}/events?limit=5"
    resp = requests.get(url, headers=headers)
    if resp.status_code == 200:
        print(f"\n--- Events for {label} ({service_id}) ---")
        for idx, item in enumerate(resp.json()):
            event = item.get("event", {})
            print(f"Event #{idx+1}: {event.get('type')} at {event.get('createdAt')}")
            print(f"  Details: {event.get('details')}")
    else:
        print(f"Failed to fetch events for {label}: {resp.status_code}")


if __name__ == "__main__":
    check_events(
        "Primary supremeai-backend", RENDER_API_KEY, "srv-d9d3n58js32c738n79k0"
    )
    check_events(
        "Backup supremeai-admin", RENDER_API_KEY_BACKUP, "srv-d9e4q5rrjlhs73bnh71g"
    )
