import os
from types import SimpleNamespace

import pytest

from database.supabase_client import SupabaseDB


class FakeCursor:
    def __init__(self):
        self.executed = []

    def execute(self, statement):
        self.executed.append(statement)

    def close(self):
        return None


class FakeConnection:
    def __init__(self):
        self.cursor_obj = FakeCursor()

    def cursor(self):
        return self.cursor_obj

    def commit(self):
        return None

    def close(self):
        return None


def test_bootstrap_schema_executes_expected_sql(monkeypatch):
    fake_conn = FakeConnection()
    fake_psycopg = SimpleNamespace(connect=lambda *args, **kwargs: fake_conn)

    monkeypatch.setattr("database.supabase_client.psycopg2", fake_psycopg)
    monkeypatch.setattr("database.supabase_client.create_client", lambda *args, **kwargs: object())
    monkeypatch.setenv("SUPABASE_URL", "https://example.supabase.co")
    monkeypatch.setenv("SUPABASE_KEY", "test-key")
    monkeypatch.setenv("SUPABASE_DATABASE_URL", "postgresql://user:pass@localhost:5432/postgres")

    db = SupabaseDB()
    db.bootstrap_schema()

    statements = "\n".join(fake_conn.cursor_obj.executed)
    assert "CREATE TABLE IF NOT EXISTS system_config" in statements
    assert "CREATE TABLE IF NOT EXISTS feature_flags" in statements
    assert "CREATE TABLE IF NOT EXISTS github_repos" in statements
    assert "CREATE TABLE IF NOT EXISTS ai_model_behavior" in statements
    assert "CREATE TABLE IF NOT EXISTS user_preferences" in statements
    assert "CREATE TABLE IF NOT EXISTS usage_metrics" in statements
    assert "CREATE TABLE IF NOT EXISTS task_history" in statements
    assert "CREATE TABLE IF NOT EXISTS skill_proposals" in statements
    assert "CREATE TABLE IF NOT EXISTS feedback_loop" in statements
    assert "CREATE TABLE IF NOT EXISTS evolution_logs" in statements


def test_bootstrap_schema_prefers_pooler_when_available(monkeypatch):
    fake_conn = FakeConnection()
    captured_urls = []

    def fake_connect(url, *args, **kwargs):
        captured_urls.append(url)
        return fake_conn

    fake_psycopg = SimpleNamespace(connect=fake_connect)
    monkeypatch.setattr("database.supabase_client.psycopg2", fake_psycopg)
    monkeypatch.setattr("database.supabase_client.create_client", lambda *args, **kwargs: object())
    monkeypatch.setenv("SUPABASE_DATABASE_URL", "postgresql://user:pass@localhost:5432/postgres")
    monkeypatch.setenv(
        "SUPABASE_DATABASE_URL_POOLER",
        "postgresql://pooler_user:pooler_pass@localhost:6543/postgres",
    )

    db = SupabaseDB()
    db.bootstrap_schema()

    assert captured_urls == [
        "postgresql://pooler_user:pooler_pass@localhost:6543/postgres",
    ]


def test_insert_task_history_retries_after_schema_cache_error(monkeypatch):
    class FakeResponse:
        def __init__(self, data=None):
            self.data = data
            self.error = None

    class FakeTable:
        def __init__(self):
            self.calls = 0

        def insert(self, entry):
            self.entry = entry
            return self

        def execute(self):
            self.calls += 1
            if self.calls == 1:
                raise Exception("Could not find the table 'public.task_history' in the schema cache")
            return FakeResponse([{"id": 1, **self.entry}])

    class FakeClient:
        def __init__(self):
            self.table_obj = FakeTable()

        def table(self, name):
            assert name == "task_history"
            return self.table_obj

    monkeypatch.setattr("database.supabase_client.create_client", lambda *args, **kwargs: FakeClient())
    monkeypatch.setenv("SUPABASE_URL", "https://example.supabase.co")
    monkeypatch.setenv("SUPABASE_KEY", "test-key")
    db = SupabaseDB()
    called = []

    def fake_bootstrap():
        called.append("bootstrap")

    db.bootstrap_schema = fake_bootstrap

    result = db.insert_task_history(
        "retry_task",
        "retry_approach",
        "retry_result",
        False,
        "2026-06-27T17:05:00Z",
    )

    assert result is not None
    assert result["task"] == "retry_task"
    assert called == ["bootstrap"]


LIVE_SUPABASE = bool(
    os.getenv("SUPABASE_URL") and os.getenv("SUPABASE_KEY") and (os.getenv("SUPABASE_DATABASE_URL") or os.getenv("SUPABASE_DATABASE_URL_POOLER"))
)


@pytest.mark.skipif(
    not LIVE_SUPABASE,
    reason="Live Supabase environment is not configured for integration testing",
)
def test_live_supabase_schema_bootstrap_and_task_history_write():
    db = SupabaseDB()
    assert db.client is not None

    db.bootstrap_schema()

    task = db.insert_task_history(
        "live_task",
        "integration_approach",
        "works",
        True,
        "2026-06-27T17:05:00Z",
    )

    assert task is not None
    assert task["task"] == "live_task"

    failures = db.get_repeated_failures(min_occurrences=1)
    assert any(entry["task"] == "live_task" for entry in failures)
