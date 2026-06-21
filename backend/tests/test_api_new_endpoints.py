from fastapi.testclient import TestClient
import os

os.environ["OPENROUTER_API_KEY"] = "mock-key-value"
os.environ["SUPREMEAI_API_TOKEN"] = "test-token"
from core.app import app

auth_headers = {"Authorization": "Bearer test-token"}
client = TestClient(app)

def test_api_email_endpoints():
    # test /integrations/email/gmail
    resp = client.post(
        "/integrations/email/gmail",
        json={"provider": "gmail", "scopes": ["https://www.googleapis.com/auth/gmail.modify"]},
        headers=auth_headers
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
            "app_password": "secret_password"
        },
        headers=auth_headers
    )
    assert resp2.status_code == 200
    assert resp2.json()["status"] == "success"

def test_api_github_endpoints():
    # test /github/connect
    resp = client.post(
        "/github/connect",
        json={"installation_id": "from_github_app", "repo_owner": "owner", "repo_name": "repo"},
        headers=auth_headers
    )
    assert resp.status_code == 200
    assert "Connected to owner/repo" in resp.json()["message"]

    # test /github/improve
    resp = client.post(
        "/github/improve",
        json={"repo": "owner/repo", "branch": "main", "improvement_type": "refactor"},
        headers=auth_headers
    )
    assert resp.status_code == 200
    assert resp.json()["analysis"]["score"] == 85

    # test /github/push
    resp = client.post(
        "/github/push",
        json={
            "branch": "supremeai-improvements-1718952000",
            "commit_message": "AI: Optimized database queries",
            "files_changed": ["src/db.py", "src/cache.py"]
        },
        headers=auth_headers
    )
    assert resp.status_code == 200
    assert resp.json()["status"] == "success"

    # test /github/discover
    resp = client.post(
        "/github/discover",
        json={
            "requirement": "React component library for data tables",
            "tech_stack": ["React", "TypeScript"],
            "criteria": {"min_stars": 500}
        },
        headers=auth_headers
    )
    assert resp.status_code == 200
    assert resp.json()["status"] == "success"

    # test /github/implement
    resp = client.post(
        "/github/implement",
        json={
            "repo_url": "https://github.com/TanStack/table",
            "integration_method": "npm",
            "target_project": "customer-ecommerce-app"
        },
        headers=auth_headers
    )
    assert resp.status_code == 200
    assert resp.json()["status"] == "success"

def test_api_marketplace_endpoints():
    # test /marketplace/search
    resp = client.post(
        "/marketplace/search",
        json={"query": "pdf", "categories": ["npm"], "filters": {"min_stars": 100}},
        headers=auth_headers
    )
    assert resp.status_code == 200
    assert resp.json()["status"] == "success"

    # test /marketplace/install
    resp = client.post(
        "/marketplace/install",
        json={
            "tool_id": "npm:pdf-parse",
            "target_environment": "supremeai-worker-01",
            "sandbox": True
        },
        headers=auth_headers
    )
    assert resp.status_code == 200
    assert resp.json()["success"] is True
