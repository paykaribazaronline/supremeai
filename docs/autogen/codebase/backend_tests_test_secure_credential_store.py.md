# 📄 ফাইল: backend/tests/test_secure_credential_store.py

**প্রকার:** .py  
**সাইজ:** 3,206 বাইট  
**আপডেট:** 2026-07-07T08:37:57.144673

---

## কোড

```py
import os
import pytest

os.environ.setdefault("OPENROUTER_API_KEY", "")
os.environ.setdefault("HF_API_KEY", "")
os.environ.setdefault("OLLAMA_URL", "http://127.0.0.1:11434")

class TestSecureCredentialStoreDisable:
    def test_plaintext_when_no_key(self):
        from core.secure_credential_store import SecureCredentialStore

        store = SecureCredentialStore()
        assert store.provider.enabled is False
        data = {"password": "secret"}
        # when disabled, encrypt does not add __enc__ flag by design? Wait, no, encrypt always adds it.
        # Actually in the code: encrypt returns {"__enc__": True, "payload": plaintext, "key_ref": "local:plaintext"}
        res = store.encrypt(data)
        assert res.get("__enc__") is True
        dec = store.decrypt(res)
        assert dec == data

    def test_mask_redacts_sensitive_fields(self):
        from core.secure_credential_store import SecureCredentialStore

        store = SecureCredentialStore()
        masked = store.mask(
            {"username": "u", "password": "passwords", "token": "tokentokentoken", "other": "v"}
        )
        assert masked["password"] == "••••••••••ords"
        assert masked["token"] == "••••••••••oken"
        assert masked["username"] == "u"

    def test_mask_no_sensitive_fields(self):
        from core.secure_credential_store import SecureCredentialStore

        store = SecureCredentialStore()
        masked = store.mask({"name": "safe"})
        assert masked["name"] == "safe"


@pytest.mark.skipif(
    __import__(
        "core.secure_credential_store", fromlist=["CRYPTO_AVAILABLE"]
    ).CRYPTO_AVAILABLE
    is False,
    reason="cryptography not installed",
)
class TestSecureCredentialStoreEncrypted:
    def test_encrypt_decrypt_roundtrip(self, monkeypatch):
        from core.secure_credential_store import SecureCredentialStore
        from core.secure_credential_store import generate_key

        key = generate_key()
        monkeypatch.setenv("SUPREMEAI_CREDENTIAL_ENC_KEY", key)
        store = SecureCredentialStore()
        assert store.provider.enabled is True
        data = {"api_key": "abc123", "url": "https://api.example.com"}
        enc = store.encrypt(data)
        assert enc.get("__enc__") is True
        assert "payload" in enc
        dec = store.decrypt(enc)
        assert dec == data

    def test_decrypt_plaintext_passthrough(self, monkeypatch):
        from core.secure_credential_store import SecureCredentialStore
        from core.secure_credential_store import generate_key

        key = generate_key()
        monkeypatch.setenv("SUPREMEAI_CREDENTIAL_ENC_KEY", key)
        store = SecureCredentialStore()
        plain = {"user": "test"}
        assert store.decrypt(plain) == plain

    def test_encrypt_empty_payload(self, monkeypatch):
        from core.secure_credential_store import SecureCredentialStore
        from core.secure_credential_store import generate_key

        key = generate_key()
        monkeypatch.setenv("SUPREMEAI_CREDENTIAL_ENC_KEY", key)
        store = SecureCredentialStore()
        enc = store.encrypt({})
        dec = store.decrypt(enc)
        assert dec == {}

```