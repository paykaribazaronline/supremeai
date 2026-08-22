"""Command Center WebSocket router (ws/command_center.py) এর ইউনিট টেস্ট।

বাংলা: /ws/command-center/health এন্ডপয়েন্ট কভার করা হয়েছে (TestClient দিয়ে)।
"""

from __future__ import annotations

from fastapi import FastAPI
from fastapi.testclient import TestClient


def _build_client():
    from ws.command_center import router

    app = FastAPI()
    app.include_router(router)
    return app


def test_ws_health_returns_ok():
    client = TestClient(_build_client())
    resp = client.get("/ws/command-center/health")
    assert resp.status_code == 200
    assert resp.json() == {"status": "ok"}
