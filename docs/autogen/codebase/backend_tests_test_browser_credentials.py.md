# 📄 ফাইল: backend/tests/test_browser_credentials.py

**প্রকার:** .py  
**সাইজ:** 2,164 বাইট  
**আপডেট:** 2026-07-05T00:31:19.002507

---

## কোড

```py
import os

import pytest
from fastapi.testclient import TestClient


os.environ.setdefault("OPENROUTER_API_KEY", "")
os.environ.setdefault("HF_API_KEY", "")
os.environ.setdefault("OLLAMA_URL", "http://127.0.0.1:11434")
from core.app import app as app_mod
from core.secure_credential_store import SecureCredentialStore
from core.secure_credential_store import generate_key


client = TestClient(app_mod)
auth_headers = {"Authorization": "Bearer test-token"}


@pytest.fixture(autouse=True)
def reset_globals():
    os.environ["SUPREMEAI_API_TOKEN"] = "test-token"
    import api.routes.browser as browser_mod

    browser_mod.CREDENTIALS.clear()
    browser_mod.RECENT_ACTIVITIES.clear()
    browser_mod.TASKS.clear()
    browser_mod.FINDINGS.clear()
    yield
    os.environ.pop("SUPREMEAI_API_TOKEN", None)


def test_secure_credential_store_encrypt_decrypt(monkeypatch):
    monkeypatch.setenv("SUPREMEAI_CREDENTIAL_ENC_KEY", generate_key())
    store = SecureCredentialStore()
    payload = {"serviceName": "example", "username": "user", "password": "secret"}
    encrypted = store.encrypt(payload)
    assert encrypted.get("__enc__") is True
    decrypted = store.decrypt(encrypted)
    assert decrypted == payload


def test_secure_credential_store_mask():
    store = SecureCredentialStore()
    payload = {"serviceName": "example", "username": "user", "password": "secrets"}
    masked = store.mask(payload)
    assert masked["password"] == "••••••••••rets"
    assert masked["username"] == "user"


def test_browser_save_and_list_credentials():
    resp = client.post(
        "/api/browser/credentials",
        json={"serviceName": "example", "username": "user", "password": "secrets"},
        headers=auth_headers,
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["serviceName"] == "example"

    resp = client.get("/api/browser/credentials?userId=default", headers=auth_headers)
    assert resp.status_code == 200
    creds = resp.json()["credentials"]
    assert len(creds) == 1
    assert creds[0]["serviceName"] == "example"
    assert creds[0]["password"] == "••••••••••rets"

```