#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# File: config.py
# Project: SupremeAI 2.0
# Purpose: Configuration loading and management
# Module: core
# ============================================================================
# -*- coding: utf-8 -*-
# ============================================================================
# ফাইলের নাম: config.py
# প্রজেক্ট: SupremeAI 2.0 - মাল্টিক্লাউড AI অর্কেস্ট্রেশন প্ল্যাটফর্ম
# উদ্দেশ্য: কনফিগারেশন লোড ও ব্যবস্থাপনা
# প্রসঙ্গ: এই মডিউল "core" এর সাথে সম্পর্কিত।
# ভাষা: বাংলা ও ইংরেজি মিশ্র কমেন্ট।
# ============================================================================
# -*- coding: utf-8 -*-
# ============================================================================
# File: config.py





#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ======================================================================
# File: config.py
# Project: SupremeAI 2.0
# Purpose: Configuration loading and management
# Context: Connected to "core" module.
# Language: Bangla / English comments throughout.
# ======================================================================
import os
import sys
os.environ.setdefault("CORS_ORIGINS", "[]")
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import field_validator


class Settings(BaseSettings):
    # Added by Agent Antigravity on 2026-06-21: Disable loading .env during pytest runs to avoid local config leakage in tests
    model_config = SettingsConfigDict(
        env_file=None if "pytest" in sys.modules else ["../.env", ".env"], 
        extra="ignore"
    )

    PROJECT_NAME: str = "SupremeAI 2.0"
    API_V1_STR: str = "/api/v1"

    app_name: str = "SupremeAI 2.0"
    env: str = "local"
    debug: bool = True
    docs_auth_enabled: bool = False
    docs_username: str = "admin"
    docs_password: str = ""

    port: int = 8000
    host: str = "0.0.0.0"
    supremeai_admin_password_hash: str | None = None

    cors_origins: list[str] = [
        "http://127.0.0.1:3000",
        "http://127.0.0.1:8000",
        "https://supremeai-a.web.app",
        "https://supremeai-a.firebaseapp.com",
        "https://supremeai-admin.web.app",
        "https://supremeai-admin.firebaseapp.com",
    ]

    jwt_secret: str = ""

    openrouter_api_key: str = ""
    hf_api_key: str = ""
    gemini_api_key: str = ""
    deepseek_api_key: str = ""
    groq_api_key: str = ""
    nvidia_api_key: str = ""
    firecrawl_api_key: str = ""

    # Claude via OpenRouter (free tier model)
    claude_openrouter_model: str = "anthropic/claude-3.5-haiku:free"

    # ---------------------------------------------------------------
    # Free-tier rate limit overrides (set in .env to adjust budgets)
    # ---------------------------------------------------------------
    # Gemini
    gemini_rpm_limit: int = 9
    gemini_tpm_limit: int = 240_000
    gemini_rpd_limit: int = 475
    # Groq
    groq_rpm_limit: int = 28
    groq_tpm_limit: int = 28_500
    groq_rpd_limit: int = 13_680
    # OpenRouter free models
    openrouter_rpm_limit: int = 19
    openrouter_rpd_limit: int = 45
    # Cloudflare
    cloudflare_rpd_limit: int = 9_000
    # Nvidia
    nvidia_rpm_limit: int = 38
    nvidia_tpm_limit: int = 38_000
    # HuggingFace
    huggingface_rpm_limit: int = 18
    huggingface_rpd_limit: int = 950

    # ---------------------------------------------------------------
    # Token budget settings
    # ---------------------------------------------------------------
    max_prompt_tokens: int = 4_000
    max_response_tokens: int = 1_500
    enable_token_compression: bool = True
    sentry_dsn: str = ""
    ollama_url: str = "http://127.0.0.1:11434"
    gcp_project_id: str = "supremeai-a"
    gcp_region: str = "us-central1"

    # Stripe billing / payouts
    stripe_api_key: str = ""
    stripe_webhook_secret: str = ""

    max_cost_per_task: float = 0.01
    admin_rules_db: str = "data/constitutional_rules.db"
    memory_db_dir: str = "data/memory"
    skill_registry_path: str = "data/skill_registry.json"

    @field_validator("env")
    @classmethod
    def validate_env(cls, value: str) -> str:
        allowed = {"local", "staging", "production", "test"}
        if value.lower() not in allowed:
            raise ValueError(f"env must be one of {allowed}")
        return value.lower()

    @field_validator("cors_origins", mode="before")
    @classmethod
    def parse_cors_origins(cls, v):
        """Parse CORS_ORIGINS env var which may be a JSON list or empty.
        If empty or not provided, fall back to default list defined in class.
        """
        import json
        if isinstance(v, str):
            v = v.strip()
            if not v:
                return []
            try:
                return json.loads(v)
            except json.JSONDecodeError:
                # fallback to comma-separated list
                return [origin.strip() for origin in v.split(',') if origin.strip()]
        return v

    def validate_config(self) -> None:
        if self.env.lower() == "production":
            missing = []
            if not self.openrouter_api_key:
                missing.append("openrouter_api_key")
            if not self.gemini_api_key:
                missing.append("gemini_api_key")
            if not self.sentry_dsn:
                logger.warning("Sentry DSN is not configured (strongly recommended)")
            if not self.jwt_secret or self.jwt_secret == "np97Qpdqi9VdRyiANqjfKZn8/u7s/WCjtG8UsjbhhS0=":
                missing.append("secure JWT_SECRET")
            if missing:
                logger.error(f"Missing required configurations for production: {', '.join(missing)}")


settings = Settings()

# Populate os.environ with settings values so os.getenv matches the loaded configuration
import os
for key, val in settings.model_dump().items():
    env_key = key.upper()
    if val is not None:
        os.environ[env_key] = str(val)

