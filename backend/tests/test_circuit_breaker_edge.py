"""Edge case tests for core.resilience.circuit_breaker — CircuitBreaker."""

import asyncio
import time

import pytest
from core.resilience.circuit_breaker import (CircuitBreaker,
                                             CircuitBreakerOpenError,
                                             CircuitBreakerState)


class TestCircuitBreakerEdgeCases:
    """Edge cases for CircuitBreaker."""

    def test_decorator_sync_preserves_return(self):
        cb = CircuitBreaker("decorator-sync")

        @cb
        def sync_func():
            return "result"

        assert sync_func() == "result"

    def test_decorator_async_preserves_return(self):
        cb = CircuitBreaker("decorator-async")

        @cb
        async def async_func():
            return "result"

        result = asyncio.run(async_func())
        assert result == "result"

    def test_allow_request_half_open_recovery_in_progress(self):
        cb = CircuitBreaker("half-open", failure_threshold=1, recovery_timeout=0.01)
        cb.mark_failure()
        time.sleep(0.02)
        assert cb.allow_request() is True
        assert cb.state == CircuitBreakerState.HALF_OPEN
        assert cb.allow_request() is False

    def test_allow_request_half_open_no_recovery(self):
        cb = CircuitBreaker("half-open-no-recovery")
        cb.state = CircuitBreakerState.HALF_OPEN
        cb._recovery_in_progress = False
        assert cb.allow_request() is True
        assert cb._recovery_in_progress is True

    def testmark_success_closed_no_state_change(self):
        cb = CircuitBreaker("success-closed")
        cb.mark_success()
        assert cb.state == CircuitBreakerState.CLOSED
        assert cb.success_count == 1

    def testmark_failure_when_already_open(self):
        cb = CircuitBreaker("already-open", failure_threshold=1)
        cb.mark_failure()
        assert cb.state == CircuitBreakerState.OPEN
        cb.mark_failure()
        assert cb.state == CircuitBreakerState.OPEN

    def testmark_failure_at_exact_threshold(self):
        cb = CircuitBreaker("exact-threshold", failure_threshold=3)
        cb.mark_failure()
        cb.mark_failure()
        cb.mark_failure()
        assert cb.state == CircuitBreakerState.OPEN

    def testmark_failure_below_threshold(self):
        cb = CircuitBreaker("below-threshold", failure_threshold=5)
        cb.mark_failure()
        cb.mark_failure()
        assert cb.state == CircuitBreakerState.CLOSED

    def test_call_recoverable_os_error(self):
        cb = CircuitBreaker("os-error", failure_threshold=2)

        def fail():
            raise OSError("connection reset")

        with pytest.raises(OSError):
            cb.call(fail)
        assert cb.failure_count == 1

    def test_call_recoverable_connection_error(self):
        cb = CircuitBreaker("conn-error", failure_threshold=2)

        def fail():
            raise ConnectionError("refused")

        with pytest.raises(ConnectionError):
            cb.call(fail)
        assert cb.failure_count == 1

    def test_call_recoverable_timeout_error(self):
        cb = CircuitBreaker("timeout-error", failure_threshold=2)

        def fail():
            raise TimeoutError("timed out")

        with pytest.raises(TimeoutError):
            cb.call(fail)
        assert cb.failure_count == 1

    def test_call_unexpected_error_counts(self):
        cb = CircuitBreaker("unexpected", failure_threshold=2)

        def fail():
            raise ValueError("unexpected")

        with pytest.raises(ValueError):
            cb.call(fail)
        assert cb.failure_count == 1

    def test_call_circuit_breaker_open_error_re_raised(self):
        cb = CircuitBreaker("cb-raise", failure_threshold=1)
        cb.mark_failure()

        def func():
            raise CircuitBreakerOpenError("test", CircuitBreakerState.OPEN)

        with pytest.raises(CircuitBreakerOpenError):
            cb.call(func)

    def test_acall_recoverable_os_error(self):
        cb = CircuitBreaker("a-os-error", failure_threshold=2)

        async def fail():
            raise OSError("reset")

        with pytest.raises(OSError):
            asyncio.run(cb.acall(fail))
        assert cb.failure_count == 1

    def test_acall_unexpected_error_counts(self):
        cb = CircuitBreaker("a-unexpected", failure_threshold=2)

        async def fail():
            raise TypeError("unexpected")

        with pytest.raises(TypeError):
            asyncio.run(cb.acall(fail))
        assert cb.failure_count == 1

    def test_get_metrics_closed_state(self):
        cb = CircuitBreaker("metrics-closed")
        m = cb.get_metrics()
        assert m['circuit_breaker_state{name="metrics-closed"}'] == 0

    def test_get_metrics_half_open_state(self):
        cb = CircuitBreaker("metrics-half")
        cb.state = CircuitBreakerState.HALF_OPEN
        m = cb.get_metrics()
        assert m['circuit_breaker_state{name="metrics-half"}'] == 1

    def test_get_metrics_open_state(self):
        cb = CircuitBreaker("metrics-open", failure_threshold=1)
        cb.mark_failure()
        m = cb.get_metrics()
        assert m['circuit_breaker_state{name="metrics-open"}'] == 2

    def test_reset_clears_all_state(self):
        cb = CircuitBreaker("reset-all", failure_threshold=1)
        cb.mark_failure()
        assert cb.state == CircuitBreakerState.OPEN
        cb.reset()
        assert cb.state == CircuitBreakerState.CLOSED
        assert cb.failure_count == 0
        assert cb.success_count == 0
        assert cb._recovery_in_progress is False
        assert cb.opened_at is None

    def test_thread_safety_concurrent_failures(self):
        cb = CircuitBreaker("concurrent", failure_threshold=50)
        import threading

        errors = []

        def fail():
            try:
                for _ in range(10):
                    cb.mark_failure()
            except Exception as e:
                errors.append(e)

        threads = [threading.Thread(target=fail) for _ in range(5)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()

        assert len(errors) == 0
        assert cb.failure_count == 50

    def test_recovery_timeout_simulation(self):
        cb = CircuitBreaker("recovery-sim", failure_threshold=1, recovery_timeout=5)
        cb.mark_failure()
        assert cb.state == CircuitBreakerState.OPEN
        cb.opened_at = time.monotonic() - 10
        assert cb._should_attempt_recovery() is True

    def testmark_success_transitions_half_open_to_closed(self):
        cb = CircuitBreaker("half-to-closed", failure_threshold=1)
        cb.mark_failure()
        cb.state = CircuitBreakerState.HALF_OPEN
        cb._recovery_in_progress = False
        cb.mark_success()
        assert cb.state == CircuitBreakerState.CLOSED

    def test_allow_request_recovery_transition_sets_recovery(self):
        cb = CircuitBreaker(
            "recovery-transition", failure_threshold=1, recovery_timeout=0.01
        )
        cb.mark_failure()
        time.sleep(0.02)
        assert cb.allow_request() is True
        assert cb.state == CircuitBreakerState.HALF_OPEN
        assert cb._recovery_in_progress is True
