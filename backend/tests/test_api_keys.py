"""
API Key Management Tests

Mocks asyncpg so tests run without a live database.
"""

import os
import sys
import types
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient


os.environ.setdefault("OPENROUTER_API_KEY", "mock-key-value")
os.environ.setdefault("ENV", "test")

asyncpg_stub = types.ModuleType("asyncpg")


class FakeConn:
    async def execute(self, *a, **k):
        return "OK"

    async def fetch(self, *a, **k):
        return []

    async def fetchrow(self, *a, **k):
        return None


class FakePool:
    async def acquire(self):
        return FakeConn()

    async def release(self, conn):
        pass

    async def close(self):
        pass

    async def execute(self, *a, **k):
        return "OK"

    async def fetch(self, *a, **k):
        return []

    async def fetchrow(self, *a, **k):
        return None


asyncpg_stub.Pool = FakePool
sys.modules["asyncpg"] = asyncpg_stub

from api.routes.api_keys import router
from core.api_key_rate_limiter import APIKeyRateLimiter
from core.app import app
from core.security import API_KEY_PREFIX
from core.security import generate_api_key
from core.security import hash_api_key
from core.security import mask_api_key
from core.security import verify_api_key


@pytest.fixture
def client():
    fake_pool = FakePool()
    with patch("core.app._ensure_api_key_tables"), patch(
        "core.pgbouncer_pool.get_db_pool", return_value=fake_pool
    ), patch("models.api_key.get_db_pool", return_value=fake_pool):
        yield TestClient(app)


@pytest.fixture
def sample_api_key():
    return generate_api_key()


class TestSecurityUtilities:
    def test_generate_api_key_has_prefix(self):
        key = generate_api_key()
        assert key.startswith(API_KEY_PREFIX)

    def test_generate_api_key_unique(self):
        keys = {generate_api_key() for _ in range(50)}
        assert len(keys) == 50

    def test_mask_api_key(self):
        key = generate_api_key()
        masked = mask_api_key(key)
        assert masked.startswith(key[:12])
        assert "****" in masked

    def test_hash_api_key(self):
        key = generate_api_key()
        h = hash_api_key(key)
        assert h.startswith("sha256$")

    def test_verify_api_key(self):
        key = generate_api_key()
        h = hash_api_key(key)
        assert verify_api_key(key, h) is True
        assert verify_api_key("wrong-key", h) is False

    def test_hash_is_deterministic(self):
        key = generate_api_key()
        assert hash_api_key(key) == hash_api_key(key)


class TestRateLimiter:
    def test_allows_under_limit(self):
        rl = APIKeyRateLimiter(burst=3)
        for _ in range(3):
            assert rl.is_allowed("pref") is True

    def test_blocks_over_limit(self):
        rl = APIKeyRateLimiter(burst=3)
        for _ in range(3):
            rl.is_allowed("pref")
        assert rl.is_allowed("pref") is False

    def test_remaining_decreases(self):
        rl = APIKeyRateLimiter(burst=5)
        rl.is_allowed("pref")
        rl.is_allowed("pref")
        assert rl.remaining("pref") == 3

    def test_different_keys_independent(self):
        rl = APIKeyRateLimiter(burst=2)
        assert rl.is_allowed("pref-a") is True
        assert rl.is_allowed("pref-b") is True


class TestRouterStructure:
    def test_router_has_correct_prefix(self):
        assert router.prefix == "/api/api-keys"

    def test_create_schema_requires_user_id(self):
        from api.routes.api_keys import CreateAPIKeyRequest

        with pytest.raises(Exception):
            CreateAPIKeyRequest(user_id="", name="Test")

    def test_rotate_schema_requires_old_key(self):
        from api.routes.api_keys import RotateAPIKeyRequest

        with pytest.raises(Exception):
            RotateAPIKeyRequest(old_key="")

    def test_bulk_delete_schema_limits_count(self):
        from api.routes.api_keys import BulkDeleteRequest

        with pytest.raises(Exception):
            BulkDeleteRequest(key_ids=list(range(51)))


class TestIntegrationViaHeaders:
    def test_endpoints_accessible_without_api_key(self, client):
        resp = client.get("/api/api-keys/")
        assert resp.status_code == 200

    def test_api_key_header_accepted_in_test_mode(self, client):
        resp = client.get("/api/api-keys/", headers={"x-api-key": "sk-supreme-test123"})
        assert resp.status_code == 200
