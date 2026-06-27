import os

from fastapi.testclient import TestClient
from jose import jwt

from core.app import app
from core.config import settings


client = TestClient(app)

mock_token = jwt.encode({"user_id": "test-user-id", "role": "admin"}, settings.jwt_secret, algorithm="HS256")
auth_headers = {"Authorization": f"Bearer {mock_token}"}


def test_get_plans():
    # Verify plans list
    resp = client.get("/payments/plans", headers=auth_headers)
    assert resp.status_code == 200
    data = resp.json()
    assert "plans" in data
    assert len(data["plans"]) == 3
    assert data["plans"][0]["id"] == "price_basic_monthly"


def test_create_checkout_session_mock():
    # Verify mock checkout flow when Stripe API key is not configured
    if "STRIPE_SECRET_KEY" in os.environ:
        del os.environ["STRIPE_SECRET_KEY"]

    resp = client.post(
        "/payments/checkout",
        json={
            "price_id": "price_basic_monthly",
            "success_url": "http://localhost/success",
            "cancel_url": "http://localhost/cancel",
            "user_id": "test-user-id",
        },
        headers=auth_headers,
    )
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "mock"
    assert data["session_id"] == "mock_session_123"
    assert "mock_session_123" in data["url"]


def test_webhook_ignored_if_missing_config():
    # Verify webhook behaves gracefully when credentials/key are missing
    headers = {**auth_headers, "stripe-signature": "invalid-sig"}
    resp = client.post("/payments/webhook", headers=headers, content=b"some-payload")
    assert resp.status_code == 200
    assert resp.json()["status"] == "ignored"
