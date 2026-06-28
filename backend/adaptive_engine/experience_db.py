import importlib.util
import json
import sqlite3
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

HAS_SENTENCE_TRANSFORMERS = importlib.util.find_spec("sentence_transformers") is not None
HAS_CHROMADB = importlib.util.find_spec("chromadb") is not None
HAS_QDRANT = importlib.util.find_spec("qdrant_client") is not None


@dataclass
class Experience:
    id: int | None = None
    timestamp: str = ""
    user_id: str = ""
    request: str = ""
    context: dict[str, Any] = field(default_factory=dict)
    action_taken: str = ""
    result: str = "success"
    error_message: str | None = None
    user_feedback: str | None = None
    generated_code: str | None = None
    deployment_logs: str | None = None
    what_worked: list[str] = field(default_factory=list)
    what_failed: list[str] = field(default_factory=list)
    suggested_improvements: list[str] = field(default_factory=list)


class ExperienceDatabase:
    def __init__(self, db_path: str = "data/experience.db"):
        self.db_path = Path(db_path)
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self._init_db()
        self.encoder = None
        self.chroma_collection = None
        self.qdrant_client = None
        self.qdrant_collection = "experience"
        if HAS_SENTENCE_TRANSFORMERS:
            try:
                from sentence_transformers import SentenceTransformer
                self.encoder = SentenceTransformer("all-MiniLM-L6-v2")
            except Exception as exc:
                import loguru
                loguru.logger.debug(f"SentenceTransformer init failed: {exc}")
        if HAS_CHROMADB:
            try:
                import chromadb
                self.chroma_collection = chromadb.EphemeralClient().get_or_create_collection("experience")
            except Exception as exc:
                import loguru
                loguru.logger.debug(f"ChromaDB init failed: {exc}")
        if HAS_QDRANT:
            try:
                from qdrant_client import QdrantClient
                self.qdrant_client = QdrantClient(":memory:")
                from qdrant_client.models import Distance, VectorParams
                self.qdrant_client.recreate_collection(
                    collection_name=self.qdrant_collection,
                    vectors_config=VectorParams(size=384, distance=Distance.COSINE),
                )
            except Exception as exc:
                import loguru

                loguru.logger.debug(f"Qdrant init failed: {exc}")

    def _init_db(self) -> None:
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
                    suggested_improvements TEXT,
                    embedding BLOB
                )
                """
            )
            conn.commit()

    def _embed(self, text: str) -> list[float] | None:
        if self.encoder:
            try:
                return self.encoder.encode(text).tolist()
            except Exception:
                return None
        return None

    def record_experience(self, exp: Experience) -> int:
        timestamp = exp.timestamp or __import__("datetime").datetime.now(__import__("datetime").timezone.utc).isoformat()
        request_text = exp.request or ""
        embedding = self._embed(request_text)
        embedding_blob = json.dumps(embedding).encode() if embedding else None
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                INSERT INTO experiences (
                    timestamp, user_id, request, context, action_taken, result,
                    error_message, user_feedback, generated_code, deployment_logs,
                    what_worked, what_failed, suggested_improvements, embedding
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    timestamp,
                    exp.user_id,
                    request_text,
                    json.dumps(exp.context or {}),
                    exp.action_taken,
                    exp.result,
                    exp.error_message,
                    exp.user_feedback,
                    exp.generated_code,
                    exp.deployment_logs,
                    json.dumps(exp.what_worked or []),
                    json.dumps(exp.what_failed or []),
                    json.dumps(exp.suggested_improvements or []),
                    embedding_blob,
                ),
            )
            conn.commit()
            exp_id = int(cursor.lastrowid or 0)
        if embedding:
            self._upsert_vector_db(exp_id, request_text, embedding, exp.result)
        return exp_id

    def _upsert_vector_db(self, exp_id: int, text: str, embedding: list[float], result: str) -> None:
        try:
            if self.chroma_collection:
                self.chroma_collection.upsert(
                    ids=[str(exp_id)],
                    embeddings=[embedding],
                    metadatas=[{"result": result}],
                    documents=[text],
                )
        except Exception:
            pass
        try:
            if self.qdrant_client:
                from qdrant_client.models import PointStruct
                self.qdrant_client.upsert(
                    collection_name=self.qdrant_collection,
                    points=[PointStruct(id=exp_id, vector=embedding, payload={"result": result, "text": text})],
                )
        except Exception:
            pass

    def _cosine_similarity(self, a: list[float], b: list[float]) -> float:
        import math
        dot = sum(x * y for x, y in zip(a, b))
        norm_a = math.sqrt(sum(x * x for x in a))
        norm_b = math.sqrt(sum(y * y for y in b))
        if not norm_a or not norm_b:
            return 0.0
        return dot / (norm_a * norm_b)

    def find_similar(self, query: str, limit: int = 5, threshold: float = 0.7) -> list[dict[str, Any]]:
        embedding = self._embed(query)
        if not embedding:
            return []
        hits: list[dict[str, Any]] = []
        try:
            if self.chroma_collection:
                res = self.chroma_collection.query(query_embeddings=[embedding], n_results=limit)
                ids = res.get("ids", [[]])[0]
                metadatas = res.get("metadatas", [[]])[0]
                distances = res.get("distances", [[]])[0]
                for idx, meta, dist in zip(ids, metadatas, distances, strict=True):
                    score = 1 - dist
                    if score >= threshold:
                        hits.append({"source": "chroma", "id": idx, "score": score, "meta": meta})
        except Exception:
            pass
        return hits

    def get_experiences(self, limit: int = 50) -> list[Experience]:
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM experiences ORDER BY id DESC LIMIT ?", (limit,))
            rows = cursor.fetchall()
            return [
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
                    suggested_improvements=json.loads(r["suggested_improvements"] or "[]"),
                )
                for r in rows
            ]
