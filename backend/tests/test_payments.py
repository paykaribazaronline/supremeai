# FILE_PATH: tests/test_payments.py
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


def test_create_checkout_session_mock(mocker):
    # Verify mock checkout flow when Stripe API key is not configured
    # Patch settings.stripe_secret_key to None for this test to activate mock mode in backend
    # This is more reliable than manipulating os.environ when the app's settings are already loaded.
    mocker.patch.object(settings, 'stripe_secret_key', None)

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


def test_webhook_ignored_if_missing_config(mocker):
    # Verify webhook behaves gracefully when credentials/key are missing
    # Patch settings.stripe_webhook_secret to None for this test to trigger the "ignored" status.
    mocker.patch.object(settings, 'stripe_webhook_secret', None)

    # Send a valid JSON payload as Stripe webhooks expect JSON,
    # rather than arbitrary bytes, to avoid early 400 Bad Request errors
    # unrelated to signature verification or missing config.
    payload = {"id": "evt_test_123", "type": "checkout.session.completed", "data": {"object": {"id": "cs_test_123"}}}
    headers = {**auth_headers, "stripe-signature": "t=123,v1=invalid_signature"}
    resp = client.post("/payments/webhook", headers=headers, json=payload)
    assert resp.status_code == 200
    assert resp.json()["status"] == "ignored"
