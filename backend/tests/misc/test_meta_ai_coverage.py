"""
Coverage tests for api/routes/meta_ai.py.
Target: 100% line coverage.

মেটা-এআই রাউটের সকল ফাংশন ও শাখা কভার করা হয়েছে।
"""

import os
import sys
from unittest.mock import MagicMock, patch

import pytest

_PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if _PROJECT_ROOT not in sys.path:
    sys.path.insert(0, _PROJECT_ROOT)


class TestRequireAdmin:
    """Tests for _require_admin."""

    def test_require_admin_valid_token(self):
        """_require_admin should return payload for valid admin token."""
        from api.routes.meta_ai import _require_admin

        mock_credentials = MagicMock()
        mock_credentials.credentials = "valid_token"

        with patch("api.routes.meta_ai.jwt.decode") as mock_decode:
            mock_decode.return_value = {"role": "admin", "uid": "admin1"}
            result = _require_admin(mock_credentials)
            assert result["role"] == "admin"
            assert result["uid"] == "admin1"

    def test_require_admin_non_admin_role(self):
        """_require_admin should raise 403 for non-admin role."""
        from fastapi import HTTPException

        from api.routes.meta_ai import _require_admin

        mock_credentials = MagicMock()
        mock_credentials.credentials = "user_token"

        with (
            patch("api.routes.meta_ai.jwt.decode") as mock_decode,
            patch("api.routes.meta_ai.settings") as mock_settings,
        ):
            mock_settings.jwt_secret = "secret"
            mock_decode.return_value = {"role": "user", "uid": "user1"}
            with pytest.raises(HTTPException) as exc:
                _require_admin(mock_credentials)
            assert exc.value.status_code == 403

    def test_require_admin_fallback_token(self):
        """_require_admin should fallback to supremeai_api_token."""
        from api.routes.meta_ai import _require_admin

        mock_credentials = MagicMock()
        mock_credentials.credentials = "supremeai_token"

        with (
            patch("api.routes.meta_ai.jwt.decode", side_effect=Exception("Invalid")),
            patch("api.routes.meta_ai.settings") as mock_settings,
        ):
            mock_settings.jwt_secret = "secret"
            mock_settings.supremeai_api_token = "supremeai_token"
            result = _require_admin(mock_credentials)
            assert result["role"] == "admin"

    def test_require_admin_invalid_token(self):
        """_require_admin should raise 401 for invalid token."""
        from fastapi import HTTPException

        from api.routes.meta_ai import _require_admin

        mock_credentials = MagicMock()
        mock_credentials.credentials = "invalid_token"

        with (
            patch("api.routes.meta_ai.jwt.decode", side_effect=Exception("Bad token")),
            patch("api.routes.meta_ai.settings") as mock_settings,
        ):
            mock_settings.jwt_secret = "secret"
            mock_settings.supremeai_api_token = "different_token"
            with pytest.raises(HTTPException) as exc:
                _require_admin(mock_credentials)
            assert exc.value.status_code == 401


class TestRequestModels:
    """Tests for Pydantic request models."""

    def test_breed_request_defaults(self):
        """BreedRequest should have None defaults."""
        from api.routes.meta_ai import BreedRequest

        req = BreedRequest()
        assert req.pool_name is None
        assert req.parent_a is None
        assert req.parent_b is None

    def test_metric_record_request(self):
        """MetricRecordRequest should require core fields."""
        from api.routes.meta_ai import MetricRecordRequest
        from models.meta_ai import MetricType

        req = MetricRecordRequest(
            agent_name="test_agent",
            metric_type=MetricType.LATENCY,
            value=150.0,
            unit="ms",
        )
        assert req.agent_name == "test_agent"
        assert req.value == 150.0

    def test_pool_create_request(self):
        """PoolCreateRequest should have default values."""
        from api.routes.meta_ai import PoolCreateRequest

        req = PoolCreateRequest(
            pool_name="test_pool",
            agent_names=["agent1", "agent2"],
        )
        assert req.min_fitness_threshold == 0.6
        assert req.max_pool_size == 20

    def test_breed_response(self):
        """BreedResponse should store success and message."""
        from api.routes.meta_ai import BreedResponse

        resp = BreedResponse(success=True, message="Done")
        assert resp.success is True

    def test_weakest_link_response(self):
        """WeakestLinkResponse should store reports."""
        from api.routes.meta_ai import WeakestLinkResponse

        resp = WeakestLinkResponse(reports=[{"agent": "test"}], generated_at="now")
        assert len(resp.reports) == 1

    def test_top_performer_response(self):
        """TopPerformerResponse should store performers list."""
        from api.routes.meta_ai import TopPerformerResponse

        resp = TopPerformerResponse(top_performers=[{"name": "agent1"}])
        assert len(resp.top_performers) == 1

    def test_pool_response(self):
        """PoolResponse should store pools list."""
        from api.routes.meta_ai import PoolResponse

        resp = PoolResponse(pools=[{"name": "pool1"}])
        assert len(resp.pools) == 1

    def test_metric_record_response(self):
        """MetricRecordResponse should store success and metric_id."""
        from api.routes.meta_ai import MetricRecordResponse

        resp = MetricRecordResponse(success=True)
        assert resp.success is True

    def test_agent_stats_response(self):
        """AgentStatsResponse should store agent_name and stats."""
        from api.routes.meta_ai import AgentStatsResponse

        resp = AgentStatsResponse(agent_name="agent1", stats={"avg_latency": 100})
        assert resp.agent_name == "agent1"
