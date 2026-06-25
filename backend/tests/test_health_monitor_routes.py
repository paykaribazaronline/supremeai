from __future__ import annotations

import pytest
from unittest.mock import patch

from fastapi.testclient import TestClient

from core.app import app


@pytest.fixture
def client():
    return TestClient(app)


def test_health_endpoint(client):
    resp = client.get("/health")
    assert resp.status_code == 200
    data = resp.json()
    assert "status" in data


def test_health_endpoint_returns_json(client):
    resp = client.get("/health")
    assert resp.headers.get("content-type") == "application/json"


def test_health_endpoint_keys(client):
    resp = client.get("/health")
    data = resp.json()
    assert "status" in data
    assert "orchestrator" in data
    assert "checks" in data


def test_health_endpoint_status_values(client):
    resp = client.get("/health")
    data = resp.json()
    assert data["status"] in ("ok", "degraded")
    assert data["orchestrator"] == "online"
    assert isinstance(data["checks"], dict)


def test_health_endpoint_degraded_status(client):
    with patch("core.app.settings") as mock_settings:
        mock_settings.openrouter_api_key = None
        mock_settings.gemini_api_key = None
        mock_settings.deepseek_api_key = None
        mock_settings.groq_api_key = None
        mock_settings.nvidia_api_key = None
        resp = client.get("/health")
    data = resp.json()
    assert data["status"] == "degraded"
