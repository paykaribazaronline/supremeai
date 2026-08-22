"""ResourceGuard এর ইউনিট টেস্ট।

বাংলা: পাথ ট্রাভার্সাল ও আন-অথোরাইজড অ্যাক্সেস প্রতিরোধ লজিক কভার করা হয়েছে।
verify_path ক্লাস-মেথড সরাসরি টেস্ট করা হয়েছে (নেটওয়ার্ক-ফ্রি)।
"""

from __future__ import annotations

import pytest

from core.security.resource_guard import ResourceGuard


def test_verify_path_rejects_traversal():
    with pytest.raises(PermissionError):
        ResourceGuard.verify_path("../etc/passwd")


def test_verify_path_rejects_nested_traversal():
    with pytest.raises(PermissionError):
        ResourceGuard.verify_path("data/../../secret.txt")


def test_verify_path_allows_sandbox_root(tmp_path, monkeypatch):
    # স্যান্ডবক্স রুট টেম্প ডিরেক্টরিতে সেট করে ভিতরের পাথ অনুমোদন যাচাই
    monkeypatch.setattr(ResourceGuard, "SANDBOX_ROOT", tmp_path.resolve())
    target = tmp_path / "output" / "result.txt"
    resolved = ResourceGuard.verify_path(target)
    assert resolved == target.resolve()


def test_verify_path_rejects_external_path(tmp_path, monkeypatch):
    monkeypatch.setattr(ResourceGuard, "SANDBOX_ROOT", tmp_path.resolve())
    monkeypatch.setattr(ResourceGuard, "PROJECT_ROOT", tmp_path.resolve())
    monkeypatch.setattr(ResourceGuard, "PERSISTENT_DATA_DIR", tmp_path.resolve())
    # GITHUB_WORKSPACE ও বাদ দেওয়া হলো যাতে external path সত্যিই reject হয়
    external = "C:\\Windows\\system32\\config" if "\\" in str(tmp_path) else "/etc/shadow"
    with pytest.raises(PermissionError):
        ResourceGuard.verify_path(external)


def test_verify_path_rejects_dotdot_string_in_subpath(tmp_path, monkeypatch):
    monkeypatch.setattr(ResourceGuard, "SANDBOX_ROOT", tmp_path.resolve())
    with pytest.raises(PermissionError):
        ResourceGuard.verify_path(tmp_path / "a" / ".." / ".." / "secret")
