import pytest
from fastapi.testclient import TestClient
from core.app import app

client = TestClient(app)

def test_markdown_export_endpoint():
    response = client.post("/api/v1/markdown/export", json={
        "root_dir": ".",
        "git_diff_only": False
    })
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert "markdown" in data
    assert "# 📄 SupremeAI 2.0 Codebase Export" in data["markdown"]

def test_markdown_export_diff_only():
    response = client.post("/api/v1/markdown/export", json={
        "root_dir": ".",
        "git_diff_only": True,
        "time_since": "2026-06-20"
    })
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert "markdown" in data
