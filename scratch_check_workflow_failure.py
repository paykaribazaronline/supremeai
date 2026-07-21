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

run_id = "29792533607"
url = f"https://api.github.com/repos/paykaribazaronline/supremeai/actions/runs/{run_id}/jobs"
resp = requests.get(url, headers=headers)
if resp.status_code == 200:
    jobs = resp.json().get("jobs", [])
    print(f"\n--- Jobs for Run {run_id} ---")
    for job in jobs:
        print(f"Job: {job.get('name')}")
        print(f"  Status: {job.get('status')}")
        print(f"  Conclusion: {job.get('conclusion')}")
        for step in job.get("steps", []):
            print(
                f"    Step: {step.get('name')} (Status: {step.get('status')}, Conclusion: {step.get('conclusion')})"
            )
else:
    print("Failed to fetch jobs:", resp.status_code, resp.text)
