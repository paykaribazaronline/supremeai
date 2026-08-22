"""SecretHunter এজেন্টের ইউনিট টেস্ট।

বাংলা: এখানে শুধুমাত্র নেটওয়ার্ক/LLM-ফ্রি লজিক কভার করা হয়েছে —
GitleaksRunner-এর রেজেক্স প্যাটার্ন ম্যাচিং, SecretReport সিরিয়ালাইজেশন
এবং pre-commit hook জেনারেশন। AISecretAnalyzer (LLM) ও DB স্ক্যান আলাদা ইন্টিগ্রেশন টেস্টের জন্য।
"""

from __future__ import annotations

from pathlib import Path


from core.security.secret_hunter import (
    GitleaksRunner,
    SecretFinding,
    SecretHunter,
    SecretReport,
)


def test_gitleaks_runner_compiles_all_patterns():
    runner = GitleaksRunner()
    # বাংলা: সব প্যাটার্ন কম্পাইল হয়েছে কিনা যাচাই
    assert len(runner.compiled_patterns) == len(runner.PATTERNS)
    assert "aws-access-key" in runner.compiled_patterns
    assert "openai-key" in runner.compiled_patterns


def test_gitleaks_detects_openai_key():
    runner = GitleaksRunner()
    finding = runner.scan_file(Path("fake_openai.py"))
    # scan_file পড়তে পারবে না, তাই সরাসরি লাইন ম্যাচ টেস্ট করি
    line = 'api_key = "sk-A' + "B" * 46 + '"'
    findings = []
    for pattern in runner.compiled_patterns.values():
        for m in pattern.finditer(line):
            findings.append(m.group(0))
    assert any("sk-A" in f for f in findings)


def test_gitleaks_detects_aws_access_key():
    runner = GitleaksRunner()
    line = "key = AKIAIOSFODNN7EXAMPLE"
    matches = [m.group(0) for p in runner.compiled_patterns.values() for m in p.finditer(line)]
    assert "AKIAIOSFODNN7EXAMPLE" in matches


def test_gitleaks_detects_github_token():
    runner = GitleaksRunner()
    # বাংলা: ghp_ প্রিফিক্সের পর ঠিক 36 ক্যারেক্টার থাকতে হবে (রেজেক্স অনুযায়ী)
    token = "ghp_" + "a" * 36
    line = f"token = {token}"
    matches = [m.group(0) for p in runner.compiled_patterns.values() for m in p.finditer(line)]
    assert token in matches


def test_gitleaks_detects_private_key_block():
    runner = GitleaksRunner()
    line = "-----BEGIN RSA PRIVATE KEY-----"
    matches = [m.group(0) for p in runner.compiled_patterns.values() for m in p.finditer(line)]
    assert "-----BEGIN RSA PRIVATE KEY-----" in matches


def test_gitleaks_detects_generic_api_key():
    runner = GitleaksRunner()
    line = 'api_key = "supersecretvaluetoken1234567890"'
    matches = [m.group(0) for p in runner.compiled_patterns.values() for m in p.finditer(line)]
    assert any("supersecretvaluetoken1234567890" in m for m in matches)


def test_gitleaks_no_false_positive_on_plain_text():
    runner = GitleaksRunner()
    line = "this is just a normal sentence without secrets"
    matches = [m.group(0) for p in runner.compiled_patterns.values() for m in p.finditer(line)]
    assert matches == []


def test_scan_file_handles_unreadable(tmp_path):
    runner = GitleaksRunner()
    # বাংলা: একটি ডিরেক্টরি পাথ দিলে read_text OSError দেবে, [] ফেরত দেওয়া উচিত
    assert runner.scan_file(tmp_path) == []


def test_secret_report_to_dict_truncates_long_match():
    report = SecretReport(
        scan_id="s1",
        scanned_at="2026-01-01T00:00:00+00:00",
        total_files=1,
        findings=[
            SecretFinding(
                rule_id="openai-key",
                file_path="a.py",
                line_number=1,
                column_start=0,
                column_end=10,
                matched_text="sk-" + "X" * 80,
                secret_type="OpenAI API Key",
                severity="high",
            )
        ],
    )
    data = report.to_dict()
    assert data["findings_count"] == 1
    assert len(data["findings"][0]["matched_text"]) <= 53  # 50 + "..."
    assert data["findings"][0]["matched_text"].endswith("...")


def test_secret_report_to_dict_empty():
    report = SecretReport(scan_id="s2", scanned_at="t")
    data = report.to_dict()
    assert data["findings_count"] == 0
    assert data["findings"] == []


def test_generate_pre_commit_hook_returns_script():
    hunter = SecretHunter()
    hook = hunter.generate_pre_commit_hook()
    assert "SecretHunter" in hook
    assert hook.strip().startswith("#!/bin/bash")


def test_secret_hunter_singleton_exists():
    from core.security.secret_hunter import secret_hunter

    assert isinstance(secret_hunter, SecretHunter)
