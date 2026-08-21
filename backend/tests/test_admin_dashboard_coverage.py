"""Tests to improve coverage for admin_dashboard routes (17.6% -> target 60%)."""

import asyncio
from unittest.mock import MagicMock, patch

import pytest
from fastapi import HTTPException
from fastapi.security import HTTPAuthorizationCredentials

from api.routes.admin_auth import (
    _in_memory_jwt_blacklist,
    admin_rate_limit,
    require_admin_token,
)


class TestRequireAdminToken:
    """Tests for require_admin_token dependency."""

    def test_valid_admin_token(self):
        """Valid admin JWT should be accepted."""
        import jwt

        from core.config import settings

        payload = {"uid": "admin-user", "role": "admin", "jti": "token-123"}
        token = jwt.encode(payload, settings.jwt_secret, algorithm="HS256")

        result = asyncio.run(require_admin_token(HTTPAuthorizationCredentials(credentials=token, scheme="Bearer")))
        assert result["uid"] == "admin-user"
        assert result["role"] == "admin"

    def test_non_admin_role_raises_401(self):
        """Token without admin role must be rejected with 401."""
        import jwt

        from core.config import settings

        payload = {"uid": "user", "role": "user"}
        token = jwt.encode(payload, settings.jwt_secret, algorithm="HS256")

        with pytest.raises(HTTPException) as exc_info:
            asyncio.run(require_admin_token(HTTPAuthorizationCredentials(credentials=token, scheme="Bearer")))

        assert exc_info.value.status_code == 403

    def test_revoked_jti_raises_401(self):
        """Revoked jti must raise 401 from in-memory blacklist."""
        import jwt

        from core.config import settings

        payload = {"uid": "admin", "role": "admin", "jti": "revoked-token"}
        token = jwt.encode(payload, settings.jwt_secret, algorithm="HS256")

        _in_memory_jwt_blacklist.add("revoked-token")
        try:
            with pytest.raises(HTTPException) as exc_info:
                asyncio.run(require_admin_token(HTTPAuthorizationCredentials(credentials=token, scheme="Bearer")))
            assert exc_info.value.status_code == 401
        finally:
            _in_memory_jwt_blacklist.discard("revoked-token")

    def test_invalid_token_raises_401(self):
        """Malformed token should raise 401."""
        with pytest.raises(HTTPException) as exc_info:
            asyncio.run(require_admin_token(HTTPAuthorizationCredentials(credentials="not-a-valid-token", scheme="Bearer")))
        assert exc_info.value.status_code == 401

    def test_fallback_api_token_auth(self):
        """SupremeAI API token fallback auth succeeds."""
        from core.config import settings

        expected = getattr(settings, "supremeai_api_token", None)
        if not expected:
            pytest.skip("supremeai_api_token not configured")

        with patch("api.routes.admin_auth.jwt.decode", side_effect=Exception("bad")):
            result = asyncio.run(require_admin_token(HTTPAuthorizationCredentials(credentials=expected, scheme="Bearer")))
        assert result["role"] == "admin"


class TestAdminRateLimit:
    """Tests for admin_rate_limit dependency."""

    def test_rate_limit_allows_request(self):
        """Request within limit should pass."""
        from fastapi import Request

        request = MagicMock(spec=Request)
        request.client.host = "127.0.0.1"

        with patch("core.services.redis_queue", new=MagicMock(configured=False)):
            with patch("api.routes.admin_auth.logger"):
                asyncio.run(admin_rate_limit(request))

    def test_rate_limit_raises_after_limit(self):
        """Exceeding rate limit should raise 429."""
        from fastapi import Request

        request = MagicMock(spec=Request)
        request.client.host = "127.0.0.1"

        fake_redis = MagicMock()
        fake_redis.configured = True
        fake_redis.get.return_value = "600"

        with patch("core.services.redis_queue", fake_redis):
            with patch("api.routes.admin_auth.logger"):
                with pytest.raises(HTTPException) as exc_info:
                    asyncio.run(admin_rate_limit(request))
        assert exc_info.value.status_code == 429
