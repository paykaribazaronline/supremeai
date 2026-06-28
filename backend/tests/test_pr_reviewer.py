import os
from unittest.mock import MagicMock
from unittest.mock import patch

import pytest

from tools.pr_reviewer import PRReviewer


@pytest.mark.anyio
async def test_static_security_scan_detects_secret():
    reviewer = PRReviewer()
    diff = "diff --git a/src/config.py b/src/config.py\n--- a/src/config.py\n+++ b/src/config.py\n@@ -1,3 +1,4 @@\n+aws_key = 'AKIA1234567890123456'\n"
    comments = await reviewer.analyze_diff(diff)
    assert len(comments) == 1
    assert comments[0]["severity"] == "critical"
    assert "AWS API Key" in comments[0]["body"]


@pytest.mark.anyio
@patch("tools.pr_reviewer.Github")
async def test_review_pr_trigger_changes(mock_github):
    os.environ["GITHUB_TOKEN"] = "fake-token"

    mock_repo = MagicMock()
    mock_pr = MagicMock()
    mock_file = MagicMock()
    mock_file.patch = (
        "diff --git a/src/db.py b/src/db.py\n"
        "--- a/src/db.py\n"
        "+++ b/src/db.py\n"
        "@@ -1,3 +1,4 @@\n"
        "+stripe_secret = 'sk_test_51ABC123XYZ789abcdefGHIjklMNOpqr'\n"
    )
    mock_pr.get_files.return_value = [mock_file]
    mock_repo.get_pull.return_value = mock_pr
    mock_github.return_value.get_repo.return_value = mock_repo

    reviewer = PRReviewer()
    res = await reviewer.review_pr("owner/repo", 42)
    assert res["status"] == "success"
    assert res["action_taken"] == "REQUEST_CHANGES"
    assert len(res["comments"]) >= 1
    assert any("Stripe Secret Key" in c["body"] for c in res["comments"])
