# backend/config/settings.py
"""SupremeAI Configuration Manager.

Centralized configuration with environment variable support.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
import json
import os
from typing import Any, Dict, List, Optional


class Environment(str, Enum):
    DEVELOPMENT = "development"
    STAGING = "staging"
    PRODUCTION = "production"
    TESTING = "testing"


@dataclass
class DatabaseConfig:
    """Database configuration."""
    url: str = "sqlite+aiosqlite:///./supremeai.db"
    pool_size: int = 5
    max_overflow: int = 10
    echo: bool = False


@dataclass
class RedisConfig:
    """Redis cache configuration."""
    host: str = "localhost"
    port: int = 6379
    db: int = 0
    password: Optional[str] = None
    use_redis: bool = False


@dataclass
class APIServerConfig:
    """API server configuration."""
    host: str = "0.0.0.0"
    port: int = 8000
    workers: int = 4
    reload: bool = False
    cors_origins: List[str] = field(default_factory=lambda: ["*"])
    rate_limit_per_minute: int = 60
    api_key_required: bool = False


@dataclass
class EvolutionConfig:
    """Auto-evolution configuration."""
    enabled: bool = True
    check_interval_seconds: int = 300
    min_performance_threshold: float = 0.7
    error_rate_threshold: float = 0.1
    max_evolutions_per_hour: int = 6
    backup_before_evolution: bool = True
    rollback_on_failure: bool = True
    population_size: int = 50
    mutation_rate: float = 0.1


@dataclass
class MemoryConfig:
    """Memory system configuration."""
    max_working_memory: int = 10
    max_episodic_memory: int = 1000
    fitness_threshold: float = 0.7
    consolidation_interval_hours: int = 6
    target_size_mb: int = 512
    compression_enabled: bool = True


@dataclass
class MonitoringConfig:
    """Monitoring and logging configuration."""
    log_level: str = "INFO"
    log_file: str = "logs/supremeai.log"
    retention_hours: int = 24
    enable_metrics: bool = True
    enable_alerts: bool = True
    alert_webhook_url: Optional[str] = None
    slack_webhook_url: Optional[str] = None


@dataclass
class SecurityConfig:
    """Security configuration."""
    secret_key: str = "change-me-in-production"
    api_keys: List[str] = field(default_factory=list)
    allowed_ips: List[str] = field(default_factory=lambda: ["0.0.0.0/0"])
    max_request_size_mb: int = 10
    session_timeout_minutes: int = 30


class Settings:
    """Centralized settings manager.

    Loads from environment variables with sensible defaults.
    """

    _instance: Optional[Settings] = None

    def __new__(cls) -> Settings:
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    def __init__(self) -> None:
        if getattr(self, "_initialized", False):
            return

        self._initialized = True
        self._load_environment()
        self._load_configs()

    def _load_environment(self) -> None:
        """Load environment configuration."""
        env_name = os.getenv("SUPREMEAI_ENV", "development").lower()
        try:
            self.environment = Environment(env_name)
        except ValueError:
            self.environment = Environment.DEVELOPMENT
        self.debug = self.environment == Environment.DEVELOPMENT
        self.testing = self.environment == Environment.TESTING

    def _load_configs(self) -> None:
        """Load all configuration sections."""
        # Database
        self.database = DatabaseConfig(
            url=os.getenv("DATABASE_URL", "sqlite+aiosqlite:///./supremeai.db"),
            pool_size=int(os.getenv("DB_POOL_SIZE", "5")),
            echo=self.debug,
        )

        # Redis
        self.redis = RedisConfig(
            host=os.getenv("REDIS_HOST", "localhost"),
            port=int(os.getenv("REDIS_PORT", "6379")),
            password=os.getenv("REDIS_PASSWORD"),
            use_redis=os.getenv("USE_REDIS", "false").lower() == "true",
        )

        # API Server
        self.api = APIServerConfig(
            host=os.getenv("API_HOST", "0.0.0.0"),
            port=int(os.getenv("API_PORT", "8000")),
            workers=int(os.getenv("API_WORKERS", "4")),
            reload=self.debug,
            rate_limit_per_minute=int(os.getenv("RATE_LIMIT", "60")),
        )

        # Evolution
        self.evolution = EvolutionConfig(
            enabled=os.getenv("EVOLUTION_ENABLED", "true").lower() == "true",
            check_interval_seconds=int(os.getenv("EVOLUTION_CHECK_INTERVAL", "300")),
            population_size=int(os.getenv("POPULATION_SIZE", "50")),
            mutation_rate=float(os.getenv("MUTATION_RATE", "0.1")),
        )

        # Memory
        self.memory = MemoryConfig(
            max_working_memory=int(os.getenv("MAX_WORKING_MEMORY", "10")),
            max_episodic_memory=int(os.getenv("MAX_EPISODIC_MEMORY", "1000")),
            target_size_mb=int(os.getenv("TARGET_MEMORY_MB", "512")),
        )

        # Monitoring
        self.monitoring = MonitoringConfig(
            log_level=os.getenv("LOG_LEVEL", "INFO"),
            alert_webhook_url=os.getenv("ALERT_WEBHOOK_URL"),
            slack_webhook_url=os.getenv("SLACK_WEBHOOK_URL"),
        )

        # Security
        api_keys_raw = os.getenv("API_KEYS", "[]")
        try:
            parsed_keys = json.loads(api_keys_raw)
        except Exception:
            parsed_keys = []

        self.security = SecurityConfig(
            secret_key=os.getenv("SECRET_KEY", "change-me-in-production"),
            api_keys=parsed_keys if isinstance(parsed_keys, list) else [],
        )

    def is_production(self) -> bool:
        return self.environment == Environment.PRODUCTION

    def is_development(self) -> bool:
        return self.environment == Environment.DEVELOPMENT

    def get_all_settings(self) -> Dict[str, Any]:
        return {
            "environment": self.environment.value,
            "debug": self.debug,
            "database": {
                "url": self.database.url,
                "pool_size": self.database.pool_size,
            },
            "redis": {
                "host": self.redis.host,
                "port": self.redis.port,
                "use_redis": self.redis.use_redis,
            },
            "api": {
                "host": self.api.host,
                "port": self.api.port,
                "workers": self.api.workers,
            },
            "evolution": {
                "enabled": self.evolution.enabled,
                "population_size": self.evolution.population_size,
            },
            "memory": {
                "max_episodic": self.memory.max_episodic_memory,
                "target_mb": self.memory.target_size_mb,
            },
            "monitoring": {
                "log_level": self.monitoring.log_level,
            },
        }

    def validate(self) -> tuple[bool, List[str]]:
        errors: List[str] = []
        if self.security.secret_key == "change-me-in-production" and self.is_production():
            errors.append("SECRET_KEY must be changed in production")
        if self.api.port < 1 or self.api.port > 65535:
            errors.append(f"Invalid port: {self.api.port}")
        if self.evolution.mutation_rate < 0 or self.evolution.mutation_rate > 1:
            errors.append(f"Invalid mutation rate: {self.evolution.mutation_rate}")
        return len(errors) == 0, errors


_settings_instance: Optional[Settings] = None


def get_settings() -> Settings:
    """Get global settings instance."""
    global _settings_instance
    if _settings_instance is None:
        _settings_instance = Settings()
    return _settings_instance
