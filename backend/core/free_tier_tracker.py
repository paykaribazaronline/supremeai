"""
free_tier_tracker.py
====================
Added by Agent Antigravity on 2026-06-22

Tracks per-provider free tier usage (RPM, TPM, RPD) with rolling windows.
Automatically pauses a provider when limits are near-exhausted and selects
the best available free provider for each request.

Supports optional Redis persistence for multi-worker environments.
"""

from __future__ import annotations

import time
from collections import deque
from dataclasses import dataclass
from dataclasses import field
from typing import Any

from loguru import logger


# ---------------------------------------------------------------------------
# Free-tier limit configuration for each provider
# These are intentionally conservative (5% buffer below official limits)
# to avoid hitting rate-limit errors in production.
# ---------------------------------------------------------------------------
DEFAULT_LIMITS: dict[str, dict[str, int]] = {
    "gemini": {
        "rpm": 9,  # official: 10  (buffer -1)
        "tpm": 240_000,  # official: 250k
        "rpd": 475,  # official: 500 (buffer -25)
    },
    "groq": {
        "rpm": 28,  # official: 30  (buffer -2)
        "tpm": 28_500,  # official: 30k
        "rpd": 13_680,  # official: 14,400 (buffer -720)
    },
    "openrouter": {
        "rpm": 19,  # official: 20  (buffer -1)
        "tpm": 999_999,  # no enforced TPM
        "rpd": 45,  # official: 50 (buffer -5); upgrade to 950 after $10 spend
    },
    "cloudflare": {
        "rpm": 999_999,  # essentially unlimited
        "tpm": 999_999,
        "rpd": 9_000,  # conservative estimate ~10k
    },
    "nvidia": {
        "rpm": 38,  # official: 40 (buffer -2)
        "tpm": 38_000,  # official: 40k
        "rpd": 999_999,  # no published daily limit
    },
    "huggingface": {
        "rpm": 18,  # official: ~20 (buffer -2)
        "tpm": 999_999,
        "rpd": 950,  # official: ~1,000 (buffer -50)
    },
    "ollama": {
        "rpm": 999_999,  # local — unlimited
        "tpm": 999_999,
        "rpd": 999_999,
    },
    "deepseek": {
        "rpm": 999_999,  # pay-as-you-go — not a free tier; treated as unlimited
        "tpm": 999_999,
        "rpd": 999_999,
    },
}

# Priority order: prefer highest-quality free providers first
FREE_PROVIDER_PRIORITY: list[str] = [
    "gemini",
    "groq",
    "cloudflare",
    "openrouter",
    "nvidia",
    "huggingface",
    "ollama",
]


@dataclass
class _Window:
    """Rolling time-window counter."""

    window_seconds: int
    timestamps: deque[float] = field(default_factory=deque)
    tokens: deque[int] = field(default_factory=deque)  # parallel list for TPM

    def _evict(self) -> None:
        cutoff = time.time() - self.window_seconds
        while self.timestamps and self.timestamps[0] < cutoff:
            self.timestamps.popleft()
            if self.tokens:
                self.tokens.popleft()

    def add(self, token_count: int = 0) -> None:
        self._evict()
        self.timestamps.append(time.time())
        self.tokens.append(token_count)

    @property
    def count(self) -> int:
        self._evict()
        return len(self.timestamps)

    @property
    def token_sum(self) -> int:
        self._evict()
        return sum(self.tokens)


@dataclass
class _DayWindow:
    """24-hour rolling request counter."""

    timestamps: deque[float] = field(default_factory=deque)

    def _evict(self) -> None:
        cutoff = time.time() - 86_400
        while self.timestamps and self.timestamps[0] < cutoff:
            self.timestamps.popleft()

    def add(self) -> None:
        self._evict()
        self.timestamps.append(time.time())

    @property
    def count(self) -> int:
        self._evict()
        return len(self.timestamps)

    def seconds_until_oldest_expires(self) -> float:
        self._evict()
        if not self.timestamps:
            return 0.0
        return max(0.0, 86_400 - (time.time() - self.timestamps[0]))


class ProviderBudget:
    """Tracks RPM, TPM, and RPD for a single provider."""

    def __init__(self, provider: str, limits: dict[str, int]) -> None:
        self.provider = provider
        self.limits = limits
        self._rpm_window = _Window(window_seconds=60)
        self._tpm_window = _Window(window_seconds=60)
        self._rpd_window = _DayWindow()
        self._paused_until: float = 0.0

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def record(self, token_count: int = 0) -> None:
        """Record one API call with optional token count."""
        self._rpm_window.add(token_count=0)
        self._tpm_window.add(token_count=token_count)
        self._rpd_window.add()

    def is_available(self) -> bool:
        """Return True if this provider can accept a request right now."""
        if time.time() < self._paused_until:
            return False
        if self._rpm_window.count >= self.limits["rpm"]:
            logger.warning(f"[FreeTier] {self.provider} RPM limit reached ({self.limits['rpm']})")
            return False
        if self._tpm_window.token_sum >= self.limits["tpm"]:
            logger.warning(f"[FreeTier] {self.provider} TPM limit reached ({self.limits['tpm']})")
            return False
        if self._rpd_window.count >= self.limits["rpd"]:
            logger.warning(
                f"[FreeTier] {self.provider} RPD limit reached ({self.limits['rpd']}). "
                f"Resets in {self._rpd_window.seconds_until_oldest_expires():.0f}s"
            )
            return False
        return True

    def pause(self, seconds: float = 60.0) -> None:
        """Temporarily pause this provider (e.g. after a 429 response)."""
        self._paused_until = time.time() + seconds
        logger.warning(f"[FreeTier] {self.provider} paused for {seconds:.0f}s")

    def remaining(self) -> dict[str, Any]:
        """Return remaining capacity across all windows."""
        return {
            "provider": self.provider,
            "rpm_used": self._rpm_window.count,
            "rpm_limit": self.limits["rpm"],
            "rpm_remaining": max(0, self.limits["rpm"] - self._rpm_window.count),
            "tpm_used": self._tpm_window.token_sum,
            "tpm_limit": self.limits["tpm"],
            "tpm_remaining": max(0, self.limits["tpm"] - self._tpm_window.token_sum),
            "rpd_used": self._rpd_window.count,
            "rpd_limit": self.limits["rpd"],
            "rpd_remaining": max(0, self.limits["rpd"] - self._rpd_window.count),
            "available": self.is_available(),
            "paused_until": (self._paused_until if self._paused_until > time.time() else None),
            "rpd_resets_in_seconds": self._rpd_window.seconds_until_oldest_expires(),
        }


class FreeTierTracker:
    """
    Central free-tier usage tracker for all AI providers.

    Usage::

        tracker = FreeTierTracker()

        # Before calling a provider:
        provider = tracker.get_best_provider(["gemini", "groq", "openrouter"])

        # After a successful call:
        tracker.record(provider, token_count=850)

        # After a 429 rate-limit error:
        tracker.mark_rate_limited(provider, pause_seconds=60)

        # Get current status for admin dashboard:
        status = tracker.get_status()
    """

    def __init__(
        self,
        custom_limits: dict[str, dict[str, int]] | None = None,
    ) -> None:
        limits = {**DEFAULT_LIMITS, **(custom_limits or {})}
        self.priority_list = list(FREE_PROVIDER_PRIORITY)

        try:
            from database.supabase_client import db

            if db.client:
                db_configs = db.get_db_provider_configs()
                if db_configs:
                    db_limits = {}
                    db_priority = []
                    for row in db_configs:
                        pname = row.get("provider_name")
                        db_limits[pname] = {
                            "rpm": row.get("rpm", 999999),
                            "tpm": row.get("tpm", 999999),
                            "rpd": row.get("rpd", 999999),
                        }
                        db_priority.append(pname)
                    limits = {**limits, **db_limits}
                    if db_priority:
                        self.priority_list = db_priority
                else:
                    for idx, (pname, plimits) in enumerate(DEFAULT_LIMITS.items()):
                        db.upsert_db_provider_config(
                            {
                                "provider_name": pname,
                                "rpm": plimits.get("rpm", 999999),
                                "tpm": plimits.get("tpm", 999999),
                                "rpd": plimits.get("rpd", 999999),
                                "priority": idx,
                                "is_active": True,
                            }
                        )
        except Exception as e:
            logger.debug(f"Failed to fetch provider configs from Supabase: {e}")

        self._budgets: dict[str, ProviderBudget] = {
            provider: ProviderBudget(provider, provider_limits) for provider, provider_limits in limits.items()
        }

    # ------------------------------------------------------------------
    # Core methods
    # ------------------------------------------------------------------

    def record(self, provider: str, token_count: int = 0) -> None:
        """Record a successful API call for *provider*."""
        budget = self._budgets.get(provider)
        if budget:
            budget.record(token_count=token_count)
            logger.debug(
                f"[FreeTier] Recorded {provider} call | "
                f"tokens={token_count} | "
                f"RPM={budget._rpm_window.count}/{budget.limits['rpm']} | "
                f"RPD={budget._rpd_window.count}/{budget.limits['rpd']}"
            )

    def mark_rate_limited(self, provider: str, pause_seconds: float = 60.0) -> None:
        """Call this when you receive a 429 from a provider."""
        budget = self._budgets.get(provider)
        if budget:
            budget.pause(seconds=pause_seconds)

    def is_available(self, provider: str) -> bool:
        """Check if a specific provider is within its free tier limits."""
        budget = self._budgets.get(provider)
        return budget.is_available() if budget else False

    def get_best_provider(
        self,
        candidates: list[str] | None = None,
        exclude: list[str] | None = None,
    ) -> str | None:
        """
        Return the highest-priority available provider from *candidates*.

        If *candidates* is None, uses FREE_PROVIDER_PRIORITY order.
        Providers in *exclude* are skipped.
        Returns None if all candidates are exhausted.
        """
        order = candidates or self.priority_list
        skip = set(exclude or [])

        for provider in order:
            if provider in skip:
                continue
            if self.is_available(provider):
                logger.debug(f"[FreeTier] Selected provider: {provider}")
                return provider

        logger.error("[FreeTier] All providers exhausted or rate-limited!")
        return None

    def get_fallback_chain(
        self,
        failed_provider: str,
        candidates: list[str] | None = None,
    ) -> list[str]:
        """Return an ordered list of available providers excluding the failed one."""
        order = candidates or self.priority_list
        return [p for p in order if p != failed_provider and self.is_available(p)]

    # ------------------------------------------------------------------
    # Status / introspection
    # ------------------------------------------------------------------

    def get_status(self) -> dict[str, Any]:
        """Return full usage status for all providers (for admin dashboard)."""
        statuses = {provider: budget.remaining() for provider, budget in self._budgets.items()}
        available_providers = [p for p, s in statuses.items() if s["available"]]
        return {
            "available_providers": available_providers,
            "total_providers": len(self._budgets),
            "providers": statuses,
        }

    def get_provider_status(self, provider: str) -> dict[str, Any] | None:
        """Return usage status for a single provider."""
        budget = self._budgets.get(provider)
        return budget.remaining() if budget else None

    def override_limits(self, provider: str, limits: dict[str, int]) -> None:
        """Dynamically override limits for a provider at runtime (e.g. after upgrade)."""
        if provider in self._budgets:
            self._budgets[provider].limits.update(limits)
            logger.info(f"[FreeTier] Updated limits for {provider}: {limits}")


# ---------------------------------------------------------------------------
# Module-level singleton — import and use directly
# ---------------------------------------------------------------------------
_tracker: FreeTierTracker | None = None


def get_tracker(custom_limits: dict[str, dict[str, int]] | None = None) -> FreeTierTracker:
    """Return the module-level singleton FreeTierTracker."""
    global _tracker
    if _tracker is None:
        _tracker = FreeTierTracker(custom_limits=custom_limits)
        logger.info("[FreeTier] FreeTierTracker initialized")
    return _tracker
