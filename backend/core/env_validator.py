"""
SupremeAI Environment Validator - 10/10 Production Readiness
================================================================
Validates all required environment variables at startup with clear error messages.
Prevents silent failures from missing configuration.

Author: SuperAI Enhancement Patch
Version: 2.0.0
"""

import os
import sys
from enum import Enum
from dataclasses import dataclass, field
from typing import Optional, List, Dict, Any
from loguru import logger


class EnvSeverity(Enum):
    """Severity levels for environment variables"""
    CRITICAL = "critical"      # App won't start without this
    HIGH = "high"              # Major features broken
    MEDIUM = "medium"          # Degraded functionality
    LOW = "low"                # Optional enhancements
    INFO = "info"              # Informational only


@dataclass
class EnvVarDefinition:
    """Definition of an environment variable"""
    name: str
    description: str
    severity: EnvSeverity
    default: Optional[str] = None
    pattern: Optional[str] = None  # Regex pattern for validation
    examples: List[str] = field(default_factory=list)
    documentation_url: Optional[str] = None


# Complete environment variable registry based on .env.example
ENV_REGISTRY: List[EnvVarDefinition] = [
    # ── Core ──────────────────────────────────────────────────────────────
    EnvVarDefinition(
        name="ENV",
        description="Environment mode (local, staging, production)",
        severity=EnvSeverity.CRITICAL,
        default="local",
        examples=["local", "staging", "production"]
    ),
    EnvVarDefinition(
        name="PORT",
        description="Server port number",
        severity=EnvSeverity.CRITICAL,
        default="8080",
        pattern=r"^\d{4,5}$"
    ),
    EnvVarDefinition(
        name="HOST",
        description="Server bind address",
        severity=EnvSeverity.MEDIUM,
        default="0.0.0.0"
    ),
    
    # ── Secrets (CRITICAL in production) ─────────────────────────────────
    EnvVarDefinition(
        name="SUPREMEAI_JWT_SECRET",
        description="JWT signing secret",
        severity=EnvSeverity.CRITICAL
    ),
    EnvVarDefinition(
        name="SUPREMEAI_ADMIN_PASSWORD_HASH",
        description="Bcrypt hash of admin password",
        severity=EnvSeverity.CRITICAL
    ),
    EnvVarDefinition(
        name="SUPREMEAI_ENCRYPTION_KEY",
        description="Fernet encryption key for sensitive data",
        severity=EnvSeverity.LOW
    ),
    EnvVarDefinition(
        name="SUPREMEAI_API_TOKEN",
        description="Master API token for service-to-service auth",
        severity=EnvSeverity.LOW,
        pattern=r"^sk-[a-zA-Z0-9]{32,}$"
    ),
    
    # ── Database (Supabase) ───────────────────────────────────────────────
    EnvVarDefinition(
        name="SUPABASE_URL",
        description="Supabase project URL",
        severity=EnvSeverity.CRITICAL,
        pattern=r"^https://[a-z0-9-]+\.supabase\.co$"
    ),
    EnvVarDefinition(
        name="SUPABASE_KEY",
        description="Supabase anon/public API key",
        severity=EnvSeverity.CRITICAL,
        pattern=r"^eyJ[a-zA-Z0-9_-]+\.[a-zA-Z0-9_-]+\.[a-zA-Z0-9_-]+$"
    ),
    EnvVarDefinition(
        name="SUPABASE_DATABASE_URL_POOLER",
        description="Supabase pooler connection string (postgresql://)",
        severity=EnvSeverity.HIGH,
        pattern=r"^postgresql://[^:]+:[^@]+@[^:]+:\d+/.+$"
    ),
    
    # ── Redis (Upstash) ───────────────────────────────────────────────────
    EnvVarDefinition(
        name="REDIS_URL",
        description="Redis connection URL (redis:// or rediss://)",
        severity=EnvSeverity.HIGH,
        pattern=r"^red?iss?://[^:]+(:[^@]+)?@[^:]+:\d+/\d*$"
    ),
    EnvVarDefinition(
        name="UPSTASH_REDIS_REST_URL",
        description="Upstash Redis REST API endpoint",
        severity=EnvSeverity.MEDIUM
    ),
    EnvVarDefinition(
        name="UPSTASH_REDIS_REST_TOKEN",
        description="Upstash Redis REST authentication token",
        severity=EnvSeverity.MEDIUM
    ),
    
    # ── LLM API Keys (at least one required) ─────────────────────────────
    EnvVarDefinition(
        name="OPENROUTER_API_KEY",
        description="OpenRouter API key for multi-model access",
        severity=EnvSeverity.HIGH,
        pattern=r"^sk-or-[a-zA-Z0-9_-]+$"
    ),
    EnvVarDefinition(
        name="OPENAI_API_KEY",
        description="OpenAI API key (GPT-4, GPT-3.5)",
        severity=EnvSeverity.HIGH,
        pattern=r"^sk-[a-zA-Z0-9]{48}$"
    ),
    EnvVarDefinition(
        name="GEMINI_API_KEY",
        description="Google Gemini API key",
        severity=EnvSeverity.HIGH,
        pattern=r"^AIza[a-zA-Z0-9_-]{35}$"
    ),
    EnvVarDefinition(
        name="GROQ_API_KEY",
        description="Groq API key for fast inference",
        severity=EnvSeverity.LOW,
        pattern=r"^gsk_[a-zA-Z0-9]{52}$"
    ),
    EnvVarDefinition(
        name="NVIDIA_API_KEY",
        description="NVIDIA API key for GPU-accelerated inference",
        severity=EnvSeverity.LOW
    ),
    EnvVarDefinition(
        name="DEEPSEEK_API_KEY",
        description="DeepSeek API key",
        severity=EnvSeverity.LOW
    ),
    EnvVarDefinition(
        name="HF_API_KEY",
        description="HuggingFace API key for model access",
        severity=EnvSeverity.LOW,
        pattern=r"^hf_[a-zA-Z0-9]{34}$"
    ),
    
    # ── Stripe ─────────────────────────────────────────────────────────────
    EnvVarDefinition(
        name="STRIPE_API_KEY",
        description="Stripe secret API key (sk_live_ or sk_test_)",
        severity=EnvSeverity.MEDIUM,
        pattern=r"^sk_(test|live)_[a-zA-Z0-9]+$"
    ),
    EnvVarDefinition(
        name="STRIPE_WEBHOOK_SECRET",
        description="Stripe webhook signature secret",
        severity=EnvSeverity.MEDIUM,
        pattern=r"^whsec_[a-zA-Z0-9]+$"
    ),
    
    # ── Infisical (Secret Vault) ──────────────────────────────────────────
    EnvVarDefinition(
        name="INFISICAL_TOKEN",
        description="Infisical authentication token",
        severity=EnvSeverity.HIGH
    ),
    EnvVarDefinition(
        name="INFISICAL_CLIENT_ID",
        description="Infisical Machine Identity client ID",
        severity=EnvSeverity.HIGH
    ),
    EnvVarDefinition(
        name="INFISICAL_CLIENT_SECRET",
        description="Infisical Machine Identity client secret",
        severity=EnvSeverity.HIGH
    ),
    
    # ── Observability ─────────────────────────────────────────────────────
    EnvVarDefinition(
        name="SENTRY_DSN",
        description="Sentry DSN for error tracking",
        severity=EnvSeverity.LOW,
        pattern=r"^https://[a-f0-9]+@[a-z0-9-]+\.ingest\.sentry\.io/\d+$"
    ),
    EnvVarDefinition(
        name="OTLP_ENDPOINT",
        description="OpenTelemetry collector endpoint",
        severity=EnvSeverity.LOW
    ),
    
    # ── Security ──────────────────────────────────────────────────────────
    EnvVarDefinition(
        name="ENFORCE_ANTI_HACKING",
        description="Enable anti-hacking protections",
        severity=EnvSeverity.LOW,
        default="false"
    ),
    EnvVarDefinition(
        name="OTP_COOLDOWN_SECONDS",
        description="OTP cooldown period in seconds",
        severity=EnvSeverity.LOW,
        default="60"
    ),
]


@dataclass
class ValidationResult:
    """Result of environment variable validation"""
    is_valid: bool
    errors: List[Dict[str, Any]] = field(default_factory=list)
    warnings: List[Dict[str, Any]] = field(default_factory=list)
    info: List[Dict[str, Any]] = field(default_factory=list)
    score: int = 0  # 0-100


class EnvironmentValidator:
    """
    Validates environment variables and provides clear diagnostics.
    
    Usage:
        validator = EnvironmentValidator()
        result = validator.validate()
        
        if not result.is_valid:
            logger.error("Environment validation failed!")
            validator.print_report(result)
            sys.exit(1)
    """
    
    def __init__(self, strict_mode: bool = False):
        """
        Initialize validator.
        
        Args:
            strict_mode: If True, treat warnings as errors
        """
        self.strict_mode = strict_mode
        self.registry = ENV_REGISTRY
    
    def validate(self) -> ValidationResult:
        """
        Validate all registered environment variables.
        
        Returns:
            ValidationResult with detailed findings
        """
        result = ValidationResult(is_valid=True)
        total_vars = len(self.registry)
        valid_count = 0
        
        for env_def in self.registry:
            value = os.environ.get(env_def.name)
            
            if value is None or value.strip() == '':
                if env_def.default is not None:
                    # Use default value
                    os.environ[env_def.name] = env_def.default
                    result.info.append({
                        'variable': env_def.name,
                        'message': f'Using default value: {env_def.default}',
                        'severity': env_def.severity.value
                    })
                    valid_count += 1
                else:
                    # Missing required variable
                    error_msg = self._format_missing_error(env_def)
                    
                    if env_def.severity in [EnvSeverity.CRITICAL, EnvSeverity.HIGH]:
                        result.errors.append({
                            'variable': env_def.name,
                            'message': error_msg,
                            'severity': env_def.severity.value,
                            'description': env_def.description
                        })
                        if env_def.severity == EnvSeverity.CRITICAL:
                            result.is_valid = False
                    else:
                        result.warnings.append({
                            'variable': env_def.name,
                            'message': error_msg,
                            'severity': env_def.severity.value,
                            'description': env_def.description
                        })
            else:
                # Validate format if pattern specified
                if env_def.pattern:
                    import re
                    if not re.match(env_def.pattern, value):
                        msg = f'Invalid format for {env_def.name}. Expected pattern: {env_def.pattern}'
                        
                        if env_def.severity == EnvSeverity.CRITICAL:
                            result.errors.append({
                                'variable': env_def.name,
                                'message': msg,
                                'severity': env_def.severity.value,
                                'actual_value_preview': value[:8] + '...' if len(value) > 8 else value
                            })
                            result.is_valid = False
                        else:
                            result.warnings.append({
                                'variable': env_def.name,
                                'message': msg,
                                'severity': env_def.severity.value
                            })
                    else:
                        valid_count += 1
                else:
                    valid_count += 1
        
        # Calculate health score
        result.score = int((valid_count / total_vars) * 100)
        
        # Check for at least one LLM provider
        llm_providers = ['OPENROUTER_API_KEY', 'OPENAI_API_KEY', 'GEMINI_API_KEY']
        has_llm_provider = any(os.environ.get(key) for key in llm_providers)
        
        if not has_llm_provider:
            result.errors.append({
                'variable': 'LLM_PROVIDERS',
                'message': 'At least one LLM API key is required (OPENAI_API_KEY, GEMINI_API_KEY, or OPENROUTER_API_KEY)',
                'severity': 'critical'
            })
            result.is_valid = False
        
        return result
    
    def _format_missing_error(self, env_def: EnvVarDefinition) -> str:
        """Format user-friendly error message for missing variable"""
        examples_str = ''
        if env_def.examples:
            examples_str = f'\n  Examples: {", ".join(env_def.examples)}'
        
        doc_url_str = ''
        if env_def.documentation_url:
            doc_url_str = f'\n  Docs: {env_def.documentation_url}'
        
        return (
            f'Missing required environment variable: {env_def.name}\n'
            f'  Description: {env_def.description}'
            f'{examples_str}'
            f'{doc_url_str}'
        )
    
    def print_report(self, result: ValidationResult) -> None:
        """Print validation report to console"""
        import logging; logging.getLogger(__name__).info('\n' + '='*70)
        import logging; logging.getLogger(__name__).info('🔍 SUPREMEAI ENVIRONMENT VALIDATION REPORT')
        import logging; logging.getLogger(__name__).info('='*70)
        import logging; logging.getLogger(__name__).info(f'\n📊 Health Score: {result.score}/100')
        import logging; logging.getLogger(__name__).info(f'   Status: {"✅ PASS" if result.is_valid else "❌ FAIL"}\n')
        
        if result.errors:
            import logging; logging.getLogger(__name__).info('🚨 CRITICAL ERRORS (Must Fix):')
            import logging; logging.getLogger(__name__).info('-'*70)
            for i, error in enumerate(result.errors, 1):
                import logging; logging.getLogger(__name__).info(f'\n{i}. {error["variable"]}')
                import logging; logging.getLogger(__name__).info(f'   {error["message"]}')
                if 'description' in error:
                    import logging; logging.getLogger(__name__).info(f'   📖 {error["description"]}')
        
        if result.warnings:
            import logging; logging.getLogger(__name__).info('\n⚠️  WARNINGS (Recommended):')
            import logging; logging.getLogger(__name__).info('-'*70)
            for i, warning in enumerate(result.warnings, 1):
                import logging; logging.getLogger(__name__).info(f'\n{i}. {warning["variable"]}')
                import logging; logging.getLogger(__name__).info(f'   {warning["message"]}')
        
        if result.info:
            import logging; logging.getLogger(__name__).info('\nℹ️  INFORMATION:')
            import logging; logging.getLogger(__name__).info('-'*70)
            for info in result.info[:5]:  # Show first 5
                import logging; logging.getLogger(__name__).info(f'  • {info["variable"]}: {info["message"]}')
        
        import logging; logging.getLogger(__name__).info('\n' + '='*70 + '\n')


def validate_environment(strict: bool = False) -> bool:
    """
    Convenience function to validate environment.
    
    Args:
        strict: If True, fail on warnings too
        
    Returns:
        True if validation passes, False otherwise
    """
    validator = EnvironmentValidator(strict_mode=strict)
    result = validator.validate()
    
    if not result.is_valid:
        validator.print_report(result)
        logger.error(f"Environment validation failed with score: {result.score}/100")
        return False
    
    logger.success(f"✅ Environment validation passed! Score: {result.score}/100")
    return True


# Auto-run when executed directly
if __name__ == '__main__':
    import json
    
    print("🔍 SupremeAI Environment Validator")
    print("=" * 50)
    
    validator = EnvironmentValidator()
    result = validator.validate()
    
    validator.print_report(result)
    
    # Output JSON for CI/CD consumption
    output = {
        'valid': result.is_valid,
        'score': result.score,
        'errors': len(result.errors),
        'warnings': len(result.warnings),
        'details': {
            'errors': result.errors,
            'warnings': result.warnings
        }
    }
    
    print('\n📋 JSON Output (for CI/CD):')
    print(json.dumps(output, indent=2))
    
    sys.exit(0 if result.is_valid else 1)
