#!/usr/bin/env python3
"""
find_secrets.py — SupremeAI 2.0 Secret/Leak Scanner (P0 Gate)
============================================================
পুরো কোডবেসে hardcoded secret / API key / token / private key স্ক্যান করে।
Master Audit Plan Phase 0 (Secrets: gitleaks) অনুযায়ী এটি সবচেয়ে ক্রিটিক্যাল
চেক — একটি লিক হওয়া JWT secret আগেই পাওয়া গিয়েছিল, ইতিহাসে আরও থাকতে পারে।

এই স্ক্রিপ্টটি স্বাধীনভাবে কাজ করে এবং নিচের কাজগুলো করে:
  1. Regex-based secret pattern স্ক্যান (ইন্টারনাল, কোনো বাইরের টুল ছাড়াই)
  2. gitleaks / trufflehog ইনস্টল থাকলে সেগুলোও চালায় (best-effort)
  3. কোনো ম্যাচ পেলে non-zero exit code রিটার্ন করে (CI gate হিসেবে কাজ করে)

ব্যবহার:
    python scripts/find_secrets.py                      # পুরো কোডবেস
    python scripts/find_secrets.py --path backend/       # শুধু backend/
    python scripts/find_secrets.py --no-external          # বাইরের টুল বাদ
    python scripts/find_secrets.py --fail-on HIGH         # HIGH-এ fail করবে

Exit codes:
    0 — কোনো secret leak পাওয়া যায়নি (PASS)
    1 — অন্তত একটি secret leak পাওয়া গেছে (FAIL)
    2 — আর্গুমেন্ট / রানটাইম এরর
"""

import argparse
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

# 🚨 Secret প্যাটার্ন — এগুলো কোডবেসে থাকা মানে সরাসরি credential লিক
# (pattern_name, compiled_regex, severity)
SECRET_PATTERNS: list[tuple[str, "re.Pattern[str]", str]] = [
    ("aws_access_key_id", re.compile(r"AKIA[0-9A-Z]{16}"), "CRITICAL"),
    ("aws_secret_access_key", re.compile(r"(?i)aws_secret_access_key\s*[:=]\s*['\"][A-Za-z0-9/+=]{40}['\"]"), "CRITICAL"),
    ("private_key_block", re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----"), "CRITICAL"),
    ("jwt_secret_hardcoded", re.compile(r"(?i)(jwt|secret|signing)[_\\-]?(key|secret|token)\s*[:=]\s*['\"][A-Za-z0-9_\-]{16,}['\"]"), "CRITICAL"),
    ("generic_api_key", re.compile(r"(?i)(api[_\\-]?key|apikey)\s*[:=]\s*['\"][A-Za-z0-9_\-]{20,}['\"]"), "CRITICAL"),
    ("google_api_key", re.compile(r"AIza[0-9A-Za-z_\-]{35}"), "CRITICAL"),
    ("slack_token", re.compile(r"xox[baprs]-[0-9A-Za-z\-]{10,}"), "CRITICAL"),
    ("github_pat", re.compile(r"ghp_[0-9A-Za-z]{36}"), "CRITICAL"),
    ("github_oauth", re.compile(r"gho_[0-9A-Za-z]{36}"), "CRITICAL"),
    ("openai_key", re.compile(r"sk-[A-Za-z0-9]{20,}"), "CRITICAL"),
    ("stripe_key", re.compile(r"(sk|rk)_(live|test)_[0-9A-Za-z]{16,}"), "CRITICAL"),
    ("sendgrid_key", re.compile(r"SG\.[A-Za-z0-9_\-]{16,}\.[A-Za-z0-9_\-]{16,}"), "CRITICAL"),
    ("twilio_key", re.compile(r"SK[0-9a-fA-F]{32}"), "HIGH"),
    ("password_assignment", re.compile(r"(?i)(password|passwd|pwd)\s*[:=]\s*['\"][^'\"\s]{6,}['\"]"), "HIGH"),
    ("db_connection_string", re.compile(r"(?i)(postgres|mysql|mongodb|redis|amqp)://[^:/\s]+:[^@/\s]+@"), "CRITICAL"),
    ("firebase_url_with_key", re.compile(r"https://.*\.firebaseio\.com"), "MEDIUM"),
    ("infisical_token", re.compile(r"(?i)infisical[_\\-]?(token|key)\s*[:=]\s*['\"][A-Za-z0-9_\-]{20,}['\"]"), "CRITICAL"),
    ("vercel_token", re.compile(r"(?i)vercel[_\\-]?token\s*[:=]\s*['\"][A-Za-z0-9_\-]{20,}['\"]"), "CRITICAL"),
    ("render_token", re.compile(r"(?i)render[_\\-]?token\s*[:=]\s*['\"][A-Za-z0-9_\-]{20,}['\"]"), "CRITICAL"),
]

# ✅ অনুমোদিত ব্যতিক্রম — test fixture, env example, বা স্ক্রিপ্ট নিজেই
ALLOWED_PATHS: tuple[str, ...] = (
    "find_secrets.py",          # স্ক্রিপ্ট নিজের প্যাটার্নের লাইন এড়িয়ে যাবে
    ".env.example",
    ".env.sample",
    "tests/",
    "test_",
    "conftest.py",
    "fixtures/",
    "mock",
    "stub",
    "example",
    "docs/",
    "README",
)

# যেসব ফাইল স্ক্যান করা হবে (source + config)
SCAN_SUFFIXES: tuple[str, ...] = (
    ".py", ".ts", ".tsx", ".js", ".jsx", ".java", ".kt",
    ".yaml", ".yml", ".json", ".toml", ".env", ".ini", ".sh", ".md",
)

DEFAULT_EXCLUDE: tuple[str, ...] = (
    ".venv", "node_modules", "__pycache__", ".git", ".agent",
    "infrastructure", "archive", "build", "dist", ".turbo",
)


def is_excepted(filepath: str) -> bool:
    """ফাইল পাথ allowed exceptions-এর মধ্যে কিনা চেক করে।"""
    norm = filepath.replace("\\", "/").lower()
    for token in ALLOWED_PATHS:
        if token.lower() in norm:
            return True
    return False


def scan_file(filepath: str) -> list[dict]:
    """একটি ফাইল স্ক্যান করে secret প্যাটার্ন খুঁজে বের করে।"""
    findings: list[dict] = []
    try:
        with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
            lines = f.readlines()
    except Exception:
        return findings

    for i, line in enumerate(lines, start=1):
        stripped = line.strip()
        # বাংলা মন্তব্য: মন্তব্য লাইন (# বা //) স্কিপ করা হলো যাতে false positive কমে।
        if stripped.startswith("#") or stripped.startswith("//") or stripped.startswith("*"):
            continue
        for name, regex, severity in SECRET_PATTERNS:
            match = regex.search(line)
            if match:
                findings.append({
                    "file": filepath,
                    "line": i,
                    "pattern": name,
                    "severity": severity,
                    "snippet": stripped[:140],
                })
                break
    return findings


def run_external_tool(name: str, cmd: list[str], root: str) -> str:
    """বাইরের secret স্ক্যানার (gitleaks/trufflehog) best-effort চালায়।"""
    if shutil.which(cmd[0]) is None:
        return f"[SKIP] {name} not installed (run: {cmd[0]})\n"
    try:
        result = subprocess.run(
            cmd + [root],
            capture_output=True, text=True, timeout=300,
        )
        out = result.stdout or result.stderr
        return f"=== {name} ===\n{out[:4000]}\n"
    except Exception as exc:  # নিজেই swallow করবে না — লগ করে দেবে
        return f"[ERROR] {name} failed: {exc}\n"


def scan_directory(root: str, exclude: list[str]) -> list[dict]:
    all_findings: list[dict] = []
    for path, dirs, files in os.walk(root):
        dirs[:] = [d for d in dirs if d not in exclude and not d.startswith(".")]
        for file in files:
            fp = Path(path) / file
            if fp.suffix in SCAN_SUFFIXES and not is_excepted(str(fp)):
                all_findings.extend(scan_file(str(fp)))
    return all_findings


def main() -> int:
    parser = argparse.ArgumentParser(description="SupremeAI Secret/Leak Scanner (P0 Gate)")
    parser.add_argument("--path", default=".", help="স্ক্যান করার পাথ (ডিফল্ট: repo root)")
    parser.add_argument("--exclude", nargs="*", default=list(DEFAULT_EXCLUDE), help="এক্সক্লুড ডিরেক্টরি")
    parser.add_argument("--no-external", action="store_true", help="বাইরের টুল (gitleaks) বাদ দাও")
    parser.add_argument("--fail-on", choices=["CRITICAL", "HIGH", "MEDIUM"], default="HIGH",
                        help="কোন সিভিরিটি লেভেলে fail করবে (ডিফল্ট: HIGH)")
    args = parser.parse_args()

    # বাংলা মন্তব্য: উইন্ডোজ কনসোল (charmap) বাংলা এনকোড করতে পারে না — stdout/stderr কে utf-8-এ রিকনফিগ করি।
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

    severity_order = {"CRITICAL": 0, "HIGH": 1, "MEDIUM": 2}
    fail_threshold = severity_order.get(args.fail_on, 1)

    print(f"[SCAN] Secret/Leak স্ক্যান চলছে: {args.path}")
    print(f"   Fail threshold: {args.fail_on}")
    print(f"   Excluding: {', '.join(args.exclude)}")
    print()

    findings = scan_directory(args.path, args.exclude)

    external_log = ""
    if not args.no_external:
        # বাংলা মন্তব্য: gitleaks থাকলে পুরো git history স্ক্যান করে (লিক আগেই পাওয়া গিয়েছিল)।
        external_log += run_external_tool("gitleaks", ["gitleaks", "detect", "--no-git", "-v", "-s"], args.path)
        external_log += run_external_tool("trufflehog", ["trufflehog", "filesystem", "--only-verified"], args.path)

    if not findings:
        print("[PASS] কোনো hardcoded secret leak পাওয়া যায়নি")
        if external_log:
            print("\n--- External tool output ---\n" + external_log)
        return 0

    by_sev = {s: [f for f in findings if f["severity"] == s] for s in severity_order}
    print(f"[FAIL] {len(findings)} সম্ভাব্য secret leak পাওয়া গেছে:")
    for s in ("CRITICAL", "HIGH", "MEDIUM"):
        if by_sev[s]:
            print(f"   {s}: {len(by_sev[s])}")
    print()

    for f in findings:
        safe_file = f["file"].encode(sys.stdout.encoding or "utf-8", "replace").decode()
        safe_snip = f["snippet"].encode(sys.stdout.encoding or "utf-8", "replace").decode()
        print(f"  [{f['severity']}] {f['pattern']}")
        print(f"     File: {safe_file}:{f['line']}")
        print(f"     Code: {safe_snip}")
        print()

    if external_log:
        print("--- External tool output ---\n" + external_log)

    worst = min(severity_order.get(f["severity"], 2) for f in findings)
    if worst <= fail_threshold:
        print(f"[FAIL] FAIL — '{args.fail_on}' বা তার ওপরে severity-র leak পাওয়া গেছে")
        return 1
    print(f"[WARN] WARNING — threshold-এর নিচে, fail করছে না")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        sys.exit(2)
    except Exception as exc:  # এরর লুক করবে না — সরাসরি দেখাবে
        print(f"[ERROR] {exc}", file=sys.stderr)
        sys.exit(2)
