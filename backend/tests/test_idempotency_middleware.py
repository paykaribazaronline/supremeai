import json
import pytest
from unittest.mock import MagicMock, AsyncMock
from starlette.requests import Request
from starlette.responses import Response
from middleware.idempotency import IdempotencyMiddleware
from datetime import datetime, timedelta, timezone


def make_middleware():
    middleware = IdempotencyMiddleware.__new__(IdempotencyMiddleware)
    middleware.collection_name = "idempotency_locks"
    middleware.db = MagicMock()
    return middleware


def build_scope(path="/api/task/execute", method="POST", headers=None):
    return {
        "type": "http",
        "method": method,
        "path": path,
        "headers": [(k.lower().encode(), v.encode()) for k, v in (headers or {}).items()],
        "query_string": b"",
    }


@pytest.mark.asyncio
async def test_idempotency_requires_key_for_post():
    middleware = make_middleware()
    scope = build_scope(headers={})
    receive = AsyncMock(return_value={"type": "http.request", "body": b"", "more_body": False})
    request = Request(scope, receive=receive)
    
    async def fake_next(req):
        return Response("OK")
    
    response = await middleware.dispatch(request, fake_next)
    assert response.status_code == 400


@pytest.mark.asyncio
async def test_idempotency_passes_non_post():
    middleware = make_middleware()
    scope = build_scope(method="GET", headers={})
    receive = AsyncMock(return_value={"type": "http.disconnect"})
    request = Request(scope, receive=receive)
    
    async def fake_next(req):
        return Response("OK")
    
    response = await middleware.dispatch(request, fake_next)
    assert response.status_code == 200


@pytest.mark.asyncio
async def test_idempotency_passes_non_task_path():
    middleware = make_middleware()
    scope = build_scope(path="/api/health", headers={})
    receive = AsyncMock(return_value={"type": "http.disconnect"})
    request = Request(scope, receive=receive)
    
    async def fake_next(req):
        return Response("OK")
    
    response = await middleware.dispatch(request, fake_next)
    assert response.status_code == 200


@pytest.mark.asyncio
async def test_idempotency_cache_hit_completed():
    middleware = make_middleware()
    now = datetime.now(timezone.utc)
    future = now + timedelta(hours=2)
    doc_data = {
        "status": "completed",
        "response_body": json.dumps({"result": "cached"}),
        "expires_at": future.isoformat(),
    }
    mock_doc = MagicMock()
    mock_doc.exists = True
    mock_doc.to_dict.return_value = doc_data
    middleware.db.document.return_value.get.return_value = mock_doc
    
    scope = build_scope(headers={"idempotency-key": "test-key-1"})
    receive = AsyncMock(return_value={"type": "http.disconnect"})
    request = Request(scope, receive=receive)
    
    async def fake_next(req):
        return Response("fresh response")
    
    response = await middleware.dispatch(request, fake_next)
    assert response.status_code == 200
    body = json.loads(response.body.decode())
    assert body["result"] == "cached"


@pytest.mark.asyncio
async def test_idempotency_processing_conflict():
    middleware = make_middleware()
    now = datetime.now(timezone.utc)
    future = now + timedelta(hours=2)
    doc_data = {
        "status": "processing",
        "response_body": "{}",
        "expires_at": future.isoformat(),
    }
    mock_doc = MagicMock()
    mock_doc.exists = True
    mock_doc.to_dict.return_value = doc_data
    middleware.db.document.return_value.get.return_value = mock_doc
    
    scope = build_scope(headers={"idempotency-key": "test-key-2"})
    receive = AsyncMock(return_value={"type": "http.request", "body": b"", "more_body": False})
    request = Request(scope, receive=receive)
    
    async def fake_next(req):
        return Response("OK")
    
    with pytest.raises(Exception) as exc_info:
        await middleware.dispatch(request, fake_next)
    assert "already being processed" in str(exc_info.value)


@pytest.mark.asyncio
async def test_idempotency_new_request_sets_processing():
    middleware = make_middleware()
    mock_doc = MagicMock()
    mock_doc.exists = False
    middleware.db.document.return_value.get.return_value = mock_doc
    
    scope = build_scope(headers={"idempotency-key": "test-key-3"})
    receive = AsyncMock(return_value={"type": "http.disconnect"})
    request = Request(scope, receive=receive)
    
    async def fake_next(req):
        return Response("OK")
    
    response = await middleware.dispatch(request, fake_next)
    assert response.status_code == 200
    middleware.db.document.return_value.set.assert_called_once()
    set_args = middleware.db.document.return_value.set.call_args[0][0]
    assert set_args["status"] == "processing"
