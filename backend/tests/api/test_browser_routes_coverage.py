"""
Coverage tests for api/routes/browser.py.
Target: 100% line coverage.

ব্রাউজার রাউটের সকল এন্ডপয়েন্ট ও ফাংশন কভার করা হয়েছে।
"""

import os
import sys
from unittest.mock import patch

import pytest

_PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if _PROJECT_ROOT not in sys.path:
    sys.path.insert(0, _PROJECT_ROOT)

# ইম্পোর্টের আগে মডিউল লেভেলের ভেরিয়েবল রিসেট করার জন্য ফিক্সচার
_TEST_PASSWORD = "pytest_password_123"


@pytest.fixture(autouse=True)
def reset_browser_state():
    """Reset all browser module-level state between tests."""
    import api.routes.browser as browser_mod

    browser_mod.BROWSER_STATUS = {"browsing": False, "currentUrl": "about:blank"}
    browser_mod.RECENT_ACTIVITIES = []
    browser_mod.CREDENTIALS = []
    browser_mod.PAUSED_STATE = {"paused": False}
    browser_mod.URL_PERMISSIONS = []
    browser_mod.PERMISSION_REQUESTS = []
    browser_mod.SYSTEM_LEARNING = {"enabled": True}
    browser_mod.TASKS = {}
    browser_mod.FINDINGS = []
    yield
    return
    return


class TestBrowserStatus:
    """Tests for /surf/status, /surf/start, /surf/stop endpoints."""

    def test_get_status_initial(self):
        """get_status should return initial state."""
        from api.routes.browser import BROWSER_STATUS, get_status

        result = get_status()
        assert result == BROWSER_STATUS
        assert result["browsing"] is False

    def test_start_surf(self):
        """start_surf should set browsing to True."""
        from api.routes.browser import BROWSER_STATUS, start_surf

        result = start_surf()
        assert result["status"] == "started"
        assert BROWSER_STATUS["browsing"] is True

    def test_stop_surf(self):
        """stop_surf should set browsing to False."""
        from api.routes.browser import BROWSER_STATUS, stop_surf

        BROWSER_STATUS["browsing"] = True
        result = stop_surf()
        assert result["status"] == "stopped"
        assert BROWSER_STATUS["browsing"] is False


class TestRecentActivity:
    """Tests for /activity/recent endpoint."""

    def test_get_recent_activity_empty(self):
        """get_recent_activity should return empty list initially."""
        from api.routes.browser import get_recent_activity

        result = get_recent_activity()
        assert result["activities"] == []

    def test_get_recent_activity_with_data(self):
        """get_recent_activity should return activities."""
        from api.routes.browser import RECENT_ACTIVITIES, get_recent_activity

        RECENT_ACTIVITIES.append({"action": "navigate", "url": "https://example.com"})
        result = get_recent_activity()
        assert len(result["activities"]) == 1
        assert result["activities"][0]["action"] == "navigate"


class TestCredentials:
    """Tests for /credentials endpoints."""

    def test_get_credentials_empty(self):
        """get_credentials should return empty list initially."""
        from api.routes.browser import get_credentials

        result = get_credentials(userId="default")
        assert result["credentials"] == []

    def test_save_and_get_credential(self):
        """save_credential and get_credentials should work together."""
        from unittest.mock import MagicMock

        from api.routes.browser import CredentialRequest, save_credential

        mock_store = MagicMock()
        mock_store.encrypt.return_value = ("encrypted_data", "key_ref_1")
        mock_audit = MagicMock()

        with (
            patch("api.routes.browser.get_credential_store", return_value=mock_store),
            patch("api.routes.browser.get_audit", return_value=mock_audit),
        ):
            cred_req = CredentialRequest(serviceName="github", username="testuser", password=_TEST_PASSWORD)
            result = save_credential(cred_req)
            assert result["serviceName"] == "github"
            assert mock_audit.log_decision.called

    def test_delete_credential(self):
        """delete_credential should remove credential."""
        import api.routes.browser as browser_mod

        browser_mod.CREDENTIALS.append({"id": "cred_1", "serviceName": "test"})
        result = browser_mod.delete_credential("cred_1")
        assert result["success"] is True
        assert len(browser_mod.CREDENTIALS) == 0


class TestPausedState:
    """Tests for /surf/pause-manual, /surf/resume, /surf/paused-state endpoints."""

    def test_pause_manual(self):
        """pause_manual should set paused to True."""
        from api.routes.browser import PAUSED_STATE, pause_manual

        result = pause_manual({"reason": "testing"})
        assert result["status"] == "paused_for_manual"
        assert PAUSED_STATE["paused"] is True

    def test_resume_surf(self):
        """resume_surf should set paused to False."""
        from api.routes.browser import PAUSED_STATE, resume_surf

        PAUSED_STATE["paused"] = True
        result = resume_surf({"action": "resume"})
        assert result["status"] == "resumed"
        assert PAUSED_STATE["paused"] is False

    def test_get_paused_state(self):
        """get_paused_state should return current state."""
        from api.routes.browser import PAUSED_STATE, get_paused_state

        PAUSED_STATE["paused"] = True
        result = get_paused_state()
        assert result["paused"] is True


class TestUrlPermissions:
    """Tests for /urls/* endpoints."""

    def test_get_allowed_urls_empty(self):
        """get_allowed_urls should return empty initially."""
        from api.routes.browser import get_allowed_urls

        result = get_allowed_urls(userId="default")
        assert result["urls"] == []

    def test_add_allowed_url(self):
        """add_allowed_url should add a URL permission."""
        from api.routes.browser import (
            UrlPermissionRequest,
            add_allowed_url,
            get_allowed_urls,
        )

        req = UrlPermissionRequest(urlPattern="https://github.com/*")
        add_allowed_url(req)
        result = get_allowed_urls(userId="default")
        assert len(result["urls"]) == 1
        assert result["urls"][0]["urlPattern"] == "https://github.com/*"

    def test_add_denied_url(self):
        """add_denied_url should add a denied URL permission."""
        from api.routes.browser import (
            UrlPermissionRequest,
            add_denied_url,
            get_denied_urls,
        )

        req = UrlPermissionRequest(urlPattern="https://evil.com/*")
        add_denied_url(req)
        result = get_denied_urls(userId="default")
        assert len(result["urls"]) == 1

    def test_allow_all_urls(self):
        """allow_all_urls should add a wildcard permission."""
        from api.routes.browser import URL_PERMISSIONS, allow_all_urls

        result = allow_all_urls(userId="default")
        assert result["urlPattern"] == "*"
        assert len(URL_PERMISSIONS) == 1

    def test_delete_url(self):
        """delete_url should remove a URL permission."""
        import api.routes.browser as browser_mod

        req = browser_mod.UrlPermissionRequest(urlPattern="https://test.com/*")
        browser_mod.add_allowed_url(req)
        perm_id = browser_mod.URL_PERMISSIONS[0]["id"]
        browser_mod.delete_url(perm_id)
        assert len(browser_mod.URL_PERMISSIONS) == 0

    def test_url_request_decision(self):
        """decision should update request status."""
        from api.routes.browser import PERMISSION_REQUESTS, DecisionRequest, decision

        PERMISSION_REQUESTS.append({"id": "req_1", "status": "PENDING"})
        result = decision("req_1", DecisionRequest(approved=True))
        assert result["success"] is True
        assert PERMISSION_REQUESTS[0]["status"] == "APPROVED"

    def test_url_request_decision_not_found(self):
        """decision should raise 404 for unknown request."""
        from fastapi import HTTPException

        from api.routes.browser import DecisionRequest, decision

        with pytest.raises(HTTPException) as exc:
            decision("nonexistent", DecisionRequest(approved=False))
        assert exc.value.status_code == 404

    def test_get_requests(self):
        """get_requests should return permission requests."""
        from api.routes.browser import PERMISSION_REQUESTS, get_requests

        PERMISSION_REQUESTS.append({"id": "req_1", "status": "PENDING"})
        result = get_requests()
        assert len(result["requests"]) == 1


class TestSystemLearning:
    """Tests for /system-learning endpoints."""

    def test_get_system_learning(self):
        """get_system_learning should return current state."""
        from api.routes.browser import SYSTEM_LEARNING, get_system_learning

        SYSTEM_LEARNING["enabled"] = True
        result = get_system_learning()
        assert result["enabled"] is True

    def test_toggle_learning(self):
        """toggle_learning should update learning state."""
        from api.routes.browser import SYSTEM_LEARNING, toggle_learning

        toggle_learning({"enabled": False})
        assert SYSTEM_LEARNING["enabled"] is False


class TestSkipAuth:
    """Tests for /surf/skip-auth endpoint."""

    def test_skip_auth(self):
        """skip_auth should set paused to False."""
        from api.routes.browser import PAUSED_STATE, skip_auth

        PAUSED_STATE["paused"] = True
        result = skip_auth({"action": "skip"})
        assert result["status"] == "auth_skipped"
        assert PAUSED_STATE["paused"] is False


class TestRequestModels:
    """Tests for Pydantic request models."""

    def test_goal_request(self):
        """GoalRequest should have a goal field."""
        from api.routes.browser import GoalRequest

        req = GoalRequest(goal="Test goal")
        assert req.goal == "Test goal"

    def test_navigate_request(self):
        """NavigateRequest should have a url field."""
        from api.routes.browser import NavigateRequest

        req = NavigateRequest(url="https://example.com")
        assert req.url == "https://example.com"

    def test_click_request(self):
        """ClickRequest should have a selector field."""
        from api.routes.browser import ClickRequest

        req = ClickRequest(selector="#button")
        assert req.selector == "#button"

    def test_fill_request(self):
        """FillRequest should have selector and value."""
        from api.routes.browser import FillRequest

        req = FillRequest(selector="#input", value="test")
        assert req.value == "test"

    def test_click_at_request(self):
        """ClickAtRequest should have x and y."""
        from api.routes.browser import ClickAtRequest

        req = ClickAtRequest(x=100, y=200)
        assert req.x == 100
        assert req.y == 200
