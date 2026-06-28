import os
from unittest.mock import MagicMock

from fastapi.testclient import TestClient


os.environ["OPENROUTER_API_KEY"] = "mock-key-value"
os.environ["SUPREMEAI_API_TOKEN"] = "test-token"
from backend.api.routes import config as config_route
from core.app import app


auth_headers = {"Authorization": "Bearer test-token"}
client = TestClient(app)

import pytest


@pytest.fixture(autouse=True)
def setup_token():
    os.environ["SUPREMEAI_API_TOKEN"] = "test-token"
    yield


def test_api_email_endpoints():
    # test /integrations/email/gmail
    resp = client.post(
        "/integrations/email/gmail",
        json={
            "provider": "gmail",
            "scopes": ["https://www.googleapis.com/auth/gmail.modify"],
        },
        headers=auth_headers,
    )
    assert resp.status_code == 200
    assert resp.json()["status"] == "success"

    # test /integrations/email/imap
    resp2 = client.post(
        "/integrations/email/imap",
        json={
            "host": "imap.gmail.com",
            "port": 993,
            "username": "supremeai@paykaribazar.com",
            "app_password": "secret_password",
        },
        headers=auth_headers,
    )
    assert resp2.status_code == 200
    assert resp2.json()["status"] == "success"


def test_api_github_endpoints():
    # test /github/connect
    resp = client.post(
        "/github/connect",
        json={
            "installation_id": "from_github_app",
            "repo_owner": "owner",
            "repo_name": "repo",
        },
        headers=auth_headers,
    )
    assert resp.status_code == 200
    assert "Connected to owner/repo" in resp.json()["message"]

    # test /github/improve
    resp = client.post(
        "/github/improve",
        json={"repo": "owner/repo", "branch": "main", "improvement_type": "refactor"},
        headers=auth_headers,
    )
    assert resp.status_code == 200
    assert resp.json()["analysis"]["score"] == 85

    # test /github/push
    resp = client.post(
        "/github/push",
        json={
            "repo": "owner/repo",
            "branch": "supremeai-improvements-1718952000",
            "commit_message": "AI: Optimized database queries",
            "files_changed": ["src/db.py", "src/cache.py"],
        },
        headers=auth_headers,
    )
    assert resp.status_code == 200
    assert resp.json()["status"] == "success"

    # test /github/discover
    resp = client.post(
        "/github/discover",
        json={
            "requirement": "React component library for data tables",
            "tech_stack": ["React", "TypeScript"],
            "criteria": {"min_stars": 500},
        },
        headers=auth_headers,
    )
    assert resp.status_code == 200
    assert resp.json()["status"] == "success"

    # test /github/implement
    resp = client.post(
        "/github/implement",
        json={
            "repo_url": "https://github.com/TanStack/table",
            "integration_method": "npm",
            "target_project": "customer-ecommerce-app",
        },
        headers=auth_headers,
    )
    assert resp.status_code == 200
    assert resp.json()["status"] == "success"


def test_api_marketplace_endpoints():
    # test /marketplace/search
    resp = client.post(
        "/marketplace/search",
        json={"query": "pdf", "categories": ["npm"], "filters": {"min_stars": 100}},
        headers=auth_headers,
    )
    assert resp.status_code == 200
    # Search endpoint returns a list of tools
    assert isinstance(resp.json(), dict)
    assert resp.json()["status"] == "success"
    assert isinstance(resp.json()["tools"], list)

    # test /marketplace/install
    resp = client.post(
        "/marketplace/install",
        json={
            "tool_id": "npm:pdf-parse",
            "target_environment": "supremeai-worker-01",
            "sandbox": True,
        },
        headers=auth_headers,
    )
    assert resp.status_code == 200
    assert resp.json()["success"] is True
    assert resp.json()["status"] == "verified_and_installed"


def test_config_endpoint_admin_control(monkeypatch):
    monkeypatch.setattr(config_route.db, "client", MagicMock())
    monkeypatch.setattr(
        config_route.db,
        "get_config",
        lambda key: (
            ["awesome-selfhosted", "libraries.io"]
            if key == "marketplace.resource_sources"
            else None
        ),
    )
    monkeypatch.setattr(
        config_route.db, "set_config", lambda key, value, category="general": None
    )

    resp = client.get("/config/marketplace.resource_sources", headers=auth_headers)
    assert resp.status_code == 200
    assert resp.json()["key"] == "marketplace.resource_sources"
    assert resp.json()["value"] == ["awesome-selfhosted", "libraries.io"]

    resp = client.put(
        "/config/marketplace.resource_sources",
        json=["awesome-python", "ossinsight"],
        headers=auth_headers,
    )
    assert resp.status_code == 200
    assert resp.json()["status"] == "success"
