"""engine/vector_db.py — Free-tier vector memory adapter.

বাংলা মন্তব্য: এই ফাইলটি আর Pinecone-এর উপর নির্ভর করে না।
এটি core/services.py-তে থাকা shared `experience_db` singleton-এর একটি adapter,
যে একই instance crew_departments.py, auto_skill_creator.py এবং task.py ব্যবহার করে।
ফলে সমস্ত agent এখন সত্যিকারের একটিই memory pool শেয়ার করে।
Pinecone-shaped interface (save_experience, find_similar_experiences) অক্ষত রাখা হয়েছে
যাতে কোনো caller ভাঙে না।
"""

from __future__ import annotations

import asyncio
import logging
from typing import Any

_logger = logging.getLogger(__name__)


class VectorDatabaseClient:
    """
    Free-tier vector memory adapter backed by the shared ExperienceDatabase singleton.
    Previously used Pinecone (paid). Now delegates to the shared ChromaDB/Qdrant/SQLite
    free backend that is already initialised in core/services.py.
    """

    def __init__(self) -> None:
        # বাংলা মন্তব্য: lazy import করা হচ্ছে circular-import এড়াতে।
        # services.py এই module কে import করলে সরাসরি top-level import ঝুঁকিপূর্ণ।
        self._exp_db = None
        self.degraded: bool = False
        _logger.debug(
            "VectorDatabaseClient initialised (free-tier adapter, shared experience_db)"
        )

    def _get_exp_db(self):
        """Lazily fetch the shared experience_db singleton from core.services."""
        if self._exp_db is None:
            try:
                import core.services as _services

                self._exp_db = _services.experience_db
                # বাংলা মন্তব্য: singleton থেকে degraded state propagate করা হচ্ছে
                if getattr(self._exp_db, "vector_backend_degraded", False):
                    self.degraded = True
            except Exception as exc:
                _logger.error(
                    f"VectorDatabaseClient: failed to fetch shared experience_db — "
                    f"memory will be DEGRADED. error={exc!r}"
                )
                self.degraded = True
        return self._exp_db

    async def save_experience(
        self, vector: list[float], metadata: dict[str, Any]
    ) -> None:
        """
        Saves an experience into the shared memory pool.
        বাংলা মন্তব্য: vector argument এখন ignore করা হচ্ছে — experience_db নিজেই
        sentence-transformers দিয়ে free embedding তৈরি করে। caller-এর interface অক্ষত রাখতে
        এই parameter signature বজায় রাখা হলো।
        """
        exp_db = self._get_exp_db()
        if exp_db is None:
            self.degraded = True
            _logger.error(
                "save_experience() skipped: shared experience_db unavailable (DEGRADED). "
                "Experience NOT persisted."
            )
            return

        try:
            from adaptive_engine.experience_db import Experience

            exp = Experience(
                request=metadata.get("request", metadata.get("patch_id", "")),
                action_taken=metadata.get("solution", metadata.get("action", "")),
                result=metadata.get("result", "success"),
                generated_code=metadata.get("generated_code"),
                what_worked=metadata.get("what_worked", []),
                what_failed=metadata.get("what_failed", []),
            )
            # বাংলা মন্তব্য: blocking SQLite write — thread-এ offload করা হচ্ছে
            await asyncio.to_thread(exp_db.record_experience, exp)
            _logger.debug(
                f"🧠 Saved neural memory experience via shared pool: {metadata.get('patch_id', 'n/a')}"
            )
        except Exception as exc:
            self.degraded = True
            _logger.error(
                f"save_experience() failed (experience NOT persisted, DEGRADED): {exc!r}"
            )

    async def find_similar_experiences(
        self, vector: list[float], top_k: int = 3
    ) -> list[dict[str, Any]]:
        """
        Retrieves past experiences from the shared free-tier vector backend.
        বাংলা মন্তব্য: vector argument ignore করা হয় — experience_db.find_similar()
        নিজেই query embedding করে। Pinecone-shaped return format বজায় রাখা হলো।
        """
        exp_db = self._get_exp_db()
        if exp_db is None:
            self.degraded = True
            _logger.error(
                "find_similar_experiences() skipped: shared experience_db unavailable "
                "(DEGRADED, not 'no matches found')."
            )
            return []

        # বাংলা মন্তব্য: vector argument থেকে raw query text বের করার কোনো উপায় নেই,
        # তাই caller যদি raw text পাঠায় (str), সেটা সরাসরি ব্যবহার করা হবে।
        # memory_middleware এখন raw text পাঠায়, তাই এটা কাজ করে।
        query_text = vector if isinstance(vector, str) else ""  # type: ignore[assignment]

        if not query_text:
            _logger.debug(
                "find_similar_experiences(): no query text available, returning empty."
            )
            return []

        try:
            hits = await asyncio.to_thread(exp_db.find_similar, query_text, top_k)
            # বাংলা মন্তব্য: experience_db থেকে পাওয়া degraded state propagate করা হচ্ছে
            if getattr(exp_db, "vector_backend_degraded", False):
                self.degraded = True
            # Pinecone-shaped format-এ রূপান্তর করা হচ্ছে callers-এর সাথে compatibility রাখতে
            return [
                {
                    "id": h.get("id"),
                    "score": h.get("score", 0.0),
                    "metadata": h.get("meta", {}),
                    "solution": h.get("response", ""),
                }
                for h in hits
            ]
        except Exception as exc:
            self.degraded = True
            _logger.error(
                f"find_similar_experiences() failed (returning empty, DEGRADED state): {exc!r}"
            )
            return []


# Global instance — lazy singleton
vector_db = VectorDatabaseClient()
