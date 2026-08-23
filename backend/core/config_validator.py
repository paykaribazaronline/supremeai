"""
SupremeAI Configuration Validator — Fail-Fast at Startup
🔬 Evolution v3.0: Schema-based environment variable validation

Validates ALL required environment variables at startup.
Provides clear error messages for misconfiguration.

Usage:
    from core.config_validator import validate_config, ConfigValidationResult
    
    result = validate_config()
    if not result.is_valid:
        print(result.format_errors())
        sys.exit(1)
"""

from __future__ import annotations

import os
import re
import sys
from dataclasses import dataclass, field
from enum import Enum
from typing import Any, Literal


class VarType(str, Enum):
    STRING = "string"
    URL = "url"
    INTEGER = "integer"
    FLOAT = "float"
    BOOLEAN = "boolean"
    LIST = "list"
    ENUM = "enum"


class Severity(str, Enum):
    ERROR = "error"      # Blocks startup
    WARNING = "warning"  # Logs but continues
    INFO = "info"        # Informational only


@dataclass
class VarDefinition:
    """Definition of an environment variable to validate."""
    name: str
    var_type: VarType = VarType.STRING
    required: bool = False
    default: Any = None
    description: str = ""
    pattern: str | None = None  # Regex pattern
    min_value: int | float | None = None
    max_value: int | float | None = None
    allowed_values: list[str] | None = None  # For ENUM type
    severity: Severity = Severity.ERROR
    examples: list[str] = field(default_factory=list)


@dataclass
class ValidationError:
    """Single validation error/warning."""
    var_name: str
    severity: Severity
    message: str
    actual_value: str | None = None
    suggestion: str | None = None


@dataclass
class ConfigValidationResult:
    """Complete validation result."""
    is_valid: bool
    errors: list[ValidationError] = field(default_factory=list)
    warnings: list[ValidationError] = field(default_factory=list)
    validated_vars: dict[str, Any] = field(default_factory=dict)
    
    def format_errors(self) -> str:
        """Format all errors for display."""
        lines = ["=" * 60, "❌ CONFIGURATION VALIDATION FAILED", "=" * 60]
        
        for err in self.errors:
            icon = "🚨" if err.severity == Severity.ERROR else "⚠️"
            lines.append(f"\n{icon} [{err.severity.value.upper()}] {err.var_name}")
            lines.append(f"   {err.message}")
            if err.actual_value:
                lines.append(f"   Actual value: '{err.actual_value}'")
            if err.suggestion:
                lines.append(f"   💡 Suggestion: {err.suggestion}")
        
        lines.extend(["", "-" * 40, f"Total errors: {len(self.errors)}, Warnings: {len(self.warnings)}"])
        return "\n".join(lines)


# ==========================================================================
# CONFIGURATION SCHEMA — Define ALL environment variables here
# ==========================================================================

CONFIG_SCHEMA: list[VarDefinition] = [
    # --- Core ---
    VarDefinition(name="ENV", var_type=VarType.ENUM, required=True,
                  allowed_values=["development", "staging", "production"],
                  description="Application environment",
                  examples=["development", "production"]),
    VarDefinition(name="PORT", var_type=VarType.INTEGER, default=8080,
                  min_value=1024, max_value=65535,
                  description="Server port"),
    VarDefinition(name="HOST", default="0.0.0.0",
                  description="Server bind address"),
    
    # --- Backend URLs ---
    VarDefinition(name="BACKEND_URL", var_type=VarType.URL,
                  required=True, severity=Severity.WARNING,
                  pattern=r"^https?://.+",
                  description="Public backend URL",
                  examples=["https://supremeai-backend.onrender.com"]),
    
    # --- CORS ---
    VarDefinition(name="USER_CORS_ORIGINS", var_type=VarType.LIST,
                  default=[],
                  description="Allowed CORS origins for user portal",
                  examples=['["https://supremeai.web.app"]']),
    VarDefinition(name="ADMIN_CORS_ORIGINS", var_type=VarType.LIST,
                  default=[],
                  description="Allowed CORS origins for admin portal"),
    
    # --- Security ---
    VarDefinition(name="JWT_SECRET", var_type=VarType.STRING,
                  required=True, severity=Severity.ERROR,
                  min_value=32,
                  description="JWT signing secret (min 32 chars)",
                  examples=["your-super-secret-key-at-least-32-chars"]),
    VarDefinition(name="ENFORCE_ANTI_HACKING", var_type=VarType.BOOLEAN,
                  default=False,
                  description="Enable anti-hacking measures"),
    
    # --- Database ---
    VarDefinition(name="DATABASE_URL", var_type=VarType.URL,
                  severity=Severity.WARNING,
                  description="Database connection URL"),
    
    # --- LLM Providers ---
    VarDefinition(name="GEMINI_API_KEY", var_type=VarType.STRING,
                  severity=Severity.INFO,
                  description="Google Gemini API key"),
    VarDefinition(name="GROQ_API_KEY", var_type=VarType.STRING,
                  severity=Severity.INFO,
                  description="Groq API key"),
    VarDefinition(name="OPENROUTER_API_KEY", var_type=VarType.STRING,
                  severity=Severity.INFO,
                  description="OpenRouter API key"),
    
    # --- Rate Limits ---
    VarDefinition(name="GEMINI_RPM_LIMIT", var_type=VarType.INTEGER,
                  default=9, min_value=1, max_value=1000),
    VarDefinition(name="GROQ_RPM_LIMIT", var_type=VarType.INTEGER,
                  default=28, min_value=1, max_value=1000),
    VarDefinition(name="OPENROUTER_RPM_LIMIT", var_type=VarType.INTEGER,
                  default=19, min_value=1, max_value=1000),
    
    # --- Scraper Service ---
    VarDefinition(name="SCRAPER_MAX_CONCURRENCY", var_type=VarType.INTEGER,
                  default=3, min_value=1, max_value=10),
    VarDefinition(name="SCRAPER_TIMEOUT_SECONDS", var_type=VarType.INTEGER,
                  default=45, min_value=10, max_value=300),
    
    # --- Feature Flags ---
    VarDefinition(name="SELF_HEALING_ENABLED", var_type=VarType.BOOLEAN,
                  default=True,
                  description="Enable self-healing mode"),
    VarDefinition(name="COST_GUARD_ENABLED", var_type=VarType.BOOLEAN,
                  default=True,
                  description="Enable cost guard"),
]


def _validate_var(var_def: VarDefinition) -> ValidationError | None:
    """Validate a single environment variable."""
    raw_value = os.getenv(var_def.name)
    value = raw_value if raw_value is not None else var_def.default

    # Check required
    if var_def.required and value is None:
        return ValidationError(
            var_name=var_def.name,
            severity=var_def.severity,
            message=f"Required variable is not set. {var_def.description}",
            suggestion=f"Set {var_def.name}={'<value>' if not var_def.examples else var_def.examples[0]}",
        )

    # Use default if empty
    if value is None:
        return None

    # Type-specific validation
    if var_def.var_type == VarType.URL and value:
        if not re.match(var_def.pattern or r"^https?://.+", str(value)):
            return ValidationError(
                var_name=var_def.name,
                severity=var_def.severity,
                message=f"Invalid URL format: '{value}'",
                suggestion="URL must start with http:// or https://",
                actual_value=str(value),
            )

    elif var_def.var_type == VarType.ENUM and var_def.allowed_values:
        if str(value) not in var_def.allowed_values:
            return ValidationError(
                var_name=var_def.name,
                severity=var_def.severity,
                message=f"Invalid value: '{value}'. Must be one of: {var_def.allowed_values}",
                actual_value=str(value),
            )

    elif var_def.var_type == VarType.INTEGER:
        try:
            int_val = int(value)
            if var_def.min_value is not None and int_val < var_def.min_value:
                return ValidationError(var_name=var_def.name, severity=Severity.WARNING,
                    message=f"Value {int_val} below minimum {var_def.min_value}")
            if var_def.max_value is not None and int_val > var_def.max_value:
                return ValidationError(var_name=var_def.name, severity=Severity.WARNING,
                    message=f"Value {int_val} above maximum {var_def.max_value}")
        except (ValueError, TypeError):
            return ValidationError(var_name=var_def.name, severity=var_def.severity,
                message=f"Invalid integer: '{value}'")

    elif var_def.var_type == VarType.BOOLEAN:
        if str(value).lower() not in ("true", "false", "1", "0", "", "none"):
            return ValidationError(var_name=var_def.name, severity=Severity.WARNING,
                message=f"Invalid boolean: '{value}'. Use true/false/1/0")

    # Pattern match
    if var_def.pattern and value:
        if not re.match(var_def.pattern, str(value)):
            return ValidationError(var_name=var_def.name, severity=var_def.severity,
                message=f"Value doesn't match pattern {var_def.pattern}",
                actual_value=str(value))

    return None


def validate_config() -> ConfigValidationResult:
    """Validate all configuration variables. Returns validation result."""
    errors = []
    warnings = []
    validated = {}

    for var_def in CONFIG_SCHEMA:
        error = _validate_var(var_def)
        raw = os.getenv(var_def.name)
        validated[var_def.name] = raw if raw is not None else var_def.default

        if error:
            if error.severity == Severity.ERROR:
                errors.append(error)
            else:
                warnings.append(error)

    return ConfigValidationResult(
        is_valid=len(errors) == 0,
        errors=errors,
        warnings=warnings,
        validated_vars=validated,
    )


def print_config_summary() -> None:
    """Print masked configuration summary for debugging."""
    print("\n" + "=" * 50)
    print("🔧 Configuration Summary")
    print("=" * 50)
    
    sensitive_keys = ("SECRET", "KEY", "PASSWORD", "TOKEN")
    
    for var_def in CONFIG_SCHEMA:
        value = os.getenv(var_def.name, var_def.default)
        if value is None:
            display = "⟨not set⟩"
        elif any(s in var_def.name for s in sensitive_keys):
            display = "*****" if len(str(value)) > 0 else "⟨empty⟩"
        else:
            display = str(value)[:50] + ("..." if len(str(value)) > 50 else "")
        
        req_marker = " ✗" if var_def.required and value is None else " ✓"
        print(f"  {var_def.name:<30} = {display:<55}{req_marker}")
    
    print("=" * 50 + "\n")
# =============================================================================
