"""Tests to improve coverage for websocket_voice route (15.9% -> target 60%)."""

from unittest.mock import AsyncMock, patch

import pytest


class TestVoiceConnectionManager:
    """Tests for VoiceConnectionManager class."""

    @pytest.mark.asyncio
    async def test_connect(self):
        """connect should accept websocket and add to active list."""
        from api.routes.websocket_voice import VoiceConnectionManager

        mgr = VoiceConnectionManager()
        ws = AsyncMock()
        ws.query_params = {"token": "test-token"}

        await mgr.connect(ws)
        assert ws in mgr.active_connections
        mgr.disconnect(ws)

    @pytest.mark.asyncio
    async def test_disconnect(self):
        """disconnect should remove websocket from active list."""
        from api.routes.websocket_voice import VoiceConnectionManager

        mgr = VoiceConnectionManager()
        ws = AsyncMock()
        await mgr.connect(ws)
        assert ws in mgr.active_connections
        mgr.disconnect(ws)
        assert ws not in mgr.active_connections

    @pytest.mark.asyncio
    async def test_disconnect_not_in_list(self):
        """disconnect should not fail if ws not in active list."""
        from api.routes.websocket_voice import VoiceConnectionManager

        mgr = VoiceConnectionManager()
        ws = AsyncMock()
        mgr.disconnect(ws)  # Should not raise

    @pytest.mark.asyncio
    async def test_authenticate_no_token(self):
        """Missing token should return None (no close when no query_params)."""
        from api.routes.websocket_voice import VoiceConnectionManager

        mgr = VoiceConnectionManager()
        ws = AsyncMock()
        ws.query_params = {}
        result = await mgr._authenticate(ws)
        assert result is None

    @pytest.mark.asyncio
    async def test_authenticate_valid_token(self):
        """Valid token should return payload."""
        from api.routes.websocket_voice import VoiceConnectionManager

        mgr = VoiceConnectionManager()
        ws = AsyncMock()
        ws.query_params = {"token": "good-token"}

        with patch("api.routes.websocket_voice.verify_token", return_value={"sub": "test-user"}):
            result = await mgr._authenticate(ws)

        assert result == {"sub": "test-user"}

    @pytest.mark.asyncio
    async def test_authenticate_invalid_token(self):
        """Invalid token should return None."""
        from api.routes.websocket_voice import VoiceConnectionManager

        mgr = VoiceConnectionManager()
        ws = AsyncMock()
        ws.query_params = {"token": "bad-token"}

        with patch("api.routes.websocket_voice.verify_token", side_effect=Exception("Invalid")):
            result = await mgr._authenticate(ws)

        assert result is None
