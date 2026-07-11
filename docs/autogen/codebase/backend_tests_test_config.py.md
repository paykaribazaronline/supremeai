# 📄 ফাইল: backend/tests/test_config.py

**প্রকার:** .py  
**সাইজ:** 4,853 বাইট  
**আপডেট:** 2026-07-11T09:15:34.020754

---

## কোড

```py
import os
from unittest.mock import patch

import pytest

from core.config import Settings


@patch.dict(os.environ, {}, clear=True)
def test_defaults():
    s = Settings()
    assert s.app_name == "SupremeAI 2.0"
    assert s.env == "local"
    assert s.debug is True
    assert s.port == 8000
    assert s.host == "0.0.0.0"
    assert s.supremeai_admin_password_hash is None
    assert s.ollama_url == ""
    assert s.gcp_project_id == ""
    assert s.gcp_region == "us-central1"
    assert s.max_cost_per_task == 0.01
    assert s.admin_rules_db == ""
    assert s.memory_db_dir == ""
    assert s.skill_registry_path == ""


@patch.dict(
    os.environ,
    {
        "PROJECT_NAME": "TestApp",
        "env": "production",
        "debug": "false",
        "port": "9000",
        "host": "0.0.0.0",
        "supremeai_admin_password_hash": "mock_hash_value_for_test_pass",
        "openrouter_api_key": "sk-openrouter",
        "hf_api_key": "sk-hf",
        "gemini_api_key": "sk-gemini",
        "deepseek_api_key": "sk-deepseek",
        "groq_api_key": "sk-groq",
        "nvidia_api_key": "sk-nvidia",
        "firecrawl_api_key": "sk-firecrawl",
        "sentry_dsn": "https://sentry.io/123",
        "ollama_url": "http://ollama:11434",
        "gcp_project_id": "test-project",
        "gcp_region": "europe-west1",
        "max_cost_per_task": "1.5",
        "STRIPE_API_KEY": "sk_test_123",
        "STRIPE_WEBHOOK_SECRET": "whsec_123",
        "CI_WEBHOOK_SECRET": "supreme-ci-secret-2026",
        "ADMIN_RULES_DB_PATH": "/tmp/rules.db",
        "MEMORY_DB_DIR": "/tmp/memory",
        "SKILL_REGISTRY_PATH": "/tmp/skills.json",
        "SUPREMEAI_JWT_SECRET": "a" * 128,
        "CORS_ORIGINS": '["https://supremeai.web.app"]',
        "ALLOWED_HOSTS": '["api.supremeai.com"]',
    },
    clear=False,
)
@patch("core.config.secret_vault.fetch_secret", side_effect=lambda k: os.environ.get(k) or os.environ.get(k.lower()))
def test_env_override(mock_fetch):
    s = Settings()
    assert s.PROJECT_NAME == "TestApp"
    assert s.env == "production"
    assert s.debug is False
    assert s.port == 9000
    assert s.host == "0.0.0.0"
    assert s.supremeai_admin_password_hash == "mock_hash_value_for_test_pass"
    assert s.openrouter_api_key == "sk-openrouter"
    assert s.hf_api_key == "sk-hf"
    assert s.gemini_api_key == "sk-gemini"
    assert s.deepseek_api_key == "sk-deepseek"
    assert s.groq_api_key == "sk-groq"
    assert s.nvidia_api_key == "sk-nvidia"
    assert s.firecrawl_api_key == "sk-firecrawl"
    assert s.sentry_dsn == "https://sentry.io/123"
    assert s.ollama_url == "http://ollama:11434"
    assert s.gcp_project_id == "test-project"
    assert s.gcp_region == "europe-west1"
    assert s.max_cost_per_task == 1.5
    assert s.admin_rules_db == "/tmp/rules.db"
    assert s.memory_db_dir == "/tmp/memory"
    assert s.skill_registry_path == "/tmp/skills.json"


@pytest.mark.parametrize(
    "bad_env",
    ["staging", "prod", ""],
)
@patch.dict(os.environ, {"env": "bad"}, clear=False)
def test_invalid_env_raises(bad_env):
    with pytest.raises(Exception):
        Settings()


def test_parse_admin_emails_empty_string():
    from core.config import Settings
    from unittest.mock import MagicMock

    validator = Settings.parse_admin_emails
    assert validator("") == []


def test_parse_allowed_hosts_empty_string():
    from core.config import Settings

    assert Settings.parse_allowed_hosts("") == []


@patch.dict(
    os.environ,
    {
        "env": "production",
        "cors_origins": '["http://127.0.0.1:3000", "https://example.com"]',
        "SUPREMEAI_JWT_SECRET": "a" * 128,
        "SUPREMEAI_ADMIN_PASSWORD_HASH": "mock_hash_value_for_test_pass",
        "ALLOWED_HOSTS": '["api.supremeai.com"]',
        "STRIPE_API_KEY": "sk_test_123",
        "STRIPE_WEBHOOK_SECRET": "whsec_123",
        "OPENROUTER_API_KEY": "sk_test",
        "GEMINI_API_KEY": "sk_test",
        "CI_WEBHOOK_SECRET": "supreme-ci-secret-2026",
    },
    clear=False,
)
def test_cors_origins_production_strips_localhost():
    s = Settings()
    assert "http://127.0.0.1:3000" not in s.cors_origins
    assert "https://example.com" in s.cors_origins


@patch("core.config.secret_vault.fetch_secret", return_value="")
def test_validate_production_completeness_raises_on_missing_production_keys(mock_fetch):
    from core.config import Settings

    s = Settings.model_construct(
        env="production",
        jwt_secret="secret",
        ci_webhook_secret="supreme-ci-secret-2026",
        stripe_api_key="sk_test_123",
        stripe_webhook_secret="whsec_123",
    )
    with pytest.raises(ValueError):
        s.validate_production_completeness()


@patch.dict(os.environ, {"max_cost_per_task": "abc"}, clear=False)
def test_invalid_type_cast():
    with pytest.raises(Exception):
        Settings()

```