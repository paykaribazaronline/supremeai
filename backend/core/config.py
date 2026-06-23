
import sys
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import field_validator
from loguru import logger
from core.secret_vault import secret_vault


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=None if "pytest" in sys.modules else ["../.env", ".env"],
        extra="ignore",
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

    jwt_secret: str = secret_vault.fetch_secret("JWT_SECRET", "np97Qpdqi9VdRyiANqjfKZn8/u7s/WCjtG8UsjbhhS0=")
    
    # ⚡ ডাইনামিকলি সরাসরি ক্লাউড মেমরি থেকে সিক্রেট রিড করা হচ্ছে
    # ডিস্কে কোনো .env ফাইল না থাকলেও প্রোডাকশন এপিআই ১০০% স্মুথলি চলবে
    supabase_database_url: str = secret_vault.fetch_secret(
        "SUPABASE_DATABASE_URL_POOLER", 
        "postgresql://localhost:5432/postgres"
    )

    openrouter_api_key: str = secret_vault.fetch_secret("OPENROUTER_API_KEY", "")
    hf_api_key: str = secret_vault.fetch_secret("HF_API_KEY", "")
    gemini_api_key: str = secret_vault.fetch_secret("GEMINI_API_KEY", "")
    deepseek_api_key: str = secret_vault.fetch_secret("DEEPSEEK_API_KEY", "")
    groq_api_key: str = secret_vault.fetch_secret("GROQ_API_KEY", "")
    nvidia_api_key: str = secret_vault.fetch_secret("NVIDIA_API_KEY", "")
    firecrawl_api_key: str = secret_vault.fetch_secret("FIRECRAWL_API_KEY", "")

    claude_openrouter_model: str = "anthropic/claude-3.5-haiku:free"

    gemini_rpm_limit: int = 9
    gemini_tpm_limit: int = 240_000
    gemini_rpd_limit: int = 475
    groq_rpm_limit: int = 28
    groq_tpm_limit: int = 28_500
    groq_rpd_limit: int = 13_680
    openrouter_rpm_limit: int = 19
    openrouter_rpd_limit: int = 45
    cloudflare_rpd_limit: int = 9_000
    nvidia_rpm_limit: int = 38
    nvidia_tpm_limit: int = 38_000
    huggingface_rpm_limit: int = 18
    huggingface_rpd_limit: int = 950

    max_prompt_tokens: int = 4_000
    max_response_tokens: int = 1_500
    enable_token_compression: bool = True
    sentry_dsn: str = ""
    ollama_url: str = "http://127.0.0.1:11434"
    gcp_project_id: str = "supremeai-a"
    gcp_region: str = "us-central1"

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
        import json
        if isinstance(v, str):
            v = v.strip()
            if not v:
                return []
            try:
                return json.loads(v)
            except json.JSONDecodeError:
                return [origin.strip() for origin in v.split(",") if origin.strip()]
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
            if not self.jwt_secret:
                missing.append("secure JWT_SECRET")
            if missing:
                raise RuntimeError(f"Missing required configurations for production: {', '.join(missing)}")


settings = Settings()
