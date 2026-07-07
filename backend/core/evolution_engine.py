from __future__ import annotations

import hashlib
import logging
import os
import sqlite3
from datetime import UTC
from datetime import datetime
from typing import Any

from brain.model_router import ModelRouter


logger = logging.getLogger(__name__)

try:
    from prometheus_client import Counter
    evolution_write_failures = Counter(
        "evolution_write_failures_total",
        "Number of failures while reading/writing evolution databases"
    )
except ImportError:
    evolution_write_failures = None

class EvolutionEngine:
    """Persists task outcomes, detects repeated failures, proposes and auto-generates skills."""

    def __init__(self, db_path: str | None = None, model_router: ModelRouter | None = None):
        base = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        self.db_path = db_path or os.getenv(
            "EVOLUTION_DB_PATH", os.path.join(base, "data", "evolution.db")
        )
        self.model_router = model_router or ModelRouter()
        os.makedirs(os.path.dirname(str(self.db_path)), exist_ok=True)
        self._ensure_schema()

    def _ensure_schema(self) -> None:
        conn = sqlite3.connect(str(self.db_path))
        try:
            conn.executescript(
                """
                CREATE TABLE IF NOT EXISTS task_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task TEXT NOT NULL,
                    approach TEXT NOT NULL,
                    result TEXT NOT NULL,
                    success INTEGER NOT NULL,
                    created_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS prompt_optimizations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_hash TEXT NOT NULL,
                    original_prompt TEXT,
                    optimized_prompt TEXT,
                    improvement REAL,
                    applied INTEGER DEFAULT 0,
                    created_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS skill_proposals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    skill_name TEXT NOT NULL,
                    source_pattern TEXT,
                    generated_code TEXT,
                    status TEXT DEFAULT 'proposed',
                    created_at TEXT NOT NULL,
                    registered_at TEXT
                );
                CREATE TABLE IF NOT EXISTS feedback_loop (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id TEXT NOT NULL,
                    query TEXT,
                    retrieved_chunks TEXT,
                    user_rating REAL,
                    adjusted INTEGER DEFAULT 0,
                    created_at TEXT NOT NULL
                );
            """
            )
            conn.commit()
        finally:
            conn.close()

    def learn_from_success(
        self, task: str, approach: str, result: str
    ) -> dict[str, Any]:
        created_at = datetime.now(UTC).isoformat()
        supabase_success = False
        try:
            from database.supabase_client import db

            if db.client:
                db.insert_task_history(task, approach, result, True, created_at)
                supabase_success = True
        except Exception as e:  # noqa: BLE001
            logger.warning(f"Failed to insert success to Supabase: {e}")
            if evolution_write_failures:
                evolution_write_failures.inc()

        if not supabase_success:
            return {"stored": False, "error": "Supabase write failed. Saga rollback: skipping SQLite."}

        conn = sqlite3.connect(str(self.db_path))
        try:
            conn.execute(
                "INSERT INTO task_history (task, approach, result, success, created_at) VALUES (?, ?, ?, ?, ?)",
                (task, approach, result, 1, created_at),
            )
            conn.commit()
            return {
                "stored": True,
                "task": task,
                "approach": approach,
                "result": result,
            }
        finally:
            conn.close()

    def learn_from_failure(
        self, task: str, approach: str, result: str
    ) -> dict[str, Any]:
        created_at = datetime.now(UTC).isoformat()
        supabase_success = False
        try:
            from database.supabase_client import db

            if db.client:
                db.insert_task_history(task, approach, result, False, created_at)
                supabase_success = True
        except Exception as e:  # noqa: BLE001
            logger.warning(f"Failed to insert failure to Supabase: {e}")
            if evolution_write_failures:
                evolution_write_failures.inc()

        if not supabase_success:
            return {"stored": False, "error": "Supabase write failed. Saga rollback: skipping SQLite."}

        conn = sqlite3.connect(str(self.db_path))
        try:
            conn.execute(
                "INSERT INTO task_history (task, approach, result, success, created_at) VALUES (?, ?, ?, ?, ?)",
                (task, approach, result, 0, created_at),
            )
            conn.commit()
            return {
                "stored": True,
                "task": task,
                "approach": approach,
                "result": result,
            }
        finally:
            conn.close()

    def detect_repeated_failures(
        self, min_occurrences: int = 3
    ) -> list[dict[str, Any]]:
        try:
            from database.supabase_client import db

            if db.client:
                failures = db.get_repeated_failures(min_occurrences=min_occurrences)
                if failures:
                    return failures
        except Exception as e:  # noqa: BLE001
            logger.warning(f"Failed to query repeated failures from Supabase: {e}")
            if evolution_write_failures:
                evolution_write_failures.inc()

        conn = sqlite3.connect(str(self.db_path))
        try:
            cursor = conn.execute(
                """
                SELECT task, approach, COUNT(*) as failures, MAX(created_at) as last_failed
                FROM task_history
                WHERE success = 0
                GROUP BY task, approach
                HAVING failures >= ?
                ORDER BY failures DESC
                """,
                (min_occurrences,),
            )
            return [
                {
                    "task": row[0],
                    "approach": row[1],
                    "failures": row[2],
                    "last_failed": row[3],
                }
                for row in cursor.fetchall()
            ]
        finally:
            conn.close()

    def detect_underperforming_prompts(
        self, min_occurrences: int = 5, min_failure_rate: float = 0.5
    ) -> list[dict[str, Any]]:
        conn = sqlite3.connect(str(self.db_path))
        try:
            # বাংলা মন্তব্য: এখানে আমরা টাস্কের নাম (প্রম্পট) দ্বারা গ্রুপ করে ব্যর্থতার হার বিশ্লেষণ করছি।
            cursor = conn.execute(
                """
                SELECT
                    task,
                    SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END) as failed_count,
                    COUNT(*) as total_count,
                    CAST(SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END) AS REAL) / COUNT(*) as failure_rate
                FROM task_history
                GROUP BY task
                HAVING total_count >= ? AND failure_rate >= ?
                ORDER BY failure_rate DESC, failed_count DESC
                """,
                (min_occurrences, min_failure_rate),
            )
            return [
                {
                    "task": row[0],
                    "failures": row[1],
                    "total": row[2],
                    "failure_rate": row[3],
                }
                for row in cursor.fetchall()
            ]
        finally:
            conn.close()

    def propose_prompt_optimization(self, original_prompt: str, failure_data: dict[str, Any]) -> dict[str, Any]:
        task_hash = hashlib.sha256(original_prompt.encode()).hexdigest()

        # বাংলা মন্তব্য: LLM ব্যবহার করে উন্নত প্রম্পট তৈরির জন্য একটি প্রম্পট তৈরি করা হচ্ছে।
        optimization_prompt = f"""
System: You are a Prompt Optimization specialist. Your task is to rewrite a failing prompt to improve its success rate.

Original Prompt:
"{original_prompt}"

This prompt has a failure rate of {failure_data['failure_rate']:.2%} after {failure_data['total']} attempts.

Based on the prompt, rewrite it to be more precise, clear, and effective. Provide only the new prompt, without any explanation or extra text.
"""

        try:
            response = self.model_router.route_and_generate(optimization_prompt, task_type="analysis")
            optimized_prompt = response.get("text", "").strip()

            if not optimized_prompt or optimized_prompt == original_prompt:
                return {"status": "no_change_generated"}

        except Exception as e:  # noqa: BLE001
            return {"status": "error", "error": str(e)}

        created_at = datetime.now(UTC).isoformat()
        conn = sqlite3.connect(self.db_path)
        try:
            conn.execute(
                """
                INSERT INTO prompt_optimizations (task_hash, original_prompt, optimized_prompt, created_at)
                VALUES (?, ?, ?, ?)
                """,
                (task_hash, original_prompt, optimized_prompt, created_at),
            )
            conn.commit()
            return {
                "task_hash": task_hash,
                "original_prompt": original_prompt,
                "optimized_prompt": optimized_prompt,
                "status": "proposed",
            }
        finally:
            conn.close()

    def propose_new_skill(self, pattern: str) -> dict[str, Any]:
        skill_name = f"auto_{pattern.strip().replace(' ', '_').lower()}"
        created_at = datetime.now(UTC).isoformat()
        class_name = ''.join(part.capitalize() for part in skill_name.split('_'))
        code = (
            f"class {class_name}:\n"
            f"    def __init__(self): ...\n"
            f"    def run(self, payload: dict) -> dict:\n"
            f"        return {{'skill': '{skill_name}', 'status': 'ok'}}\n"
        )
        try:
            from database.supabase_client import db

            if db.client:
                db.insert_skill_proposal(
                    skill_name,
                    pattern,
                    code,
                    "proposed",
                    created_at,
                )
        except Exception as e:  # noqa: BLE001
            logger.warning(f"Failed to insert skill proposal to Supabase: {e}")
            if evolution_write_failures:
                evolution_write_failures.inc()

        conn = sqlite3.connect(str(self.db_path))
        try:
            conn.execute(
                "INSERT INTO skill_proposals (skill_name, source_pattern, generated_code, status, created_at) VALUES (?, ?, ?, ?, ?)",
                (skill_name, pattern, code, "proposed", created_at),
            )
            conn.commit()
            return {
                "skill_name": skill_name,
                "source_pattern": pattern,
                "status": "proposed",
                "generated_code": code,
                "generated_at": created_at,
            }
        finally:
            conn.close()

    def record_feedback(
        self, session_id: str, query: str, retrieved_chunks: str, user_rating: float
    ) -> dict[str, Any]:
        created_at = datetime.now(UTC).isoformat()
        try:
            from database.supabase_client import db

            if db.client:
                db.insert_feedback(
                    session_id,
                    query,
                    retrieved_chunks,
                    user_rating,
                    created_at,
                )
        except Exception as e:  # noqa: BLE001
            logger.warning(f"Failed to insert feedback to Supabase: {e}")
            if evolution_write_failures:
                evolution_write_failures.inc()

        conn = sqlite3.connect(str(self.db_path))
        try:
            conn.execute(
                "INSERT INTO feedback_loop (session_id, query, retrieved_chunks, user_rating, created_at) VALUES (?, ?, ?, ?, ?)",
                (session_id, query, retrieved_chunks, user_rating, created_at),
            )
            conn.commit()
            return {"recorded": True, "session_id": session_id, "rating": user_rating}
        finally:
            conn.close()

    def run_daily_evolution(self, task_history: list[dict[str, Any]]) -> dict[str, Any]:
        total = len(task_history)
        successful = sum(1 for t in task_history if t.get("success"))
        success_rate = (successful / total * 100.0) if total > 0 else 100.0

        # Skill proposal based on repeated failures
        failures = self.detect_repeated_failures()
        failed_tasks = [f["task"] for f in failures]
        new_skills_proposed = []
        for task in failed_tasks:
            proposal = self.propose_new_skill(task)
            new_skills_proposed.append(proposal["skill_name"])

        # Prompt optimization proposals
        underperforming_prompts = self.detect_underperforming_prompts()
        prompt_optimizations_proposed = []
        for prompt_data in underperforming_prompts:
            proposal = self.propose_prompt_optimization(prompt_data["task"], prompt_data)
            if proposal.get("status") == "proposed":
                prompt_optimizations_proposed.append(proposal)

        optimizations = (
            ["Increase RAG context depth to reduce hallucination."]
            if success_rate < 95
            else []
        )

        report = {
            "timestamp": datetime.now(UTC).isoformat(),
            "total_tasks_processed": total,
            "success_rate": success_rate,
            "repeated_failures": len(failures),
            "new_skills_proposed": new_skills_proposed,
            "prompt_optimizations_proposed": len(prompt_optimizations_proposed),
            "optimizations": optimizations,
        }
        try:
            from database.supabase_client import db

            if db.client:
                db.append_evolution_log(report)
        except Exception as e:  # noqa: BLE001
            logger.warning(f"Failed to append evolution log to Supabase: {e}")
            if evolution_write_failures:
                evolution_write_failures.inc()
        return report
