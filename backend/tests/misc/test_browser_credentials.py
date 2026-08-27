import os

import pytest
from fastapi.testclient import TestClient

os.environ.setdefault("OPENROUTER_API_KEY", "")
os.environ.setdefault("HF_API_KEY", "")
os.environ.setdefault("OLLAMA_URL", "http://127.0.0.1:11434")
from api.routes.admin_dashboard import require_admin_token
from core.app import app as app_mod
from core.security.secure_credential_store import SecureCredentialStore, generate_key

client = TestClient(app_mod)
auth_headers = {"Authorization": "Bearer test-token"}


@pytest.fixture(autouse=True)
def override_admin_auth():
    """বাংলা: test env এ JWT নেই — require_admin_token override করা হচ্ছে।"""
    app_mod.dependency_overrides[require_admin_token] = lambda: {"uid": "admin", "role": "admin"}
    yield
    app_mod.dependency_overrides = {}


@pytest.fixture(autouse=True)
def reset_globals():
    os.environ["SUPREMEAI_API_KEY"] = "test-token"
    import api.routes.browser as browser_mod

    browser_mod.CREDENTIALS.clear()
    browser_mod.RECENT_ACTIVITIES.clear()
    browser_mod.TASKS.clear()
    browser_mod.FINDINGS.clear()
    try:
        yield
    finally:
        os.environ.pop("SUPREMEAI_API_KEY", None)


def test_secure_credential_store_encrypt_decrypt(monkeypatch):
    import json

    monkeypatch.setenv("SUPREMEAI_CREDENTIAL_ENC_KEY", generate_key())
    store = SecureCredentialStore()
    payload = {"serviceName": "example", "username": "user", "password": "secret"}
    ciphertext, key_ref = store.encrypt(json.dumps(payload))
    assert isinstance(ciphertext, str)
    decrypted = store.decrypt(ciphertext, key_ref)
    assert json.loads(decrypted) == payload


def test_secure_credential_store_mask():
    store = SecureCredentialStore()
    assert store.mask("secrets") == "secr***"


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
    assert creds[0]["password"] == "secr***"
