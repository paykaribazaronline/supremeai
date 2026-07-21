"""Circuit Breaker — Resilience pattern for preventing cascading failures.

বাংলা: সার্কিট ব্রেকার — ক্যাসকেডিং ফেইলিওর প্রতিরোধের জন্য রেজিলিয়েন্স প্যাটার্ন।

Tracks failure/success counts and opens the circuit when threshold exceeded.
After cooldown, transitions to half-open state for recovery testing.
"""

from __future__ import annotations

import threading
import time
from collections.abc import Awaitable, Callable
from enum import StrEnum
from typing import Any, TypeVar

from core.config import settings
from loguru import logger

T = TypeVar("T")


class CircuitBreakerState(StrEnum):
    """Circuit breaker states."""

    CLOSED = "CLOSED"  # Normal operation — requests pass through
    OPEN = "OPEN"  # Failing — requests are rejected immediately
    HALF_OPEN = "HALF_OPEN"  # Testing — limited requests allowed


class CircuitBreakerOpenError(RuntimeError):
    """Raised when the circuit breaker is OPEN and a request is rejected.

    বাংলা: সার্কিট ব্রেকার OPEN থাকলে রিকোয়েস্ট রিজেক্ট হলে এই এক্সেপশন রেইজ হয়।
    RuntimeError থেকে inherit করা হয়েছে যাতে contextlib.suppress(RuntimeError) দিয়ে
    suppress করা যায় এবং pytest.raises(RuntimeError) দিয়ে catch করা যায়।
    """

    def __init__(self, name: str, state: CircuitBreakerState) -> None:
        self.name = name
        self.state = state
        super().__init__(
            f"Circuit breaker '{name}' is {state.value}. Request rejected."
        )


class CircuitBreaker:
    """Circuit breaker for a specific operation or service.

    বাংলা: নির্দিষ্ট অপারেশন বা সার্ভিসের জন্য সার্কিট ব্রেকার।

    Attributes:
        name: Identifier for this breaker (e.g., service name).
        failure_threshold: Number of consecutive failures to open the circuit.
        recovery_timeout: Seconds to wait before transitioning to HALF_OPEN.
        state: Current circuit state.
        failure_count: Current consecutive failure count.
        success_count: Current consecutive success count (for half-open recovery).
        last_failure_time: Timestamp of the last failure.
        last_success_time: Timestamp of the last success.
    """

    def __init__(
        self,
        name: str,
        failure_threshold: int | None = None,
        recovery_timeout: float | None = None,
        **kwargs: Any,
    ) -> None:
        self.name = name
        self.failure_threshold = (
            failure_threshold or settings.circuit_breaker_failure_threshold
        )
        self.recovery_timeout = float(
            recovery_timeout or settings.circuit_breaker_cooldown_period
        )

        self.state: CircuitBreakerState = CircuitBreakerState.CLOSED
        self.failure_count: int = 0
        self.success_count: int = 0
        self.last_failure_time: float | None = None
        self.last_success_time: float | None = None
        self.opened_at: float | None = None
        self._recovery_in_progress: bool = False
        self._lock = threading.Lock()

    def __repr__(self) -> str:
        with self._lock:
            return f"CircuitBreaker(name='{self.name}', state={self.state.value}, failures={self.failure_count}, successes={self.success_count})"

    @property
    def is_open(self) -> bool:
        """Check if the circuit is currently open.

        বাংলা: সার্কিট বর্তমানে OPEN কিনা চেক করে।
        """
        with self._lock:
            return self.state == CircuitBreakerState.OPEN

    def _should_attempt_recovery(self) -> bool:
        """Check if enough time has passed to attempt recovery.

        বাংলা: রিকভারি চেষ্টা করার জন্য যথেষ্ট সময় পেরিয়েছে কিনা চেক করে।
        opened_at ব্যবহার করা হয় যাতে টেস্টে সহজে ম্যানিপুলেট করা যায়।
        """
        if self.opened_at is None:
            return True
        return (time.monotonic() - self.opened_at) >= self.recovery_timeout

    def call(self, func: Callable[..., T], *args: Any, **kwargs: Any) -> T:
        """Execute a function with circuit breaker protection (sync or async).

        বাংলা: সার্কিট ব্রেকার প্রোটেকশন সহ ফাংশন এক্সিকিউট করে।
        যদি func একটি async function হয়, তাহলে acall() coroutine return করা হয়
        যাতে caller নিজে await বা asyncio.run() করতে পারে।
        এটি নিশ্চিত করে যে async function এর failure সঠিকভাবে ট্র্যাক হবে।

        Raises:
            CircuitBreakerOpenError: If circuit is OPEN and not ready for recovery.
        """
        import inspect

        # বাংলা মন্তব্য: async function detect করে acall() coroutine return করা হচ্ছে।
        # এটা করলে caller asyncio.run() বা await দিয়ে সঠিকভাবে execute করতে পারবে।
        # nested asyncio.run() এড়াতে এখানে আমরা asyncio.run() করি না।
        if inspect.iscoroutinefunction(func):
            return self.acall(func, *args, **kwargs)  # type: ignore[return-value]

        kwargs.pop("_correlation_id", None)

        with self._lock:
            if self.state == CircuitBreakerState.OPEN:
                if self._should_attempt_recovery():
                    logger.info(
                        f"Circuit breaker '{self.name}' transitioning to HALF_OPEN for recovery test"
                    )
                    self.state = CircuitBreakerState.HALF_OPEN
                    self._recovery_in_progress = True
                else:
                    err = CircuitBreakerOpenError(self.name, self.state)
                    logger.error(
                        f"Circuit breaker '{self.name}' rejected request - state: {self.state.value}"
                    )
                    raise err
            elif self.state == CircuitBreakerState.HALF_OPEN:
                if self._recovery_in_progress:
                    err = CircuitBreakerOpenError(self.name, self.state)
                    raise err
                self._recovery_in_progress = True

        try:
            result = func(*args, **kwargs)
            self._mark_success()
            return result
        except (ConnectionError, TimeoutError, OSError) as exc:
            logger.warning(
                f"Circuit breaker '{self.name}' caught recoverable error: {exc}"
            )
            self._mark_failure()
            raise
        except CircuitBreakerOpenError:
            raise
        except Exception as exc:
            logger.opt(exception=True).error(
                f"Circuit breaker '{self.name}' caught unexpected error type={type(exc).__name__}"
            )
            self._mark_failure()
            raise

    async def acall(
        self, func: Callable[..., Awaitable[T]], *args: Any, **kwargs: Any
    ) -> T:
        """Execute an async function with circuit breaker protection.

        বাংলা: সার্কিট ব্রেকার প্রোটেকশন সহ অ্যাসিঙ্ক্রোনাস ফাংশন এক্সিকিউট করে।

        Raises:
            CircuitBreakerOpenError: If circuit is OPEN and not ready for recovery.
        """
        kwargs.pop("_correlation_id", None)

        with self._lock:
            if self.state == CircuitBreakerState.OPEN:
                if self._should_attempt_recovery():
                    logger.info(
                        f"Circuit breaker '{self.name}' transitioning to HALF_OPEN for recovery test"
                    )
                    self.state = CircuitBreakerState.HALF_OPEN
                    self._recovery_in_progress = True
                else:
                    err = CircuitBreakerOpenError(self.name, self.state)
                    logger.error(
                        f"Circuit breaker '{self.name}' rejected request - state: {self.state.value}"
                    )
                    raise err
            elif self.state == CircuitBreakerState.HALF_OPEN:
                if self._recovery_in_progress:
                    err = CircuitBreakerOpenError(self.name, self.state)
                    raise err
                self._recovery_in_progress = True

        try:
            result = await func(*args, **kwargs)
            self._mark_success()
            return result
        except (ConnectionError, TimeoutError, OSError) as exc:
            logger.warning(
                f"Circuit breaker '{self.name}' caught recoverable error: {exc}"
            )
            self._mark_failure()
            raise
        except CircuitBreakerOpenError:
            raise
        except Exception as exc:
            logger.opt(exception=True).error(
                f"Circuit breaker '{self.name}' caught unexpected error type={type(exc).__name__}"
            )
            self._mark_failure()
            raise

    def _mark_success(self) -> None:
        """Record a successful call and potentially close the circuit.

        বাংলা: সফল কল রেকর্ড করে এবং সম্ভবত সার্কিট বন্ধ করে।
        """
        with self._lock:
            self.success_count += 1
            self.failure_count = 0
            self.last_success_time = time.monotonic()

            if self.state == CircuitBreakerState.HALF_OPEN:
                logger.info(
                    f"Circuit breaker '{self.name}' recovered — transitioning to CLOSED"
                )
                self.state = CircuitBreakerState.CLOSED
                # বাংলা মন্তব্য: সফলভাবে CLOSED হলে opened_at রিসেট করা হচ্ছে
                self.opened_at = None
                self._recovery_in_progress = False

    def _mark_failure(self) -> None:
        """Record a failed call and potentially open the circuit.

        বাংলা: ব্যর্থ কল রেকর্ড করে এবং সম্ভবত সার্কিট খোলে।
        """
        with self._lock:
            self.failure_count += 1
            self.success_count = 0
            self.last_failure_time = time.monotonic()

            if (
                self.failure_count >= self.failure_threshold
                and self.state != CircuitBreakerState.OPEN
            ):
                logger.warning(
                    f"Circuit breaker '{self.name}' opened after {self.failure_count} consecutive failures"
                )
                self.state = CircuitBreakerState.OPEN
                # বাংলা মন্তব্য: সার্কিট খোলার সময় opened_at সেট করা হচ্ছে
                # যাতে _should_attempt_recovery() সঠিকভাবে কাজ করে এবং টেস্টে ম্যানিপুলেট করা যায়
                self.opened_at = time.monotonic()
                self._recovery_in_progress = False

    def reset(self) -> None:
        """Manually reset the circuit breaker to CLOSED state.

        বাংলা: ম্যানুয়ালি সার্কিট ব্রেকারকে CLOSED স্টেটে রিসেট করে।
        """
        with self._lock:
            logger.info(f"Circuit breaker '{self.name}' manually reset")
            self.state = CircuitBreakerState.CLOSED
            self.failure_count = 0
            self.success_count = 0
            self.last_failure_time = None
            self.last_success_time = None
            # বাংলা মন্তব্য: রিসেটে opened_at ক্লিয়ার করা হচ্ছে
            self.opened_at = None
            self._recovery_in_progress = False

    def get_metrics(self) -> dict[str, Any]:
        """Get current metrics for monitoring.

        বাংলা: মনিটরিংয়ের জন্য বর্তমান মেট্রিক্স রিটার্ন করে।
        """
        with self._lock:
            state_val = 0
            if self.state == CircuitBreakerState.OPEN:
                state_val = 2
            elif self.state == CircuitBreakerState.HALF_OPEN:
                state_val = 1

            return {
                f'circuit_breaker_state{{name="{self.name}"}}': state_val,
                f'circuit_breaker_failures_total{{name="{self.name}"}}': self.failure_count,
                f'circuit_breaker_successes_total{{name="{self.name}"}}': self.success_count,
            }

    def __call__(self, func: Callable[..., Any]) -> Callable[..., Any]:
        """Allow CircuitBreaker to be used as a decorator."""
        import functools
        import inspect

        if inspect.iscoroutinefunction(func):

            @functools.wraps(func)
            async def async_wrapper(*args: Any, **kwargs: Any) -> Any:
                return await self.acall(func, *args, **kwargs)

            return async_wrapper
        else:

            @functools.wraps(func)
            def sync_wrapper(*args: Any, **kwargs: Any) -> Any:
                return self.call(func, *args, **kwargs)

            return sync_wrapper

    def allow_request(self) -> bool:
        """Check if request is allowed through the breaker.

        বাংলা: রিকোয়েস্ট সার্কিট দিয়ে পাস হতে পারবে কিনা চেক করে।
        """
        with self._lock:
            if self.state == CircuitBreakerState.OPEN:
                if self._should_attempt_recovery():
                    logger.info(
                        f"Circuit breaker '{self.name}' transitioning to HALF_OPEN for recovery test"
                    )
                    self.state = CircuitBreakerState.HALF_OPEN
                    self._recovery_in_progress = True
                    return True
                return False
            elif self.state == CircuitBreakerState.HALF_OPEN:
                if self._recovery_in_progress:
                    return False
                self._recovery_in_progress = True
                return True
            return True

    def mark_success(self) -> None:
        """Record successful request."""
        self._mark_success()

    def mark_failure(self) -> None:
        """Record failed request."""
        self._mark_failure()
