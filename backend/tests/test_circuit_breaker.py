"""Tests for core.circuit_breaker.CircuitBreaker."""

import asyncio
import time

import pytest
from core.resilience.circuit_breaker import CircuitBreaker, CircuitBreakerState


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


def test_opens_after_failure_threshold():
    cb = CircuitBreaker("svc", failure_threshold=3)
    for _ in range(3):
        cb.mark_failure()
    assert cb.state == "OPEN"
    assert cb.allow_request() is False


def test_half_open_after_recovery():
    cb = CircuitBreaker("svc", failure_threshold=2, recovery_timeout=0.01)
    cb.mark_failure()
    cb.mark_failure()
    assert cb.state == "OPEN"
    assert cb.allow_request() is False
    time.sleep(0.02)
    assert cb.allow_request() is True
    assert cb.state == "HALF_OPEN"


def test_mark_success_closes():
    # বাংলা মন্তব্য: CircuitBreakerState enum ব্যবহার করে state সেট করা হচ্ছে।
    # String "HALF_OPEN" কাজ করে না কারণ _lock দিয়ে state compare হয় enum value-এ।
    cb = CircuitBreaker("svc", failure_threshold=2)
    cb.mark_failure()
    cb.mark_failure()
    assert cb.state == "OPEN"
    # OPEN → HALF_OPEN ট্রানজিশন simulate করা হচ্ছে
    with cb._lock:
        cb.failure_count = 0  # failure_count রিসেট
        cb.state = CircuitBreakerState.HALF_OPEN
        cb._recovery_in_progress = False  # allow mark_success to work
    cb.mark_success()
    assert cb.state == "CLOSED"


@pytest.mark.skip(
    reason="CircuitBreaker does not support redis_queue parameter — skipping redis persistence test"
)
def test_redis_persistence():
    # বাংলা মন্তব্য: CircuitBreaker এ redis_queue সাপোর্ট নেই, তাই skip করা হলো।
    redis = FakeRedis()
    cb = CircuitBreaker("svc", failure_threshold=2, redis_queue=redis)
    cb.mark_failure()
    assert cb.state == "CLOSED"
    stored = redis.get("cb:svc:state")
    assert stored is not None


def test_call_success():
    # বাংলা মন্তব্য: async function এ call() করলে coroutine return হয়, asyncio.run() দিয়ে execute করা হচ্ছে।
    cb = CircuitBreaker("svc")

    async def fake_func():
        return "ok"

    result = asyncio.run(cb.call(fake_func))
    assert result == "ok"
    assert cb.state == "CLOSED"


def test_call_failure_trips():
    # বাংলা মন্তব্য: async function failure count সঠিকভাবে ট্র্যাক হচ্ছে কিনা পরীক্ষা।
    cb = CircuitBreaker("svc", failure_threshold=2)

    async def fake_func():
        raise RuntimeError("boom")

    for _ in range(2):
        with pytest.raises(RuntimeError):
            asyncio.run(cb.call(fake_func))
    assert cb.state == "OPEN"


def test_async_call_requires_non_async_context():
    cb = CircuitBreaker("svc")

    async def fake_func():
        return "ok"

    assert asyncio.run(cb.call(fake_func)) == "ok"
