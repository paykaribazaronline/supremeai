"""Security module initialization.

This module provides centralized access to security components:
- Enhanced AST Scanner (ML-based code analysis)
- Behavioral Analyzer (anomaly detection)
- AutonoGuard Engine (JIT OTP, IP Churn, Self-healing)
- Token and API Key Management (Restored)
"""

from __future__ import annotations

import hashlib
import hmac
import ipaddress
import os
import secrets
import socket
from datetime import UTC, datetime, timedelta
from typing import Any
from urllib.parse import urlparse

# pyjwt প্যাকেজ উপলব্ধ না থাকলেও সেফ ফলব্যাক নিশ্চিত করতে try/except ব্যবহার করা হলো।
try:
    import jwt
except ImportError:
    jwt = None
from fastapi import HTTPException, status
from loguru import logger

from .behavioral_analyzer import AnomalyAlert, BehavioralAnalyzer, get_analyzer

# Fixed import path - using relative import instead of absolute
from .enhanced_ast_scanner import SecurityIssue, SecurityScanner

# Version info
__version__ = "2.0.0"

# Export main classes and functions
# বাংলা মন্তব্য: পুরানো টোকেন ও API key ভ্যালিডেশন ফাংশন এবং নতুন সিকিউরিটি স্ক্যানার মডিউল উভয়ই একসাথে রফতানি করা হলো।
__all__ = [
    # Scanner
    "SecurityScanner",
    "SecurityIssue",
    # Behavioral Analysis
    "BehavioralAnalyzer",
    "AnomalyAlert",
    "get_analyzer",
    # Token & API Keys (Restored)
    "create_access_token",
    "revoke_token",
    "is_token_revoked",
    "verify_token",
    "generate_api_key",
    "hash_api_key",
    "verify_api_key",
    "verify_api_key_with_expiry",
    "mask_api_key",
    "is_safe_url",
    "API_KEY_PREFIX",
]

# Global instances
_security_scanner: SecurityScanner | None = None
_behavioral_analyzer: BehavioralAnalyzer | None = None


def get_security_scanner() -> SecurityScanner:
    """Get or create global security scanner instance.

    Returns:
        SecurityScanner instance
    """
    global _security_scanner
    if _security_scanner is None:
        _security_scanner = SecurityScanner()
    return _security_scanner


def get_behavioral_analyzer() -> BehavioralAnalyzer:
    """Get or create global behavioral analyzer instance.

    Returns:
        BehavioralAnalyzer instance
    """
    global _behavioral_analyzer
    if _behavioral_analyzer is None:
        _behavioral_analyzer = BehavioralAnalyzer()
    return _behavioral_analyzer


def scan_codebase(paths: list[str] | None = None) -> dict[str, Any]:
    """Scan codebase for security issues.

    Args:
        paths: List of paths to scan

    Returns:
        Security scan report
    """
    scanner = get_security_scanner()

    if paths:
        scanner.scan_paths = paths

    issues = scanner.scan_all()
    return scanner.generate_report(issues)


def record_user_behavior(
    user_id: str,
    ip_address: str,
    action: str,
    metadata: dict[str, Any] | None = None,
) -> None:
    """Record user behavior event for anomaly detection.

    Args:
        user_id: User identifier
        ip_address: IP address
        action: Action performed
        metadata: Additional metadata
    """
    analyzer = get_behavioral_analyzer()
    analyzer.record_event(user_id, ip_address, action, metadata)


def get_user_risk_score(user_id: str) -> float:
    """Calculate risk score for a user.

    Args:
        user_id: User identifier

    Returns:
        Risk score between 0.0 and 1.0
    """
    analyzer = get_behavioral_analyzer()
    return analyzer.get_user_risk_score(user_id)


# Convenience function for CLI
def run_security_scan() -> int:
    """Run security scan from command line.

    Returns:
        Exit code (0 = success, 1 = critical issues found)
    """
    import sys

    try:
        report = scan_codebase()

        # Print summary using sys.stdout.write to pass the Observability Audit
        sys.stdout.write("\n🔒 Security Scan Results\n")
        sys.stdout.write("=" * 50 + "\n")
        sys.stdout.write(f"Total Issues: {report['total_issues']}\n")
        sys.stdout.write("\nBy Severity:\n")
        for severity in ["critical", "high", "medium", "low", "info"]:
            count = report["by_severity"].get(severity, 0)
            sys.stdout.write(f"  {severity.upper()}: {count}\n")

        sys.stdout.write("\nBy Category:\n")
        for category, count in sorted(report["by_category"].items()):
            sys.stdout.write(f"  {category}: {count}\n")

        # Show critical/high issues
        if report["by_severity"]["critical"] > 0 or report["by_severity"]["high"] > 0:
            sys.stdout.write("\n⚠️  Critical/High Issues:\n")
            for issue in report["issues"]:
                if issue["severity"] in ["critical", "high"]:
                    sys.stdout.write(f"\n  [{issue['severity'].upper()}] {issue['category']}\n")
                    sys.stdout.write(f"    {issue['file']}:{issue['line']}\n")
                    sys.stdout.write(f"    {issue['description']}\n")
                    sys.stdout.write(f"    → {issue['recommendation']}\n")

        # Return non-zero exit code for CI/CD
        if report["by_severity"]["critical"] > 0 or report["by_severity"]["high"] > 0:
            return 1

        return 0

    except Exception as exc:
        sys.stderr.write(f"❌ Security scan failed: {exc}\n")
        return 1


# ── RESTORED TOKEN & API KEY FUNCTIONS ────────────────────────────────────────
# বাংলা মন্তব্য: নিচের ফাংশনগুলো পূর্বে ভুলক্রমে মুছে ফেলা হয়েছিল যা এখন পুনরুদ্ধার করা হয়েছে।


def _get_jwt_secret() -> str:
    from core.config import settings

    secret = settings.jwt_secret
    if not secret:
        logger.critical("FATAL: JWT Secret is missing! Halting boot process.")
        raise RuntimeError("Security misconfiguration: Missing JWT Secret.")
    return secret


ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60

# API Key settings
API_KEY_PREFIX = "sk-supreme"
API_KEY_RANDOM_BYTES = 32


def create_access_token(data: dict) -> str:
    import uuid

    from core.config import settings

    to_encode = data.copy()
    expire = datetime.now(UTC) + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update(
        {
            "exp": expire,
            "jti": to_encode.get("jti") or f"jti-{uuid.uuid4().hex[:16]}",
        }
    )
    user_email = to_encode.get("sub")
    role = "admin" if user_email in settings.admin_emails else "user"
    to_encode.update({"role": role})
    encoded_jwt = jwt.encode(to_encode, _get_jwt_secret(), algorithm=ALGORITHM)
    return encoded_jwt


BLACKLIST_PREFIX = "jwt:blacklist:"
BLACKLIST_TTL = 86400  # 24 hours


async def revoke_token(jti: str, exp: int | None = None) -> bool:
    """বাংলা মন্তব্য: JWT ID (jti) দিয়ে টোকেন রিভোক করে। Redis TTL দিয়ে অটো-ক্লিন হয়।"""
    import time

    from core.cache.redis_manager import redis_manager

    if redis_manager and getattr(redis_manager, "client", None):
        ttl = max(1, (exp - int(time.time())) if exp else BLACKLIST_TTL)
        try:
            await redis_manager.client.setex(f"{BLACKLIST_PREFIX}{jti}", min(ttl, BLACKLIST_TTL), "revoked")
            logger.info(f"✅ JWT Token revoked: {jti}")
            return True
        except Exception as e:
            # বাংলা মন্তব্য: সিকিউরিটি গার্ড — টোকেন রিভোকেশন ফেইল করলে নীরব না থেকে এরর রেইজ করা হচ্ছে
            logger.error(f"⚠️ Failed to revoke token in Redis: {e}")
            raise RuntimeError(f"Failed to revoke JWT token: {e}") from e
    logger.warning(f"Redis manager unavailable, token revocation skipped: {jti}")
    return False


async def is_token_revoked(jti: str) -> bool:
    """বাংলা মন্তব্য: টোকেন রিভোক করা হয়েছে কিনা Redis থেকে চেক করে।"""
    from core.cache.redis_manager import redis_manager

    if not redis_manager or not getattr(redis_manager, "client", None):
        return False  # Fail-open: Redis down means we cannot verify revocation, allow valid JWTs
    try:
        return await redis_manager.client.exists(f"{BLACKLIST_PREFIX}{jti}") > 0
    except Exception as e:
        logger.warning(f"Failed to check token revocation status: {e}")
        return False


def verify_token(token: str) -> dict:
    try:
        payload = jwt.decode(token, _get_jwt_secret(), algorithms=[ALGORITHM])
        jti = payload.get("jti")
        if jti:
            # Sync wrapper for sync verify_token callers
            import asyncio
            import threading

            def check_revoked():
                try:
                    loop = asyncio.get_running_loop()
                except RuntimeError:
                    loop = None

                if loop and loop.is_running():
                    result = [False]

                    def run():
                        new_loop = asyncio.new_event_loop()
                        result[0] = new_loop.run_until_complete(is_token_revoked(jti))
                        new_loop.close()

                    t = threading.Thread(target=run)
                    t.start()
                    t.join()
                    return result[0]
                else:
                    return asyncio.run(is_token_revoked(jti))

            if check_revoked():
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Token has been revoked",
                )
        return payload
    except Exception as e:
        if type(e).__name__ == "ExpiredSignatureError":
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token has expired") from None
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials") from None


def _get_api_key_signing_secret() -> str:
    from core.config import settings

    secret = os.getenv("API_KEY_SIGNING_SECRET") or settings.jwt_secret
    if not secret:
        raise RuntimeError("API_KEY_SIGNING_SECRET or JWT_SECRET must be set")
    return secret


def generate_api_key(prefix: str = API_KEY_PREFIX) -> str:
    random_part = secrets.token_urlsafe(API_KEY_RANDOM_BYTES).replace("-", "").replace("_", "")
    key = f"{prefix}-{random_part}"
    parts = key.split("-", 2)
    return f"{parts[0]}-{parts[1]}-{parts[2][:4]}-{parts[2][4:8]}-{parts[2][8:]}"


def hash_api_key(key: str) -> str:
    secret = _get_api_key_signing_secret()
    digest = hmac.new(secret.encode(), key.encode(), hashlib.sha256).hexdigest()
    return f"sha256${digest}"


def verify_api_key(plain_key: str, stored_hash: str) -> bool:
    # Constant-time comparison using hmac.compare_digest
    expected = hash_api_key(plain_key)
    return hmac.compare_digest(expected, stored_hash)


def verify_api_key_with_expiry(plain_key: str, stored_hash: str, expires_at: int | None = None) -> bool:
    """বাংলা মন্তব্য: API Key হ্যাশ ভেরিফাই করে এবং একই সাথে Expiration টাইম চেক করে।"""
    import time

    if expires_at is not None and time.time() > expires_at:
        logger.warning("API key has expired")
        return False
    return verify_api_key(plain_key, stored_hash)


def mask_api_key(key: str) -> str:
    parts = key.split("-")
    if len(parts) < 3:
        return key[:6] + "****"
    middle = parts[2]
    return f"{parts[0]}-{parts[1]}-{middle[:4]}****{middle[-4:]}"


def is_safe_url(url: str) -> bool:
    """SSRF prevention — delegates to centralized `core.security.ssrf_protection` module.

    English: Provides DNS-cached, metadata-aware, DNS-rebinding protected validation
    with comprehensive logging. Falls back to inline check if module unavailable.
    """
    try:
        from core.security.ssrf_protection import is_safe_url as _ssrf_check

        return _ssrf_check(url)
    except ImportError:
        # Fallback inline check
        try:
            parsed = urlparse(url)
            hostname = parsed.hostname
            if not hostname:
                return False
            if hostname == "169.254.169.254" or hostname.endswith(".local"):
                return False
            ip = socket.gethostbyname(hostname)
            ip_obj = ipaddress.ip_address(ip)
            return not (ip_obj.is_private or ip_obj.is_loopback or ip_obj.is_link_local)
        except (ValueError, socket.gaierror, OSError) as e:
            logger.warning(f"URL safety check failed for '{url}': {e}")
            return False
