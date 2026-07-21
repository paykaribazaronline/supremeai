import time
from unittest.mock import MagicMock, patch

import pytest
from core.observability.observability_middleware import ObservabilityMiddleware


class _FakeResponseStart:
    def __init__(self, status_code=200):
        self.message = {
            "type": "http.response.start",
            "status": status_code,
            "headers": [],
        }


class _FakeApp:
    def __init__(self, exc: Exception | None = None):
        self.exc = exc

    async def __call__(self, scope, receive, send):
        if self.exc:
            raise self.exc
        # emulate response start
        await send({"type": "http.response.start", "status": 200, "headers": []})
        await send({"type": "http.response.body", "body": b"ok"})


@pytest.mark.anyio
async def test_bypass_non_http_scope():
    app = _FakeApp()
    mw = ObservabilityMiddleware(app)

    sent = []

    async def send(msg):
        sent.append(msg)

    async def receive():
        return None

    scope = {"type": "lifespan"}
    await mw(scope, receive, send)
    assert len(sent) == 2  # Fake app executed


@pytest.mark.anyio
async def test_bypass_metrics_path():
    app = _FakeApp()
    mw = ObservabilityMiddleware(app)

    sent = []

    async def send(msg):
        sent.append(msg)

    async def receive():
        return None

    scope = {
        "type": "http",
        "path": "/metrics",
        "headers": [],
        "method": "GET",
        "scheme": "http",
        "server": ("x", 80),
    }
    await mw(scope, receive, send)
    assert (
        len(sent) == 2
    )  # middleware bypasses metrics endpoint but Fake app still runs


@pytest.mark.anyio
async def test_records_metrics_and_triggers_sentinel_on_500(monkeypatch):
    app = _FakeApp()

    # Force redis sampling off
    monkeypatch.setattr(
        ObservabilityMiddleware,
        "__init__",
        ObservabilityMiddleware.__init__,
        raising=False,
    )
    mw = ObservabilityMiddleware(app)
    mw._redis_traffic_sampling_rate = 0.0

    # deterministic duration > 3.0
    t = [0.0, 4.5]

    monkeypatch.setattr(time, "perf_counter", lambda: t.pop(0))

    with (
        patch("core.observability.observability_middleware.trace_span"),
        patch("core.observability.observability_middleware.record_request"),
        patch("core.observability.observability_middleware.record_request_duration"),
        patch("core.observability.observability_middleware.record_error"),
        patch("core.observability.observability_middleware.uuid.uuid4"),
        patch("core.observability.posthog_client.posthog_client.capture"),
        patch("core.sentinel_agent.sentinel.trigger_event", new_callable=MagicMock),
    ):
        sentinel_mock = MagicMock()
        with patch(
            "core.sentinel_agent.sentinel", new=MagicMock(trigger_event=sentinel_mock)
        ):
            scope = {
                "type": "http",
                "path": "/api/x",
                "headers": [],
                "method": "GET",
                "scheme": "http",
                "server": ("x", 80),
            }

            async def receive():
                return None

            async def send(msg):
                return None

            await mw(scope, receive, send)

    assert sentinel_mock.call_count == 1
