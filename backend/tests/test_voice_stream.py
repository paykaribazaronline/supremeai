from __future__ import annotations

import os

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from core.app import app as base_app


@pytest.fixture()
def test_app() -> FastAPI:
    os.environ["SUPREMEAI_API_TOKEN"] = "test-token"
    from api.routes.voice import router as voice_router
    try:
        base_app.include_router(voice_router, prefix="/api/voice")
    except ValueError:
        pass # Router already added
    return base_app


def test_stream_audio_requires_text(test_app: FastAPI):
    client = TestClient(test_app)
    resp = client.get("/api/voice/stream_audio")
    assert resp.status_code == 400
    assert "Text cannot be empty" in resp.text
    os.environ.pop("SUPREMEAI_API_TOKEN", None)


def test_stream_audio_returns_audio_mpeg(test_app: FastAPI):
    client = TestClient(test_app)
    resp = client.get("/api/voice/stream_audio?text=hello")
    assert resp.status_code == 200
    assert "audio/mpeg" in resp.headers["content-type"]
    os.environ.pop("SUPREMEAI_API_TOKEN", None)
