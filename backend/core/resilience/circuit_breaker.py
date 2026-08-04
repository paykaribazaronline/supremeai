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

from loguru import logger

from ..config import settings  # Fixed import path - using relative import

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

    def allow_request(self) -> bool:
        """Check if a request should be allowed to proceed."""
        with self._lock:
            if self.state == CircuitBreakerState.CLOSED:
                return True

            if self.state == CircuitBreakerState.OPEN:
                if self._should_attempt_recovery():
                    logger.info(
                        f"Circuit breaker '{self.name}' transitioning to HALF_OPEN for recovery test"
                    )
                    self.state = CircuitBreakerState.HALF_OPEN
                    self._recovery_in_progress = True
                    return True
                return False

            if self.state == CircuitBreakerState.HALF_OPEN:
                if not self._recovery_in_progress:
                    self._recovery_in_progress = True
                    return True
                return False

    def __call__(self, func: Callable[..., Any]) -> Callable[..., Any]:
        """Allow CircuitBreaker instance to be used as a decorator.

        বাংলা: CircuitBreaker ইন্সট্যান্সকে ডেকোরেটর হিসেবে ব্যবহার করতে দেয়।
        """
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

    def call(self, func: Callable[..., T], *args: Any, **kwargs: Any) -> T:
        """Execute a function with circuit breaker protection (sync or async).

        Implements FAIL-CLOSED strategy: raises CircuitBreakerOpenError when
        the circuit is OPEN and not ready for recovery, preventing execution
        of the underlying function.

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

        # Check if request is allowed before executing
        if not self.allow_request():
            err = CircuitBreakerOpenError(self.name, self.state)
            logger.error(
                f"Circuit breaker '{self.name}' rejected request - state: {self.state.value}"
            )
            raise err

        try:
            result = func(*args, **kwargs)
            self.mark_success()
            return result
        except (ConnectionError, TimeoutError, OSError) as exc:
            logger.warning(
                f"Circuit breaker '{self.name}' caught recoverable error: {exc}"
            )
            self.mark_failure()
            raise
        except CircuitBreakerOpenError:
            raise
        except Exception as exc:
            logger.opt(exception=True).error(
                f"Circuit breaker '{self.name}' caught unexpected error type={type(exc).__name__}"
            )
            self.mark_failure()
            raise

    async def acall(
        self, func: Callable[..., Awaitable[T]], *args: Any, **kwargs: Any
    ) -> T:
        """Execute an async function with circuit breaker protection.

        Implements FAIL-CLOSED strategy: raises CircuitBreakerOpenError when
        the circuit is OPEN and not ready for recovery, preventing execution
        of the underlying function.

        বাংলা: সার্কিট ব্রেকার প্রোটেকশন সহ অ্যাসিঙ্ক্রোনাস ফাংশন এক্সিকিউট করে।

        Raises:
            CircuitBreakerOpenError: If circuit is OPEN and not ready for recovery.
        """
        kwargs.pop("_correlation_id", None)

        # Check if request is allowed before executing
        if not self.allow_request():
            err = CircuitBreakerOpenError(self.name, self.state)
            logger.error(
                f"Circuit breaker '{self.name}' rejected request - state: {self.state.value}"
            )
            raise err

        try:
            result = await func(*args, **kwargs)
            self.mark_success()
            return result
        except (ConnectionError, TimeoutError, OSError) as exc:
            logger.warning(
                f"Circuit breaker '{self.name}' caught recoverable error: {exc}"
            )
            self.mark_failure()
            raise
        except CircuitBreakerOpenError:
            raise
        except Exception as exc:
            logger.opt(exception=True).error(
                f"Circuit breaker '{self.name}' caught unexpected error type={type(exc).__name__}"
            )
            self.mark_failure()
            raise

    def mark_success(self) -> None:
        """Record a successful call and potentially close the circuit.

        বাংলা: সফল কল রেকর্ড করে এবং সম্ভবত সার্কিট বন্ধ করে।
        """
        with self._lock:
            self.success_count += 1
            self.failure_count = 0  # Reset failure count on success
            self.last_success_time = time.monotonic()

            if self.state == CircuitBreakerState.HALF_OPEN:
                # After a successful test in HALF_OPEN, close the circuit
                logger.info(
                    f"Circuit breaker '{self.name}' closing after successful recovery test"
                )
                self.state = CircuitBreakerState.CLOSED
                self._recovery_in_progress = False
            elif self.state == CircuitBreakerState.CLOSED:
                logger.debug(
                    f"Circuit breaker '{self.name}' recorded success (total: {self.success_count})"
                )

    def mark_failure(self) -> None:
        """Record a failed call and potentially open the circuit.

        Implements FAIL-CLOSED strategy: when failure threshold is exceeded,
        the circuit is opened to prevent further damage.

        বাংলা: ব্যর্থ কল রেকর্ড করে এবং সম্ভবত সার্কিট খুলে।
        """
        with self._lock:
            self.failure_count += 1
            self.success_count = 0  # Reset success count on failure
            self.last_failure_time = time.monotonic()

            if self.state == CircuitBreakerState.HALF_OPEN:
                # Recovery test failed, reopen the circuit
                logger.warning(
                    f"Circuit breaker '{self.name}' reopening after failed recovery test"
                )
                self._open_circuit()
            elif (
                self.state == CircuitBreakerState.CLOSED
                and self.failure_count >= self.failure_threshold
            ):
                # Threshold exceeded, open the circuit
                logger.warning(
                    f"Circuit breaker '{self.name}' opening after {self.failure_count} consecutive failures"
                )
                self._open_circuit()
            elif self.state == CircuitBreakerState.CLOSED:
                logger.debug(
                    f"Circuit breaker '{self.name}' recorded failure ({self.failure_count}/{self.failure_threshold})"
                )

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

    def _open_circuit(self) -> None:
        """Open the circuit and record the time.

        Implements FAIL-CLOSED strategy: opens the circuit to prevent further
        requests from passing through when the service is unstable.

        বাংলা: সার্কিট খুলে দেয় এবং সময় রেকর্ড করে।
        """
        self.state = CircuitBreakerState.OPEN
        self.opened_at = time.monotonic()
        self._recovery_in_progress = False
        logger.info(
            f"Circuit breaker '{self.name}' is now OPEN - requests will be rejected"
        )

    def force_close(self) -> None:
        """Force the circuit to close (use with caution in emergency situations).

        বাংলা: জোর করে সার্কিট বন্ধ করে দেয় (জরুরি অবস্থায় সাবধানে ব্যবহার করুন)।
        """
        with self._lock:
            logger.warning(f"Circuit breaker '{self.name}' force closed by operator")
            self.state = CircuitBreakerState.CLOSED
            self.failure_count = 0
            self.success_count = 0
            self.opened_at = None
            self._recovery_in_progress = False

    def force_open(self) -> None:
        """Force the circuit to open (use for maintenance or emergency shutdown).

        Implements FAIL-CLOSED strategy: can be used to manually open the circuit
        when a service needs to be taken offline safely.

        বাংলা: জোর করে সার্কিট খুলে দেয় (রক্ষণাবেক্ষণ বা জরুরি বন্ধের জন্য ব্যবহার করুন)।
        """
        with self._lock:
            logger.warning(f"Circuit breaker '{self.name}' force opened by operator")
            self._open_circuit()

    def get_state_info(self) -> dict[str, Any]:
        """Get detailed information about the circuit breaker state.

        বাংলা: সার্কিট ব্রেকারের বর্তমান অবস্থা সম্পর্কে বিস্তারিত তথ্য দেয়।
        """
        with self._lock:
            return {
                "name": self.name,
                "state": self.state.value,
                "failure_count": self.failure_count,
                "success_count": self.success_count,
                "failure_threshold": self.failure_threshold,
                "recovery_timeout": self.recovery_timeout,
                "last_failure_time": self.last_failure_time,
                "last_success_time": self.last_success_time,
                "opened_at": self.opened_at,
                "is_recovery_in_progress": self._recovery_in_progress,
                "is_open": self.is_open,
            }

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
