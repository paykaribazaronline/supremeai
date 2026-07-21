import json
import os

import requests

RENDER_API_KEY = os.getenv("RENDER_API_KEY")
RENDER_API_KEY_BACKUP = os.getenv("RENDER_API_KEY_BACKUP")


def get_env_vars(label, api_key, service_id):
    if not api_key:
        print(f"[{label}] No API key")
        return
    headers = {"Authorization": f"Bearer {api_key}", "Accept": "application/json"}
    url = f"https://api.render.com/v1/services/{service_id}/env-vars?limit=50"
    resp = requests.get(url, headers=headers)
    if resp.status_code == 200:
        print(f"\n--- Env vars for {label} (Service: {service_id}) ---")
        vars_list = resp.json()
        for item in vars_list:
            ev = item.get("envVar", {})
            print(f"{ev.get('key')} = {ev.get('value')}")
    else:
        print(
            f"Failed to fetch env vars for {service_id}: {resp.status_code} {resp.text}"
        )


if __name__ == "__main__":
    # Primary Account Services
    get_env_vars(
        "Primary - supremeai-studio-client", RENDER_API_KEY, "srv-d9d3pgvavr4c738a46mg"
    )
    get_env_vars(
        "Primary - supremeai-backend", RENDER_API_KEY, "srv-d9d3n58js32c738n79k0"
    )

    # Backup Account Services
    get_env_vars(
        "Backup - supremeai-admin", RENDER_API_KEY_BACKUP, "srv-d9e4q5rrjlhs73bnh71g"
    )
    get_env_vars(
        "Backup - supremeai-studio-client",
        RENDER_API_KEY_BACKUP,
        "srv-d9ckgn61a83c7398o9u0",
    )
