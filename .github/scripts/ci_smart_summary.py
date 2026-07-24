#!/usr/bin/env python3
import os
import re

import requests


def extract_errors(log_text):
    """Extract tracebacks and error messages from log text using regex."""
    errors = []

    # Match Python tracebacks
    traceback_pattern = re.compile(
        r"(Traceback \(most recent call last\):[\s\S]+?(?:\n\S|$))", re.MULTILINE
    )
    for match in traceback_pattern.finditer(log_text):
        errors.append(match.group(1).strip())

    # Match Pytest failures
    pytest_pattern = re.compile(
        r"(_{3,}\s+.*?_{3,}\n[\s\S]+?)(?=\n_{3,}|\Z)", re.MULTILINE
    )
    for match in pytest_pattern.finditer(log_text):
        if "E   " in match.group(1) or "FAILURES" in match.group(1):
            errors.append(match.group(1).strip())

    # Match generic errors if none found
    if not errors:
        error_pattern = re.compile(
            r"^.*?(?:Error|Exception|Failed):.*$", re.MULTILINE | re.IGNORECASE
        )
        for match in error_pattern.finditer(log_text):
            errors.append(match.group(0).strip())

    # Return unique truncated errors
    return list(
        dict.fromkeys([e[:1000] + ("..." if len(e) > 1000 else "") for e in errors])
    )[:5]


def main():
    token = os.environ.get("GITHUB_TOKEN")
    repo = os.environ.get("GITHUB_REPOSITORY")
    summary_file = os.environ.get("GITHUB_STEP_SUMMARY")

    if not all([token, repo, summary_file]):
        print("Missing GITHUB_TOKEN, GITHUB_REPOSITORY, or GITHUB_STEP_SUMMARY")
        return

    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github.v3+json",
    }

    # 1. Fetch latest failed run
    print("Fetching latest failed runs...")
    runs_url = (
        f"https://api.github.com/repos/{repo}/actions/runs?status=failure&per_page=1"
    )
    response = requests.get(runs_url, headers=headers)
    if not response.ok:
        print(f"Failed to fetch runs: {response.status_code}")
        return

    runs_data = response.json()
    if not runs_data.get("workflow_runs"):
        print("No failed runs found.")
        with open(summary_file, "a", encoding="utf-8") as f:
            f.write("### 🟢 CI Health Report\nNo recent failed runs detected.\n")
        return

    run = runs_data["workflow_runs"][0]
    run_id = run["id"]
    workflow_name = run["name"]

    # 2. Fetch failed jobs for this run
    jobs_url = f"https://api.github.com/repos/{repo}/actions/runs/{run_id}/jobs"
    response = requests.get(jobs_url, headers=headers)
    if not response.ok:
        print(f"Failed to fetch jobs: {response.status_code}")
        return

    failed_jobs = [
        j for j in response.json().get("jobs", []) if j["conclusion"] == "failure"
    ]

    summary_lines = [
        "### 🚨 Smart CI Failure Summary",
        f"**Workflow:** `{workflow_name}` (Run ID: [{run_id}]({run['html_url']}))\n",
    ]

    if not failed_jobs:
        summary_lines.append("No specific failed jobs found in the run data.")

    # 3. Download and analyze logs
    for job in failed_jobs:
        job_name = job["name"]
        job_url = job["html_url"]
        summary_lines.append(f"#### ❌ Job: [{job_name}]({job_url})")

        log_url = f"https://api.github.com/repos/{repo}/actions/jobs/{job['id']}/logs"
        log_response = requests.get(log_url, headers=headers)

        if log_response.ok:
            errors = extract_errors(log_response.text)
            if errors:
                for idx, err in enumerate(errors, 1):
                    summary_lines.append(f"**Error {idx}:**")
                    summary_lines.append("```python\n" + err + "\n```")
            else:
                summary_lines.append(
                    "_Could not extract specific error stacktrace from logs._"
                )
        else:
            summary_lines.append(f"_Log download failed ({log_response.status_code})._")

        summary_lines.append("---")

    with open(summary_file, "a", encoding="utf-8") as f:
        f.write("\n".join(summary_lines) + "\n")

    print("Smart CI Failure Summary generated successfully.")


if __name__ == "__main__":
    main()
