"""Ecosystem shared storage — self-contained SQLite (ROADMAP §50).

বাংলা: সব ecosystem table একই SQLite DB-তে। WAL mode, idempotent create.
Production supremeai-এর Postgres থেকে আলাদা — কোনো migration লাগে না।
"""

from __future__ import annotations

import json
import sqlite3
from pathlib import Path
from typing import Any

_DATA_DIR = Path(__file__).resolve().parent.parent / "data"
_DB_PATH = _DATA_DIR / "ecosystem.db"


def get_db_path() -> Path:
    _DATA_DIR.mkdir(parents=True, exist_ok=True)
    return _DB_PATH


def get_conn() -> sqlite3.Connection:
    conn = sqlite3.connect(get_db_path(), timeout=30)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    return conn


def ensure_columns(conn: sqlite3.Connection, table: str, migrations: dict[str, str]) -> None:
    existing = {r["name"] for r in conn.execute(f"PRAGMA table_info({table})").fetchall()}
    for col, ddl in migrations.items():
        if col not in existing:
            conn.execute(ddl)


def jdump(value: Any) -> str:
    return json.dumps(value, sort_keys=True, default=str, separators=(",", ":"))


def jload(value: str | None, default: Any = None) -> Any:
    if not value:
        return default if default is not None else {}
    try:
        return json.loads(value)
    except (TypeError, ValueError):
        return default if default is not None else {}


__all__ = ["get_db_path", "get_conn", "ensure_columns", "jdump", "jload"]
