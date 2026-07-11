# 📄 ফাইল: backend/tests/test_billing_api_integration.py

**প্রকার:** .py  
**সাইজ:** 1,030 বাইট  
**আপডেট:** 2026-07-11T16:26:09.381068

---

## কোড

```py
import pytest
from unittest.mock import patch
from fastapi.testclient import TestClient
from core.app import app

client = TestClient(app)


@patch("api.dependencies.is_test_environment", return_value=False)
def test_billing_wallet_unauthorized(mock_is_test):
    response = client.get("/api/billing/wallet")
    assert response.status_code == 401
    assert response.json()["detail"] == "Unauthorized"


@patch("api.dependencies.is_test_environment", return_value=False)
def test_billing_history_unauthorized(mock_is_test):
    response = client.get("/api/billing/history")
    assert response.status_code == 401
    assert response.json()["detail"] == "Unauthorized"


@patch("api.dependencies.is_test_environment", return_value=False)
def test_billing_checkout_unauthorized(mock_is_test):
    response = client.post("/api/billing/checkout", json={"price_id": "test_price", "success_url": "http://test", "cancel_url": "http://test"})
    assert response.status_code == 401
    assert response.json()["detail"] == "Unauthorized"

```