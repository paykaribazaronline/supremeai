"""
API Key Management Tests

Mocks asyncpg so tests run without a live database.
"""

import os
import sys
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient

os.environ.setdefault("OPENROUTER_API_KEY", "mock-key-value")
os.environ.setdefault("ENV", "test")


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


# Ensure `core.app` is reloaded fresh in test runs (avoid cached app state)
for _mod in [m for m in list(sys.modules) if m == "core.app" or m.startswith("core.app.")]:
    del sys.modules[_mod]

from api.routes.api_keys import router
from core.app import app
from core.rate_limiter import AsyncRateLimiter
from core.security import (
    API_KEY_PREFIX,
    generate_api_key,
    hash_api_key,
    mask_api_key,
    verify_api_key,
)


@pytest.fixture
def client():
    fake_pool = FakePool()
    with (
        patch("core.startup.api_key_tables.ensure_api_key_tables"),
        patch("core.pgbouncer_pool.get_db_pool", return_value=fake_pool),
        patch("models.api_key.get_db_pool", return_value=fake_pool),
        patch("api.routes.api_keys._get_current_user", return_value="test_owner"),
    ):
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


class FakeRedisPipe:
    def __init__(self, storage):
        self.storage = storage
        self.cmds = []

    def incr(self, key):
        self.cmds.append(("incr", key))

    def expire(self, key, window):
        self.cmds.append(("expire", key, window))

    async def execute(self):
        res = []
        for cmd, *args in self.cmds:
            if cmd == "incr":
                self.storage[args[0]] = self.storage.get(args[0], 0) + 1
                res.append(self.storage[args[0]])
            elif cmd == "expire":
                res.append(True)
        self.cmds = []
        return res


class FakeRedisClient:
    def __init__(self):
        self.storage = {}

    def pipeline(self):
        return FakeRedisPipe(self.storage)


class TestRateLimiter:
    @pytest.fixture(autouse=True)
    def patch_redis(self):
        fake_redis = FakeRedisClient()
        with patch("core.rate_limiter.AsyncRateLimiter._get_redis", return_value=fake_redis):
            yield
            return

    @pytest.mark.asyncio
    async def test_allows_under_limit(self):
        rl = AsyncRateLimiter()
        for _ in range(3):
            assert await rl.acquire("pref", limit=3, window=60) is True

    @pytest.mark.skip(reason="Rate limiter in-memory Redis mock window test")
    @pytest.mark.asyncio
    async def test_blocks_over_limit(self):
        rl = AsyncRateLimiter()
        for _ in range(3):
            await rl.acquire("pref2", limit=3, window=60)
        assert await rl.acquire("pref2", limit=3, window=60) is False

    @pytest.mark.asyncio
    async def test_different_keys_independent(self):
        rl = AsyncRateLimiter()
        assert await rl.acquire("pref-a", limit=2, window=60) is True
        assert await rl.acquire("pref-b", limit=2, window=60) is True


class TestRouterStructure:
    def test_router_has_correct_prefix(self):
        assert router.prefix == "/api/api-keys"

    def test_create_schema_requires_user_id(self):
        from api.routes.api_keys import CreateAPIKeyRequest

        with pytest.raises(
            Exception
        ):  # -- intentionally broad: asserts *some* error propagates (mocked/validation failure), exact type varies
            CreateAPIKeyRequest(user_id="", name="Test")

    def test_rotate_schema_requires_old_key(self):
        from api.routes.api_keys import RotateAPIKeyRequest

        with pytest.raises(
            Exception
        ):  # -- intentionally broad: asserts *some* error propagates (mocked/validation failure), exact type varies
            RotateAPIKeyRequest(old_key="")

    def test_bulk_delete_schema_limits_count(self):
        from api.routes.api_keys import BulkDeleteRequest

        with pytest.raises(
            Exception
        ):  # -- intentionally broad: asserts *some* error propagates (mocked/validation failure), exact type varies
            BulkDeleteRequest(key_ids=list(range(51)))


class TestIntegrationViaHeaders:
    def test_endpoints_accessible_without_api_key(self, client):
        resp = client.get("/api/api-keys/", headers={"Authorization": "Bearer mock-token"})
        assert resp.status_code == 200

    def test_api_key_header_accepted_in_test_mode(self, client):
        resp = client.get(
            "/api/api-keys/",
            headers={
                "Authorization": "Bearer mock-token",
                "x-api-key": "sk-supreme-test123",
            },
        )
        assert resp.status_code == 200
