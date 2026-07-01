"""Tests for core.circuit_breaker.CircuitBreaker."""
import time

import pytest

from core.circuit_breaker import CircuitBreaker


class FakeRedis:
    def __init__(self):
        self.store = {}
        self.configured = True

    def get(self, key):
        return self.store.get(key)

    def set(self, key, value, ex=None):
        self.store[key] = value


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
    cb = CircuitBreaker("svc", failure_threshold=2)
    cb.mark_failure()
    cb.mark_failure()
    assert cb.state == "OPEN"
    cb.failures = 0
    cb.state = "HALF_OPEN"
    cb.mark_success()
    assert cb.state == "CLOSED"


def test_redis_persistence():
    redis = FakeRedis()
    cb = CircuitBreaker("svc", failure_threshold=2, redis_queue=redis)
    cb.mark_failure()
    assert cb.state == "CLOSED"
    stored = redis.get("cb:svc:state")
    assert stored is not None


@pytest.mark.anyio
async def test_call_success():
    cb = CircuitBreaker("svc")

    async def fake_func():
        return "ok"

    result = await cb.call(fake_func)
    assert result == "ok"
    assert cb.state == "CLOSED"


@pytest.mark.anyio
async def test_call_failure_trips():
    cb = CircuitBreaker("svc", failure_threshold=2)

    async def fake_func():
        raise RuntimeError("boom")

    for _ in range(2):
        try:
            await cb.call(fake_func)
        except RuntimeError:
            pass
    assert cb.state == "OPEN"


import asyncio


def test_async_call_requires_non_async_context():
    cb = CircuitBreaker("svc")

    async def fake_func():
        return "ok"

    assert asyncio.run(cb.call(fake_func)) == "ok"
