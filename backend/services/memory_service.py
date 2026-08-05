import ast
import importlib.util
import json
import math
import os
import sqlite3
from typing import Any

from loguru import logger

from core.persistence import pooled_pg

# বাংলা মন্তব্য: রেন্ডার ফ্রি টায়ারে মেমোরি সংকট এড়াতে LOW_MEMORY_MODE চেক করা হচ্ছে
LOW_MEMORY_MODE = os.getenv("LOW_MEMORY_MODE", "false").lower() == "true"
HAS_SENTENCE_TRANSFORMERS = (not LOW_MEMORY_MODE) and importlib.util.find_spec("sentence_transformers") is not None


def hash_vectorize(text: str, size: int = 384) -> list[float]:
    """
    Pure Python Feature Hashing (Hashing Trick) to convert text into a 384-dimensional vector.
    Serves as a robust, zero-cost fallback when SentenceTransformer is unavailable.
    """
    vector = [0.0] * size
    words = [w.lower() for w in text.split() if len(w) > 1]
    if not words:
        # Return a non-empty unit vector to prevent division by zero
        vector[0] = 1.0
        return vector

    for word in words:
        # Generate stable hash key using fnv1a style simple hashing
        h = abs(hash(word)) % size
        sign = 1 if (abs(hash(word)) // size) % 2 == 0 else -1
        vector[h] += sign

    # L2 Normalization
    norm = math.sqrt(sum(x * x for x in vector))
    if norm > 0:
        vector = [x / norm for x in vector]
    return vector


_PG_SCHEMA = """
    CREATE TABLE IF NOT EXISTS file_memories (
        id SERIAL PRIMARY KEY,
        file_path TEXT UNIQUE,
        content TEXT,
        summary TEXT,
        structure TEXT,
        embedding TEXT
    )
"""


class CascadeMemoryService:
    """
    Handles context memory ("Summary of Functions" / "File Structure") operations for SupremeAI.
    Persists to pooled Postgres by default (durable across restarts). Pass an explicit `db_path`
    (or omit Postgres config) to force the local SQLite fallback — used by the __main__ self-test
    below so it never touches the live memory store.
    """

    def __init__(self, db_path: str | None = None):
        self._use_pg = db_path is None and pooled_pg.is_available()
        if self._use_pg:
            try:
                pooled_pg.execute(_PG_SCHEMA)
                self.db_path = None
                logger.info("CascadeMemoryService: using pooled Postgres backend.")
            except Exception as exc:
                logger.error(f"CascadeMemoryService: Postgres schema init failed, falling back to SQLite: {exc}")
                self._use_pg = False

        if not self._use_pg:
            self.db_path = db_path or "data/memory.db"
            os.makedirs(os.path.dirname(self.db_path), exist_ok=True)
            self._init_db()
            logger.warning(
                f"CascadeMemoryService: running on local SQLite fallback at {self.db_path} — NOT durable across restarts."
            )
        self.encoder = None

        if HAS_SENTENCE_TRANSFORMERS:
            try:
                from sentence_transformers import SentenceTransformer

                self.encoder = SentenceTransformer("all-MiniLM-L6-v2")
                logger.info("Initialized SentenceTransformer encoder for memory service")
            except Exception as e:
                logger.warning(f"Failed to load SentenceTransformer: {e}. Using hash fallback.")

    def _init_db(self) -> None:
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS file_memories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    file_path TEXT UNIQUE,
                    content TEXT,
                    summary TEXT,
                    structure TEXT,
                    embedding TEXT
                )
                """)
            conn.commit()

    def _embed(self, text: str) -> list[float]:
        if self.encoder:
            try:
                return self.encoder.encode(text).tolist()
            except Exception as e:
                logger.warning(f"Embedding failed: {e}. Falling back to hash vectorizer.")
        return hash_vectorize(text)

    def _parse_code_structure(self, file_path: str, content: str) -> dict[str, Any]:
        """
        Parses Python file AST structure and extracts function/class names and docstrings.
        """
        if not file_path.endswith(".py"):
            # Simple line-based fallback for non-python files
            lines = content.splitlines()
            summary = f"File: {file_path}\nLines: {len(lines)}"
            return {"summary": summary, "structure": json.dumps({"lines": len(lines)})}

        try:
            tree = ast.parse(content)
            summary_parts = [f"File: {file_path}"]
            structure: dict[str, list[Any]] = {"classes": [], "functions": []}

            for node in ast.iter_child_nodes(tree):
                if isinstance(node, ast.ClassDef):
                    # mypy-এর জন্য class_info-এ explicit type দেওয়া হচ্ছে যাতে .append() কাজ করে
                    class_info: dict[str, Any] = {
                        "name": node.name,
                        "methods": [],
                        "docstring": ast.get_docstring(node) or "",
                    }
                    summary_parts.append(f"Class: {node.name}")
                    if class_info["docstring"]:
                        summary_parts.append(f"  Docstring: {class_info['docstring']}")

                    for subnode in node.body:
                        if isinstance(subnode, ast.FunctionDef):
                            method_info = {
                                "name": subnode.name,
                                "docstring": ast.get_docstring(subnode) or "",
                            }
                            class_info["methods"].append(method_info)
                            summary_parts.append(f"  Method: {subnode.name}")
                            if method_info["docstring"]:
                                summary_parts.append(f"    Docstring: {method_info['docstring']}")
                    structure["classes"].append(class_info)

                elif isinstance(node, ast.FunctionDef):
                    func_info = {
                        "name": node.name,
                        "docstring": ast.get_docstring(node) or "",
                    }
                    summary_parts.append(f"Function: {node.name}")
                    if func_info["docstring"]:
                        summary_parts.append(f"  Docstring: {func_info['docstring']}")
                    structure["functions"].append(func_info)

            return {
                "summary": "\n".join(summary_parts),
                "structure": json.dumps(structure),
            }
        except Exception as e:
            logger.warning(f"AST parsing failed for {file_path}: {e}")
            return {
                "summary": f"File: {file_path} (AST parsing error)",
                "structure": json.dumps({"error": str(e)}),
            }

    def store_memory(self, file_path: str, content: str, summary: str, structure: str) -> None:
        """Stores or updates a memory entry in the database.

        বাংলা মন্তব্য: ডেটাবেসে মেমোরি এন্ট্রি স্টোর বা আপডেট করার কোর মেথড।
        """
        embedding = self._embed(summary)
        embedding_str = json.dumps(embedding)

        if self._use_pg:
            try:
                pooled_pg.execute(
                    """
                    INSERT INTO file_memories (file_path, content, summary, structure, embedding)
                    VALUES (%s, %s, %s, %s, %s)
                    ON CONFLICT (file_path) DO UPDATE SET
                        content = EXCLUDED.content,
                        summary = EXCLUDED.summary,
                        structure = EXCLUDED.structure,
                        embedding = EXCLUDED.embedding
                    """,
                    (file_path, content, summary, structure, embedding_str),
                )
            except Exception as exc:
                logger.error(f"CascadeMemoryService.store_memory: Postgres write failed: {exc}")
            return

        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                INSERT INTO file_memories (file_path, content, summary, structure, embedding)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(file_path) DO UPDATE SET
                    content=excluded.content,
                    summary=excluded.summary,
                    structure=excluded.structure,
                    embedding=excluded.embedding
                """,
                (file_path, content, summary, structure, embedding_str),
            )
            conn.commit()

    def retrieve_memories(self) -> list[dict[str, Any]]:
        """Retrieves all memory entries from the database.

        বাংলা মন্তব্য: ডেটাবেসে থাকা সকল মেমোরি এন্ট্রি রিট্রিভ করার কোর মেথড।
        """
        results = []
        if self._use_pg:
            try:
                rows = pooled_pg.query_dicts("SELECT file_path, content, summary, structure FROM file_memories")
            except Exception as exc:
                logger.error(f"CascadeMemoryService.retrieve_memories: Postgres read failed: {exc}")
                rows = []
            for row in rows:
                results.append(
                    {
                        "file_path": row["file_path"],
                        "content": row["content"],
                        "summary": row["summary"],
                        "structure": row["structure"],
                    }
                )
            return results

        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.cursor()
            cursor.execute("SELECT file_path, content, summary, structure FROM file_memories")
            rows = cursor.fetchall()
            for row in rows:
                results.append(
                    {
                        "file_path": row["file_path"],
                        "content": row["content"],
                        "summary": row["summary"],
                        "structure": row["structure"],
                    }
                )
        return results

    def delete_memory(self, file_path: str) -> None:
        """Deletes a memory entry from the database by its file path.

        বাংলা মন্তব্য: ফাইল পাথ দিয়ে ডেটাবেস থেকে কোনো নির্দিষ্ট মেমোরি এন্ট্রি মুছে ফেলে।
        """
        if self._use_pg:
            try:
                pooled_pg.execute("DELETE FROM file_memories WHERE file_path = %s", (file_path,))
            except Exception as exc:
                logger.error(f"CascadeMemoryService.delete_memory: Postgres delete failed: {exc}")
            return

        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute("DELETE FROM file_memories WHERE file_path = ?", (file_path,))
            conn.commit()

    def chunk_and_embed(self, file_path: str, content: str) -> list[dict[str, Any]]:
        """
        Parses raw code, extracts function summaries and structure,
        generates vector embeddings, and saves them to the local SQLite database.
        """
        logger.info(f"Extracting summary and embedding for {file_path}")
        parsed_data = self._parse_code_structure(file_path, content)
        summary = parsed_data["summary"]
        structure = parsed_data["structure"]

        # Generate embedding for the structural summary
        embedding = self._embed(summary)

        # Save memory
        self.store_memory(file_path, content, summary, structure)

        return [{"file": file_path, "summary": summary, "vector": embedding}]

    def _cosine_similarity(self, a: list[float], b: list[float]) -> float:
        dot = sum(x * y for x, y in zip(a, b, strict=False))
        norm_a = math.sqrt(sum(x * x for x in a))
        norm_b = math.sqrt(sum(y * y for y in b))
        if not norm_a or not norm_b:
            return 0.0
        return dot / (norm_b * norm_a)

    def query_context(self, prompt: str, top_k: int = 5) -> list[dict[str, Any]]:
        """
        Takes the user's prompt, embeds it, and queries local SQLite for the top_k
        most relevant structural contexts using cosine similarity.
        """
        logger.info(f"Querying context for prompt: {prompt[:30]}...")
        query_vector = self._embed(prompt)

        results = []

        if self._use_pg:
            try:
                rows = pooled_pg.query_dicts("SELECT file_path, summary, structure, embedding FROM file_memories")
            except Exception as exc:
                logger.error(f"CascadeMemoryService.query_context: Postgres read failed: {exc}")
                rows = []
            for row in rows:
                try:
                    stored_vector = json.loads(row["embedding"])
                    score = self._cosine_similarity(query_vector, stored_vector)
                    results.append(
                        {
                            "file": row["file_path"],
                            "summary": row["summary"],
                            "structure": json.loads(row["structure"]),
                            "score": score,
                        }
                    )
                except Exception as e:
                    logger.warning(f"Error calculating similarity for {row.get('file_path')}: {e}")
            results.sort(key=lambda x: x["score"], reverse=True)
            return results[:top_k]

        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.cursor()
            cursor.execute("SELECT file_path, summary, structure, embedding FROM file_memories")
            rows = cursor.fetchall()

            for row in rows:
                try:
                    stored_vector = json.loads(row["embedding"])
                    score = self._cosine_similarity(query_vector, stored_vector)
                    results.append(
                        {
                            "file": row["file_path"],
                            "summary": row["summary"],
                            "structure": json.loads(row["structure"]),
                            "score": score,
                        }
                    )
                except Exception as e:
                    logger.warning(f"Error calculating similarity for {row['file_path']}: {e}")

        # Sort by similarity score descending
        results.sort(key=lambda x: x["score"], reverse=True)
        return results[:top_k]

    # Backward-compatible aliases for test compatibility
    def store(self, user_id: str, agent_id: str, content: str, metadata: dict[str, Any]) -> None:
        """Backward-compatible alias for store_memory."""
        summary = metadata.get("summary", content[:200])
        structure = metadata.get("structure", json.dumps({}))
        self.store_memory(f"{user_id}/{agent_id}", content, summary, structure)

    def get_memories(self, user_id: str) -> list[dict[str, Any]]:
        """Backward-compatible alias for retrieve_memories."""
        return self.retrieve_memories()

    def search_memories(self, user_id: str, query: str) -> list[dict[str, Any]]:
        """Backward-compatible alias for query_context."""
        return self.query_context(query)

    def delete(self, memory_id: str) -> None:
        """Backward-compatible alias for delete_memory."""
        self.delete_memory(memory_id)

    def clear_user_memories(self, user_id: str) -> None:
        """Backward-compatible alias to clear all memories."""
        if not self._use_pg:
            with sqlite3.connect(self.db_path) as conn:
                conn.execute("DELETE FROM file_memories")
                conn.commit()

    def get_context_window(self, user_id: str, agent_id: str, limit: int) -> list[dict[str, Any]]:
        """Backward-compatible alias for query_context."""
        return self.query_context(f"{user_id} {agent_id}", top_k=limit)

    def update_context_window(self, user_id: str, messages: list[dict[str, Any]]) -> None:
        """Backward-compatible method to store messages in context window."""
        for msg in messages:
            content = msg.get("content", str(msg))
            self.store_memory(f"{user_id}/context", content, content, json.dumps({"role": msg.get("role", "user")}))

    def semantic_search(self, query: str, limit: int = 5) -> list[dict[str, Any]]:
        """Backward-compatible alias for query_context."""
        return self.query_context(query, top_k=limit)

    def get_recent_interactions(self, user_id: str, limit: int = 20) -> list[dict[str, Any]]:
        """Backward-compatible method to get recent interactions."""
        return self.retrieve_memories()[:limit]


# Global instance
memory_service = CascadeMemoryService()

# Test Execution (If run directly)
if __name__ == "__main__":
    import tempfile

    # Run audit/test with temporary DB to verify functionality without corrupting live DB
    temp_db = tempfile.mktemp(suffix=".db")
    test_service = CascadeMemoryService(db_path=temp_db)

    test_code = """
class DataAnalyzer:
    \"\"\"Analyzes numerical datasets.\"\"\"
    def __init__(self, data):
        self.data = data

    def run_analysis(self):
        \"\"\"Runs complex calculations on data.\"\"\"
        return sum(self.data)

def helper_utils():
    \"\"\"Helper logic.\"\"\"
    return True
"""
    # 1. Test indexing
    indexed = test_service.chunk_and_embed("test_file.py", test_code)
    # বাংলা মন্তব্য: Ruff T201 print এরর এড়াতে logger.info ব্যবহার করা হলো।
    logger.info(f"Indexed output: {indexed}")

    # 2. Test semantic search query
    matches = test_service.query_context("Need a class to calculate and analyze data", top_k=1)
    logger.info(f"Semantic search match: {matches}")

    # Clean up temp file
    try:
        if os.path.exists(temp_db):
            os.remove(temp_db)
    except Exception as e:
        logger.debug(f"Temporary DB cleanup skipped: {e}")
