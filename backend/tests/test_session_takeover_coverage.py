"""Tests to improve coverage for session_takeover route (32.4% -> target 60%)."""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from fastapi import HTTPException


class TestSessionTakeover:
    """Tests for session_takeover endpoints."""

    def test_request_takeover_success(self):
        """Valid request should create takeover token."""
        from api.routes.session_takeover import (TakeoverRequest,
                                                 request_takeover)

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "admin-user", "role": "admin"}

        payload = TakeoverRequest(session_id="session-123")

        with patch(
            "api.routes.session_takeover.secrets.token_urlsafe",
            return_value="takeover-token-xyz",
        ):
            with patch(
                "api.routes.session_takeover._redis_client",
                new=AsyncMock(return_value=MagicMock()),
            ):
                result = request_takeover(payload, mock_request)

        assert "token" in result
        assert "expires_in" in result

    def test_request_takeover_unauthorized(self):
        """Non-admin should raise 403."""
        from api.routes.session_takeover import (TakeoverRequest,
                                                 request_takeover)

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "user", "role": "user"}

        payload = TakeoverRequest(session_id="session-123")

        with pytest.raises(HTTPException) as exc_info:
            request_takeover(payload, mock_request)

        assert exc_info.value.status_code == 403

    def test_request_takeover_no_auth(self):
        """Unauthenticated should raise 401."""
        from api.routes.session_takeover import (TakeoverRequest,
                                                 request_takeover)

        mock_request = MagicMock()
        mock_request.state.user = None

        payload = TakeoverRequest(session_id="session-123")

        with pytest.raises(HTTPException) as exc_info:
            request_takeover(payload, mock_request)

        assert exc_info.value.status_code == 401

    def test_release_takeover_success(self):
        """Valid release should free the session."""
        from api.routes.session_takeover import release_takeover

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "admin-user", "role": "admin"}

        with patch(
            "api.routes.session_takeover._redis_client",
            new=AsyncMock(return_value=MagicMock()),
        ):
            result = release_takeover("session-123", mock_request)

        assert result["status"] == "released"

    def test_release_takeover_unauthorized(self):
        """Non-admin should raise 403."""
        from api.routes.session_takeover import release_takeover

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "user", "role": "user"}

        with pytest.raises(HTTPException) as exc_info:
            release_takeover("session-123", mock_request)

        assert exc_info.value.status_code == 403

    def test_get_takeover_status_active(self):
        """Active takeover should return status."""
        from api.routes.session_takeover import get_takeover_status

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "admin-user", "role": "admin"}

        mock_redis = MagicMock()
        mock_redis.get = AsyncMock(
            return_value=b'{"admin": "admin-user", "started_at": 1000}'
        )

        with patch(
            "api.routes.session_takeover._redis_client",
            new=AsyncMock(return_value=mock_redis),
        ):
            result = get_takeover_status("session-123", mock_request)

        assert result["status"] == "active"

    def test_get_takeover_status_inactive(self):
        """Inactive takeover should return inactive status."""
        from api.routes.session_takeover import get_takeover_status

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "admin-user", "role": "admin"}

        mock_redis = MagicMock()
        mock_redis.get = AsyncMock(return_value=None)

        with patch(
            "api.routes.session_takeover._redis_client",
            new=AsyncMock(return_value=mock_redis),
        ):
            result = get_takeover_status("session-123", mock_request)

        assert result["status"] == "inactive"
