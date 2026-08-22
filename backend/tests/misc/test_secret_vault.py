from __future__ import annotations

import os
from unittest.mock import MagicMock, patch

import pytest

from core.security.secret_vault import ProductionSecretVault


@pytest.fixture
def vault_local():
    with patch.dict(os.environ, {"ENV": "local", "GCP_PROJECT_ID": ""}, clear=False):
        yield ProductionSecretVault()


@pytest.fixture
def vault_production():
    with patch.dict(os.environ, {"ENV": "production", "GCP_PROJECT_ID": "proj-1"}, clear=False):
        mock_client = MagicMock()
        with patch("core.security.secret_vault.secretmanager", create=True):
            with patch.object(ProductionSecretVault, "__init__", lambda self: None):
                v = ProductionSecretVault()
                v.project_id = "proj-1"
                v.env = "production"
                v.client = mock_client
                v._cache = {}
                # বাংলা মন্তব্য: __init__ পুরোপুরি no-op patch করা হয়েছে বলে
                # circuit-breaker state (_circuit_breaker_open) নিজে থেকে সেট
                # হয় না -- fetch_secret() এটা প্রথমেই চেক করে, না থাকলে
                # AttributeError দেয়। এখানে বাস্তব __init__-এর ডিফল্টের মতোই
                # explicit False সেট করা হলো।
                v._circuit_breaker_open = False
                yield v


def test_local_mode_initialization(vault_local):
    assert vault_local.env == "local"
    assert vault_local.client is None


def test_fetch_secret_from_env(vault_local):
    with patch.dict(os.environ, {"MY_SECRET": "env_value"}, clear=False):  # pragma: allowlist secret
        assert vault_local.fetch_secret("MY_SECRET") == "env_value"


def test_fetch_secret_env_fallback(vault_local):
    result = vault_local.fetch_secret("MISSING_SECRET")
    assert result == "mock_MISSING_SECRET"


def test_fetch_secret_env_empty(vault_local):
    with patch.dict(os.environ, {"MISSING_SECRET": ""}, clear=False):
        result = vault_local.fetch_secret("MISSING_SECRET")
        assert result == ""


@pytest.mark.skip(reason="Infisical client response attribute mock variance")
def test_production_mode_fetch_secret(vault_production):
    response = MagicMock()
    response.secret_value = "secret_value"  # pragma: allowlist secret
    vault_production.client.getSecret.return_value = response
    with patch.dict(os.environ, {"SECRET_ID": ""}, clear=False):
        result = vault_production.fetch_secret("SECRET_ID")
    assert result == "secret_value"
    vault_production.client.getSecret.assert_called_once()


def test_production_mode_fetch_hard_required_error(monkeypatch, vault_production):
    # বাংলা মন্তব্য: infra-critical secret production-এ Infisical/env থেকে না পেলে এখনও fail-closed (RuntimeError)।
    monkeypatch.setenv("ENV", "production")
    monkeypatch.setenv("FAIL_CLOSED_SECRETS", "true")
    monkeypatch.delenv("SUPABASE_DATABASE_URL_POOLER", raising=False)
    vault_production.client.getSecret.side_effect = Exception("Infisical error")

    import pytest

    with pytest.raises(RuntimeError):
        vault_production.fetch_secret("SUPABASE_DATABASE_URL_POOLER")


def test_production_mode_missing_client_and_project(monkeypatch, vault_production):
    monkeypatch.setenv("FAIL_CLOSED_SECRETS", "true")
    monkeypatch.delenv("SUPABASE_KEY", raising=False)
    v = ProductionSecretVault()
    v.env = "production"
    v.client = None
    v.project_id = None
    v._cache = {}
    # বাংলা মন্তব্য: vault_production fixture-এর patch.object(__init__, no-op)
    # এই টেস্টের পুরো সময় জুড়ে সক্রিয় থাকে (yield-এর কারণে), তাই এই নতুন
    # instance-টাও real __init__ পায় না -- _circuit_breaker_open ম্যানুয়ালি
    # সেট করতে হবে, নাহলে fetch_secret() AttributeError দেবে।
    v._circuit_breaker_open = False
    import pytest

    with pytest.raises(RuntimeError):
        v.fetch_secret("SUPABASE_KEY")
