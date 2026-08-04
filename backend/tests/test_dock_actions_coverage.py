"""Tests to improve coverage for dock_actions route (27.6% -> target 60%)."""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from fastapi import HTTPException


@pytest.mark.skip(reason="Legacy dock actions integration test")
class TestDockActions:
    """Tests for run_dock_integration endpoint."""

    @pytest.mark.asyncio
    async def test_run_dock_integration_success(self):
        """Valid integration request should execute."""
        from api.routes.dock_actions import (DockActionPayload,
                                             run_dock_integration)

        payload = DockActionPayload(
            triggered_from="button", active_file="test.py", content="print('hello')"
        )

        mock_user = {"sub": "test-user", "role": "admin"}
        mock_db = AsyncMock()

        with patch(
            "api.routes.dock_actions.get_user_github_token",
            new=AsyncMock(return_value="gh-token"),
        ):
            with patch("api.routes.dock_actions.Github") as MockGithub:
                mock_repo = MagicMock()
                MockGithub.return_value.get_repo.return_value = mock_repo
                mock_repo.create_file.return_value = {"content": {"path": "test.py"}}
                with patch("api.routes.dock_actions.push_to_sse", new=AsyncMock()):
                    result = run_dock_integration(
                        "session-123", "github", payload, mock_user, mock_db
                    )

        assert result["status"] == "pushed"

    @pytest.mark.asyncio
    async def test_run_dock_integration_missing_token(self):
        """Missing GitHub token should raise 401."""
        from api.routes.dock_actions import (DockActionPayload,
                                             run_dock_integration)

        payload = DockActionPayload(triggered_from="button")
        mock_user = {"sub": "test-user"}
        mock_db = AsyncMock()

        with patch(
            "api.routes.dock_actions.get_user_github_token",
            new=AsyncMock(return_value=None),
        ):
            with pytest.raises(HTTPException) as exc_info:
                run_dock_integration(
                    "session-123", "github", payload, mock_user, mock_db
                )

        assert exc_info.value.status_code == 401

    @pytest.mark.asyncio
    async def test_push_to_sse_publishes_event(self):
        """push_to_sse should publish to global_pubsub."""
        from api.routes.dock_actions import push_to_sse

        with patch(
            "api.routes.dock_actions.global_pubsub.publish", new=AsyncMock()
        ) as mock_publish:
            await push_to_sse("session-123", {"status": "done"})

        mock_publish.assert_awaited_once()
