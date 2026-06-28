import datetime
import json
import sqlite3
from dataclasses import dataclass
from dataclasses import field
from typing import Any


try:
    import chromadb

    HAS_VECTOR_DB = True
except ImportError:
    HAS_VECTOR_DB = False


@dataclass
class Experience:
    id: int | None = None
    timestamp: str = ""
    user_id: str = ""
    request: str = ""
    context: dict[str, Any] = field(default_factory=dict)
    action_taken: str = ""
    result: str = "success"  # "success", "partial", "failure"
    error_message: str | None = None
    user_feedback: str | None = None  # "great", "needs work", "failed"
    generated_code: str | None = None
    deployment_logs: str | None = None
    what_worked: list[str] = field(default_factory=list)
    what_failed: list[str] = field(default_factory=list)
    suggested_improvements: list[str] = field(default_factory=list)


class ExperienceDatabase:
    def __init__(self, db_path: str = "data/experience.db"):
        import os

        os.makedirs(os.path.dirname(db_path), exist_ok=True)
        self.db_path = db_path
        self._init_db()
        self.vector_collection = None
        if HAS_VECTOR_DB:
            try:
                self.chroma_client = chromadb.Client()
                self.vector_collection = self.chroma_client.get_or_create_collection(
                    "experience"
                )
            except Exception as exc:
                logger = __import__("loguru").logger
                logger.debug(f"ChromaDB init failed: {exc}")

    def _init_db(self):
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                CREATE TABLE IF NOT EXISTS experiences (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT,
                    user_id TEXT,
                    request TEXT,
                    context TEXT,
                    action_taken TEXT,
                    result TEXT,
                    error_message TEXT,
                    user_feedback TEXT,
                    generated_code TEXT,
                    deployment_logs TEXT,
                    what_worked TEXT,
                    what_failed TEXT,
                    suggested_improvements TEXT
                )
            """
            )
            conn.commit()

    def record_experience(self, exp: Experience) -> int:
        timestamp = (
            exp.timestamp or datetime.datetime.now(datetime.timezone.utc).isoformat()
        )
        context_json = json.dumps(exp.context or {})
        what_worked_json = json.dumps(exp.what_worked or [])
        what_failed_json = json.dumps(exp.what_failed or [])
        suggested_json = json.dumps(exp.suggested_improvements or [])

        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                INSERT INTO experiences (
                    timestamp, user_id, request, context, action_taken, result,
                    error_message, user_feedback, generated_code, deployment_logs,
                    what_worked, what_failed, suggested_improvements
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                (
                    timestamp,
                    exp.user_id,
                    exp.request,
                    context_json,
                    exp.action_taken,
                    exp.result,
                    exp.error_message,
                    exp.user_feedback,
                    exp.generated_code,
                    exp.deployment_logs,
                    what_worked_json,
                    what_failed_json,
                    suggested_json,
                ),
            )
            conn.commit()
            return int(cursor.lastrowid or 0)

    def get_experiences(self, limit: int = 50) -> list[Experience]:
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.cursor()
            cursor.execute(
                "SELECT * FROM experiences ORDER BY id DESC LIMIT ?", (limit,)
            )
            rows = cursor.fetchall()

            experiences = []
            for r in rows:
                experiences.append(
                    Experience(
                        id=r["id"],
                        timestamp=r["timestamp"],
                        user_id=r["user_id"],
                        request=r["request"],
                        context=json.loads(r["context"] or "{}"),
                        action_taken=r["action_taken"],
                        result=r["result"],
                        error_message=r["error_message"],
                        user_feedback=r["user_feedback"],
                        generated_code=r["generated_code"],
                        deployment_logs=r["deployment_logs"],
                        what_worked=json.loads(r["what_worked"] or "[]"),
                        what_failed=json.loads(r["what_failed"] or "[]"),
                        suggested_improvements=json.loads(
                            r["suggested_improvements"] or "[]"
                        ),
                    )
                )
            return experiences
