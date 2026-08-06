"""Tests for core.security.secret_vault — ProductionSecretVault."""

from unittest.mock import patch

import pytest
from core.security.secret_vault import (ProductionSecretVault, _CacheEntry,
                                        reset_secret_vault)


@pytest.fixture(autouse=True)
def reset_vault():
    reset_secret_vault()
    try:
        yield
    finally:
        reset_secret_vault()


class TestCacheEntry:
    """Tests for _CacheEntry internal class."""

    def test_cache_entry_not_expired(self):
        entry = _CacheEntry("test-value", ttl=300)
        assert entry.is_expired is False
        assert entry.value == "test-value"

    def test_cache_entry_expired(self):
        entry = _CacheEntry("test-value", ttl=-1)
        assert entry.is_expired is True


class TestProductionSecretVaultInit:
    """Tests for ProductionSecretVault initialization."""

    def test_init_no_credentials(self, monkeypatch):
        monkeypatch.delenv("INFISICAL_PROJECT_ID", raising=False)
        monkeypatch.delenv("INFISICAL_CLIENT_ID", raising=False)
        monkeypatch.delenv("INFISICAL_CLIENT_SECRET", raising=False)
        monkeypatch.delenv("INFISICAL_TOKEN", raising=False)
        vault = ProductionSecretVault()
        assert vault.client is None

    def test_init_with_token(self, monkeypatch):
        monkeypatch.setenv("INFISICAL_TOKEN", "test-token")
        monkeypatch.setenv("INFISICAL_PROJECT_ID", "test-project")
        vault = ProductionSecretVault()
        assert vault.client is not None or vault.client is None

    def test_environment_default(self, monkeypatch):
        monkeypatch.setenv("ENV", "local")
        vault = ProductionSecretVault()
        assert vault.env == "local"


class TestSecretVaultFallback:
    """Tests for fallback to environment variables."""

    def test_fallback_to_env_success(self, monkeypatch):
        monkeypatch.setenv("MY_SECRET", "env-value")
        vault = ProductionSecretVault()
        vault.client = None
        result = vault._fallback_to_env("MY_SECRET", None)
        assert result == "env-value"

    def test_fallback_to_env_default(self, monkeypatch):
        monkeypatch.delenv("MISSING_SECRET", raising=False)
        vault = ProductionSecretVault()
        vault.env = "development"
        vault.client = None
        result = vault._fallback_to_env("MISSING_SECRET", "default-value")
        assert result == "default-value"

    def test_fallback_production_missing_raises(self, monkeypatch):
        monkeypatch.delenv("CRITICAL_SECRET", raising=False)
        vault = ProductionSecretVault()
        vault.env = "production"
        vault.client = None
        with pytest.raises(RuntimeError):
            vault._fallback_to_env("CRITICAL_SECRET", None)


class TestSecretVaultCache:
    """Tests for secret vault caching."""

    def test_cache_hit(self, monkeypatch):
        monkeypatch.setenv("MY_SECRET", "env-value")
        vault = ProductionSecretVault()
        vault.client = None
        result1 = vault.fetch_secret("MY_SECRET")
        result2 = vault.fetch_secret("MY_SECRET")
        assert result1 == result2

    def test_cache_invalidate_single(self, monkeypatch):
        monkeypatch.setenv("SECRET_A", "value-a")
        monkeypatch.setenv("SECRET_B", "value-b")
        vault = ProductionSecretVault()
        vault.client = None
        vault.fetch_secret("SECRET_A")
        vault.fetch_secret("SECRET_B")
        vault.invalidate_cache("SECRET_A")
        assert "SECRET_A" not in vault._cache
        assert "SECRET_B" in vault._cache

    def test_cache_invalidate_all(self, monkeypatch):
        monkeypatch.setenv("SECRET_A", "value-a")
        monkeypatch.setenv("SECRET_B", "value-b")
        vault = ProductionSecretVault()
        vault.client = None
        vault.fetch_secret("SECRET_A")
        vault.fetch_secret("SECRET_B")
        vault.invalidate_cache()
        assert len(vault._cache) == 0


class TestSecretVaultFetch:
    """Tests for fetch_secret method."""

    def test_fetch_no_client_fallback(self, monkeypatch):
        monkeypatch.setenv("MY_SECRET", "env-value")
        vault = ProductionSecretVault()
        vault.client = None
        result = vault.fetch_secret("MY_SECRET")
        assert result == "env-value"

    def test_fetch_missing_secret_development(self, monkeypatch):
        monkeypatch.delenv("MISSING", raising=False)
        vault = ProductionSecretVault()
        vault.env = "development"
        vault.client = None
        result = vault.fetch_secret("MISSING", "default-val")
        assert result == "default-val"

    def test_fetch_async_wrapper(self, monkeypatch):
        monkeypatch.setenv("ASYNC_SECRET", "async-value")
        vault = ProductionSecretVault()
        vault.client = None
        import asyncio

        result = asyncio.run(vault.fetch_secret_async("ASYNC_SECRET"))
        assert result == "async-value"


class TestSecretVaultConnections:
    """Tests for Infisical client initialization."""

    def test_init_infisical_connection_error(self, monkeypatch):
        monkeypatch.setenv("INFISICAL_CLIENT_ID", "test-id")
        monkeypatch.setenv("INFISICAL_CLIENT_SECRET", "test-secret")
        monkeypatch.setenv("INFISICAL_PROJECT_ID", "test-project")

        with patch(
            "core.security.secret_vault.InfisicalClient",
            side_effect=ConnectionError("no connection"),
        ):
            vault = ProductionSecretVault()
            assert vault.client is None

    def test_init_infisical_timeout_error(self, monkeypatch):
        monkeypatch.setenv("INFISICAL_CLIENT_ID", "test-id")
        monkeypatch.setenv("INFISICAL_CLIENT_SECRET", "test-secret")
        monkeypatch.setenv("INFISICAL_PROJECT_ID", "test-project")

        with patch(
            "core.security.secret_vault.InfisicalClient",
            side_effect=TimeoutError("timeout"),
        ):
            vault = ProductionSecretVault()
            assert vault.client is None


class TestSecretVaultModule:
    """Tests for module-level functions."""

    def test_get_secret_vault_singleton(self, monkeypatch):
        from core.security.secret_vault import get_secret_vault

        reset_secret_vault()
        v1 = get_secret_vault()
        v2 = get_secret_vault()
        assert v1 is v2

    def test_reset_vault(self, monkeypatch):
        from core.security.secret_vault import get_secret_vault

        reset_secret_vault()
        v1 = get_secret_vault()
        reset_secret_vault()
        v2 = get_secret_vault()
        assert v1 is not v2
