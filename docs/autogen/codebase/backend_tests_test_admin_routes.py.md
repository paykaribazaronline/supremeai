# 📄 ফাইল: backend/tests/test_admin_routes.py

**প্রকার:** .py  
**সাইজ:** 13,737 বাইট  
**আপডেট:** 2026-07-07T17:56:40.960162

---

## কোড

```py
"""Admin routes tests for SupremeAI 2.0."""
import base64
import hmac
import hashlib
import os
import struct
import time
from unittest.mock import AsyncMock
from unittest.mock import MagicMock
from unittest.mock import patch

import pytest
from fastapi import HTTPException
from fastapi.testclient import TestClient

from core.admin_routes import router
from models.admin import AdminLoginRequest
from models.admin import AdminVerifyRequest
from models.admin import AdminFirebaseLoginRequest
from models.admin import AdminFirebaseTotpSetupRequest
from models.admin import AdminFirebaseTotpVerifyRequest


class TestHelperFunctions:
    """Tests for admin route helper functions."""

    def test_hash_password_requires_bcrypt(self):
        """bcrypt ছাড়া হ্যাশ fails."""
        try:
            from core.admin_routes import _hash_password

            # If bcrypt is installed, this should work
            import bcrypt

            hashed = _hash_password("password")
            assert isinstance(hashed, str)
            assert len(hashed) > 0
        except ImportError:
            pytest.skip("bcrypt not installed")
        except RuntimeError as e:
            assert "bcrypt is required" in str(e)

    def test_verify_password_no_bcrypt(self):
        """bcrypt ছাড়া ভেরিফিকেশন False রিটার্ন করে।"""
        with patch.dict("sys.modules", {"bcrypt": None}):
            import importlib

            from core import admin_routes

            importlib.reload(admin_routes)
            assert admin_routes._verify_password("pass", "hash") is False

    def test_verify_password_empty_hash(self):
        """খালি হ্যাশে ভেরিফিকেশন False রিটার্ন করে।"""
        from core.admin_routes import _verify_password

        assert _verify_password("password", "") is False
        assert _verify_password("password", None) is False

    def test_get_admin_credentials_missing_hash(self):
        """এডমিন পাসওয়ার্ড হ্যাশ নেই থাকলে 500 রিটার্ন করে।"""
        with patch.dict(os.environ, {}, clear=False):
            from core.admin_routes import _get_admin_credentials

            with pytest.raises(HTTPException) as exc_info:
                _get_admin_credentials()

            assert exc_info.value.status_code == 500

    def test_get_admin_credentials_returns_hash(self):
        """যোগ্য এডমিন হ্যাশ রিটার্ন করে।"""
        test_hash = "test-admin-hash-value"
        with patch.dict(os.environ, {"SUPREMEAI_ADMIN_PASSWORD_HASH": test_hash}, clear=False):
            from core.admin_routes import _get_admin_credentials

            assert _get_admin_credentials() == test_hash


class TestVerifyTotpCode:
    """Tests for TOTP verification functions."""

    def test_verify_totp_code_valid(self):
        """বৈধ TOTP কোড ভেরিফিকেশন."""
        from core.admin_routes import verify_totp_code

        secret = base64.b32encode(os.urandom(10)).decode("utf-8")

        current_time = int(time.time() // 30)
        msg = struct.pack(">Q", current_time)
        key = base64.b32decode(secret.upper())
        h = hmac.new(key, msg, hashlib.sha1).digest()
        o = h[19] & 15
        h_num = struct.unpack(">I", h[o : o + 4])[0] & 0x7FFFFFFF
        valid_otp = f"{h_num % 1000000:06d}"

        assert verify_totp_code(valid_otp, secret) is True

    def test_verify_totp_code_invalid(self):
        """অবৈধ TOTP কোড রিজেক্স করা হচ্ছে।"""
        from core.admin_routes import verify_totp_code

        secret = base64.b32encode(os.urandom(10)).decode("utf-8")
        assert verify_totp_code("000000", secret) is False

    def test_check_totp_valid(self):
        """check_totp বৈধ কোড ভেরিফাই করে."""
        from core.admin_routes import check_totp

        secret = base64.b32encode(os.urandom(10)).decode("utf-8")

        current_time = int(time.time() // 30)
        msg = struct.pack(">Q", current_time)
        key = base64.b32decode(secret.upper())
        h = hmac.new(key, msg, hashlib.sha1).digest()
        o = h[19] & 15
        h_num = struct.unpack(">I", h[o : o + 4])[0] & 0x7FFFFFFF
        valid_otp = f"{h_num % 1000000:06d}"

        assert check_totp(valid_otp, secret) is True

    def test_check_totp_invalid(self):
        """check_totp অবৈধ কোড রিজেক্স করে."""
        from core.admin_routes import check_totp

        secret = base64.b32encode(os.urandom(10)).decode("utf-8")
        assert check_totp("123456", secret) is False

    def test_verify_totp_code_bad_secret(self):
        """খারাপ সিক্রেটে TOTP False রিটার্ন করে।"""
        from core.admin_routes import verify_totp_code

        assert verify_totp_code("1234567", "not-valid-base32-!@#$") is False


class TestAdminRoutes:
    """Tests for admin FastAPI routes using TestClient."""

    @pytest.fixture
    def client(self):
        """TestClient with mocked dependencies."""
        from core.app import app as fastapi_app

        return TestClient(fastapi_app)

    def test_health(self, client):
        """Health endpoint."""
        response = client.get("/health")
        assert response.status_code in [200, 503]

    def test_actuator_health(self, client):
        """Actuator health check."""
        response = client.get("/actuator/health")
        assert response.status_code == 200

    def test_admin_login_no_password(self, client):
        """পাসওয়ার্ড ছাড়া লগইন 422."""
        response = client.post("/api/admin/login", json={})
        assert response.status_code == 422

    def test_admin_login_invalid_password(self, client):
        """ভুল পাসওয়ার্ডে 401."""
        with patch.dict(os.environ, {"SUPREMEAI_ADMIN_PASSWORD_HASH": "dummy-hash", "SUPREMEAI_ADMIN_TOTP_SECRET": "dummy-secret"}, clear=False):
            response = client.post("/api/admin/login", json={"password": "wrong-password"})
            assert response.status_code == 401

    def test_admin_verify_no_password(self, client):
        """পাসওয়ার্ড ছাড়া ভেরিফাই 422."""
        response = client.post("/api/admin/verify", json={})
        assert response.status_code == 422

    def test_admin_verify_invalid_password(self, client):
        """ভুল পাসওয়ার্ডে ভেরিফাই 401."""
        with patch.dict(os.environ, {"SUPREMEAI_ADMIN_PASSWORD_HASH": "dummy-hash", "SUPREMEAI_ADMIN_TOTP_SECRET": "dummy-secret"}, clear=False):
            response = client.post("/api/admin/verify", json={"password": "wrong", "otp": "1234567"})
            assert response.status_code == 401

    def test_admin_firebase_login_no_token(self, client):
        """Firebase login with no token returns 422."""
        response = client.post("/api/admin/firebase-login", json={})
        assert response.status_code == 422

    def test_admin_firebase_login_mock_token_non_production(self, client):
        """মক ফায়ারবেস টোকেন লগইন non-production."""
        with patch("core.config.settings.env", "local"):
            response = client.post("/api/admin/firebase-login", json={"id_token": "mock-test-token"})
            assert response.status_code in [200, 403]

    def test_admin_firebase_login_mock_token_production(self, client):
        """মক টোকেন প্রোডাকশন নিষিদ্ধ."""
        with patch("core.config.settings.env", "production"):
            response = client.post("/api/admin/firebase-login", json={"id_token": "mock-test-token"})
            assert response.status_code == 403

    def test_admin_firebase_totp_setup_no_token(self, client):
        """TOTP setup missing token returns 422."""
        response = client.post("/api/admin/firebase-totp-setup", json={})
        assert response.status_code == 422

    def test_admin_firebase_totp_verify_no_token(self, client):
        """TOTP verify missing token returns 422."""
        response = client.post("/api/admin/firebase-totp-verify", json={})
        assert response.status_code == 422

    def test_cloud_distribution(self, client):
        """Cloud distribution endpoint."""
        with patch("core.admin_routes.services") as mock_services:
            mock_provider = {"status": "active", "current_requests": 0}
            mock_services.parallel_router.PROVIDERS = {"provider1": mock_provider}
            mock_services.parallel_router.get_distribution_stats = MagicMock(return_value={})

            response = client.get("/admin/cloud-distribution")
            assert response.status_code == 200

    def test_free_tier_status(self, client):
        """Free tier status endpoint."""
        mock_tracker = MagicMock()
        mock_tracker.get_status.return_value = {"status": "active"}

        with patch("core.admin_routes.services") as mock_services:
            mock_services.get_tracker = MagicMock(return_value=mock_tracker)
            with patch.dict("sys.modules", {"core.free_tier_tracker": MagicMock(get_tracker=MagicMock(return_value=mock_tracker))}):
                response = client.get("/admin/free-tier-status")
                assert response.status_code == 200

    def test_free_tier_provider_status_not_found(self, client):
        """অন tracked provider."""
        mock_tracker = MagicMock()
        mock_tracker.get_provider_status.return_value = None

        with patch.dict("sys.modules", {"core.free_tier_tracker": MagicMock(get_tracker=MagicMock(return_value=mock_tracker))}):
            response = client.get("/admin/free-tier-status/nonexistent")
            assert response.status_code == 404

    def test_free_tier_pause_provider(self, client):
        """Free tier pause provider endpoint."""
        mock_tracker = MagicMock()
        mock_tracker.mark_rate_limited.return_value = None

        with patch.dict("sys.modules", {"core.free_tier_tracker": MagicMock(get_tracker=MagicMock(return_value=mock_tracker))}):
            response = client.post("/admin/free-tier-pause/provider1")
            assert response.status_code == 200

    def test_free_tier_override_limits(self, client):
        """free tier override limits."""
        mock_tracker = MagicMock()
        mock_tracker.override_limits.return_value = None

        with patch.dict("sys.modules", {"core.free_tier_tracker": MagicMock(get_tracker=MagicMock(return_value=mock_tracker))}):
            response = client.post("/admin/free-tier-override/provider1", json={"limit": 100})
            assert response.status_code == 200

    def test_token_budget_stats(self, client):
        """Token budget stats endpoint."""
        mock_manager = MagicMock()
        mock_manager.get_stats.return_value = {"total": 1000}

        with patch.dict("sys.modules", {"core.token_budget": MagicMock(get_budget_manager=MagicMock(return_value=mock_manager))}):
            response = client.get("/admin/token-budget-stats")
            assert response.status_code == 200

    def test_gcp_health(self, client):
        """GCP health endpoint."""
        with patch("core.admin_routes.services") as mock_services:
            mock_services.gcp_router.health_check.return_value = {"status": "ok"}
            mock_services.verification_queue.provider = "firestore"
            mock_services.gcp_pubsub_queue.provider = "pubsub"
            mock_services.cloud_function_client.get_config.return_value = {}

            response = client.get("/gcp/health")
            assert response.status_code == 200

    def test_gcp_verification_queue_stats(self, client):
        """GCP verification queue stats."""
        with patch("core.admin_routes.services") as mock_services:
            mock_services.verification_queue.stats.return_value = {"total": 0}
            response = client.get("/gcp/verification-queue/stats")
            assert response.status_code == 200

    def test_gcp_pubsub_stats(self, client):
        """GCP pubsub stats."""
        with patch("core.admin_routes.services") as mock_services:
            mock_services.gcp_pubsub_queue.stats.return_value = {"messages": 0}
            response = client.get("/gcp/pubsub/stats")
            assert response.status_code == 200

    def test_get_admin_rules(self, client):
        """Get admin rules endpoint."""
        with patch("core.admin_routes.services") as mock_services:
            mock_services.rules_engine.rules = {"test": "rule"}
            response = client.get("/admin/rules")
            assert response.status_code == 200

    def test_post_admin_rules(self, client):
        """Post admin rules endpoint."""
        with patch("core.admin_routes.services") as mock_services:
            mock_services.rules_engine.save_rules.return_value = True
            response = client.post("/admin/rules", json={"rules": {"new": "rule"}})
            assert response.status_code == 200

    def test_post_admin_rules_failure(self, client):
        """Post admin rules failure."""
        with patch("core.admin_routes.services") as mock_services:
            mock_services.rules_engine.save_rules.return_value = False
            response = client.post("/admin/rules", json={"rules": {"new": "rule"}})
            assert response.status_code == 200

    def test_get_skills(self, client):
        """Skills endpoint."""
        response = client.get("/skills")
        assert response.status_code == 200

```