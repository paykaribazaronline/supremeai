"""
Health Endpoint Tests — System Monitoring Validation
v4.0: Verifies /health endpoints work correctly
"""

from __future__ import annotations

import pytest
from unittest.mock import AsyncMock, patch


class TestHealthEndpoint:
    """Test /health endpoint responses."""

    @pytest.mark.unit
    async def test_health_returns_200_when_healthy(self, client):
        """Health endpoint returns 200 when all checks pass."""
        with patch('core.health._run_check') as mock_check:
            from core.health import HealthResult, HealthStatus

            mock_result = HealthResult(
                name="database",
                status=HealthStatus.HEALTHY,
                latency_ms=5.0,
            )
            mock_check.return_value = AsyncMock(return_value=mock_result)

            response = await client.get("/health")

            assert response.status_code == 200
            data = response.json()
            assert data["status"] == "healthy"
            assert "uptime_seconds" in data
            assert "checks" in data

    @pytest.mark.unit
    async def test_health_returns_503_when_unhealthy(self, client):
        """Health endpoint returns 503 when critical check fails."""
        with patch('core.health._run_check') as mock_check:
            from core.health import HealthResult, HealthStatus

            mock_result = HealthResult(
                name="database",
                status=HealthStatus.UNHEALTHY,
                latency_ms=5000.0,
                error="Connection refused",
                critical=True,
            )
            mock_check.return_value = AsyncMock(return_value=mock_result)

            response = await client.get("/health")

            assert response.status_code == 503
            data = response.json()
            assert data["status"] == "unhealthy"

    @pytest.mark.unit
    async def test_readiness_probe(self, client):
        """Readiness probe checks critical services only."""
        response = await client.get("/health/ready")

        assert response.status_code in (200, 503)
        data = response.json()
        assert "status" in data
        assert data["status"] in ("ready", "not_ready")

    @pytest.mark.unit
    async def test_liveness_probe(self, client):
        """Liveness probe confirms process is alive."""
        response = await client.get("/health/live")

        assert response.status_code == 200
        data = response.json()
        assert data["alive"] is True
        assert "timestamp" in data

    @pytest.mark.unit
    async def test_root_endpoint_includes_health_link(self, client):
        """Root endpoint should include health check URL."""
        response = await client.get("/")

        assert response.status_code == 200
        data = response.json()
        assert "health_check" in data
        assert data["health_check"] == "/health"


class TestConfigPublicEndpoint:
    """Test public configuration endpoint."""

    @pytest.mark.unit
    async def test_config_public_accessible_without_auth(self, client):
        """Public config should be accessible without auth."""
        response = await client.get("/api/config/public")

        assert response.status_code == 200
        data = response.json()
        assert "features" in data
        assert "version" in data

    @pytest.mark.unit
    async def test_config_public_no_secrets(self, client):
        """Public config must not expose secrets."""
        response = await client.get("/api/config/public")

        assert response.status_code == 200
        data = response.json()

        # Ensure no sensitive keys leaked
        sensitive_keys = ["jwt_secret", "api_key", "password", "database_url"]
        for key in sensitive_keys:
            assert key not in data, f"Sensitive key '{key}' exposed in public config"


# -----------------------------------------------------------------------------
# FILE 11: playwright.config.ts — Stable E2E Config
# -----------------------------------------------------------------------------
