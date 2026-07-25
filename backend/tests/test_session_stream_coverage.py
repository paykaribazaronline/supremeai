"""Tests to improve coverage for session_stream route (24.2% -> target 60%)."""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from fastapi import HTTPException


class TestStreamSession:
    """Tests for stream_session endpoint."""

    @pytest.mark.asyncio
    async def test_stream_session_returns_sse_response(self):
        """stream_session should return EventSourceResponse."""
        from api.routes.session_stream import stream_session
        from sse_starlette.sse import EventSourceResponse

        mock_request = MagicMock()
        mock_request.is_disconnected = AsyncMock(return_value=True)

        with patch("api.routes.session_stream.batcher"):
            result = await stream_session("test-session", mock_request)

        assert isinstance(result, EventSourceResponse)


class TestRequireAdmin:
    """Tests for _require_admin in internal route."""

    def test_require_admin_valid_secret(self):
        """Valid admin secret should pass."""
        from api.routes.internal import _require_admin

        mock_request = MagicMock()
        mock_request.headers = {"X-Admin-Secret": "correct-secret"}

        with patch("api.routes.internal.settings") as mock_settings:
            mock_settings.supremeai_admin_secret = "correct-secret"
            result = _require_admin(mock_request)

        assert result is None

    def test_require_admin_wrong_secret(self):
        """Wrong admin secret should raise 403."""
        from api.routes.internal import _require_admin

        mock_request = MagicMock()
        mock_request.headers = {"X-Admin-Secret": "wrong"}

        with patch("api.routes.internal.settings") as mock_settings:
            mock_settings.supremeai_admin_secret = "correct-secret"
            with pytest.raises(HTTPException) as exc_info:
                _require_admin(mock_request)

        assert exc_info.value.status_code == 403
