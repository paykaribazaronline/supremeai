# 📄 ফাইল: backend/tests/test_secret_vault.py

**প্রকার:** .py  
**সাইজ:** 2,581 বাইট  
**আপডেট:** 2026-07-11T17:37:52.630496

---

## কোড

```py
from __future__ import annotations

import os
from unittest.mock import MagicMock
from unittest.mock import patch

import pytest

from core.secret_vault import ProductionSecretVault


@pytest.fixture
def vault_local():
    with patch.dict(os.environ, {"ENV": "local", "GCP_PROJECT_ID": ""}, clear=False):
        yield ProductionSecretVault()


@pytest.fixture
def vault_production():
    with patch.dict(os.environ, {"ENV": "production", "GCP_PROJECT_ID": "proj-1"}, clear=False):
        mock_client = MagicMock()
        with patch("core.secret_vault.secretmanager", create=True):
            with patch.object(ProductionSecretVault, "__init__", lambda self: None):
                v = ProductionSecretVault()
                v.project_id = "proj-1"
                v.env = "production"
                v.client = mock_client
                v._cached_secrets = {}
                yield v


def test_local_mode_initialization(vault_local):
    assert vault_local.env == "local"
    assert vault_local.client is None


def test_fetch_secret_from_env(vault_local):
    with patch.dict(os.environ, {"MY_SECRET": "env_value"}, clear=False):
        assert vault_local.fetch_secret("MY_SECRET") == "env_value"


def test_fetch_secret_env_fallback(vault_local):
    result = vault_local.fetch_secret("MISSING_SECRET")
    assert result == ""


def test_fetch_secret_env_empty(vault_local):
    with patch.dict(os.environ, {"MISSING_SECRET": ""}, clear=False):
        result = vault_local.fetch_secret("MISSING_SECRET")
        assert result == ""


def test_production_mode_fetch_secret(vault_production):
    response = MagicMock()
    response.secret_value = "secret_value"
    vault_production.client.getSecret.return_value = response
    with patch.dict(os.environ, {"SECRET_ID": ""}, clear=False):
        result = vault_production.fetch_secret("SECRET_ID")
    assert result == "secret_value"
    vault_production.client.getSecret.assert_called_once()


def test_production_mode_fetch_secret_error(vault_production):
    vault_production.client.getSecret.side_effect = Exception("Infisical error")
    with patch.dict(os.environ, {"SECRET_ID": ""}, clear=False):
        import pytest

        with pytest.raises(RuntimeError):
            vault_production.fetch_secret("SECRET_ID")


def test_production_mode_missing_client_and_project(vault_production):
    v = ProductionSecretVault()
    v.env = "production"
    v.client = None
    v.project_id = None
    v._cached_secrets = {}
    import pytest

    with pytest.raises(RuntimeError):
        v.fetch_secret("SECRET_ID")

```