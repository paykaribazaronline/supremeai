"""Tests to improve coverage for websocket_agent route (15.5% -> target 60%)."""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest


class TestConnectionManager:
    """Tests for ConnectionManager class."""

    @pytest.mark.asyncio
    async def test_connect(self):
        """connect should accept websocket and add to active list."""
        from api.routes.websocket_agent import manager

        ws = AsyncMock()
        ws.query_params = {"token": "test-token"}
        await manager.connect(ws)
        assert ws in manager.active_connections
        manager.disconnect(ws)

    @pytest.mark.asyncio
    async def test_disconnect(self):
        """disconnect should remove websocket from active list."""
        from api.routes.websocket_agent import manager

        ws = AsyncMock()
        await manager.connect(ws)
        assert ws in manager.active_connections
        manager.disconnect(ws)
        assert ws not in manager.active_connections

    @pytest.mark.asyncio
    async def test_disconnect_not_in_list(self):
        """disconnect should not fail if ws not in active list."""
        from api.routes.websocket_agent import manager

        ws = AsyncMock()
        manager.disconnect(ws)  # Should not raise

    @pytest.mark.asyncio
    async def test_authenticate_no_token(self):
        """Missing token should close with policy violation."""
        from api.routes.websocket_agent import manager

        ws = AsyncMock()
        ws.query_params = {}
        result = await manager._authenticate(ws)
        assert result is None
        ws.close.assert_called_once()

    @pytest.mark.asyncio
    async def test_authenticate_valid_token(self):
        """Valid token should return payload."""
        from api.routes.websocket_agent import manager

        ws = AsyncMock()
        ws.query_params = {"token": "good-token"}

        with patch(
            "api.routes.websocket_agent.verify_token",
            return_value={"sub": "test-user", "role": "admin"},
        ):
            result = await manager._authenticate(ws)

        assert result == {"sub": "test-user", "role": "admin"}
        ws.close.assert_not_called()

    @pytest.mark.asyncio
    async def test_authenticate_invalid_token(self):
        """Invalid token should close with policy violation."""
        from api.routes.websocket_agent import manager

        ws = AsyncMock()
        ws.query_params = {"token": "bad-token"}

        with patch(
            "api.routes.websocket_agent.verify_token", side_effect=Exception("Invalid")
        ):
            result = await manager._authenticate(ws)

        assert result is None
        ws.close.assert_called_once()

    def test_track_pref_task(self):
        """track_pref_task should store task per user."""
        from api.routes.websocket_agent import manager

        task = AsyncMock()
        manager.track_pref_task("user-1", task)
        assert task in manager._pref_tasks.get("user-1", set())

    def test_cancel_pref_tasks(self):
        """cancel_pref_tasks should cancel and remove tasks."""
        from api.routes.websocket_agent import manager

        task = AsyncMock()
        task.cancel = MagicMock()
        manager.track_pref_task("user-1", task)
        manager.cancel_pref_tasks("user-1")
        task.cancel.assert_called_once()
        assert "user-1" not in manager._pref_tasks


class TestAnalyzeAndSavePreferences:
    """Tests for analyze_and_save_preferences."""

    @pytest.mark.asyncio
    async def test_analyze_preferences_success(self):
        """Happy path should analyze and save preferences."""
        from api.routes.websocket_agent import analyze_and_save_preferences

        mock_db = MagicMock()
        mock_db.get_user_preferences.return_value = {
            "preferences": {"answering_style": "default"}
        }

        mock_response = {"text": '{"answering_style": "direct code"}'}

        with patch("api.routes.websocket_agent.SupabaseDB", return_value=mock_db):
            with patch(
                "api.routes.websocket_agent.llm_gateway.acompletion",
                new=AsyncMock(return_value=mock_response),
            ):
                await analyze_and_save_preferences("user-1", "write code for me")

        mock_db.upsert_user_preferences.assert_called_once()

    @pytest.mark.asyncio
    async def test_analyze_preferences_llm_failure(self):
        """LLM failure should be caught gracefully."""
        from api.routes.websocket_agent import analyze_and_save_preferences

        mock_db = MagicMock()
        mock_db.get_user_preferences.return_value = None

        with patch("api.routes.websocket_agent.SupabaseDB", return_value=mock_db):
            with patch(
                "api.routes.websocket_agent.llm_gateway.acompletion",
                new=AsyncMock(side_effect=Exception("LLM down")),
            ):
                with patch("api.routes.websocket_agent.logger"):
                    await analyze_and_save_preferences("user-1", "write code")

        # Should not raise, just log warning
        mock_db.upsert_user_preferences.assert_not_called()
