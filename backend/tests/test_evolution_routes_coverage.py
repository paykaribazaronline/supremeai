"""Tests to improve coverage for evolution route (32.1% -> target 60%)."""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from fastapi import HTTPException


class TestQuarantineSkill:
    """Tests for quarantine_skill endpoint."""

    def test_quarantine_skill_success(self):
        """Admin can quarantine a skill."""
        from api.routes.evolution import QuarantineRequest, quarantine_skill

        mock_admin = {"uid": "admin", "role": "admin"}
        payload = QuarantineRequest(skill_name="bad_skill")

        fake_registry = MagicMock()
        fake_registry.get_skill.return_value = {"name": "bad_skill", "status": "active"}
        fake_fitness = MagicMock()
        fake_fitness.registry = fake_registry

        with patch(
            "api.routes.evolution.get_fitness_engine", return_value=fake_fitness
        ):
            with patch("api.routes.evolution.time"):
                with patch("api.routes.evolution.datetime") as mock_dt:
                    mock_dt.now.return_value.isoformat.return_value = (
                        "2026-01-01T00:00:00"
                    )
                    response = quarantine_skill(payload=payload, admin=mock_admin)

        assert response["status"] == "quarantined"

    def test_quarantine_skill_not_found(self):
        """Quarantine unknown skill should raise 404."""
        from api.routes.evolution import QuarantineRequest, quarantine_skill

        mock_admin = {"uid": "admin", "role": "admin"}
        payload = QuarantineRequest(skill_name="missing_skill")

        fake_registry = MagicMock()
        fake_registry.get_skill.return_value = None
        fake_fitness = MagicMock()
        fake_fitness.registry = fake_registry

        with patch(
            "api.routes.evolution.get_fitness_engine", return_value=fake_fitness
        ):
            with pytest.raises(HTTPException) as exc_info:
                quarantine_skill(payload=payload, admin=mock_admin)

        assert exc_info.value.status_code == 404


class TestGetSwarmGraph:
    """Tests for get_swarm_graph endpoint."""

    def test_get_swarm_graph_returns_graph(self):
        """Should return current swarm graph."""
        from api.routes.evolution import get_swarm_graph

        response = get_swarm_graph()
        assert "nodes" in response
        assert "edges" in response


class TestForgeDynamicSkill:
    """Tests for forge_dynamic_skill endpoint."""

    def test_forge_skill_success(self):
        """Valid request should forge a skill."""
        from api.routes.evolution import EvolutionRequest, forge_dynamic_skill

        payload = EvolutionRequest(
            skill_name="test_skill", user_demand="create a calculator"
        )
        mock_db = MagicMock()

        with patch("api.routes.evolution.AutoSkillCreator") as MockCreator:
            mock_creator = MagicMock()
            mock_creator.generate_and_deploy_skill = AsyncMock(
                return_value={"success": True, "skill": "test_skill"}
            )
            MockCreator.return_value = mock_creator
            response = forge_dynamic_skill(payload, mock_db)

        assert response["success"] is True

    def test_forge_skill_failure(self):
        """Failed skill creation should raise 400."""
        from api.routes.evolution import EvolutionRequest, forge_dynamic_skill

        payload = EvolutionRequest(
            skill_name="test_skill", user_demand="create a calculator"
        )
        mock_db = MagicMock()

        with patch("api.routes.evolution.AutoSkillCreator") as MockCreator:
            mock_creator = MagicMock()
            mock_creator.generate_and_deploy_skill = AsyncMock(
                return_value={"success": False, "error": "Failed"}
            )
            MockCreator.return_value = mock_creator
            with pytest.raises(HTTPException) as exc_info:
                forge_dynamic_skill(payload, mock_db)

        assert exc_info.value.status_code == 400


class TestGetEvolutionLogs:
    """Tests for get_evolution_logs endpoint."""

    def test_get_evolution_logs_returns_logs(self):
        """Should return evolution logs."""
        from api.routes.evolution import get_evolution_logs

        with patch("api.routes.evolution.Path") as MockPath:
            mock_path = MagicMock()
            mock_path.exists.return_value = True
            mock_path.read_text.return_value = '{"log": "test"}\n'
            MockPath.return_value = mock_path
            MockPath.__truediv__ = lambda self, other: mock_path

            with patch("api.routes.evolution.Path.__truediv__", return_value=mock_path):
                response = get_evolution_logs()

        assert "logs" in response
