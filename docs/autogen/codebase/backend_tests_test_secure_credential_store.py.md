# 📄 ফাইল: backend/tests/test_secure_credential_store.py

**প্রকার:** .py  
**সাইজ:** 2,938 বাইট  
**আপডেট:** 2026-07-11T16:17:51.595415

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
        import json

        store = SecureCredentialStore()
        assert store.provider.enabled is False
        data = {"password": "secret"}
        ciphertext, key_ref = store.encrypt(json.dumps(data))
        assert key_ref is None
        dec = store.decrypt(ciphertext, key_ref)
        assert json.loads(dec) == data

    def test_mask_redacts_sensitive_fields(self):
        from core.secure_credential_store import SecureCredentialStore

        store = SecureCredentialStore()
        assert store.mask("passwords") == "pass*****"
        assert store.mask("tokentokentoken") == "toke***********"
        assert store.mask("u") == "****"

    def test_mask_no_sensitive_fields(self):
        from core.secure_credential_store import SecureCredentialStore

        store = SecureCredentialStore()
        assert store.mask("safe") == "****"


@pytest.mark.skipif(
    __import__("core.secure_credential_store", fromlist=["CRYPTO_AVAILABLE"]).CRYPTO_AVAILABLE is False,
    reason="cryptography not installed",
)
class TestSecureCredentialStoreEncrypted:
    def test_encrypt_decrypt_roundtrip(self, monkeypatch):
        from core.secure_credential_store import SecureCredentialStore
        from core.secure_credential_store import generate_key
        import json

        key = generate_key()
        monkeypatch.setenv("SUPREMEAI_CREDENTIAL_ENC_KEY", key)
        store = SecureCredentialStore()
        assert store.provider.enabled is True
        data = {"api_key": "abc123", "url": "https://api.example.com"}
        ciphertext, key_ref = store.encrypt(json.dumps(data))
        assert isinstance(ciphertext, str)
        dec = store.decrypt(ciphertext, key_ref)
        assert json.loads(dec) == data

    def test_decrypt_plaintext_passthrough(self, monkeypatch):
        from core.secure_credential_store import SecureCredentialStore
        from core.secure_credential_store import generate_key
        import json

        key = generate_key()
        monkeypatch.setenv("SUPREMEAI_CREDENTIAL_ENC_KEY", key)
        store = SecureCredentialStore()
        plain = json.dumps({"user": "test"})
        assert store.decrypt(plain) == plain

    def test_encrypt_empty_payload(self, monkeypatch):
        from core.secure_credential_store import SecureCredentialStore
        from core.secure_credential_store import generate_key

        key = generate_key()
        monkeypatch.setenv("SUPREMEAI_CREDENTIAL_ENC_KEY", key)
        store = SecureCredentialStore()
        ciphertext, key_ref = store.encrypt("")
        dec = store.decrypt(ciphertext, key_ref)
        assert dec == ""

```