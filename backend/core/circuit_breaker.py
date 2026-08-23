"""
SupremeAI Circuit Breaker — Resilience Pattern
🔬 Evolution v3.0: Prevents cascading failures with automatic recovery

States:
  CLOSED → Normal operation, requests pass through
  OPEN → Failing, requests fail immediately (no backend calls)
  HALF_OPEN → Testing, allows one request through to check recovery

Usage:
    from core.circuit_breaker import CircuitBreaker
    
    cb = CircuitBreaker(
        name="gemini_api",
        failure_threshold=5,
        recovery_timeout=30,
    )
    
    async with cb.protect():
        result = await call_external_api()
"""

from __future__ import annotations

import asyncio
import time
from collections import deque
from dataclasses import dataclass, field
from enum import Enum
from typing import Any, TypeVar
from contextlib import asynccontextmanager

T = TypeVar("T")


class CircuitState(str, Enum):
    CLOSED = "closed"       # Normal operation
    OPEN = "open"           # Failing, reject immediately
    HALF_OPEN = "half_open" # Testing recovery


@dataclass
class CircuitStats:
    """Statistics for a circuit breaker."""
    total_requests: int = 0
    total_successes: int = 0
    total_failures: int = 0
    total_rejections: int = 0  # Rejected while OPEN
    current_state: CircuitState = CircuitState.CLOSED
    last_failure_time: float = 0
    last_success_time: float = 0
    consecutive_failures: int = 0
    consecutive_successes: int = 0


class CircuitBreakerError(Exception):
    """Raised when circuit breaker is OPEN and request is rejected."""
    def __init__(self, name: str, state: CircuitState, recovery_in: float):
        self.name = name
        self.state = state
        self.recovery_in = recovery_in
        super().__init__(
            f"Circuit '{name}' is OPEN. "
            f"Recovery in ~{recovery_in:.0f}s. "
            f"Requests are being rejected."
        )


class CircuitBreaker:
    """
    Circuit Breaker implementation for external service calls.
    
    Prevents cascading failures by temporarily stopping calls to
    failing services and automatically testing for recovery.
    """
    
    def __init__(
        self,
        name: str,
        failure_threshold: int = 5,
        success_threshold: int = 3,
        recovery_timeout: float = 30.0,
        half_open_max_calls: int = 1,
    ):
        """
        Initialize circuit breaker.
        
        Args:
            name: Identifier for this circuit (for logging/metrics)
            failure_threshold: Consecutive failures before opening
            success_threshold: Successes in HALF_OPEN before closing
            recovery_timeout: Seconds before trying HALF_OPEN
            half_open_max_calls: Max concurrent test requests in HALF_OPEN
        """
        self.name = name
        self.failure_threshold = failure_threshold
        self.success_threshold = success_threshold
        self.recovery_timeout = recovery_timeout
        self.half_open_max_calls = half_open_max_calls
        
        self._state = CircuitState.CLOSED
        self._failure_count = 0
        self._success_count = 0
        self._last_failure_time = 0.0
        self._half_open_calls = 0
        self._lock = asyncio.Lock()
        self._stats = CircuitStats()
    
    @property
    def state(self) -> CircuitState:
        return self._state
    
    @property
    def stats(self) -> CircuitStats:
        return self._stats

    def _should_attempt_reset(self) -> bool:
        """Check if enough time has passed to try HALF_OPEN."""
        if self._state != CircuitState.OPEN:
            return False
        elapsed = time.time() - self._last_failure_time
        return elapsed >= self.recovery_timeout

    async def _on_success(self) -> None:
        """Handle successful call."""
        async with self._lock:
            self._stats.total_successes += 1
            self._stats.last_success_time = time.time()
            
            if self._state == CircuitState.HALF_OPEN:
                self._success_count += 1
                if self._success_count >= self.success_threshold:
                    self._state = CircuitState.CLOSED
                    self._failure_count = 0
                    self._success_count = 0
                    self._half_open_calls = 0
            else:  # CLOSED
                self._failure_count = 0
                self._consecutive_failures = 0

    async def _on_failure(self) -> None:
        """Handle failed call."""
        async with self._lock:
            self._stats.total_failures += 1
            self._stats.last_failure_time = time.time()
            self._failure_count += 1
            
            if self._state == CircuitState.HALF_OPEN:
                # Failure in HALF_OPEN → back to OPEN
                self._state = CircuitState.OPEN
                self._last_failure_time = time.time()
                self._half_open_calls = 0
            elif self._failure_count >= self.failure_threshold:
                # Threshold reached → OPEN
                self._state = CircuitState.OPEN
                self._last_failure_time = time.time()

    @asynccontextmanager
    async def protect(self):
        """
        Context manager that wraps a call with circuit breaker protection.
        
        Usage:
            async with cb.protect():
                result = await risky_call()
        
        Raises:
            CircuitBreakerError: If circuit is OPEN
        """
        self._stats.total_requests += 1
        
        async with self._lock:
            # Check if we should try reset
            if self._should_attempt_reset():
                self._state = CircuitState.HALF_OPEN
                self._half_open_calls = 0
            
            self._stats.current_state = self._state
            
            if self._state == CircuitState.OPEN:
                self._stats.total_rejections += 1
                recovery_in = self.recovery_timeout - (time.time() - self._last_failure_time)
                raise CircuitBreakerError(self.name, self._state, recovery_in)
            
            if self._state == CircuitState.HALF_OPEN:
                if self._half_open_calls >= self.half_open_max_calls:
                    self._stats.total_rejections += 1
                    raise CircuitBreakerError(self.name, self._state, 0)
                self._half_open_calls += 1
        
        try:
            yield
            await self._on_success()
        except Exception:
            await self._on_failure()
            raise

    def get_recovery_time(self) -> float:
        """Get seconds until circuit may attempt recovery."""
        if self._state != CircuitState.OPEN:
            return 0.0
        elapsed = time.time() - self._last_failure_time
        return max(0, self.recovery_timeout - elapsed)

    def reset(self) -> None:
        """Manually reset circuit to CLOSED state."""
        self._state = CircuitState.CLOSED
        self._failure_count = 0
        self._success_count = 0
        self._half_open_calls = 0


# Pre-configured circuits for common services
CIRCUITS: dict[str, CircuitBreaker] = {
    "gemini_api": CircuitBreaker("gemini_api", failure_threshold=5, recovery_timeout=30),
    "groq_api": CircuitBreaker("groq_api", failure_threshold=5, recovery_timeout=30),
    "openrouter_api": CircuitBreaker("openrouter_api", failure_threshold=5, recovery_timeout=30),
    "database": CircuitBreaker("database", failure_threshold=3, recovery_timeout=15),
    "external_http": CircuitBreaker("external_http", failure_threshold=5, recovery_timeout=20),
}


def get_circuit(name: str) -> CircuitBreaker:
    """Get or create a circuit breaker by name."""
    if name not in CIRCUITS:
        CIRCUITS[name] = CircuitBreaker(name)
    return CIRCUITS[name]


# =============================================================================
