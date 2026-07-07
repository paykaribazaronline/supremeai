# 📄 ফাইল: backend/tests/test_auth_middleware.py

**প্রকার:** .py  
**সাইজ:** 10,420 বাইট  
**আপডেট:** 2026-07-07T22:11:19.783834

---

## কোড

```py
"""Auth middleware tests for SupremeAI 2.0."""
import os
import pytest
from unittest.mock import AsyncMock
from unittest.mock import MagicMock
from unittest.mock import patch

from core.auth_middleware import AuthMiddleware
from core.auth_middleware import _get_bearer_token
from core.auth_middleware import verify_admin_session_fail_closed


class TestGetBearerToken:
    """Tests for _get_bearer_token helper function."""

    def test_get_bearer_token_valid(self):
        """বৈধ Bearer টোকেন এক্সট্রাকশন করা হচ্ছে।"""
        headers = [
            (b"authorization", b"Bearer test-token-123"),
            (b"content-type", b"application/json"),
        ]
        result = _get_bearer_token(headers)
        assert result == "test-token-123"

    def test_get_bearer_token_no_auth_header(self):
        """Authorization হেডার ছাড়াই টোকেন পাওয়া যায় না।"""
        headers = [(b"content-type", b"application/json")]
        result = _get_bearer_token(headers)
        assert result is None

    def test_get_bearer_token_malformed(self):
        """ফরম্যাট ভুল Authorization হেডার রিজেক্স করা হচ্ছে।"""
        headers = [(b"authorization", b"test-token-123")]
        result = _get_bearer_token(headers)
        assert result is None

    def test_get_bearer_token_wrong_scheme(self):
        """ভিন্ন scheme সহ হেডার রিজেক্স করা হচ্ছে।"""
        headers = [(b"authorization", b"Basic test-token")]
        result = _get_bearer_token(headers)
        assert result is None


class TestAuthMiddleware:
    """Tests for AuthMiddleware class."""

    @pytest.mark.anyio
    async def test_middleware_non_http_scope(self):
        """HTTP নয় এমন স্কোপে মিডলওয়্যার বংয়েজ করা হচ্ছে।"""
        mock_app = AsyncMock()
        middleware = AuthMiddleware(mock_app)

        scope = {"type": "websocket", "path": "/ws"}
        await middleware(scope, MagicMock(), MagicMock())
        mock_app.assert_called_once()

    @pytest.mark.anyio
    async def test_middleware_public_path(self):
        """পাবলিক পাথে মিডলওয়্যার বংয়েজ করা হচ্ছে।"""
        mock_app = AsyncMock()
        middleware = AuthMiddleware(mock_app)

        scope = {"type": "http", "path": "/health", "headers": []}
        await middleware(scope, MagicMock(), MagicMock())
        mock_app.assert_called_once()

    @pytest.mark.anyio
    @patch.dict("os.environ", {"SUPREMEAI_API_TOKEN": "test-token"})
    async def test_middleware_valid_api_token(self):
        """সঠিক API টোকেন সহ মিডলওয়্যার বংয়েজ করা হচ্ছে।"""
        mock_app = AsyncMock()
        middleware = AuthMiddleware(mock_app)

        scope = {
            "type": "http",
            "path": "/api/test",
            "headers": [(b"authorization", b"Bearer test-token")],
        }
        await middleware(scope, MagicMock(), MagicMock())
        mock_app.assert_called_once()

    @pytest.mark.anyio
    @patch.dict("os.environ", {"SUPREMEAI_API_TOKEN": "test-token"}, clear=False)
    async def test_middleware_invalid_api_token(self):
        """ভুল API টোকেন রিজেক্স করা হচ্ছে।"""
        mock_app = AsyncMock()
        middleware = AuthMiddleware(mock_app)

        scope = {
            "type": "http",
            "path": "/api/test",
            "headers": [(b"authorization", b"Bearer wrong-token")],
        }
        send = AsyncMock()
        await middleware(scope, MagicMock(), send)
        assert mock_app.called is False
        send.assert_called()

    @pytest.mark.anyio
    @patch.dict("os.environ", {"SUPREMEAI_API_TOKEN": "test-token"}, clear=False)
    async def test_middleware_no_api_token_env(self):
        """API টোকেন এনভ ভ্যারিয়েbl না থাকলে মিডলওয়্যার বংয়েজ করা হচ্ছে।"""
        mock_app = AsyncMock()
        middleware = AuthMiddleware(mock_app)

        scope = {
            "type": "http",
            "path": "/api/test",
            "headers": [],
        }
        send = AsyncMock()
        await middleware(scope, MagicMock(), send)
        mock_app.assert_not_called()
        send.assert_called()


class TestVerifyAdminSessionFailClosed:
    """Tests for verify_admin_session_fail_closed function."""

    def test_missing_authorization_header(self):
        """Authorization হেডার ছাড়াই রিকোয়েস্ট রিজেক্স করা হচ্ছে।"""
        from fastapi import HTTPException

        mock_request = MagicMock()
        mock_request.headers.get.return_value = None
        mock_request.client.host = "127.0.0.1"

        with pytest.raises(HTTPException) as exc_info:
            import asyncio
            asyncio.run(verify_admin_session_fail_closed(mock_request))

        assert exc_info.value.status_code == 401

    def test_malformed_authorization_header(self):
        """ফরম্যাট ভুল Authorization হেডার রিজেক্স করা হচ্ছে।"""
        from fastapi import HTTPException

        mock_request = MagicMock()
        mock_request.headers.get.return_value = "InvalidFormat"

        with pytest.raises(HTTPException) as exc_info:
            import asyncio
            asyncio.run(verify_admin_session_fail_closed(mock_request))

        assert exc_info.value.status_code == 401

    def test_missing_jwt_secret(self):
        """JWT সিক্রেট ছাড়াই রিকোয়েস্ট রিজেক্স করা হচ্ছে।"""
        from fastapi import HTTPException
        from core.config import settings

        mock_request = MagicMock()
        mock_request.headers.get.return_value = "Bearer test-token"

        with patch.object(settings, "jwt_secret", None):
            with pytest.raises(HTTPException) as exc_info:
                import asyncio
                asyncio.run(verify_admin_session_fail_closed(mock_request))

            assert exc_info.value.status_code == 500

    def test_expired_jwt_token(self):
        """এক্সপায়ার্ড JWT টোকেন রিজেক্স করা হচ্ছে।"""
        from fastapi import HTTPException
        from jose import ExpiredSignatureError

        mock_request = MagicMock()
        mock_request.headers.get.return_value = "Bearer expired-token"

        with patch("core.auth_middleware.settings") as mock_settings:
            mock_settings.jwt_secret = "test-secret"
            with patch("core.auth_middleware.jwt.decode") as mock_decode:
                mock_decode.side_effect = ExpiredSignatureError("Expired")
                with pytest.raises(HTTPException) as exc_info:
                    import asyncio
                    asyncio.run(verify_admin_session_fail_closed(mock_request))

                assert exc_info.value.status_code == 401

    def test_invalid_jwt_token(self):
        """অবৈধ JWT টোকেন রিজেক্স করা হচ্ছে।"""
        from fastapi import HTTPException
        from jose import JWTError

        mock_request = MagicMock()
        mock_request.headers.get.return_value = "Bearer invalid-token"

        with patch("core.auth_middleware.settings") as mock_settings:
            mock_settings.jwt_secret = "test-secret"
            with patch("core.auth_middleware.jwt.decode") as mock_decode:
                mock_decode.side_effect = JWTError("Invalid")
                with pytest.raises(HTTPException) as exc_info:
                    import asyncio
                    asyncio.run(verify_admin_session_fail_closed(mock_request))

                assert exc_info.value.status_code == 401

    def test_non_admin_role(self):
        """অ্যাডমিন নন-অ্যাডমিন রোল রিজেক্স করা হচ্ছে।"""
        from fastapi import HTTPException

        mock_request = MagicMock()
        mock_request.headers.get.return_value = "Bearer user-token"

        with patch("core.auth_middleware.settings") as mock_settings:
            mock_settings.jwt_secret = "test-secret"
            with patch("core.auth_middleware.jwt.decode") as mock_decode:
                mock_decode.return_value = {"sub": "user-123", "role": "user"}
                with pytest.raises(HTTPException) as exc_info:
                    import asyncio
                    asyncio.run(verify_admin_session_fail_closed(mock_request))

                assert exc_info.value.status_code == 401

    def test_master_admin_role_allowed(self):
        """master_admin রোল অনুমোদিত হয়।"""
        mock_request = MagicMock()
        mock_request.client.host = "127.0.0.1"
        mock_request.headers.get.return_value = "Bearer admin-token"

        with patch("core.auth_middleware.settings") as mock_settings:
            mock_settings.jwt_secret = "test-secret"
            with patch("core.auth_middleware.jwt.decode") as mock_decode:
                mock_decode.return_value = {"sub": "admin-123", "role": "master_admin"}
                import asyncio

                result = asyncio.run(verify_admin_session_fail_closed(mock_request))
                assert result["sub"] == "admin-123"

    def test_admin_role_success(self):
        """অ্যাডমিন রোল সফল ভেরিফিকেশন।"""
        mock_request = MagicMock()
        mock_request.client.host = "127.0.0.1"
        mock_request.headers.get.return_value = "Bearer admin-token"

        with patch("core.auth_middleware.settings") as mock_settings:
            mock_settings.jwt_secret = "test-secret"
            with patch("core.auth_middleware.jwt.decode") as mock_decode:
                mock_decode.return_value = {"sub": "admin-123", "role": "admin"}
                import asyncio

                result = asyncio.run(verify_admin_session_fail_closed(mock_request))
                assert result["sub"] == "admin-123"

```