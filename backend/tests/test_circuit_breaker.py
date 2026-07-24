"""Tests for core.circuit_breaker.CircuitBreaker."""

import asyncio
import time

import pytest
from core.resilience.circuit_breaker import (CircuitBreaker,
                                             CircuitBreakerOpenError,
                                             CircuitBreakerState)


class FakeRedis:
    def __init__(self):
        self.store = {}
        self.configured = True

    def get(self, key):
        return self.store.get(key)

    def set(self, key, value, ex=None):
        self.store[key] = value

    def incr(self, key):
        val = int(self.store.get(key, 0)) + 1
        self.store[key] = str(val)
        return val

    def expire(self, key, time):
        pass

    def delete(self, key):
        if key in self.store:
            del self.store[key]


def test_initial_state():
    cb = CircuitBreaker("svc")
    assert cb.state == "CLOSED"
    assert cb.allow_request() is True
    assert cb.is_open is False


def test_repr():
    cb = CircuitBreaker("svc")
    r = repr(cb)
    assert "CircuitBreaker(name='svc'" in r
    assert "state=CLOSED" in r


def test_opens_after_failure_threshold():
    cb = CircuitBreaker("svc", failure_threshold=3)
    for _ in range(3):
        cb.mark_failure()
    assert cb.state == "OPEN"
    assert cb.allow_request() is False
    assert cb.is_open is True


def test_half_open_after_recovery():
    cb = CircuitBreaker("svc", failure_threshold=2, recovery_timeout=0.01)
    cb.mark_failure()
    cb.mark_failure()
    assert cb.state == "OPEN"
    assert cb.allow_request() is False
    time.sleep(0.02)
    # The first allow_request transitions it to HALF_OPEN and allows the request
    assert cb.allow_request() is True
    assert cb.state == "HALF_OPEN"
    # A subsequent allow_request should fail because recovery is in progress
    assert cb.allow_request() is False


def test_mark_success_closes():
    cb = CircuitBreaker("svc", failure_threshold=2)
    cb.mark_failure()
    cb.mark_failure()
    assert cb.state == "OPEN"
    with cb._lock:
        cb.failure_count = 0
        cb.state = CircuitBreakerState.HALF_OPEN
        cb._recovery_in_progress = False
    cb.mark_success()
    assert cb.state == "CLOSED"


@pytest.mark.skip(
    reason="CircuitBreaker does not support redis_queue parameter — skipping redis persistence test"
)
def test_redis_persistence():
    redis = FakeRedis()
    cb = CircuitBreaker("svc", failure_threshold=2, redis_queue=redis)
    cb.mark_failure()
    assert cb.state == "CLOSED"
    stored = redis.get("cb:svc:state")
    assert stored is not None


def test_call_success():
    cb = CircuitBreaker("svc")

    def fake_func():
        return "ok"

    result = cb.call(fake_func)
    assert result == "ok"
    assert cb.state == "CLOSED"


def test_call_failure_trips():
    cb = CircuitBreaker("svc", failure_threshold=2)

    def fake_func():
        raise RuntimeError("boom")

    for _ in range(2):
        with pytest.raises(RuntimeError):
            cb.call(fake_func)
    assert cb.state == "OPEN"


def test_call_recoverable_error():
    cb = CircuitBreaker("svc", failure_threshold=2)

    def fake_func():
        raise ConnectionError("conn failed")

    with pytest.raises(ConnectionError):
        cb.call(fake_func)
    assert cb.failure_count == 1
    assert cb.state == "CLOSED"


def test_call_open_error():
    cb = CircuitBreaker("svc", failure_threshold=1)
    cb.mark_failure()

    def fake_func():
        return "ok"

    with pytest.raises(CircuitBreakerOpenError):
        cb.call(fake_func)


def test_call_half_open_in_progress():
    cb = CircuitBreaker("svc", failure_threshold=1, recovery_timeout=0.01)
    cb.mark_failure()
    time.sleep(0.02)

    def fake_func():
        return "ok"

    # transition to HALF_OPEN
    assert cb.allow_request() is True
    # now it's HALF_OPEN and recovery in progress
    with pytest.raises(CircuitBreakerOpenError):
        cb.call(fake_func)


def test_async_call_success():
    cb = CircuitBreaker("svc")

    async def fake_func():
        return "ok"

    result = asyncio.run(cb.call(fake_func))
    assert result == "ok"
    assert cb.state == "CLOSED"


def test_async_call_failure():
    cb = CircuitBreaker("svc", failure_threshold=2)

    async def fake_func():
        raise RuntimeError("boom")

    for _ in range(2):
        with pytest.raises(RuntimeError):
            asyncio.run(cb.call(fake_func))
    assert cb.state == "OPEN"


def test_async_call_recoverable_error():
    cb = CircuitBreaker("svc", failure_threshold=2)

    async def fake_func():
        raise TimeoutError("timeout")

    with pytest.raises(TimeoutError):
        asyncio.run(cb.call(fake_func))
    assert cb.failure_count == 1


def test_async_call_open_error():
    cb = CircuitBreaker("svc", failure_threshold=1)
    cb.mark_failure()

    async def fake_func():
        return "ok"

    with pytest.raises(CircuitBreakerOpenError):
        asyncio.run(cb.call(fake_func))


def test_async_call_half_open_in_progress():
    cb = CircuitBreaker("svc", failure_threshold=1, recovery_timeout=0.01)
    cb.mark_failure()
    time.sleep(0.02)

    async def fake_func():
        return "ok"

    assert cb.allow_request() is True
    with pytest.raises(CircuitBreakerOpenError):
        asyncio.run(cb.call(fake_func))


def test_reset():
    cb = CircuitBreaker("svc", failure_threshold=1)
    cb.mark_failure()
    assert cb.state == "OPEN"
    cb.reset()
    assert cb.state == "CLOSED"
    assert cb.failure_count == 0


def test_get_metrics():
    cb = CircuitBreaker("svc", failure_threshold=1, recovery_timeout=0.01)
    m = cb.get_metrics()
    assert m['circuit_breaker_state{name="svc"}'] == 0
    assert m['circuit_breaker_failures_total{name="svc"}'] == 0
    assert m['circuit_breaker_successes_total{name="svc"}'] == 0

    cb.mark_failure()
    m2 = cb.get_metrics()
    assert m2['circuit_breaker_state{name="svc"}'] == 2

    time.sleep(0.02)
    cb.allow_request()
    m3 = cb.get_metrics()
    assert m3['circuit_breaker_state{name="svc"}'] == 1


def test_decorator_sync():
    cb = CircuitBreaker("svc")

    @cb
    def fake_func():
        return "ok"

    assert fake_func() == "ok"


def test_decorator_async():
    cb = CircuitBreaker("svc")

    @cb
    async def fake_func():
        return "ok"

    assert asyncio.run(fake_func()) == "ok"


def test_mark_success_no_op_when_closed():
    cb = CircuitBreaker("svc")
    cb.mark_success()
    assert cb.state == "CLOSED"
    assert cb.success_count == 1


def test_should_attempt_recovery_opened_at_none():
    cb = CircuitBreaker("svc")
    # If opened_at is None, it should return True
    assert cb._should_attempt_recovery() is True


def test_circuit_breaker_open_error_repr():
    err = CircuitBreakerOpenError("svc", CircuitBreakerState.OPEN)
    assert "Circuit breaker 'svc' is OPEN" in str(err)


# --- Missing Coverage Tests ---


def test_call_transitions_to_half_open():
    cb = CircuitBreaker("svc", failure_threshold=1, recovery_timeout=0.01)
    cb.mark_failure()
    assert cb.state == "OPEN"
    time.sleep(0.02)

    # Should transition to HALF_OPEN and run the func, then mark success -> CLOSED
    def fake_func():
        return "ok"

    assert cb.call(fake_func) == "ok"
    assert cb.state == "CLOSED"


def test_call_when_already_half_open():
    cb = CircuitBreaker("svc")
    with cb._lock:
        cb.state = CircuitBreakerState.HALF_OPEN
        cb._recovery_in_progress = False

    def fake_func():
        return "ok"

    assert cb.call(fake_func) == "ok"


def test_call_func_raises_open_error():
    cb = CircuitBreaker("svc")

    def fake_func():
        raise CircuitBreakerOpenError("other", CircuitBreakerState.OPEN)

    with pytest.raises(CircuitBreakerOpenError):
        cb.call(fake_func)

    # failure count doesn't increase, it's passed through without being marked as failure
    assert cb.failure_count == 0


def test_async_call_transitions_to_half_open():
    cb = CircuitBreaker("svc", failure_threshold=1, recovery_timeout=0.01)
    cb.mark_failure()
    assert cb.state == "OPEN"
    time.sleep(0.02)

    async def fake_func():
        return "ok"

    assert asyncio.run(cb.call(fake_func)) == "ok"
    assert cb.state == "CLOSED"


def test_async_call_when_already_half_open():
    cb = CircuitBreaker("svc")
    with cb._lock:
        cb.state = CircuitBreakerState.HALF_OPEN
        cb._recovery_in_progress = False

    async def fake_func():
        return "ok"

    assert asyncio.run(cb.call(fake_func)) == "ok"


def test_async_call_func_raises_open_error():
    cb = CircuitBreaker("svc")

    async def fake_func():
        raise CircuitBreakerOpenError("other", CircuitBreakerState.OPEN)

    with pytest.raises(CircuitBreakerOpenError):
        asyncio.run(cb.call(fake_func))

    assert cb.failure_count == 0


def test_allow_request_when_half_open():
    cb = CircuitBreaker("svc")
    with cb._lock:
        cb.state = CircuitBreakerState.HALF_OPEN
        cb._recovery_in_progress = False
    assert cb.allow_request() is True
    assert cb._recovery_in_progress is True
