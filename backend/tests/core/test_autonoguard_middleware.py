"""Tests for core.security.autonoguard_middleware — AutonoGuardMiddleware."""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from core.security.autonoguard_middleware import AutonoGuardMiddleware


class TestAutonoGuardMiddleware:
    """Tests for AutonoGuardMiddleware."""

    @pytest.mark.asyncio
    async def test_non_sensitive_path_passes(self):
        app = AsyncMock()
        middleware = AutonoGuardMiddleware(app)
        middleware._initialized = True
        request = MagicMock()
        request.url.path = "/api/v1/public"
        request.method = "GET"
        request.headers = {}
        request.state.user = {"sub": "test-user"}

        with patch(
            "core.security.autonoguard_middleware.autonoguard_engine"
        ) as mock_engine:
            mock_engine.enforce_operation = AsyncMock(return_value=(True, None))
            response = await middleware.dispatch(request, app)
            app.assert_awaited_once()

    @pytest.mark.asyncio
    async def test_public_path_skips_check(self):
        app = AsyncMock()
        middleware = AutonoGuardMiddleware(app)
        middleware._initialized = True
        request = MagicMock()
        request.url.path = "/health"
        request.method = "GET"
        request.headers = {}
        request.state.user = {"sub": "test-user"}

        response = await middleware.dispatch(request, app)
        app.assert_awaited_once()

    @pytest.mark.asyncio
    async def test_sensitive_path_calls_enforce(self):
        app = AsyncMock()
        middleware = AutonoGuardMiddleware(app)
        middleware._initialized = True
        request = MagicMock()
        request.url.path = "/api/v1/admin/users"
        request.method = "POST"
        request.headers = {"X-JIT-OTP": "123456"}
        request.state.user = {"sub": "admin-user"}
        request.body = AsyncMock(return_value=b"{}")

        with patch("core.config.settings.supremeai_public_paths", ["/health"]):
            with patch(
                "core.security.autonoguard_middleware.autonoguard_engine"
            ) as mock_engine:
                mock_engine.enforce_operation = AsyncMock(return_value=(True, None))
                await middleware.dispatch(request, app)
                mock_engine.enforce_operation.assert_awaited_once()

    @pytest.mark.asyncio
    async def test_sensitive_path_denied_returns_401(self):
        app = AsyncMock()
        middleware = AutonoGuardMiddleware(app)
        middleware._initialized = True
        request = MagicMock()
        request.url.path = "/api/v1/admin/users"
        request.method = "POST"
        request.headers = {}
        request.client.host = "127.0.0.1"
        request.state.correlation_id = "test-corr-id"

        with patch("core.config.settings.supremeai_public_paths", ["/health"]):
            with patch(
                "core.security.autonoguard_middleware.autonoguard_engine"
            ) as mock_engine:
                mock_engine.enforce_operation = AsyncMock(
                    return_value=(False, "JIT OTP required")
                )
                mock_engine.heal_error = AsyncMock()
                response = await middleware.dispatch(request, app)
                assert response.status_code == 401
                app.assert_not_awaited()

    @pytest.mark.asyncio
    async def test_no_user_in_state(self):
        app = AsyncMock()
        middleware = AutonoGuardMiddleware(app)
        middleware._initialized = True
        request = MagicMock()
        request.url.path = "/api/v1/admin/settings"
        request.method = "GET"
        request.headers = {}
        del request.state.user

        with patch("core.config.settings.supremeai_public_paths", ["/health"]):
            response = await middleware.dispatch(request, app)
            assert response.status_code == 401
            assert (
                response.body
                == b'{"detail":"Authentication required for this operation"}'
            )

    @pytest.mark.asyncio
    async def test_otp_from_headers(self):
        app = AsyncMock()
        middleware = AutonoGuardMiddleware(app)
        middleware._initialized = True
        request = MagicMock()
        request.url.path = "/api/v1/admin/deploy"
        request.method = "POST"
        request.headers = {"X-JIT-OTP": "654321"}
        request.state.user = {"sub": "admin-1"}
        request.body = AsyncMock(return_value=b"{}")

        with patch("core.config.settings.supremeai_public_paths", ["/health"]):
            with patch(
                "core.security.autonoguard_middleware.autonoguard_engine"
            ) as mock_engine:
                mock_engine.enforce_operation = AsyncMock(return_value=(True, None))
                await middleware.dispatch(request, app)
                assert mock_engine.enforce_operation.call_count == 1
                call_kwargs = mock_engine.enforce_operation.call_args.kwargs
                assert call_kwargs["otp_code"] == "654321"

    @pytest.mark.asyncio
    async def test_otp_from_x_otp_header(self):
        app = AsyncMock()
        middleware = AutonoGuardMiddleware(app)
        middleware._initialized = True
        request = MagicMock()
        request.url.path = "/api/v1/admin/config"
        request.method = "POST"
        request.headers = {"X-OTP": "111222"}
        request.state.user = {"sub": "admin-1"}
        request.body = AsyncMock(return_value=b"{}")

        with patch("core.config.settings.supremeai_public_paths", ["/health"]):
            with patch(
                "core.security.autonoguard_middleware.autonoguard_engine"
            ) as mock_engine:
                mock_engine.enforce_operation = AsyncMock(return_value=(True, None))
                await middleware.dispatch(request, app)
                assert mock_engine.enforce_operation.call_count == 1
                call_kwargs = mock_engine.enforce_operation.call_args.kwargs
                assert call_kwargs["otp_code"] == "111222"
