#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# file >> test_pr_reviewer.py
# project >> SupremeAI 2.0
# purpose >> Unit testing and QC
# module >> tests
# ============================================================================
import pytest
from unittest.mock import patch, MagicMock
from tools.pr_reviewer import PRReviewer

@pytest.mark.anyio
async def test_static_security_scan_detects_secret():
    reviewer = PRReviewer()
    diff = (
        "diff --git a/src/config.py b/src/config.py\n"
        "--- a/src/config.py\n"
        "+++ b/src/config.py\n"
        "@@ -1,3 +1,4 @@\n"
        "+aws_key = 'AKIA1234567890123456'\n"
    )
    comments = await reviewer.analyze_diff(diff)
    assert len(comments) == 1
    assert comments[0]["severity"] == "critical"
    assert "AWS API Key" in comments[0]["body"]

@pytest.mark.anyio
@patch("httpx.AsyncClient.get")
async def test_review_pr_trigger_changes(mock_get):
    # Mock GitHub API returns a diff containing stripe secret key
    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.text = (
        "diff --git a/src/db.py b/src/db.py\n"
        "--- a/src/db.py\n"
        "+++ b/src/db.py\n"
        "@@ -1,3 +1,4 @@\n"
         "+stripe_secret = 'sk_test_REPLACED_DO_NOT_USE_IN_PROD'\n"
    )
    mock_get.return_value = mock_resp

    reviewer = PRReviewer()
    res = await reviewer.review_pr("owner/repo", 42)
    assert res["status"] == "success"
    assert res["action_taken"] == "REQUEST_CHANGES"
    assert len(res["comments"]) >= 1
    assert any("Stripe Secret Key" in c["body"] for c in res["comments"])
