"""বাংলা: api.routes.swarm আগে api/routers.py-তে register-ই হতো না, ফলে পুরো
/api/v1/swarm/* সারফেস (SSE stream, patch-telemetry, VSCode self-healing,
emergency-stop halt/resume) 404 দিত। এই টেস্টগুলো নিশ্চিত করে সেই router
mounting এবং নতুন halt/resume/telemetry endpoint-গুলো সত্যিই কাজ করে।
"""

from unittest.mock import AsyncMock, patch

import pytest
from api.dependencies import get_current_user_token
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


@patch("api.routes.swarm.swarm_streamer")
def test_swarm_router_is_registered(mock_streamer):
    """বাংলা: /api/v1/swarm/* রুট রেজিস্টার্ড কিনা — regression guard, যেন
    ভবিষ্যতে কেউ router.py থেকে এন্ট্রিটা আবার সাইলেন্টলি বাদ না দেয়।
    """
    mock_streamer.set_halt = AsyncMock()
    mock_streamer.broadcast = AsyncMock()
    response = client.post("/api/v1/swarm/halt")
    assert response.status_code != 404, "Swarm router not registered! Returned 404."


@patch("api.routes.admin.get_current_user_token")
@patch("core.security.auth_middleware._decode_jwt")
def test_halt_requires_admin(mock_decode_jwt, mock_token):
    mock_decode_jwt.return_value = {"sub": "user_test", "role": "user"}
    app.dependency_overrides[get_current_user_token] = lambda: {
        "sub": "user_test",
        "role": "user",
    }

    response = client.post(
        "/api/v1/swarm/halt", headers={"Authorization": "Bearer dummy"}
    )
    assert response.status_code in (401, 403)

    app.dependency_overrides = {}


@patch("api.routes.admin.get_current_user_token")
@patch("core.security.auth_middleware._decode_jwt")
def test_halt_sets_flag_and_broadcasts(mock_decode_jwt, mock_token):
    mock_decode_jwt.return_value = {"sub": "admin_test", "role": "admin"}
    app.dependency_overrides[get_current_user_token] = lambda: {
        "sub": "admin_test",
        "role": "admin",
    }

    from api.routes.admin import get_current_admin

    app.dependency_overrides[get_current_admin] = lambda: {
        "sub": "admin_test",
        "role": "admin",
    }

    with patch("api.routes.swarm.swarm_streamer") as mock_streamer:
        mock_streamer.set_halt = AsyncMock()
        mock_streamer.broadcast = AsyncMock()

        response = client.post(
            "/api/v1/swarm/halt", headers={"Authorization": "Bearer dummy"}
        )

        assert response.status_code == 202
        assert response.json()["status"] == "halted"
        mock_streamer.set_halt.assert_called_once()
        mock_streamer.broadcast.assert_called_once()
        assert mock_streamer.broadcast.call_args.kwargs["event_type"] == "CIRCUIT_OPEN"

    app.dependency_overrides = {}


@patch("api.routes.admin.get_current_user_token")
@patch("core.security.auth_middleware._decode_jwt")
def test_resume_clears_flag_and_broadcasts(mock_decode_jwt, mock_token):
    mock_decode_jwt.return_value = {"sub": "admin_test", "role": "admin"}
    app.dependency_overrides[get_current_user_token] = lambda: {
        "sub": "admin_test",
        "role": "admin",
    }

    from api.routes.admin import get_current_admin

    app.dependency_overrides[get_current_admin] = lambda: {
        "sub": "admin_test",
        "role": "admin",
    }

    with patch("api.routes.swarm.swarm_streamer") as mock_streamer:
        mock_streamer.clear_halt = AsyncMock()
        mock_streamer.broadcast = AsyncMock()

        response = client.post(
            "/api/v1/swarm/resume", headers={"Authorization": "Bearer dummy"}
        )

        assert response.status_code == 202
        assert response.json()["status"] == "resumed"
        mock_streamer.clear_halt.assert_called_once()
        assert (
            mock_streamer.broadcast.call_args.kwargs["event_type"] == "CIRCUIT_CLOSED"
        )

    app.dependency_overrides = {}


def test_telemetry_persists_to_db_not_just_logs():
    """বাংলা: আগে _save_telemetry_to_db() শুধু logger.info() করত। এখন সত্যিই
    session.add()+commit() হয় কিনা যাচাই করা হচ্ছে।
    """
    from api.routes.swarm import _save_telemetry_to_db

    fake_session = AsyncMock()

    async def fake_get_db_session():
        yield fake_session

    with patch("api.routes.swarm.get_db_session", fake_get_db_session):
        import asyncio

        asyncio.get_event_loop().run_until_complete(
            _save_telemetry_to_db(
                {
                    "error_id": "err-1",
                    "patch_id": "patch-1",
                    "file_path": "foo.py",
                    "status": "ACCEPTED",
                    "similarity_score": 0.9,
                }
            )
        )

    fake_session.add.assert_called_once()
    fake_session.commit.assert_called_once()


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
