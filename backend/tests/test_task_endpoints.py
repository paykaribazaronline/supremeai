from unittest.mock import AsyncMock
from unittest.mock import MagicMock

import pytest
from fastapi.testclient import TestClient

from core.app import app


client = TestClient(app)


@pytest.fixture
def mock_dependencies():
    import core.app as app_mod

    previous_admin = app_mod.admin_god
    previous_router = app_mod.model_router
    previous_intent = app_mod.intent_clf

    fake_admin = MagicMock()
    fake_admin.enforce.return_value = None
    fake_router = MagicMock()
    fake_router.async_route_and_generate = AsyncMock(
        return_value={
            "success": True,
            "text": "def hello():\n    pass",
            "provider": "test",
            "cost": 0.001,
        }
    )
    fake_intent = MagicMock()
    fake_intent.classify.return_value = MagicMock(task_type="general", confidence=1.0)

    app_mod.admin_god = fake_admin
    app_mod.model_router = fake_router
    app_mod.intent_clf = fake_intent

    def resolve():
        app_mod.admin_god = previous_admin
        app_mod.model_router = previous_router
        app_mod.intent_clf = previous_intent

    return resolve


@pytest.fixture
def mock_session():
    import core.app as app_mod

    previous_admin = app_mod.admin_god
    previous_router = app_mod.model_router
    previous_intent = app_mod.intent_clf

    fake_admin = MagicMock()
    fake_admin.enforce.return_value = None
    fake_router = MagicMock()
    fake_router.async_route_and_generate = AsyncMock(
        return_value={
            "success": True,
            "text": "def hello():\n    pass",
            "provider": "test",
            "cost": 0.001,
        }
    )
    fake_intent = MagicMock()
    fake_intent.classify.return_value = MagicMock(task_type="general", confidence=1.0)

    app_mod.admin_god = fake_admin
    app_mod.model_router = fake_router
    app_mod.intent_clf = fake_intent

    yield

    app_mod.admin_god = previous_admin
    app_mod.model_router = previous_router
    app_mod.intent_clf = previous_intent


def test_task_execute_returns_200(mock_session):
    response = client.post(
        "/task/execute",
        json={"task": "write hello world", "task_type": "general"},
        headers={"Authorization": "Bearer test-token"},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert "provider" in data


def test_task_execute_with_messages(mock_session):
    payload = {
        "task": "continue the code",
        "task_type": "general",
        "messages": [
            {"role": "user", "content": "start"},
            {"role": "assistant", "content": "here is code"},
        ],
    }
    response = client.post(
        "/task/execute",
        json=payload,
        headers={"Authorization": "Bearer test-token"},
    )
    assert response.status_code == 200
    assert response.json()["success"] is True


def test_task_execute_with_session_id(mock_session):
    response = client.post(
        "/task/execute",
        json={"task": "test", "task_type": "general", "session_id": "sess-123"},
        headers={"Authorization": "Bearer test-token"},
    )
    assert response.status_code == 200


def test_task_execute_upstream_failure(mock_session):
    import core.app as app_mod

    previous_router = app_mod.model_router
    fake_router = MagicMock()
    fake_router.async_route_and_generate = AsyncMock(
        return_value={
            "success": False,
            "error": "upstream timeout",
            "provider": "test",
            "cost": 0.0,
        }
    )
    app_mod.model_router = fake_router

    try:
        response = client.post(
            "/task/execute",
            json={"task": "test", "task_type": "general"},
            headers={"Authorization": "Bearer test-token"},
        )
        assert response.status_code == 502
    finally:
        app_mod.model_router = previous_router


def test_chat_completion_streaming():
    import core.app as app_mod

    previous_router = app_mod.model_router
    fake_router = MagicMock()

    async def fake_stream(*args, **kwargs):
        for token in ["def", " hello()", ":", "\n", "    pass"]:
            yield token.encode()

    fake_router.async_route_and_stream = fake_stream
    app_mod.model_router = fake_router

    try:
        response = client.post(
            "/api/chat/stream",
            json={"message": "write hello", "sessionId": "s-1"},
            headers={"Authorization": "Bearer test-token"},
        )
        assert response.status_code == 200
        assert "text/event-stream" in response.headers["content-type"]
    finally:
        app_mod.model_router = previous_router
