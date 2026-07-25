"""
Tests for core/agent_factory.py — DynamicAgentFactory
"""

from __future__ import annotations

import json
from pathlib import Path
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from core.agent_factory import DynamicAgentFactory


@pytest.fixture
def mock_db_session():
    return AsyncMock()


@pytest.fixture
def factory(mock_db_session):
    return DynamicAgentFactory(db_session=mock_db_session)


def test_get_registered_agent_found(factory, tmp_path):
    """Test get_registered_agent returns agent config when found in registry."""
    agent_data = {"name": "test-agent", "description": "A test agent"}
    registry_path = (
        Path(__file__).resolve().parent.parent / "core" / "agent_registry.json"
    )

    # Create a temporary registry
    test_registry = {"test-agent": agent_data}
    with patch.object(Path, "exists", return_value=True), patch(
        "builtins.open", MagicMock()
    ) as mock_open:
        mock_file = MagicMock()
        mock_file.__enter__.return_value.read.return_value = json.dumps(test_registry)
        mock_open.return_value = mock_file

        result = factory.get_registered_agent("test-agent")
        assert result == agent_data


def test_get_registered_agent_not_found(factory):
    """Test get_registered_agent returns None when agent not in registry."""
    with patch.object(Path, "exists", return_value=True), patch(
        "builtins.open", MagicMock()
    ) as mock_open:
        mock_file = MagicMock()
        mock_file.__enter__.return_value.read.return_value = json.dumps({})
        mock_open.return_value = mock_file

        result = factory.get_registered_agent("nonexistent")
        assert result is None


def test_get_registered_agent_no_registry(factory):
    """Test get_registered_agent returns None when registry file doesn't exist."""
    with patch.object(Path, "exists", return_value=False):
        result = factory.get_registered_agent("test-agent")
        assert result is None


def test_get_registered_agent_bad_json(factory):
    """Test get_registered_agent returns None on JSON parse error."""
    with patch.object(Path, "exists", return_value=True), patch(
        "builtins.open", MagicMock()
    ) as mock_open:
        mock_file = MagicMock()
        mock_file.__enter__.return_value.read.return_value = "invalid json{{{"
        mock_open.return_value = mock_file

        result = factory.get_registered_agent("test-agent")
        assert result is None


@pytest.mark.asyncio
async def test_create_specialized_agent_success(factory, mock_db_session):
    """Test create_specialized_agent successfully creates an agent."""
    mock_response = {
        "text": json.dumps(
            {
                "agent_name": "test_agent_123",
                "description": "Solve a test task",
                "script": "print('hello world')",
            }
        )
    }

    with patch("core.agent_factory.llm_gateway") as mock_llm:
        mock_llm.acompletion = AsyncMock(return_value=mock_response)

        # Mock DB operations
        mock_result = MagicMock()
        mock_result.scalars.return_value.first.return_value = None
        mock_db_session.execute.return_value = mock_result

        result = await factory.create_specialized_agent("Solve a test task")

        assert result["agent_name"] == "test_agent_123"
        assert result["description"] == "Solve a test task"
        assert "script" in result
        mock_db_session.add.assert_called_once()
        mock_db_session.commit.assert_called_once()


@pytest.mark.asyncio
async def test_create_specialized_agent_parse_fallback(factory, mock_db_session):
    """Test create_specialized_agent falls back when JSON parsing fails."""
    mock_response = {"text": "not valid json at all"}

    with patch("core.agent_factory.llm_gateway") as mock_llm:
        mock_llm.acompletion = AsyncMock(return_value=mock_response)

        mock_result = MagicMock()
        mock_result.scalars.return_value.first.return_value = None
        mock_db_session.execute.return_value = mock_result

        result = await factory.create_specialized_agent("Solve a test task")

        assert "agent_name" in result
        assert "AutoAgent_" in result["agent_name"]
        mock_db_session.add.assert_called_once()
        mock_db_session.commit.assert_called_once()


@pytest.mark.asyncio
async def test_create_specialized_agent_db_rollback(factory, mock_db_session):
    """Test create_specialized_agent handles DB errors gracefully."""
    mock_response = {
        "text": json.dumps(
            {
                "agent_name": "test_agent",
                "description": "Test",
                "script": "print('hello')",
            }
        )
    }

    with patch("core.agent_factory.llm_gateway") as mock_llm:
        mock_llm.acompletion = AsyncMock(return_value=mock_response)
        mock_db_session.commit.side_effect = Exception("DB error")

        result = await factory.create_specialized_agent("Solve a test task")

        assert result["agent_name"] == "test_agent"
        mock_db_session.rollback.assert_called_once()


@pytest.mark.asyncio
async def test_save_agent_to_registry_existing(factory, mock_db_session):
    """Test _save_agent_to_registry updates existing agent."""
    mock_agent = MagicMock()
    mock_result = MagicMock()
    mock_result.scalars.return_value.first.return_value = mock_agent
    mock_db_session.execute.return_value = mock_result

    await factory._save_agent_to_registry(
        name="existing_agent",
        description="Updated description",
        steps={"script": "print('updated')"},
    )

    assert mock_agent.execution_steps is not None
    assert mock_agent.description == "Updated description"
    mock_db_session.commit.assert_called_once()


@pytest.mark.asyncio
async def test_save_agent_to_registry_new(factory, mock_db_session):
    """Test _save_agent_to_registry creates new agent."""
    mock_result = MagicMock()
    mock_result.scalars.return_value.first.return_value = None
    mock_db_session.execute.return_value = mock_result

    await factory._save_agent_to_registry(
        name="new_agent",
        description="New agent",
        steps={"script": "print('new')"},
    )

    mock_db_session.add.assert_called_once()
    mock_db_session.commit.assert_called_once()


def test_factory_no_db_session():
    """Test factory can be created without a DB session."""
    factory = DynamicAgentFactory()
    assert factory.db is None
