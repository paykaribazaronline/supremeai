from fastapi import FastAPI
from fastapi.testclient import TestClient

from api.routes.webhooks_ai import router as webhooks_router

app = FastAPI()
app.include_router(webhooks_router)

client = TestClient(app)


def test_send_telegram_alert():
    response = client.post(
        "/api/v1/webhooks/telegram/send-alert",
        json={
            "title": "High Latency Warning",
            "description": "OpenAI provider latency > 3000ms. Shifted to Gemini.",
            "patch_code": "def patch(): pass",
            "pr_id": "PR-102",
            "severity": "WARNING",
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "sent"
    assert "inline_keyboard" in data
    assert data["inline_keyboard"][0][0]["text"] == "✅ Approve PR & Merge"


def test_telegram_callback_approve():
    response = client.post(
        "/api/v1/webhooks/telegram/callback",
        json={"callback_id": "cb_001", "user_id": "dev_user_99", "action": "approve_pr", "pr_id": "PR-102"},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "approved"
    assert "PR-102" in data["message"]


def test_telegram_callback_reject():
    response = client.post(
        "/api/v1/webhooks/telegram/callback",
        json={"callback_id": "cb_002", "user_id": "dev_user_99", "action": "reject_pr", "pr_id": "PR-102"},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "rejected"
