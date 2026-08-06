"""Tests to improve coverage for websocket_hitl route (18.5% -> target 60%)."""

from unittest.mock import AsyncMock

import pytest


class TestHITLConnectionManager:
    """Tests for HITLConnectionManager class."""

    @pytest.mark.asyncio
    async def test_connect(self):
        """connect should accept websocket and add to active set."""
        from api.routes.websocket_hitl import HITLConnectionManager

        mgr = HITLConnectionManager()
        ws = AsyncMock()

        await mgr.connect(ws)
        assert ws in mgr.active_connections
        ws.accept.assert_awaited_once()

    @pytest.mark.asyncio
    async def test_disconnect(self):
        """disconnect should remove websocket from active set."""
        from api.routes.websocket_hitl import HITLConnectionManager

        mgr = HITLConnectionManager()
        ws = AsyncMock()
        await mgr.connect(ws)
        assert ws in mgr.active_connections

        await mgr.disconnect(ws)
        assert ws not in mgr.active_connections

    @pytest.mark.asyncio
    async def test_disconnect_not_in_set(self):
        """disconnect should not fail if ws not in active set."""
        from api.routes.websocket_hitl import HITLConnectionManager

        mgr = HITLConnectionManager()
        ws = AsyncMock()
        await mgr.disconnect(ws)  # Should not raise

    @pytest.mark.asyncio
    async def test_broadcast_sends_to_all(self):
        """broadcast should send message to all active connections."""
        from api.routes.websocket_hitl import HITLConnectionManager

        mgr = HITLConnectionManager()
        ws1 = AsyncMock()
        ws2 = AsyncMock()
        await mgr.connect(ws1)
        await mgr.connect(ws2)

        await mgr.broadcast("test message")

        ws1.send_text.assert_awaited_once_with("test message")
        ws2.send_text.assert_awaited_once_with("test message")

    @pytest.mark.asyncio
    async def test_broadcast_handles_disconnect_error(self):
        """broadcast should handle WebSocketDisconnect and remove failed connections."""
        from api.routes.websocket_hitl import HITLConnectionManager
        from fastapi import WebSocketDisconnect

        mgr = HITLConnectionManager()
        ws1 = AsyncMock()
        ws2 = AsyncMock()
        ws2.send_text = AsyncMock(side_effect=WebSocketDisconnect)
        await mgr.connect(ws1)
        await mgr.connect(ws2)

        await mgr.broadcast("test")

        assert ws1 in mgr.active_connections
        assert ws2 not in mgr.active_connections
