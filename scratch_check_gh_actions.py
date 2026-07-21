import json
import os
import sys

import requests

sys.stdout.reconfigure(encoding="utf-8")

GITHUB_TOKEN = os.getenv("GITHUB_TOKEN") or os.getenv("GITHUB_API_TOKEN")

headers = {
    "Authorization": f"token {GITHUB_TOKEN}",
    "Accept": "application/vnd.github.v3+json",
}

url = (
    "https://api.github.com/repos/paykaribazaronline/supremeai/actions/runs?per_page=5"
)
resp = requests.get(url, headers=headers)
if resp.status_code == 200:
    runs = resp.json().get("workflow_runs", [])
    print("\n--- Recent GitHub Actions Workflow Runs ---")
    for r in runs:
        print(f"Run ID: {r.get('id')}")
        print(f"  Name: {r.get('name')}")
        print(f"  Event: {r.get('event')}")
        print(f"  Status: {r.get('status')}")
        print(f"  Conclusion: {r.get('conclusion')}")
        print(f"  Head Branch: {r.get('head_branch')}")
        print(f"  Head Commit: {r.get('head_commit', {}).get('message')}")
        print(f"  URL: {r.get('html_url')}")
else:
    print("Failed to fetch workflow runs:", resp.status_code, resp.text)
