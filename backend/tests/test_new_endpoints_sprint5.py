import os
import sys
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
if ROOT not in sys.path:
    sys.path.insert(0, ROOT)
os.environ.setdefault("OPENROUTER_API_KEY", "mock-key-value")
os.environ.setdefault("HF_API_KEY", "")
os.environ.setdefault("OLLAMA_URL", "http://127.0.0.1:11434")

from unittest.mock import MagicMock
from fastapi.testclient import TestClient

import core.app as app_mod
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
            "model_preference": "gpt-4o",
            "team_name": "Test Team"
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "success"
        assert data["is_new_user"] is True
        assert "access_token" in data
        assert data["preferences"]["model_preference"] == "gpt-4o"
        assert data["preferences"]["onboarded"] is True

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
            "api_key": "sk-test-key-12345",
            "model_preference": "gpt-4o",
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["is_new_user"] is False
        assert data["status"] == "success"

    def test_get_onboarding_status(self, _mock_db):
        existing = MagicMock()
        existing.data = [{"preferences": {"onboarded": True, "model_preference": "gpt-4o"}}]
        _mock_db.table.return_value.select.return_value.eq.return_value.execute = existing

        resp = client.get("/api/onboarding/status/user_abc")
        assert resp.status_code == 200
        data = resp.json()
        assert data["preferences"]["onboarded"] is True


class TestSmellCheck:
    def test_smell_check_requires_path(self):
        resp = client.post("/api/tools/smell-check", json={})
        assert resp.status_code == 422

    def test_smell_check_invalid_path(self):
        resp = client.post("/api/tools/smell-check", json={"path": "/nonexistent/path"})
        assert resp.status_code == 404
