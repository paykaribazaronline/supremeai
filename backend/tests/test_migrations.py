import os
import re


MIGRATIONS_DIR = os.path.join(os.path.dirname(__file__), "..", "database", "migrations")


def test_migrations_are_numbered_sequentially():
    files = [f for f in os.listdir(MIGRATIONS_DIR) if f.endswith(".sql") and f.startswith("0") and f != "07_tenant_sso_offline.sql"]
    numbers = sorted(re.match(r"(\d+)", f).group(1) for f in files)
    assert numbers[0] == "01"
    for i in range(len(numbers) - 1):
        current = int(numbers[i])
        next_n = int(numbers[i + 1])
        assert next_n == current + 1, f"Migration gap between {numbers[i]} and {numbers[i + 1]}"


def test_migrations_contain_required_tables():
    files = sorted(f for f in os.listdir(MIGRATIONS_DIR) if f.endswith(".sql") and f.startswith("0"))
    all_sql = "\n".join(open(os.path.join(MIGRATIONS_DIR, f), encoding="utf-8").read() for f in files)
    required_tables = [
        "referral_codes",
        "credit_wallets",
        "credit_ledger",
        "tenant_limits",
        "sso_configs",
        "offline_sync_logs",
    ]
    for table in required_tables:
        assert "CREATE TABLE" in all_sql and table in all_sql, f"Required table '{table}' missing from migrations"


def test_referral_system_constraints():
    path = os.path.join(MIGRATIONS_DIR, "06_referral_system.sql")
    sql = open(path, encoding="utf-8").read()
    assert "PRIMARY KEY" in sql
    assert "referral_redemptions" in sql
    assert "credit_ledger" in sql


def test_tenant_config_constraints():
    path = os.path.join(MIGRATIONS_DIR, "07_tenant_config.sql")
    sql = open(path, encoding="utf-8").read()
    assert "billing_tier TEXT" in sql
    assert "CHECK (billing_tier" in sql


def test_sso_config_constraints():
    path = os.path.join(MIGRATIONS_DIR, "08_sso_configs.sql")
    sql = open(path, encoding="utf-8").read()
    assert "provider TEXT CHECK" in sql
    assert "group_role_mapping JSONB" in sql
    assert "sso_sessions" in sql


def test_offline_sync_logs_constraints():
    path = os.path.join(MIGRATIONS_DIR, "09_offline_sync_logs.sql")
    sql = open(path, encoding="utf-8").read()
    assert "offline_sync_logs" in sql
    assert "payload JSONB" in sql
    assert "status TEXT DEFAULT 'synced'" in sql
    assert "offline_sync_conflicts" in sql


def test_sw_registers_service_worker():
    index_path = os.path.join(os.path.dirname(__file__), "..", "..", "apps", "studio-client", "index.html")
    html = open(index_path, encoding="utf-8").read()
    assert "register('/sw.js')" in html, "Service Worker must be registered in index.html"


def test_pwa_manifest_exists():
    manifest_path = os.path.join(
        os.path.dirname(__file__),
        "..",
        "..",
        "apps",
        "studio-client",
        "public",
        "manifest.json",
    )
    assert os.path.isfile(manifest_path), "manifest.json must exist for PWA installability"
    import json

    manifest = json.load(open(manifest_path, encoding="utf-8"))
    assert manifest["name"]
    assert manifest["start_url"]
