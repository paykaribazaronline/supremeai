import json
import os
import re
import urllib.request
from urllib.error import HTTPError, URLError


def fetch_json(url: str, token: str) -> dict:
    """Fetch JSON from GitHub API using standard urllib.request (zero dependencies)."""
    req = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github.v3+json",
            "User-Agent": "SupremeAI-CI-Summary-Bot",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            if resp.status == 200:
                return json.loads(resp.read().decode("utf-8"))
    except (HTTPError, URLError, OSError) as e:
        print(f"⚠️ API fetch error for {url}: {e}")
    return {}


def fetch_text(url: str, token: str) -> str:
    """Fetch raw text/logs from GitHub API using urllib.request."""
    req = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github.v3+json",
            "User-Agent": "SupremeAI-CI-Summary-Bot",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.read().decode("utf-8", errors="replace")
    except (HTTPError, URLError, OSError) as e:
        print(f"⚠️ Log fetch error for {url}: {e}")
    return ""


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
    run_id_env = os.environ.get("GITHUB_RUN_ID")

    if not all([token, repo, summary_file]):
        print("Missing GITHUB_TOKEN, GITHUB_REPOSITORY, or GITHUB_STEP_SUMMARY")
        return

    if run_id_env:
        # Fetch current run details directly
        run_url = f"https://api.github.com/repos/{repo}/actions/runs/{run_id_env}"
        run = fetch_json(run_url, token)
        if not run:
            print(f"Failed to fetch run {run_id_env}")
            return
    else:
        # Fallback: Fetch latest run
        print("Fetching latest workflow runs...")
        runs_url = f"https://api.github.com/repos/{repo}/actions/runs?per_page=1"
        data = fetch_json(runs_url, token)
        runs = data.get("workflow_runs", [])
        if not runs:
            print("Failed to fetch runs")
            return
        run = runs[0]

    run_id = run.get("id")
    workflow_name = run.get("name", "SupremeAI Pipeline")

    # Fetch ALL jobs for this run dynamically (no hardcoding)
    jobs_url = (
        f"https://api.github.com/repos/{repo}/actions/runs/{run_id}/jobs?per_page=100"
    )
    jobs_data = fetch_json(jobs_url, token)
    all_jobs = jobs_data.get("jobs", [])
    total_jobs = len(all_jobs)
    passed_jobs = [j for j in all_jobs if j["conclusion"] == "success"]
    failed_jobs = [j for j in all_jobs if j["conclusion"] == "failure"]
    skipped_jobs = [j for j in all_jobs if j["conclusion"] in ["skipped", "cancelled"]]
    in_progress_jobs = [j for j in all_jobs if j["conclusion"] is None]

    status_icon = "🟢" if len(failed_jobs) == 0 else "🚨"
    summary_lines = [
        f"### {status_icon} Smart CI Pipeline Summary — `{workflow_name}`",
        f"**Run ID:** [{run_id}](https://github.com/{repo}/actions/runs/{run_id}) | **Commit:** `{run.get('head_sha', '')[:7]}`",
        f"- 📊 **Total Real Jobs:** `{total_jobs}`",
        f"- ✅ **Passed:** `{len(passed_jobs)}` | ❌ **Failed:** `{len(failed_jobs)}` | ⏭️ **Skipped/Cancelled:** `{len(skipped_jobs)}` | ⏳ **In Progress:** `{len(in_progress_jobs)}`\n",
    ]

    if not failed_jobs:
        summary_lines.append(
            "🎉 **All executed jobs completed successfully with zero failures!**"
        )
    else:
        summary_lines.append("### 🔴 Failed Job Analysis & Diagnosis")
        for job in failed_jobs:
            job_name = job["name"]
            job_url = job["html_url"]
            summary_lines.append(f"#### ❌ Job: [{job_name}]({job_url})")

            log_url = (
                f"https://api.github.com/repos/{repo}/actions/jobs/{job['id']}/logs"
            )
            log_text = fetch_text(log_url, token)

            if log_text:
                errors = extract_errors(log_text)
                if errors:
                    for idx, err in enumerate(errors, 1):
                        summary_lines.append(f"**Extracted Traceback {idx}:**")
                        summary_lines.append("```python\n" + err + "\n```")
                else:
                    summary_lines.append(
                        "_Could not extract specific error stacktrace from logs._"
                    )
            else:
                summary_lines.append("_Log download failed or logs empty._")

            summary_lines.append("---")

    with open(summary_file, "a", encoding="utf-8") as f:
        f.write("\n".join(summary_lines) + "\n")

    print(
        f"Smart CI Summary generated dynamically for workflow '{workflow_name}' ({total_jobs} total jobs)."
    )


if __name__ == "__main__":
    main()
