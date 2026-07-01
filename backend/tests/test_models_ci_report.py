import json

import pytest
from pydantic import ValidationError

from models.ci_report import CIReportPayload, now_epoch


def test_now_epoch_returns_int():
    epoch = now_epoch()
    assert isinstance(epoch, int)
    assert epoch > 0


def test_ci_report_payload_valid():
    payload = CIReportPayload(
        run_id=1,
        run_number=2,
        event_name="push",
        actor="user",
        workflow_name="CI",
        status="success",
        runtime_seconds=120,
        commit_sha="abc123",
        branch="main",
        jobs_summary={"job1": "success"},
        error_logs=None,
    )
    assert payload.run_id == 1
    assert payload.jobs_summary == {"job1": "success"}


def test_ci_report_payload_defaults():
    payload = CIReportPayload(
        run_id=1,
        run_number=2,
        event_name="push",
        actor="user",
        workflow_name="CI",
        status="success",
        runtime_seconds=120,
        commit_sha="abc123",
        branch="main",
    )
    assert payload.jobs_summary is None
    assert payload.error_logs is None


def test_ci_report_payload_missing_required():
    with pytest.raises(ValidationError):
        CIReportPayload(
            run_number=2,
            event_name="push",
            actor="user",
            workflow_name="CI",
            status="success",
            runtime_seconds=120,
            commit_sha="abc123",
            branch="main",
        )


def test_ci_report_payload_jobs_summary_json_roundtrip():
    data = {
        "run_id": 1,
        "run_number": 2,
        "event_name": "push",
        "actor": "user",
        "workflow_name": "CI",
        "status": "success",
        "runtime_seconds": 10,
        "commit_sha": "abc",
        "branch": "main",
        "jobs_summary": {"a": 1},
    }
    payload = CIReportPayload(**data)
    dumped = json.dumps(payload.jobs_summary)
    reloaded = json.loads(dumped)
    assert reloaded == {"a": 1}
