import json
import os
import urllib.request

RENDER_API_KEY = os.getenv("RENDER_API_KEY")
RENDER_API_KEY_BACKUP = os.getenv("RENDER_API_KEY_BACKUP")


def update_render_env(api_key, service_id, env_key, env_value):
    url = f"https://api.render.com/v1/services/{service_id}/env-vars?limit=100"
    headers = {"Authorization": f"Bearer {api_key}", "Accept": "application/json"}

    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req) as response:
            existing = json.loads(response.read().decode())

        new_vars = [
            {"key": item["envVar"]["key"], "value": item["envVar"]["value"]}
            for item in existing
        ]

        # Add or update key
        updated = False
        for var in new_vars:
            if var["key"] == env_key:
                var["value"] = env_value
                updated = True
                break
        if not updated:
            new_vars.append({"key": env_key, "value": env_value})

        put_req = urllib.request.Request(
            f"https://api.render.com/v1/services/{service_id}/env-vars",
            data=json.dumps(new_vars).encode("utf-8"),
            headers={**headers, "Content-Type": "application/json"},
            method="PUT",
        )
        with urllib.request.urlopen(put_req) as res:
            print(f"OK Render ({service_id}) updated with {env_key}={env_value}")
    except Exception as e:
        print(f"FAILED to update Render ({service_id}): {e}")


if __name__ == "__main__":
    if RENDER_API_KEY:
        update_render_env(
            RENDER_API_KEY, "srv-d9d3n58js32c738n79k0", "UVICORN_WORKERS", "1"
        )
    else:
        print("Missing RENDER_API_KEY")

    if RENDER_API_KEY_BACKUP:
        update_render_env(
            RENDER_API_KEY_BACKUP, "srv-d9e4q5rrjlhs73bnh71g", "UVICORN_WORKERS", "1"
        )
    else:
        print("Missing RENDER_API_KEY_BACKUP")
