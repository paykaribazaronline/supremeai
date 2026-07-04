# 📄 ফাইল: backend/api/routes/site_actions.py

**প্রকার:** .py  
**সাইজ:** 4,905 বাইট  
**আপডেট:** 2026-07-04T08:43:35.148314

---

## কোড

```py
# বাংলা মন্তব্য: site_actions_registry — ডাটাবেস-চালিত (SQLite) CRUD রাউটার।
# সুপার-অ্যাডমিন টার্গেট ওয়েবসাইটের URL, DOM সিলেক্টর ও ইন্টার‌্যাকশন রুল ডায়নামিকভাবে
# ম্যাপ করতে পারেন — হার্ডকোডেড কনফিগ ছাড়াই অ্যাকশন ইঞ্জিন চালানোর জন্য।
# /api/admin/site-actions প্রিফিক্স স্টুডিও ড্যাশবোর্ড থেকে রিচেবল; প্ল্যাটফর্মের সাধারণ
# SUPREMEAI_API_TOKEN গেট (auth_middleware) সেট থাকলে এই রুটগুলো টোকেন দাবি করে।

import os
import sqlite3
import threading
import time

from fastapi import APIRouter
from fastapi import HTTPException
from pydantic import BaseModel


router = APIRouter(prefix="/api/admin/site-actions", tags=["Site Actions Registry"])

DB_PATH = os.getenv("SITE_ACTIONS_DB", "data/site_actions.db")
_lock = threading.Lock()


def _conn() -> sqlite3.Connection:
    os.makedirs(os.path.dirname(DB_PATH) or ".", exist_ok=True)
    conn = sqlite3.connect(DB_PATH, check_same_thread=False)
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS site_actions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            site_name TEXT NOT NULL,
            url_pattern TEXT NOT NULL,
            action_name TEXT NOT NULL,
            selector TEXT NOT NULL,
            action_type TEXT NOT NULL DEFAULT 'click',
            notes TEXT DEFAULT '',
            enabled INTEGER NOT NULL DEFAULT 1,
            updated_at REAL NOT NULL
        )
        """
    )
    return conn


class SiteActionIn(BaseModel):
    site_name: str
    url_pattern: str
    action_name: str
    selector: str
    action_type: str = "click"
    notes: str = ""
    enabled: bool = True


def _row_to_dict(row: tuple) -> dict:
    return {
        "id": row[0],
        "site_name": row[1],
        "url_pattern": row[2],
        "action_name": row[3],
        "selector": row[4],
        "action_type": row[5],
        "notes": row[6],
        "enabled": bool(row[7]),
        "updated_at": row[8],
    }


@router.get("/")
def list_site_actions():
    with _lock, _conn() as conn:
        rows = conn.execute(
            "SELECT * FROM site_actions ORDER BY updated_at DESC"
        ).fetchall()
    return {"items": [_row_to_dict(r) for r in rows], "total": len(rows)}


@router.post("/")
def create_site_action(payload: SiteActionIn):
    with _lock, _conn() as conn:
        cur = conn.execute(
            """
            INSERT INTO site_actions
                (site_name, url_pattern, action_name, selector, action_type, notes, enabled, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                payload.site_name,
                payload.url_pattern,
                payload.action_name,
                payload.selector,
                payload.action_type,
                payload.notes,
                int(payload.enabled),
                time.time(),
            ),
        )
        conn.commit()
        new_id = cur.lastrowid
        row = conn.execute(
            "SELECT * FROM site_actions WHERE id = ?", (new_id,)
        ).fetchone()
    return _row_to_dict(row)


@router.put("/{action_id}")
def update_site_action(action_id: int, payload: SiteActionIn):
    with _lock, _conn() as conn:
        cur = conn.execute(
            """
            UPDATE site_actions SET
                site_name = ?, url_pattern = ?, action_name = ?, selector = ?,
                action_type = ?, notes = ?, enabled = ?, updated_at = ?
            WHERE id = ?
            """,
            (
                payload.site_name,
                payload.url_pattern,
                payload.action_name,
                payload.selector,
                payload.action_type,
                payload.notes,
                int(payload.enabled),
                time.time(),
                action_id,
            ),
        )
        conn.commit()
        if cur.rowcount == 0:
            raise HTTPException(status_code=404, detail="Site action not found")
        row = conn.execute(
            "SELECT * FROM site_actions WHERE id = ?", (action_id,)
        ).fetchone()
    return _row_to_dict(row)


@router.delete("/{action_id}")
def delete_site_action(action_id: int):
    with _lock, _conn() as conn:
        cur = conn.execute("DELETE FROM site_actions WHERE id = ?", (action_id,))
        conn.commit()
        if cur.rowcount == 0:
            raise HTTPException(status_code=404, detail="Site action not found")
    return {"success": True}

```