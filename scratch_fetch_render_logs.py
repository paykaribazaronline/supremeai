import os
import sys

import requests

sys.stdout.reconfigure(encoding="utf-8")

RENDER_API_KEY_BACKUP = os.getenv("RENDER_API_KEY_BACKUP")
RENDER_API_KEY = os.getenv("RENDER_API_KEY")


def fetch_logs(label, api_key, target_srv_id, owner_id, name):
    if not api_key:
        print(f"[{label}] No API key")
        return

    headers = {"Authorization": f"Bearer {api_key}", "Accept": "application/json"}

    # Get latest deploy status
    deploy_resp = requests.get(
        f"https://api.render.com/v1/services/{target_srv_id}/deploys?limit=1",
        headers=headers,
    )
    if deploy_resp.status_code == 200:
        deploys = deploy_resp.json()
        if deploys:
            status = deploys[0].get("deploy", {}).get("status")
            print(
                f"\n[{label}] Service: {name} ({target_srv_id}) | Deploy Status: {status}"
            )
            print(f"Fetching /v1/logs (most recent first)...")

            # Fetch with direction=backward to get the newest logs first
            logs_resp = requests.get(
                f"https://api.render.com/v1/logs?ownerId={owner_id}&resource={target_srv_id}&limit=100&direction=backward",
                headers=headers,
            )
            if logs_resp.status_code == 200:
                try:
                    log_data = logs_resp.json()
                    entries = (
                        log_data
                        if isinstance(log_data, list)
                        else log_data.get("logs", [])
                    )
                    print(f"Fetched {len(entries)} log entries.")
                    # Reverse so they print in chronological order (oldest to newest)
                    for entry in reversed(entries[:40]):
                        text = entry.get("text") or entry.get("message")
                        timestamp = entry.get("timestamp")
                        print(f"[{timestamp}] {text}")
                except Exception as e:
                    print(f"Error parsing/printing logs: {e}")
            else:
                print(f"Failed to fetch logs: {logs_resp.status_code} {logs_resp.text}")
        else:
            print(f"[{label}] Service: {name} ({target_srv_id}) | No deploys")


if __name__ == "__main__":
    fetch_logs(
        "Backup",
        RENDER_API_KEY_BACKUP,
        "srv-d9e4q5rrjlhs73bnh71g",
        "tea-d995dc1o3t8c73etc45g",
        "supremeai-admin",
    )
    fetch_logs(
        "Primary",
        RENDER_API_KEY,
        "srv-d9d3n58js32c738n79k0",
        "tea-d747ms1aae7s73bcasu0",
        "supremeai-backend",
    )
