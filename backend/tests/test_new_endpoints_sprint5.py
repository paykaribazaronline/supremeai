import os
import sys
import pytest
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
if ROOT not in sys.path:
    sys.path.insert(0, ROOT)
os.environ.setdefault("OPENROUTER_API_KEY", "mock-key-value")
os.environ.setdefault("HF_API_KEY", "")
os.environ.setdefault("OLLAMA_URL", "http://127.0.0.1:11434")

from unittest.mock import MagicMock
from fastapi.testclient import TestClient

from core.app import app

client = TestClient(app)


@pytest.fixture(autouse=True)
def _mock_db():
    import database.supabase_client as db_mod
    original = db_mod.db.client
    mock_client = MagicMock()
    db_mod.db.client = mock_client
    yield mock_client
    db_mod.db.client = original


class TestOnboardingFlow:
    def test_complete_onboarding_new_user(self, _mock_db):
        mock_upsert = MagicMock()
        _mock_db.table.return_value.upsert.return_value.execute = mock_upsert
        _mock_db.table.return_value.insert.return_value.execute = MagicMock()

        resp = client.post("/api/onboarding/complete", json={
            "user_id": "test_user_new",
            "provider": "openrouter",
            "api_key": "mock-key",
            "default_model": "gpt-4o",
            "first_chat_sent": True
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "success"
        assert data["user_id"] == "test_user_new"
        assert data["setup_complete"] is True

    def test_complete_onboarding_existing_api_key(self, _mock_db):
        existing = MagicMock()
        existing.data = [{
            "user_id": "existing_user_123",
            "preferences": {"model_preference": "gpt-3.5-turbo"},
        }]
        _mock_db.table.return_value.select.return_value.eq.return_value.execute = existing
        mock_upsert = MagicMock()
        _mock_db.table.return_value.upsert.return_value.execute = mock_upsert

        resp = client.post("/api/onboarding/complete", json={
            "user_id": "existing_user_123",
            "api_key": "sk-test-key-12345",
            "default_model": "gpt-4o",
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "success"

    def test_get_onboarding_status(self, _mock_db):
        existing = MagicMock()
        existing.data = [{"custom_shortcuts": {"onboarding_completed_at": 12345678}, "theme": "dark", "default_model": "gpt-4o"}]
        _mock_db.table.return_value.select.return_value.eq.return_value.execute.return_value = existing

        resp = client.get("/api/onboarding/status/user_abc")
        assert resp.status_code == 200
        data = resp.json()
        assert data["onboarding_complete"] is True
        assert data["preferences"]["default_model"] == "gpt-4o"


class TestSmellCheck:
    def test_smell_check_requires_path(self):
        resp = client.post("/tools/smell-check", json={})
        assert resp.status_code == 422

    def test_smell_check_invalid_path(self):
        resp = client.post("/tools/smell-check", json={"path": "/nonexistent/path"})
        assert resp.status_code == 404
