#!/usr/bin/env python3
"""
SupremeAI - Smart CI Failure Summary Script
বাংলা মন্তব্য: এই স্ক্রিপ্টটি GitHub API ব্যবহার করে সর্বশেষ ব্যর্থ Core CI রান বিশ্লেষণ করে
এবং অ্যাডমিনের জন্য একটি সুন্দর Markdown রিপোর্ট তৈরি করে।
"""

import json
import os
import sys
from datetime import datetime, timezone
from typing import Any

import requests

# ═══════════════════════════════════════════════════
# GitHub Environment Variables
# ═══════════════════════════════════════════════════
REPO = os.environ.get("GITHUB_REPOSITORY", "")
TOKEN = os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN", "")
CORE_CI_WORKFLOW_NAME = "🧠 SupremeAI Core CI"
MAINTENANCE_WORKFLOW_NAME = "🤖 Manual Maintenance & Auto-Fix"

HEADERS = {
    "Accept": "application/vnd.github+json",
    "Authorization": f"Bearer {TOKEN}",
    "X-GitHub-Api-Version": "2022-11-28",
}

# ═══════════════════════════════════════════════════
# Auto-Fix Mapping: Core CI ব্যর্থ জব → Maintenance Action
# বাংলা মন্তব্য: কোন CI জব ফেইল হলে কোন Maintenance জব চালালে ফিক্স হতে পারে
# ═══════════════════════════════════════════════════
FIX_MAPPING: dict[str, dict[str, Any]] = {
    "pre-merge-gate": {
        "label": "🚧 Pre-Merge Gate (Iron Curtain)",
        "likely_cause": "Ruff lint এরর (`print()`, `BLE001`) অথবা httpx timeout মিসিং",
        "fix_action": "auto-lint-fix",
        "fix_label": "🔧 রান করুন `auto-lint-fix`",
        "fixable": True,
        "fix_description": "Ruff + Black + isort দিয়ে সব lint error অটো-ফিক্স করবে এবং PR তৈরি করবে।",
    },
    "backend-core": {
        "label": "🐍 Backend (Test & Auto-Fix)",
        "likely_cause": "pytest ব্যর্থতা — test assertion এরর বা import এরর",
        "fix_action": "auto-lint-fix",
        "fix_label": "🔧 রান করুন `auto-lint-fix`",
        "fixable": True,
        "fix_description": "কোড ফরম্যাটিং ঠিক করবে। যদি লজিক এরর হয়, তবে লগ দেখতে হবে।",
    },
    "production-readiness": {
        "label": "🚀 Production Readiness",
        "likely_cause": "Stub/placeholder ডেটা পাওয়া গেছে অথবা ভ্যালিডেটরে গুরুতর সমস্যা",
        "fix_action": None,
        "fix_label": "👁️ ম্যানুয়াল রিভিউ প্রয়োজন",
        "fixable": False,
        "fix_description": "Stub ডেটা ম্যানুয়ালি খুঁজে বের করে সরাতে হবে। কোনো অটো-ফিক্স নেই।",
    },
    "security-audit": {
        "label": "🛡️ CodeQL & Trivy Security Scan",
        "likely_cause": "ডিপেন্ডেন্সি CVE পাওয়া গেছে অথবা CodeQL ভায়োলেশন",
        "fix_action": "dependency-vulnerability-scan",
        "fix_label": "🔍 রান করুন `dependency-vulnerability-scan`",
        "fixable": True,
        "fix_description": "সম্পূর্ণ vulnerability স্ক্যান চালাবে এবং কোন প্যাকেজ ঝুঁকিপূর্ণ তা দেখাবে।",
    },
    "frontend-core": {
        "label": "🌐 Frontend Monorepo (Turbo)",
        "likely_cause": "TypeScript এরর, ESLint ভায়োলেশন অথবা Vitest ব্যর্থতা",
        "fix_action": "auto-lint-fix",
        "fix_label": "🔧 রান করুন `auto-lint-fix`",
        "fixable": True,
        "fix_description": "ESLint অটো-ফিক্স চালাবে এবং PR তৈরি করবে।",
    },
    "deploy-to-render": {
        "label": "🚀 Deploy Backend (Render)",
        "likely_cause": "Render ডিপ্লয় হুক ব্যর্থতা অথবা Docker বিল্ড এরর",
        "fix_action": None,
        "fix_label": "👁️ Render ড্যাশবোর্ড চেক করুন",
        "fixable": False,
        "fix_description": "Render ড্যাশবোর্ড এবং `RENDER_DEPLOY_HOOK_URL` সিক্রেট চেক করুন।",
    },
    "deploy-frontend-prod": {
        "label": "🌐 Deploy Frontend (Firebase)",
        "likely_cause": "Firebase ডিপ্লয় টোকেন মেয়াদোত্তীর্ণ অথবা প্রজেক্ট ID ভুল",
        "fix_action": None,
        "fix_label": "👁️ Firebase কনফিগ চেক করুন",
        "fixable": False,
        "fix_description": "`FIREBASE_TOKEN` এবং `GCP_PROJECT_ID` সিক্রেট চেক করুন।",
    },
    "deploy-to-vercel": {
        "label": "🚀 Deploy User Portal (Vercel)",
        "likely_cause": "Vercel ফ্রি টিয়ার লিমিট (১০০/দিন) অথবা কনফিগ ভুল",
        "fix_action": None,
        "fix_label": "👁️ Vercel ড্যাশবোর্ড চেক করুন",
        "fixable": False,
        "fix_description": "Vercel এর ডেইলি লিমিট শেষ হয়ে গেছে। ২৪ ঘণ্টা পরে আবার চেষ্টা করুন।",
    },
}

# ═══════════════════════════════════════════════════
# Helper Functions
# ═══════════════════════════════════════════════════

def api_get(path: str, params: dict | None = None) -> dict:
    """GitHub API GET request করে result return করে।"""
    url = f"https://api.github.com/repos/{REPO}{path}"
    resp = requests.get(url, headers=HEADERS, params=params, timeout=30)
    if resp.status_code >= 400:
        print(f"⚠️ GitHub API error {resp.status_code} for {path}")
        return {}
    return resp.json()


def get_latest_failed_core_ci_run() -> dict | None:
    """সর্বশেষ ব্যর্থ Core CI রান খুঁজে বের করে।"""
    params = {"per_page": 20, "status": "failure"}
    data = api_get("/actions/runs", params=params)
    runs = data.get("workflow_runs", [])
    for run in runs:
        if run.get("name") == CORE_CI_WORKFLOW_NAME:
            return run
    return None


def get_failed_jobs_for_run(run_id: int) -> list[dict]:
    """নির্দিষ্ট রান-এর সব ব্যর্থ জব fetch করে।"""
    data = api_get(f"/actions/runs/{run_id}/jobs", params={"per_page": 100})
    jobs = data.get("jobs", [])
    return [j for j in jobs if j.get("conclusion") in ("failure", "timed_out")]


def get_job_annotations(job_id: int) -> list[dict]:
    """কোনো জবের annotations (এরর মেসেজ) fetch করে।"""
    data = api_get(f"/actions/jobs/{job_id}/logs")
    # Annotations endpoint আলাদা
    data = api_get(f"/check-runs/{job_id}/annotations")
    return data if isinstance(data, list) else []


def match_job_to_fix(job_name: str) -> dict:
    """জবের নাম থেকে auto-fix mapping বের করে।"""
    job_lower = job_name.lower()
    for key, fix in FIX_MAPPING.items():
        if key in job_lower or key.replace("-", " ") in job_lower:
            return fix
    # Default fallback
    return {
        "label": job_name,
        "likely_cause": "অজানা — লগ ম্যানুয়ালি চেক করুন",
        "fix_action": None,
        "fix_label": "👁️ ম্যানুয়াল রিভিউ",
        "fixable": False,
        "fix_description": "এই জবের কোনো জানা অটো-ফিক্স নেই। লগ দেখুন।",
    }


def build_maintenance_dispatch_url(job_input_name: str) -> str:
    """Maintenance Pipeline ট্রিগার করার জন্য GitHub Actions URL তৈরি করে।"""
    return f"https://github.com/{REPO}/actions/workflows/maintenance_pipeline.yml"


def format_time_ago(iso_time: str) -> str:
    """ISO time string থেকে 'X মিনিট আগে' ফরম্যাটে কনভার্ট করে।"""
    try:
        dt = datetime.fromisoformat(iso_time.replace("Z", "+00:00"))
        now = datetime.now(timezone.utc)
        diff = now - dt
        minutes = int(diff.total_seconds() / 60)
        if minutes < 60:
            return f"{minutes} মিনিট আগে"
        hours = minutes // 60
        if hours < 24:
            return f"{hours} ঘণ্টা আগে"
        days = hours // 24
        return f"{days} দিন আগে"
    except Exception:
        return iso_time


# ═══════════════════════════════════════════════════
# Main Report Generator
# ═══════════════════════════════════════════════════

def generate_smart_summary() -> str:
    """
    বাংলা মন্তব্য: সর্বশেষ ব্যর্থ Core CI রান বিশ্লেষণ করে একটি
    সুন্দর Markdown অ্যাডমিন প্যানেল তৈরি করে।
    """
    lines = []
    lines.append("# 🧠 SupremeAI Smart CI Failure Summary")
    lines.append("")
    lines.append(f"> **Generated:** {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M UTC')}")
    lines.append("")

    # ── Step 1: সর্বশেষ ব্যর্থ রান খুঁজে বের করা
    failed_run = get_latest_failed_core_ci_run()

    if not failed_run:
        lines.append("## ✅ সব ঠিক আছে!")
        lines.append("")
        lines.append("সর্বশেষ Core CI রানে কোনো ব্যর্থতা পাওয়া যায়নি। সব সিস্টেম সচল আছে।")
        return "\n".join(lines)

    run_id = failed_run["id"]
    run_url = failed_run["html_url"]
    run_number = failed_run["run_number"]
    run_time = format_time_ago(failed_run.get("updated_at", ""))
    triggered_by = failed_run.get("head_commit", {}).get("author", {}).get("name", "Unknown")
    commit_msg = failed_run.get("head_commit", {}).get("message", "")[:80]
    branch = failed_run.get("head_branch", "main")

    lines.append(f"## ❌ ব্যর্থ রান ধরা পড়েছে")
    lines.append("")
    lines.append(f"| ফিল্ড | বিস্তারিত |")
    lines.append(f"|-------|-----------|")
    lines.append(f"| **রান** | [#{run_number}]({run_url}) |")
    lines.append(f"| **ব্রাঞ্চ** | `{branch}` |")
    lines.append(f"| **ব্যর্থ হয়েছে** | {run_time} |")
    lines.append(f"| **ট্রিগার করেছেন** | {triggered_by} |")
    lines.append(f"| **কমিট** | `{commit_msg}` |")
    lines.append("")

    # ── Step 2: ব্যর্থ জবগুলো fetch করা
    failed_jobs = get_failed_jobs_for_run(run_id)

    if not failed_jobs:
        lines.append("⚠️ রানটি ব্যর্থ হিসেবে চিহ্নিত হয়েছে কিন্তু নির্দিষ্ট কোনো ব্যর্থ জব পাওয়া যায়নি (সম্ভবত টাইম আউট বা বাতিল হয়েছে)।")
        return "\n".join(lines)

    lines.append(f"## 🔍 ব্যর্থ জবস ({len(failed_jobs)} টি পাওয়া গেছে)")
    lines.append("")

    # ── Step 3: প্রতিটি ব্যর্থ জবের জন্য fix recommendation তৈরি করা
    fixable_jobs = []
    manual_jobs = []

    for job in failed_jobs:
        job_name = job.get("name", "Unknown Job")
        job_url = job.get("html_url", run_url)
        job_duration = ""
        if job.get("started_at") and job.get("completed_at"):
            start = datetime.fromisoformat(job["started_at"].replace("Z", "+00:00"))
            end = datetime.fromisoformat(job["completed_at"].replace("Z", "+00:00"))
            secs = int((end - start).total_seconds())
            job_duration = f"{secs // 60}m {secs % 60}s"

        fix = match_job_to_fix(job_name)

        job_info = {
            "name": job_name,
            "url": job_url,
            "duration": job_duration,
            "fix": fix,
        }

        if fix["fixable"]:
            fixable_jobs.append(job_info)
        else:
            manual_jobs.append(job_info)

    # ── Auto-fixable jobs
    if fixable_jobs:
        lines.append("### ✅ অটো-ফিক্সযোগ্য — Maintenance Pipeline দিয়ে ঠিক করা যাবে")
        lines.append("")

        for info in fixable_jobs:
            fix = info["fix"]
            duration_str = f" _(ran {info['duration']})_" if info["duration"] else ""
            lines.append(f"#### ❌ [{info['name']}]({info['url']}){duration_str}")
            lines.append(f"- **সম্ভাব্য কারণ:** {fix['likely_cause']}")
            lines.append(f"- **সমাধান:** {fix['fix_label']}")
            lines.append(f"- **কীভাবে:** {fix['fix_description']}")
            maintenance_url = build_maintenance_dispatch_url(fix.get("fix_action", ""))
            lines.append(f"- 🔗 **[Maintenance Fix রান করতে এখানে ক্লিক করুন]({maintenance_url})**")
            lines.append(f"  - ওয়ার্কফ্লো: `maintenance_pipeline.yml`")
            lines.append(f"  - এনাবল করুন: `{fix['fix_action']}`")
            lines.append("")

    # ── Manual review jobs
    if manual_jobs:
        lines.append("### 👁️ ম্যানুয়াল রিভিউ প্রয়োজন — অটো-ফিক্স সম্ভব নয়")
        lines.append("")

        for info in manual_jobs:
            fix = info["fix"]
            duration_str = f" _(ran {info['duration']})_" if info["duration"] else ""
            lines.append(f"#### ❌ [{info['name']}]({info['url']}){duration_str}")
            lines.append(f"- **সম্ভাব্য কারণ:** {fix['likely_cause']}")
            lines.append(f"- **করণীয়:** {fix['fix_label']}")
            lines.append(f"- **বিস্তারিত:** {fix['fix_description']}")
            lines.append("")

    # ── Quick Action Summary Table
    lines.append("---")
    lines.append("## 🚀 অ্যাডমিনদের জন্য কুইক অ্যাকশন গাইড")
    lines.append("")
    lines.append("| ব্যর্থ জব | মেইনটেন্যান্স অ্যাকশন | ফিক্সযোগ্য? |")
    lines.append("|------------|----------------------|----------|")

    for info in fixable_jobs + manual_jobs:
        fix = info["fix"]
        fixable_emoji = "✅" if fix["fixable"] else "❌"
        action = fix["fix_label"]
        lines.append(f"| {info['name']} | {action} | {fixable_emoji} |")

    lines.append("")
    maintenance_main_url = f"https://github.com/{REPO}/actions/workflows/maintenance_pipeline.yml"
    lines.append(f"🔗 **[মেইনটেন্যান্স পাইপলাইন খুলুন]({maintenance_main_url})** — উপরের জবগুলো ঠিক করতে এখানে ক্লিক করুন।")
    lines.append("")
    lines.append("---")
    lines.append("_এই রিপোর্টটি SupremeAI Smart CI Failure Summary স্ক্রিপ্ট দ্বারা স্বয়ংক্রিয়ভাবে তৈরি হয়েছে।_")

    return "\n".join(lines)

def main() -> int:
    """
    বাংলা মন্তব্য: Main entry point — summary তৈরি করে GITHUB_STEP_SUMMARY-তে লেখে।
    """
    if not REPO or not TOKEN:
        print("❌ Missing GITHUB_REPOSITORY or GITHUB_TOKEN environment variables.")
        sys.exit(1)

    print("🔍 Fetching latest Core CI failure data from GitHub API...")
    summary = generate_smart_summary()

    # GitHub Step Summary-তে লেখা
    step_summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary_path:
        with open(step_summary_path, "a", encoding="utf-8") as f:
            f.write(summary + "\n")
        print("✅ Smart summary written to GitHub Step Summary.")
    else:
        # Local run এর জন্য stdout-এ প্রিন্ট করা
        print(summary)

    return 0


if __name__ == "__main__":
    sys.exit(main())

