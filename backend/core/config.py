import sys
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import field_validator


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=None if ("pytest" in sys.modules or any("pytest" in arg for arg in sys.argv)) else ".env",
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

    openrouter_api_key: str = ""
    hf_api_key: str = ""
    gemini_api_key: str = ""
    deepseek_api_key: str = ""
    groq_api_key: str = ""
    nvidia_api_key: str = ""
    firecrawl_api_key: str = ""
    sentry_dsn: str = ""
    ollama_url: str = "http://127.0.0.1:11434"
    gcp_project_id: str = "supremeai-a"
    gcp_region: str = "us-central1"

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

    def validate(self) -> None:
        # [2026-06-21] Relaxed validation: at least one AI provider key required (not all)
        if self.env.lower() == "production":
            has_any_key = bool(
                self.openrouter_api_key
                or self.gemini_api_key
                or self.deepseek_api_key
                or self.groq_api_key
                or self.nvidia_api_key
            )
            if not has_any_key:
                raise RuntimeError(
                    "Production mode requires at least one AI provider API key "
                    "(OPENROUTER_API_KEY, GEMINI_API_KEY, DEEPSEEK_API_KEY, GROQ_API_KEY, or NVIDIA_API_KEY)"
                )


settings = Settings()

