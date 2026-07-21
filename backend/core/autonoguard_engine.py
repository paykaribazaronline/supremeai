"""AutonoGuard Engine — Zero-Breakage Autonomous Governance Layer.

বাংলা মন্তব্য: এটি SupremeAI-এর একমাত্র Master Agent যা JIT OTP, Immune System Scanning,
Error Remediation এবং Circuit Breaker-কে সমন্বিত করে। Zero silent failure, fully stateless,
IP churn-aware design with Redis-backed distributed state.

Key Features:
- JIT OTP Injection for sensitive operations
- AST Security Scanning before code execution
- Self-Healing Loop with autonomous error remediation
- IP Churn Detection + Fault-Tolerant Context
"""

from __future__ import annotations

import hashlib
import secrets
import time
from typing import Any

from core.cache.redis_manager import redis_manager
from core.config import settings
from core.error_remediation import error_remediator
from core.failure_fingerprint import make_fingerprint
from core.immune_system import ImmuneSystemScanner
from core.messaging.event_bus import ErrorContext, ErrorEvent, error_event_bus
from core.otp_router import send_otp
from core.resilience.circuit_breaker import CircuitBreaker
from loguru import logger
from pydantic import BaseModel

# ── Configuration ─────────────────────────────────────────────────────────────

SENSITIVE_OPS = {
    "/api/v1/admin/",
    "/api/v1/billing/",
    "/api/v1/orchestrate/",
    "/api/v1/skills/execute",
    "/api/v1/system/",
}

ANTI_HACKING_ENABLED = settings.enforce_anti_hacking
OTP_COOLDOWN_SECONDS = settings.otp_cooldown_seconds

_redis_key_prefix = "autonoguard:otp:"
_ip_churn_prefix = "autonoguard:churn:"


# ── Models ───────────────────────────────────────────────────────────────────────


class OperationContext(BaseModel):
    """রিকোয়েস্ট/অপারেশনের পূর্ণ Context।"""

    admin_id: str
    ip_address: str
    path: str
    method: str
    headers: dict[str, str]
    correlation_id: str | None = None


class ChurnDetection(BaseModel):
    """IP Churn Detection result।"""

    is_churn: bool
    previous_ips: list[str]
    first_seen: float
    churn_count: int


# ── Core Engine ────────────────────────────────────────────────────────────────


class AutonoGuardEngine:
    """Unified Autonomous Governance Engine.

    বাংলা: JIT OTP + Immune Scan + Self-Heal + IP Churn Detection-এর একমাত্র এন্ডপইন্ট।
    """

    _circuit_breaker: CircuitBreaker = CircuitBreaker(
        name="autonoguard", failure_threshold=5, recovery_timeout=60.0
    )
    _scanner: ImmuneSystemScanner = ImmuneSystemScanner()

    def __init__(self) -> None:
        self._initialized: bool = False

    async def initialize(self) -> None:
        """Async initialization (idempotent)।"""
        if self._initialized:
            return
        if redis_manager and redis_manager.client:
            await redis_manager.set_cache("autonoguard:boot", "1", ex_seconds=3600)
            logger.info("🔐 AutonoGuard Engine initialized with Redis backing")
        self._initialized = True

    # ── IP Churn Detection ─────────────────────────────────────────────────────

    async def detect_ip_churn(self, admin_id: str, current_ip: str) -> ChurnDetection:
        """Detect IP address thrashing (anomaly indicator)।

        বাংলা: অ্যাডমিনের IP যদি অল্প সময়ে অনেকবার বদলে যায় তাহলে Churn ডিটেক্ট করা হয়।
        এটি Malware Immunity (DNA #5) এর অংশ।
        """
        if not redis_manager or not redis_manager.client:
            return ChurnDetection(
                is_churn=False, previous_ips=[], first_seen=time.time(), churn_count=0
            )

        key = f"{_ip_churn_prefix}{admin_id}"
        # বাংলা: hgetall ব্যবহার করা হচ্ছে যাতে একাধিক IP track করা যায়
        try:
            raw_ips = await redis_manager.client.hgetall(key)
            if isinstance(raw_ips, dict):
                ips = list(raw_ips.keys())
                # Handle bytes/str keys from Redis
                ips = [ip.decode() if isinstance(ip, bytes) else ip for ip in ips]
                first_seen = float(raw_ips.get("first_seen", time.time()))
            else:
                ips = []
                first_seen = time.time()
        except Exception:  # noqa: BLE001
            ips = []
            first_seen = time.time()

        # Track current IP with timestamp
        try:
            await redis_manager.client.hset(key, current_ip, str(time.time()))
            await redis_manager.client.expire(key, 3600)
        except Exception as exc:  # noqa: BLE001
            logger.error(f"Redis churn tracking failed: {exc}")

        # Detect churn: more than 5 different IPs in 1 hour
        churn_count = len(ips) - 1 if ips else 0
        is_churn = churn_count > 5

        return ChurnDetection(
            is_churn=is_churn,
            previous_ips=ips,
            first_seen=first_seen,
            churn_count=churn_count,
        )

    # ── JIT OTP Verification ─────────────────────────────────────────────────────

    async def verify_jit_otp(self, admin_id: str, code: str) -> bool:
        """Verify OTP code with Redis backing.

        বাংলা: request_jit_otp-এ `_redis_key_prefix{admin_id}`-এ OTP-এর sha256 hash (hexdigest) স্টোর হয়।
        তাই এখানে ইনপুট code-এর sha256 compute করে stored hash-এর সাথে compare করা হয়।
        """
        if not redis_manager or not redis_manager.client:
            logger.warning("Redis unavailable for OTP verification")
            return False

        key = f"{_redis_key_prefix}{admin_id}"
        stored_hash = await redis_manager.get_cache(key)
        if not stored_hash:
            return False

        provided_hash = hashlib.sha256(code.encode("utf-8")).hexdigest()

        if secrets.compare_digest(str(stored_hash), provided_hash):
            # Delete the OTP hash after successful verification
            try:
                await redis_manager.client.delete(key)
            except Exception as exc:  # noqa: BLE001
                logger.debug(f"Failed to delete OTP hash key: {exc}")
            logger.info(f"🔓 OTP verified for admin {admin_id}")
            return True

        return False

    async def request_jit_otp(self, admin_id: str, context: dict[str, Any]) -> bool:
        """Request OTP with cooldown enforcement.

        বাংলা: OTP রিকুয়েস্ট করে। Cooldown apply করে।
        Redis-এ OTP-এর sha256 হ্যাশ হিসেবে স্টোর করা হয় যাতে verify_jit_otp deterministic থাকে।
        """
        requested_key = f"{_redis_key_prefix}{admin_id}:requested"
        last_request = (
            await redis_manager.get_cache(requested_key)
            if redis_manager and redis_manager.client
            else None
        )

        if last_request:
            return False  # Cooldown active

        code = f"{secrets.randbelow(1_000_000):06d}"
        code_hash = hashlib.sha256(code.encode()).hexdigest()

        if redis_manager and redis_manager.client:
            await redis_manager.set_cache(
                requested_key, "1", ex_seconds=OTP_COOLDOWN_SECONDS
            )
            # Store only hash for verification
            await redis_manager.set_cache(
                f"{_redis_key_prefix}{admin_id}",
                code_hash,
                ex_seconds=OTP_COOLDOWN_SECONDS * 2,
            )

        return await send_otp(admin_id, code, context)

    async def can_bypass_otp(self, admin_id: str, ip: str) -> bool:
        """Check if OTP can be bypassed based on churn detection।

        বাংলা: IP Churn ডিটেক্ট করে যদি suspicious হয় তাহলে OTP enforce করে।
        """
        if not ANTI_HACKING_ENABLED:
            return True

        churn = await self.detect_ip_churn(admin_id, ip)
        if churn.is_churn:
            logger.warning(
                f"🚨 IP Churn detected for admin {admin_id} ({churn.churn_count} IPs in 1h)"
            )
            return False

        return True

    # ── AST Security Scan ───────────────────────────────────────────────────────

    def scan_for_threats(self, code: str) -> dict[str, Any]:
        """Run AST security scan on generated code।

        বাংলা: কন্ট্রোল করা না হলে Jailbreak attempt detect করে।
        """
        return self._scanner.scan_code(code)

    # ── Self-Healing Loop ───────────────────────────────────────────────────────

    async def heal_error(self, exc: Exception, context: OperationContext) -> str | None:
        """Trigger autonomous error remediation।

        বাংলা: Exception-এর উপর remediation lookup চালায় এবং DLQ-এ emit করে।
        """
        if not self._circuit_breaker.allow_request():
            logger.warning("Circuit breaker open — skipping error remediation")
            return None

        fingerprint = make_fingerprint(exc)
        error_sig = f"{type(exc).__name__}: {str(exc)[:500]}"

        # Emit to Error Event Bus (Anti-Silent Failure)
        await error_event_bus.async_emit(
            ErrorEvent(
                module="autonoguard",
                error_type=f"remediation:{fingerprint[:16]}",
                message=str(exc)[:500],
                severity="ERROR",
                context={"path": context.path, "method": context.method},
                structured_context=ErrorContext(
                    module="autonoguard",
                    user_id=context.admin_id,
                    task_id=context.correlation_id,
                    request_id=context.correlation_id,
                    env=settings.env,
                ),
            )
        )

        # Lookup fix
        fix = await error_remediator.lookup_fix(error_sig)

        if fix:
            logger.info(
                f"🔧 AutonoGuard found remediation for {fingerprint[:16]}: {fix[:80]}"
            )
            self._circuit_breaker.mark_success()
            return fix

        self._circuit_breaker.mark_failure()
        return None

    # ── Enforcement Entry Point ─────────────────────────────────────────────────

    async def enforce_operation(
        self,
        admin_id: str,
        ip: str,
        otp_code: str | None,
        path: str,
        method: str,
        code_to_scan: str | None = None,
    ) -> tuple[bool, str | None]:
        """Main enforcement point for sensitive operations।

        Returns: (is_allowed, error_message)
        """
        # Check IP churn
        if not await self.can_bypass_otp(admin_id, ip):
            return False, "IP anomaly detected — OTP required"

        # JIT OTP check
        if ANTI_HACKING_ENABLED:
            bypass_key = f"{_redis_key_prefix}{admin_id}:bypass"
            bypass_verified = (
                await redis_manager.get_cache(bypass_key)
                if redis_manager and redis_manager.client
                else None
            )

            if not bypass_verified and not otp_code:
                if await self.request_jit_otp(admin_id, {"ip": ip, "path": path}):
                    return False, "OTP sent — provide code to continue"

            if otp_code and not bypass_verified:
                if not await self.verify_jit_otp(admin_id, otp_code):
                    return False, "Invalid OTP code"

                # Mark session bypass
                if redis_manager and redis_manager.client:
                    await redis_manager.set_cache(
                        bypass_key, "1", ex_seconds=OTP_COOLDOWN_SECONDS * 2
                    )

        # AST Security Scan (if code provided)
        if code_to_scan:
            result = self.scan_for_threats(code_to_scan)
            if not result.get("safe"):
                error_msg = result.get("error", "Unknown security threat")
                logger.critical(f"🚨 Security threat blocked: {error_msg}")
                return False, f"Security validation failed: {error_msg}"

        return True, None


# ── Singleton ─────────────────────────────────────────────────────────────────────

autonoguard_engine = AutonoGuardEngine()
