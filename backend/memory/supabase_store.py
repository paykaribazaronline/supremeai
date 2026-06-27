from __future__ import annotations

import json
import os
from datetime import datetime
from datetime import timezone
from urllib.parse import urlparse

from memory.sqlite_store import SQLiteMemoryStore


class SupabaseStore(SQLiteMemoryStore):
    def __init__(self, database_url: str | None = None, local_path: str | None = None):
        self.database_url = (
            database_url
            or os.getenv("SUPABASE_DATABASE_URL_POOLER")
            or os.getenv("SUPABASE_DATABASE_URL")
            or os.getenv("SUPABASE_DB_URL")
            or os.getenv("DATABASE_URL")
        )
        self.local_path = local_path or os.getenv("SQLITE_PATH", "data/supremeai.db")
        self._provider = "supabase" if self.database_url else "sqlite"
        self._supabase_client = None
        super().__init__(str(self.local_path))

    @property
    def provider(self) -> str:
        return self._provider

    def _get_supabase_client(self):
        if self._supabase_client is None:
            try:
                from supabase import create_client

                url = os.getenv("SUPABASE_URL")
                if not url and self.database_url:
                    parsed = urlparse(self.database_url)
                    hostname = parsed.hostname or ""
                    if hostname.endswith("supabase.co"):
                        if hostname.startswith("db."):
                            hostname = hostname[3:]
                        url = f"https://{hostname}"
                    elif parsed.scheme in ("http", "https"):
                        url = self.database_url.rstrip("/")

                if not url:
                    raise RuntimeError("Unable to derive a valid Supabase URL. Set SUPABASE_URL or use a direct Supabase DB URL.")

                key = os.getenv("SUPABASE_KEY", "")
                if not key:
                    raise RuntimeError("SUPABASE_KEY is required for Supabase client initialization")

                self._supabase_client = create_client(url, key)
            except Exception as exc:
                raise RuntimeError(f"Supabase client init failed: {exc}") from exc
        return self._supabase_client

    def save_conversation(self, session_id: str, messages: list) -> None:
        if self._provider == "supabase":
            client = self._get_supabase_client()
            client.table("conversations").upsert(
                {
                    "session_id": session_id,
                    "messages": json.dumps(messages),
                    "updated_at": datetime.now(timezone.utc).isoformat(),
                }
            ).execute()
        else:
            self.get_session_messages(session_id)
            for msg in messages:
                if isinstance(msg, dict):
                    self.save_message(session_id, msg.get("role", "user"), msg.get("content", ""))

    def get_conversation(self, session_id: str) -> list:
        if self._provider == "supabase":
            client = self._get_supabase_client()
            result = client.table("conversations").select("messages").eq("session_id", session_id).execute()
            rows = result.data
            if rows:
                return json.loads(rows[0]["messages"])
            return []
        return self.get_session_messages(session_id)

    def save_learned_fact(self, fact: dict) -> None:
        fact_id = fact.get("id")
        if not fact_id:
            fact_id = f"fact_{datetime.now(timezone.utc).timestamp()}"
            fact["id"] = fact_id
        fact["created_at"] = fact.get("created_at", datetime.now(timezone.utc).isoformat())
        if self._provider == "supabase":
            client = self._get_supabase_client()
            client.table("learned_facts").upsert(
                {
                    "id": fact_id,
                    "content": json.dumps(fact),
                    "tags": json.dumps(fact.get("tags", [])),
                    "created_at": fact["created_at"],
                }
            ).execute()
        else:
            conn = self._get_connection()
            cursor = conn.cursor()
            cursor.execute(
                "INSERT OR REPLACE INTO tasks (id, task_description, task_type, success, cost, outcome_text) VALUES (?, ?, ?, ?, ?, ?)",
                (fact_id, json.dumps(fact), "learned_fact", 1, 0.0, json.dumps(fact)),
            )
            conn.commit()
            self._close_connection(conn)

    def search_facts(self, query: str) -> list:
        if self._provider == "supabase":
            client = self._get_supabase_client()
            result = client.table("learned_facts").select("content").ilike("content", f"%{query}%").execute()
            return [json.loads(row["content"]) for row in result.data]
        return []
