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
    with patch.dict(
        os.environ, {"ENV": "production", "GCP_PROJECT_ID": "proj-1"}, clear=False
    ):
        mock_client = MagicMock()
        with patch("core.secret_vault.secretmanager", create=True):
            with patch.object(ProductionSecretVault, "__init__", lambda self: None):
                v = ProductionSecretVault()
                v.project_id = "proj-1"
                v.env = "production"
                v.client = mock_client
                yield v


def test_local_mode_initialization(vault_local):
    assert vault_local.env == "local"
    assert vault_local.client is None


def test_fetch_secret_from_env(vault_local):
    with patch.dict(os.environ, {"MY_SECRET": "env_value"}, clear=False):
        assert vault_local.fetch_secret("MY_SECRET") == "env_value"


def test_fetch_secret_env_fallback(vault_local):
    result = vault_local.fetch_secret("MISSING_SECRET", default_fallback="default")
    assert result == "default"


def test_fetch_secret_env_empty(vault_local):
    with patch.dict(os.environ, {"MISSING_SECRET": ""}, clear=False):
        result = vault_local.fetch_secret("MISSING_SECRET", default_fallback="default")
        assert result == "default"


def test_production_mode_fetch_secret(vault_production):
    response = MagicMock()
    response.payload.data.decode.return_value = "secret_value\n"
    vault_production.client.access_secret_version.return_value = response
    with patch.dict(os.environ, {"SECRET_ID": ""}, clear=False):
        result = vault_production.fetch_secret("SECRET_ID")
    assert result == "secret_value"
    vault_production.client.access_secret_version.assert_called_once()
    called_name = vault_production.client.access_secret_version.call_args[1]["request"][
        "name"
    ]
    assert called_name == "projects/proj-1/secrets/SECRET_ID/versions/latest"


def test_production_mode_fetch_secret_error(vault_production):
    vault_production.client.access_secret_version.side_effect = Exception("GCP error")
    with patch.dict(os.environ, {"SECRET_ID": ""}, clear=False):
        result = vault_production.fetch_secret("SECRET_ID", default_fallback="fallback")
    assert result == "fallback"


def test_production_mode_missing_client_and_project(vault_production):
    v = ProductionSecretVault()
    v.env = "production"
    v.client = None
    v.project_id = None
    result = v.fetch_secret("SECRET_ID", default_fallback="default")
    assert result == "default"
