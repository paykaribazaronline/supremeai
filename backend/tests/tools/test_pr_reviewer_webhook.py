# বাংলা মন্তব্য: PR Reviewer-এর webhook integration এবং GitHub API ইন্টিগ্রেশন টেস্ট।

from unittest.mock import MagicMock, patch

import pytest

from tools.pr_reviewer import PRReviewer


@pytest.mark.anyio
async def test_webhook_endpoint_receives_pr_event():
    # বাংলা মন্তব্য: GitHub webhook event প্রকার যাচাই করা হচ্ছে
    reviewer = PRReviewer()

    # Mock PR event payload
    pr_event = {
        "action": "opened",
        "pull_request": {
            "number": 42,
            "html_url": "https://github.com/owner/repo/pull/42",
            "title": "Test PR",
            "user": {"login": "testuser"},
            "head": {"ref": "feature-branch"},
        },
        "repository": {"full_name": "owner/repo"},
    }

    with patch.object(reviewer, "review_pr", return_value={"status": "success", "comments": []}) as mock_review:
        result = await reviewer.handle_webhook(pr_event)

        assert result["status"] == "success"
        mock_review.assert_called_once()


@pytest.mark.anyio
async def test_check_style_compliance():
    # বাংলা মন্তব্য: Style compliance check টেস্ট
    reviewer = PRReviewer()

    diff = "diff --git a/src/main.py b/src/main.py\n--- a/src/main.py\n+++ b/src/main.py\n@@ -1,3 +1,4 @@\n+def MyFunction():\n+    pass\n"

    with patch("tools.pr_reviewer.settings") as mock_settings:
        mock_settings.code_style_preference = "snake_case"
        result = await reviewer.check_style_compliance(diff, "user_123")

    assert "style_issues" in result


@pytest.mark.anyio
async def test_run_code_smell_scan():
    # বাংলা মন্তব্য: Code smell scan integration টেস্ট
    reviewer = PRReviewer()

    diff = (
        "diff --git a/src/app.py b/src/app.py\n"
        "--- a/src/app.py\n"
        "+++ b/src/app.py\n"
        "@@ -1,3 +1,4 @@\n"
        "+def long_function(a, b, c, d, e, f, g, h, i, j):\n"
        "+    # Very long function with 50+ lines of code\n"
    )

    with patch("tools.pr_reviewer.CodeSmellDetector") as mock_detector:
        mock_instance = MagicMock()
        mock_instance.scan_code.return_value = {"issues": [{"type": "long_function", "severity": "medium"}]}
        mock_detector.return_value = mock_instance

        result = await reviewer.run_code_smell_scan(diff)

    assert "smell_issues" in result


@pytest.mark.anyio
async def test_auto_approve_on_clean_pr():
    # বাংলা মন্তব্য: সব চেক পাস করলে auto-approve হয় কিনা টেস্ট
    reviewer = PRReviewer()

    with patch.object(reviewer, "check_style_compliance", return_value={"style_issues": []}):
        with patch.object(reviewer, "run_code_smell_scan", return_value={"smell_issues": []}):
            with patch.object(reviewer, "analyze_diff", return_value=[]):
                result = await reviewer._auto_approve("owner/repo", 42)

    assert result is True


@pytest.mark.anyio
async def test_post_pr_comment():
    # বাংলা মন্তব্য: GitHub API দিয়ে PR-এ কমেন্ট পোস্ট করা হচ্ছে
    reviewer = PRReviewer()

    mock_repo = MagicMock()
    mock_pr = MagicMock()
    mock_repo.get_pull.return_value = mock_pr

    with patch("tools.pr_reviewer.Github") as mock_github:
        mock_github.return_value.get_repo.return_value = mock_repo

        with patch("tools.pr_reviewer.settings") as mock_settings:
            mock_settings.github_token = "fake-token"
            result = await reviewer._post_pr_comment("owner/repo", 42, "Test comment")

    assert result is True


@pytest.mark.anyio
async def test_security_vulnerability_scan():
    # বাংলা মন্তব্য: Security vulnerability detection টেস্ট
    reviewer = PRReviewer()

    diff = (
        "diff --git a/src/config.py b/src/config.py\n"
        "--- a/src/config.py\n"
        "+++ b/src/config.py\n"
        "@@ -1,3 +1,4 @@\n"
        "+password = 'super_secret_password_123'\n"
    )

    comments = await reviewer.analyze_diff(diff)

    assert len(comments) >= 1
    assert any("password" in c.get("body", "").lower() or c.get("severity") == "critical" for c in comments)
