import os

import requests

RENDER_API_KEY = os.getenv("RENDER_API_KEY")
RENDER_API_KEY_BACKUP = os.getenv("RENDER_API_KEY_BACKUP")


def check_account_logs(label, api_key):
    if not api_key:
        print(f"[{label}] No API key")
        return

    headers = {"Authorization": f"Bearer {api_key}", "Accept": "application/json"}

    # Get services
    resp = requests.get("https://api.render.com/v1/services?limit=20", headers=headers)
    if resp.status_code != 200:
        print(f"[{label}] Failed to list services: {resp.status_code} {resp.text}")
        return

    services = resp.json()
    for item in services:
        srv = item.get("service", {})
        srv_id = srv.get("id")
        name = srv.get("name")

        # Get latest deploy
        deploy_resp = requests.get(
            f"https://api.render.com/v1/services/{srv_id}/deploys?limit=1",
            headers=headers,
        )
        if deploy_resp.status_code == 200:
            deploys = deploy_resp.json()
            if deploys:
                latest_deploy = deploys[0].get("deploy", {})
                status = latest_deploy.get("status")
                print(f"[{label}] Service: {name} ({srv_id}) | Deploy Status: {status}")

                # Fetch logs if failed
                if status in ["build_failed", "update_failed"]:
                    print(f"Fetching logs for {name} ({srv_id})...")
                    # Try fetching logs endpoint
                    logs_resp = requests.get(
                        f"https://api.render.com/v1/services/{srv_id}/logs",
                        headers=headers,
                    )
                    if logs_resp.status_code == 200:
                        try:
                            # Render logs endpoint might return SSE or list of json objects
                            print("Logs:")
                            print(logs_resp.text[:2000])
                        except Exception as e:
                            print(f"Error printing logs: {e}")
                    else:
                        print(
                            f"Failed to fetch logs: {logs_resp.status_code} {logs_resp.text}"
                        )
            else:
                print(f"[{label}] Service: {name} ({srv_id}) | No deploys")
        else:
            print(
                f"[{label}] Service: {name} ({srv_id}) | Failed to get deploys: {deploy_resp.status_code}"
            )


if __name__ == "__main__":
    check_account_logs("Primary", RENDER_API_KEY)
    check_account_logs("Backup", RENDER_API_KEY_BACKUP)
