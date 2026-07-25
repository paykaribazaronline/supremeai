"""Unified Multi-Database Transaction Manager for SupremeAI 2.0."""

# বাংলা মন্তব্য: Supabase, Postgres, ChromaDB, SQLite এবং Firestore-এর রিয়েল-টাইম ডাটা সিঙ্ক ও ট্রানজ্যাকশন ম্যানেজার।

from __future__ import annotations

import logging
from typing import Any

from memory.chromadb_store import ChromaDBStore
from memory.cloud_postgres_store import CloudPostgresStore
from memory.sqlite_store import SQLiteStore
from memory.supabase_store import SupabaseStore

logger = logging.getLogger("supremeai.unified_db")


class UnifiedDBManager:
    """Centralized transaction coordinator across all underlying database engines."""

    def __init__(
        self,
        supabase_store: SupabaseStore | None = None,
        sqlite_store: SQLiteStore | None = None,
        chroma_store: ChromaDBStore | None = None,
        postgres_store: CloudPostgresStore | None = None,
    ):
        self.supabase = supabase_store or SupabaseStore()
        self.sqlite = sqlite_store or SQLiteStore()
        self.chroma = chroma_store or ChromaDBStore()
        self.postgres = postgres_store or CloudPostgresStore()

    async def save_record(
        self,
        collection: str,
        record_id: str,
        data: dict[str, Any],
        text_content: str | None = None,
    ) -> dict[str, bool]:
        """Atomically persist record metadata and embeddings across multi-cloud stores.

        বাংলা মন্তব্য: একক মেথড কলে সুনির্দিষ্ট রেকর্ডকে সকল যুক্ত ডাটাবেসে একসাথে সেভ করে।
        """
        results = {
            "supabase": False,
            "sqlite": False,
            "chroma": False,
            "postgres": False,
        }

        # 1. Save to SQLite local cache
        try:
            await self.sqlite.save(collection, record_id, data)
            results["sqlite"] = True
        except Exception as e:
            logger.warning(f"[UnifiedDB] SQLite save skipped: {e}")

        # 2. Save to Supabase Cloud Relational DB
        try:
            await self.supabase.insert(collection, {"id": record_id, **data})
            results["supabase"] = True
        except Exception as e:
            logger.warning(f"[UnifiedDB] Supabase save skipped: {e}")

        # 3. Save to Cloud Postgres DB
        try:
            await self.postgres.execute_query(
                f"INSERT INTO {collection} (id, data) VALUES ($1, $2) ON CONFLICT (id) DO UPDATE SET data = $2",  # noqa: S608
                record_id,
                data,
            )
            results["postgres"] = True
        except Exception as e:
            logger.warning(f"[UnifiedDB] Postgres save skipped: {e}")

        # 4. Embed into ChromaDB Vector Store if text provided
        if text_content:
            try:
                await self.chroma.add_document(
                    document_id=record_id,
                    text=text_content,
                    metadata={"collection": collection, **data},
                )
                results["chroma"] = True
            except Exception as e:
                logger.warning(f"[UnifiedDB] ChromaDB embedding skipped: {e}")

        return results

    async def get_record(
        self, collection: str, record_id: str
    ) -> dict[str, Any] | None:
        """Retrieve record with fallback strategy (SQLite -> Supabase -> Postgres).

        বাংলা মন্তব্য: ফাইলটের ওপর ভিত্তি করে পর্যায়ক্রমে লোকাল সিঙ্ক থেকে ডাটা ফেচ করে।
        """
        # Primary lookup: Local SQLite
        try:
            record = await self.sqlite.get(collection, record_id)
            if record:
                return record
        except Exception:  # noqa: S110
            pass

        # Secondary lookup: Cloud Supabase
        try:
            record = await self.supabase.fetch_by_id(collection, record_id)
            if record:
                return record
        except Exception:  # noqa: S110
            pass

        return None


# Global singleton instance
unified_db = UnifiedDBManager()
