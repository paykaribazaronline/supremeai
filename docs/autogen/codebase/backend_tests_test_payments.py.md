# 📄 ফাইল: backend/tests/test_payments.py

**প্রকার:** .py  
**সাইজ:** 2,171 বাইট  
**আপডেট:** 2026-07-11T13:28:09.004335

---

## কোড

```py
import os

from fastapi.testclient import TestClient
from jose import jwt

from core.app import app
from core.config import settings


import pytest
from unittest.mock import patch

client = TestClient(app)

mock_token = jwt.encode({"user_id": "test-user-id", "role": "admin"}, settings.jwt_secret, algorithm="HS256")
auth_headers = {"Authorization": f"Bearer {mock_token}"}


@pytest.fixture(autouse=True)
def mock_stripe():
    with patch("stripe.checkout.Session.create") as mock_session:
        # Instead of a dict, make the mock return an object with .id and .url
        mock_session.return_value.id = "cs_test_123"
        mock_session.return_value.url = "https://stripe.com/test"
        yield mock_session


def test_get_plans():
    # Verify plans list
    resp = client.get("/payments/plans", headers=auth_headers)
    assert resp.status_code == 200
    data = resp.json()
    assert "plans" in data
    assert len(data["plans"]) == 3
    assert data["plans"][0]["id"] == "price_basic_monthly"


def test_create_checkout_session_mock():
    # Because conftest sets dummy STRIPE_API_KEY, the API will hit the mocked Stripe method
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
    assert data["status"] == "success"
    assert data["session_id"] == "cs_test_123"
    assert "https://stripe.com/test" in data["url"]


from pydantic import SecretStr
from unittest.mock import patch


def test_webhook_ignored_if_missing_config():
    # Verify webhook behaves gracefully when credentials/key are missing
    with patch("api.routes.payments.settings.stripe_webhook_secret", new=SecretStr("")):
        headers = {**auth_headers, "stripe-signature": "invalid-sig"}
        resp = client.post("/payments/webhook", headers=headers, content=b"some-payload")
        assert resp.status_code == 200
        assert resp.json()["status"] == "ignored"

```