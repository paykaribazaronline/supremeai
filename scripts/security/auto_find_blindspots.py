#!/usr/bin/env python
"""
auto_find_blindspots.py
=======================
An automated security and configuration scanner for the SupremeAI 2.0 codebase.

This script scans the entire repository to find known vulnerabilities, insecure
patterns, and common configuration mistakes based on the provided 'blind spot'
analysis documents (`blindspots-bangla.md` and `blink_spots_gemini.md`).

Usage:
python scripts/security/auto_find_blindspots.py
"""

import json
import os
import re
from collections.abc import Callable
from pathlib import Path
from typing import Any

# --- Configuration ---

# Directories and files to ignore during scanning
IGNORED_DIRS = {
    ".git",
    ".worktrees",
    "__pycache__",
    "node_modules",
    "build",
    "dist",
    "target",
    ".venv",
    "venv",
    "docs",
}
IGNORED_FILES = {".DS_Store"}

# Get the project root (assuming this script is in `scripts/security/`)
PROJECT_ROOT = Path(__file__).parent.parent.parent

# --- Checker Functions ---


def find_hardcoded_secrets(content: str, file_path: str) -> list[tuple[int, str]]:
    """Finds hardcoded passwords, API keys, or other secrets."""
    findings = []
    lines = content.splitlines()
    # More robust regex to find keys, avoiding variable assignments like `api_key = os.getenv(...)`
    secret_pattern = re.compile(
        r'(api_key|secret_key|password|token)\s*[:=]\s*["\']([A-Za-z0-9_/.+-]{16,})["\']',
        re.IGNORECASE,
    )

    for i, line in enumerate(lines, 1):
        # Specific hardcoded password from `blindspots-bangla.md`
        if "supreme-god-" + "password" in line:
            findings.append(
                (i, "🔴 Critical: Hardcoded 'supreme-god-" + "password' found.")
            )
        # Generic patterns for keys and passwords
        if secret_pattern.search(line):
            findings.append(
                (i, "🟠 High: Potential hardcoded secret or API key found.")
            )
    return findings


def check_cicd_vulnerabilities(content: str, file_path: str) -> list[str]:
    """Checks for vulnerabilities in CI/CD pipeline configurations."""
    findings = []
    # Applicable only to GitHub workflow files
    if ".github/workflows" in str(file_path) and file_path.endswith((".yml", ".yaml")):
        # Check for dangerously low test coverage threshold
        match = re.search(r"--cov-fail-under=(\d+)", content)
        if match and int(match.group(1)) < 38:
            findings.append(
                f"🔴 Critical: Test coverage threshold is critically low (`--cov-fail-under={match.group(1)}`)."
            )
        # Check for suppressed errors in shell commands
        if "|| true" in content:
            findings.append(
                "🟡 Medium: Potential error suppression using '|| true' in a CI step, which can hide failures."
            )
    # Check for direct git push in auto-fix scripts
    if "ci-auto-fix" in str(file_path) and file_path.endswith(".py"):
        if (
            'subprocess.run(["git", "push"]' in content
            or 'os.system("git push"' in content
        ):
            findings.append(
                "🔴 Critical: Script appears to perform a direct `git push`, bypassing PR and review processes."
            )
    return findings


def check_insecure_storage(content: str, file_path: str) -> list[str]:
    """Checks for insecure storage of tokens in frontend/mobile code."""
    findings = []
    # Check for JWTs in localStorage (for .ts, .js, .tsx files)
    if file_path.endswith((".ts", ".js", ".tsx")):
        if "localStorage.setItem" in content and (
            "jwt" in content.lower() or "token" in content.lower()
        ):
            findings.append(
                "🟠 High: JWT token is being stored in `localStorage`, which is vulnerable to XSS attacks."
            )
    # Check for insecure token storage in Flutter
    if file_path.endswith(".dart"):
        if (
            "SharedPreferences" in content
            and "token" in content.lower()
            and "flutter_secure_storage" not in content
        ):
            findings.append(
                "🟠 High: Token appears to be stored insecurely using `SharedPreferences` in Flutter. Use `flutter_secure_storage` instead."
            )
    return findings


def check_network_configuration(content: str, file_path: str) -> list[str]:
    """Checks for insecure network configurations, e.g., in Tauri."""
    findings = []
    if "tauri.conf.json" in str(file_path):
        try:
            config = json.loads(content)
            scope = (
                config.get("tauri", {})
                .get("allowlist", {})
                .get("http", {})
                .get("scope", [])
            )
            if "http://*/*" in scope or "https://*/*" in scope:
                findings.append(
                    "🔴 Critical: Tauri network scope is wide open (`*/*`), breaking sandbox security."
                )
        except json.JSONDecodeError:
            pass  # Ignore malformed JSON
    return findings


def check_database_issues(content: str, file_path: str) -> list[str]:
    """Checks for common database-related security issues."""
    findings = []
    if file_path.endswith(".py"):
        # Check for potential SQL injection via f-strings
        if (
            re.search(r'f"S?SE' + r"LECT .*\{.*\}", content, re.IGNORECASE)
            or re.search(r'f"UP' + r"DATE .*\{.*\}", content, re.IGNORECASE)
            or re.search(r'f"IN' + r"SERT INTO .*\{.*\}", content, re.IGNORECASE)
            or re.search(r'f"DE' + r"LETE FROM .*\{.*\}", content, re.IGNORECASE)
        ):
            findings.append(
                "🔴 Critical: SQL query built with an f-string, creating a high risk of SQL injection."
            )
        # Check for SQLite's `check_same_thread=False`
        if "check_same_thread" + "=False" in content:
            findings.append(
                "🟡 Medium: SQLite connection with `check_same_thread=False` can lead to data corruption or race conditions if not handled carefully."
            )
    return findings


def check_committed_env_file(file_path: Path) -> list[str]:
    """Checks if a .env file has been committed to the repository."""
    if file_path.name == ".env":
        return [
            "🔴 Critical: A `.env` file was found committed to the repository. This may leak production secrets."
        ]
    return []


# List of all checker functions to be executed on file content
CONTENT_CHECKERS: list[Callable[[str, str], Any]] = [
    find_hardcoded_secrets,
    check_cicd_vulnerabilities,
    check_insecure_storage,
    check_network_configuration,
    check_database_issues,
]

# --- Main Scan Logic ---


def scan_file(file_path: Path) -> list[tuple[str, str]]:
    """Scans a single file for vulnerabilities and returns findings."""
    findings = []
    try:
        # File-based checks (run on the path itself)
        for finding in check_committed_env_file(file_path):
            findings.append((str(file_path), finding))

        # Content-based checks (run on the file's content)
        with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
            content = f.read()

        for checker_func in CONTENT_CHECKERS:
            results = checker_func(content, str(file_path))
            for finding in results:
                if isinstance(finding, tuple):  # New format with line numbers
                    findings.append((str(file_path), f"L{finding[0]}: {finding[1]}"))
                else:  # Old format
                    findings.append((str(file_path), finding))

    except Exception as e:
        print(f"⚠️  Could not scan file: {file_path} ({e})")

    return findings


def is_test_file(file_path: Path) -> bool:
    """
    ফাইলটি টেস্ট ফাইল কিনা তা পরীক্ষা করে।
    এটি টেস্ট ফাইলগুলোকে নির্দিষ্ট নিরাপত্তা স্ক্যান থেকে বাদ দিতে সাহায্য করে।
    """
    path_parts = set(file_path.parts)
    # সাধারণ টেস্ট ডিরেক্টরির নাম
    if {"tests", "test", "__tests__", "e2e"}.intersection(path_parts):
        return True

    # সাধারণ টেস্ট ফাইলের প্যাটার্ন
    file_name = file_path.name
    return file_name.startswith("test_") or file_name.endswith(
        ("_test.py", ".spec.ts", ".test.ts", ".spec.js", ".test.js")
    )


def main():
    """Main function to orchestrate the scanning of the project directory."""
    print("🚀 Starting SupremeAI 2.0 Blind Spot Scanner...")
    print(f"📂 Project Root: {PROJECT_ROOT}\n")

    all_findings: dict[str, list[str]] = {}
    critical_issue_found = False
    file_count = 0

    for root, dirs, files in os.walk(PROJECT_ROOT):
        # Prune ignored directories to avoid traversing them
        dirs[:] = [d for d in dirs if d not in IGNORED_DIRS]

        for file_name in files:
            if file_name in IGNORED_FILES:
                continue

            file_path = Path(root) / file_name

            # টেস্ট ফাইলগুলোকে কিছু স্ক্যান থেকে বাদ দেওয়া হচ্ছে, তবে সিক্রেট স্ক্যান করা উচিত
            if is_test_file(file_path):
                # শুধুমাত্র হার্ডকোডেড সিক্রেট চেক করা হচ্ছে
                try:
                    content = file_path.read_text(encoding="utf-8", errors="ignore")
                    results = find_hardcoded_secrets(content, str(file_path))
                    if results:
                        if str(file_path) not in all_findings:
                            all_findings[str(file_path)] = []
                        for finding in results:
                            all_findings[str(file_path)].append(
                                f"L{finding[0]}: {finding[1]}"
                            )
                except (
                    Exception
                ):  # বাইনারি বা অন্যান্য ফাইল পড়ার সমস্যা উপেক্ষা করা হচ্ছে
                    pass
                continue

            file_count += 1

            file_findings = scan_file(file_path)
            if file_findings:
                for path, finding_text in file_findings:
                    if path not in all_findings:
                        all_findings[path] = []
                    # Avoid duplicate findings for the same file
                    if finding_text not in all_findings[path]:
                        if "🔴 Critical" in finding_text:
                            critical_issue_found = True
                        all_findings[path].append(finding_text)

    print("-" * 80)
    if not all_findings:
        print(
            f"✅ Congratulations! Scanned {file_count} files and found no known blind spots."
        )
    else:
        total_issues = sum(len(issues) for issues in all_findings.values())
        print(
            f"🚨 Found {total_issues} potential blind spot(s) across {len(all_findings)} file(s).\n"
        )

        # Sort files alphabetically for consistent output
        for file_path in sorted(all_findings.keys()):
            relative_path = os.path.relpath(file_path, PROJECT_ROOT)
            print(f"📄 File: {relative_path}")
            for finding in sorted(all_findings[file_path]):
                print(f"   - {finding}")
            print()  # Add a blank line for readability

    print("-" * 80)
    print("🔍 Scan complete.")

    if critical_issue_found:
        print(
            "\n🔥 বিল্ড ব্যর্থ হয়েছে কারণ এক বা একাধিক 'Critical' নিরাপত্তা ঝুঁকি পাওয়া গেছে।"
        )
        exit(1)


if __name__ == "__main__":
    main()
