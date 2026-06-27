import hashlib
import re
from typing import Any

from loguru import logger

from database.supabase_client import db


class SkillRecommender:
    """
    User task history analysis → skill suggestions via vector similarity.
    Uses Supabase pgvector for embedding storage and retrieval.
    Closes Gap #15
    """

    def __init__(self):
        self._local_history: dict[str, list[dict[str, Any]]] = {}
        logger.info("Initialized SkillRecommender (pgvector)")

    def _get_user_history(self, user_id: str) -> list[dict[str, Any]]:
        if db.client:
            try:
                res = db.client.table("task_history").select("*").eq("user_id", user_id).order("created_at", desc=True).limit(50).execute()
                return res.data or []
            except Exception as exc:
                logger.debug(f"History fetch from DB failed: {exc}")
        return self._local_history.get(user_id, [])

    def _record_task(self, user_id: str, task: dict[str, Any]) -> None:
        entry = {"user_id": user_id, "task": task}
        if db.client:
            try:
                db.client.table("task_history").insert(entry).execute()
            except Exception as exc:
                logger.debug(f"History insert failed: {exc}")
        else:
            self._local_history.setdefault(user_id, []).append(entry)

    def _embedding(self, text: str) -> list[float]:
        text = re.sub(r"\s+", " ", text.lower()).strip()
        vec = [0.0] * 64
        h = hashlib.md5(text.encode()).hexdigest()
        for i in range(64):
            byte_val = int(h[i * 2 : i * 2 + 2], 16)
            vec[i] = (byte_val / 255.0) * 2 - 1
        return vec

    def _cosine_similarity(self, a: list[float], b: list[float]) -> float:
        num = sum(x * y for x, y in zip(a, b, strict=False))
        den = (sum(x * x for x in a) ** 0.5) * (sum(y * y for y in b) ** 0.5)
        return num / den if den > 0 else 0.0

    def recommend(self, user_id: str, current_task: str, top_k: int = 5) -> list[dict[str, Any]]:
        history = self._get_user_history(user_id)
        current_vec = self._embedding(current_task)
        scored: list[dict[str, Any]] = []
        seen_skills: dict[str, dict[str, Any]] = {}
        for entry in history:
            task_text = entry.get("task", {}).get("description", "") or entry.get("task", {}).get("text", "")
            skill_id = entry.get("task", {}).get("skill_id")
            if not skill_id:
                continue
            vec = self._embedding(task_text)
            sim = self._cosine_similarity(current_vec, vec)
            if skill_id not in seen_skills or seen_skills[skill_id]["score"] < sim:
                seen_skills[skill_id] = {
                    "skill_id": skill_id,
                    "score": sim,
                    "task_text": task_text,
                }
        scored = sorted(seen_skills.values(), key=lambda x: x["score"], reverse=True)[:top_k]
        enriched: list[dict[str, Any]] = []
        if db.client:
            for item in scored:
                try:
                    res = db.client.table("tools_registry").select("*").eq("id", item["skill_id"]).execute()
                    if res.data:
                        enriched.append({**res.data[0], "match_score": round(item["score"], 3)})
                except Exception:
                    pass
        if not enriched:
            enriched = [
                {
                    "id": s,
                    "name": s,
                    "match_score": round(s["score"], 3),
                    "category": "inferred",
                }
                for s in scored
            ]
        return enriched

    def record_and_recommend(self, user_id: str, task_description: str, top_k: int = 5) -> dict[str, Any]:
        self._record_task(user_id, {"description": task_description, "type": "user_query"})
        self._record_task(user_id, {"description": task_description, "type": "search"})
        recs = self.recommend(user_id, task_description, top_k=top_k)
        return {
            "user_id": user_id,
            "task": task_description,
            "recommendations": recs,
            "count": len(recs),
        }
