import os

import pytest
from fastapi import Request
from middleware.chaos_injector import ChaosInjectorMiddleware


class _CallNext:
    def __init__(self, response):
        self._response = response

    async def __call__(self, request):
        return self._response


@pytest.fixture
def middleware():
    os.environ["LOCAL_CHAOS_MODE"] = "false"
    app = type("FakeApp", (), {})()
    mw = ChaosInjectorMiddleware(app)
    yield mw
    os.environ.pop("LOCAL_CHAOS_MODE", None)


@pytest.mark.asyncio
async def test_dispatch_passes_when_chaos_disabled(middleware):
    response = {"status": "ok"}
    request = Request({"type": "http", "path": "/api/task/stream", "headers": {}})
    result = await middleware.dispatch(request, _CallNext(response))
    assert result == response


@pytest.mark.asyncio
async def test_dispatch_injects_delay_and_drop_when_enabled(monkeypatch):
    os.environ["LOCAL_CHAOS_MODE"] = "true"
    app = type("FakeApp", (), {})()
    # বাংলা মন্তব্য: monkeypatch target সঠিক module path-এ করা হচ্ছে
    mw = ChaosInjectorMiddleware(app)

    sleeps = []

    async def fake_sleep(duration):
        sleeps.append(duration)

    monkeypatch.setattr("middleware.chaos_injector.asyncio.sleep", fake_sleep)

    # 1st call: roll (0.2 < 0.3 delay zone), 2nd call: random() for delay calculation, 3rd call: post-delay drop check (0.0 < 0.15 drop zone)
    values = [0.2, 0.5, 0.0]

    def fake_random():
        return values.pop(0) if values else 0.0

    monkeypatch.setattr("middleware.chaos_injector.random.random", fake_random)

    request = Request({"type": "http", "path": "/api/test", "headers": {}})

    async def call_next(request):
        return {"passed": True}

    result = await mw.dispatch(request, call_next)

    assert len(sleeps) == 1
    assert 0.5 <= sleeps[0] <= 3.5
    assert result.status_code == 504


@pytest.mark.asyncio
async def test_dispatch_packet_drop_returns_504(monkeypatch):
    os.environ["LOCAL_CHAOS_MODE"] = "true"
    app = type("FakeApp", (), {})()
    mw = ChaosInjectorMiddleware(app)

    # বাংলা মন্তব্য: 0.0 < 0.15 drop threshold — সরাসরি 504 return হবে
    monkeypatch.setattr("middleware.chaos_injector.random.random", lambda: 0.0)

    request = Request({"type": "http", "path": "/api/test", "headers": {}})

    async def call_next(request):
        return {"should_not_reach": True}

    result = await mw.dispatch(request, call_next)
    assert result.status_code == 504


def test_chaos_disabled_by_default(monkeypatch):
    os.environ.pop("LOCAL_CHAOS_MODE", None)
    app = type("FakeApp", (), {})()
    mw = ChaosInjectorMiddleware(app)
    assert mw.chaos_enabled is False


def test_chaos_enabled_when_env_set():
    os.environ["LOCAL_CHAOS_MODE"] = "true"
    app = type("FakeApp", (), {})()
    mw = ChaosInjectorMiddleware(app)
    assert mw.chaos_enabled is True
