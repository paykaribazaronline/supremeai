"""
Configuration settings for Gitingest
"""
from functools import lru_cache
from typing import Optional
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables and .env file"""
    
    # Server
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    ALLOWED_HOSTS: list[str] = ["*"]
    
    # GitHub
    GITHUB_TOKEN: Optional[str] = None
    
    # S3/MinIO Cache
    S3_ENABLED: bool = False
    S3_ENDPOINT: Optional[str] = None
    S3_ACCESS_KEY: Optional[str] = None
    S3_SECRET_KEY: Optional[str] = None
    S3_BUCKET_NAME: str = "gitingest-cache"
    S3_USE_SSL: bool = True
    
    # Logging
    LOG_FORMAT: str = "json"  # json or text
    LOG_LEVEL: str = "INFO"
    SENTRY_DSN: Optional[str] = None
    POSTHOG_API_KEY: Optional[str] = None
    
    # Limits
    MAX_FILE_SIZE: int = 10 * 1024 * 1024  # 10 MB
    MAX_DIR_DEPTH: int = 20
    MAX_FILES: int = 10000
    MAX_TOTAL_SIZE: int = 500 * 1024 * 1024  # 500 MB
    CLONE_TIMEOUT: int = 60  # seconds
    
    # Rate limiting
    RATE_LIMIT_PER_MINUTE: int = 10
    
    # Prometheus
    METRICS_PORT: int = 9090
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = True


@lru_cache
def get_settings() -> Settings:
    """Get cached settings instance"""
    return Settings()
