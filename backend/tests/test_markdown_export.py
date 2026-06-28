import time

from fastapi.testclient import TestClient

from core.app import app


client = TestClient(app)


def test_markdown_export_async_flow():
    # 1. Trigger export job
    response = client.post(
        "/api/v1/markdown/export", json={"root_dir": ".", "git_diff_only": False}
    )
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert "job_id" in data
    job_id = data["job_id"]

    # 2. Check status (may need to wait briefly, but we check if endpoint returns status)
    time.sleep(0.5)
    status_response = client.get(f"/api/v1/markdown/export/{job_id}/status")
    assert status_response.status_code == 200
    status_data = status_response.json()
    assert "status" in status_data
    assert "progress" in status_data


def test_markdown_history():
    response = client.get("/api/v1/markdown/export/history")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert "history" in data


def test_markdown_compare():
    response = client.post(
        "/api/v1/markdown/compare",
        json={
            "root_dir": ".",
            "range_a_since": "2026-06-20",
            "range_b_since": "2026-06-21",
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert "compare_report" in data


def test_markdown_share():
    response = client.post(
        "/api/v1/markdown/share", json={"markdown": "# Test", "target_ai": "claude"}
    )
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert "share_url" in data
