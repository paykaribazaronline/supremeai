# 📄 ফাইল: backend/tests/test_config_coverage.py

**প্রকার:** .py  
**সাইজ:** 7,776 বাইট  
**আপডেট:** 2026-07-08T03:57:12.446305

---

## কোড

```py
# বাংলা মন্তব্য: core/config.py মডিউলের কভারেজ বাড়াতে ভ্যালিডেটর ও validate_config
# লজিকের জন্য অতিরিক্ত ইউনিট টেস্ট। ValidationInfo প্রোটোকল সরাসরি ইনস্ট্যানশিয়েট
# করার বদলে হালকা SimpleNamespace ব্যবহার করা হয়েছে যাতে টেস্ট pydantic ভার্সনের
# উপর নির্ভরশীল না হয়।
from __future__ import annotations

import os
from types import SimpleNamespace
from unittest.mock import patch

import pytest

from core.config import Settings


def _info(**data) -> SimpleNamespace:
    # বাংলা মন্তব্য: ValidationInfo এর পরিবর্তে শুধু .data অ্যাট্রিবিউট দরকার হয়
    return SimpleNamespace(data=data)


# ── parse_cors_origins ─────────────────────────────────────────────────
def test_parse_cors_origins_local_env_keeps_localhost():
    origins = ["http://127.0.0.1:3000", "https://example.com"]
    assert Settings.parse_cors_origins(list(origins), _info(env="local")) == origins

def test_parse_cors_origins_production_strips_localhost():
    # বাংলা মন্তব্য: _env_context প্রোডাকশন হলে localhost অরিজিন বাদ যাবে
    origins = ["http://127.0.0.1:3000", "http://localhost:5173", "https://example.com"]
    result = Settings.parse_cors_origins(list(origins), _info(env="production"))
    assert result == ["https://example.com"]

# ── parse_cors_origins ─────────────────────────────────────────────────
def test_parse_cors_origins_empty_string():
    assert Settings(cors_origins="").cors_origins == []
    assert Settings(cors_origins="  ").cors_origins == []


def test_parse_cors_origins_comma_separated():
    assert Settings(cors_origins="a, b, c").cors_origins == ["a", "b", "c"]


def test_parse_cors_origins_json_string():
    assert Settings(cors_origins='["a", "b"]').cors_origins == ["a", "b"]


def test_parse_cors_origins_non_string_passthrough():
    assert Settings(cors_origins=["a"]).cors_origins == ["a"]


# ── parse_admin_emails ─────────────────────────────────────────────────
def test_parse_admin_emails_comma_separated():
    result = Settings.parse_admin_emails("a@x.com, b@y.com ,, c@z.com")
    assert result == ["a@x.com", "b@y.com", "c@z.com"]


def test_parse_admin_emails_empty_returns_empty_list():
    assert Settings.parse_admin_emails("   ") == []


def test_parse_admin_emails_list_passthrough():
    assert Settings.parse_admin_emails(["a@x.com"]) == ["a@x.com"]


# ── parse_allowed_hosts ────────────────────────────────────────────────
def test_parse_allowed_hosts_comma_separated():
    assert Settings.parse_allowed_hosts("localhost, example.com") == ["localhost", "example.com"]


def test_parse_allowed_hosts_empty_returns_empty_list():
    assert Settings.parse_allowed_hosts("") == []


def test_parse_allowed_hosts_list_passthrough():
    assert Settings.parse_allowed_hosts(["localhost"]) == ["localhost"]


# ── validate_env ───────────────────────────────────────────────────────
@pytest.mark.parametrize("value,expected", [("LOCAL", "local"), ("Production", "production"), ("test", "test")])
def test_validate_env_normalizes_case(value, expected):
    assert Settings.validate_env(value) == expected


def test_validate_env_rejects_unknown():
    with pytest.raises(ValueError):
        Settings.validate_env("banana")


# ── set_test_secret ────────────────────────────────────────────────────
def test_set_test_secret_returns_placeholder_in_local():
    assert Settings.set_test_secret(None, _info(env="local")) == "test-secret-placeholder"


def test_set_test_secret_raises_in_production_when_missing():
    with pytest.raises(ValueError):
        Settings.set_test_secret(None, _info(env="production"))


def test_set_test_secret_keeps_provided_value():
    assert Settings.set_test_secret("real-secret", _info(env="production")) == "real-secret"


# ── debug_must_be_false_in_production ──────────────────────────────────
def test_debug_forced_false_in_production():
    assert Settings.debug_must_be_false_in_production(True, _info(env="production")) is False


def test_debug_preserved_outside_production():
    assert Settings.debug_must_be_false_in_production(True, _info(env="local")) is True


# ── validate_config ────────────────────────────────────────────────────
def _bare_settings(**attrs) -> Settings:
    # বাংলা মন্তব্য: পুরো pydantic ভ্যালিডেশন এড়াতে খালি ইনস্ট্যান্স বানিয়ে অ্যাট্রিবিউট সেট করা হয়
    s = Settings.__new__(Settings)
    s._cached_secrets = {}
    for key, value in attrs.items():
        try:
            object.__setattr__(s, key, value)
        except AttributeError:
            if key == "jwt_secret":
                s._cached_secrets["SUPREMEAI_JWT_SECRET"] = value
            elif key == "ci_webhook_secret":
                s._cached_secrets["CI_WEBHOOK_SECRET"] = value
            else:
                s._cached_secrets[key.upper()] = value
    return s


def test_validate_config_noop_for_non_production():
    s = _bare_settings(env="local")
    # কোনো এক্সসেপশন ছাড়াই রিটার্ন করবে
    assert s.validate_config() is None


def test_validate_config_raises_when_production_keys_missing():
    s = _bare_settings(
        env="production",
        openrouter_api_key="",
        gemini_api_key="",
        sentry_dsn="",
        jwt_secret="",
        ci_webhook_secret="",
    )
    with pytest.raises(RuntimeError) as exc:
        s.validate_config()
    message = str(exc.value)
    assert "openrouter_api_key" in message
    assert "gemini_api_key" in message
    assert "secure JWT_SECRET" in message
    assert "secure CI_WEBHOOK_SECRET" in message


def test_validate_config_passes_when_production_keys_present():
    s = _bare_settings(
        env="production",
        openrouter_api_key="sk-open",
        gemini_api_key="sk-gemini",
        sentry_dsn="https://sentry.example",
        jwt_secret="super-secret",
        ci_webhook_secret="a-strong-unique-secret",
    )
    assert s.validate_config() is None


# ── construction smoke test ────────────────────────────────────────────
@patch.dict(os.environ, {}, clear=True)
def test_settings_construction_defaults():
    s = Settings()
    assert s.env == "local"
    assert s.jwt_secret == "test-secret-placeholder"
    assert isinstance(s.admin_emails, list)
    assert isinstance(s.allowed_hosts, list)

```