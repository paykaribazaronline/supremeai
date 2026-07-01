import importlib

import pytest


class _FakeRedisQueue:
    configured = True

    def get(self, key):
        return "ok"

    def set(self, key, value, ex=None):
        return True


class _DownRedisQueue:
    configured = True

    def get(self, key):
        raise RuntimeError("redis down")

    def set(self, key, value, ex=None):
        raise RuntimeError("redis down")


@pytest.fixture()
def healthy_app(monkeypatch: pytest.MonkeyPatch):
    import core.services as services

    importlib.reload(services)
    queue = _FakeRedisQueue()
    monkeypatch.setattr(services, "redis_queue", queue, raising=True)
    return services


@pytest.fixture()
def down_app(monkeypatch: pytest.MonkeyPatch):
    import core.services as services

    importlib.reload(services)
    queue = _DownRedisQueue()
    monkeypatch.setattr(services, "redis_queue", queue, raising=True)
    return services


@pytest.fixture()
def unconfigured_app(monkeypatch: pytest.MonkeyPatch):
    import core.services as services

    importlib.reload(services)
    queue = _FakeRedisQueue()
    queue.configured = False
    monkeypatch.setattr(services, "redis_queue", queue, raising=True)
    return services


def test_health_when_redis_healthy(healthy_app):
    from fastapi.testclient import TestClient

    with TestClient(healthy_app.app) as client:
        response = client.get("/health")
        assert response.status_code == 200
        body = response.json()
        assert body["status"] in {"ok", "degraded"}
        assert body["checks"]["redis"] is True
        assert body["checks"]["api_keys_configured"] in {True, False}


def test_health_when_redis_down(down_app):
    from fastapi.testclient import TestClient

    with TestClient(down_app.app) as client:
        response = client.get("/health")
        assert response.status_code == 200
        body = response.json()
        assert body["status"] in {"ok", "degraded"}
        assert body["checks"]["redis"] is False


def test_health_when_redis_unconfigured(unconfigured_app):
    from fastapi.testclient import TestClient

    with TestClient(unconfigured_app.app) as client:
        response = client.get("/health")
        assert response.status_code == 200
        body = response.json()
        assert body["status"] in {"ok", "degraded"}
        assert body["checks"]["redis"] is True
