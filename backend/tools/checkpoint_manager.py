import json
import sqlite3
from typing import Dict, Any, List, Optional
from datetime import datetime, timezone
from dataclasses import dataclass
from loguru import logger

try:
    from google.cloud import firestore
except ImportError:
    firestore = None

@dataclass
class Checkpoint:
    task_id: str
    step_index: int
    state: Dict[str, Any]
    created_at: str
    resumed: bool = False

class CheckpointManager:
    """Persists task execution state in SQLite (local) or Google Cloud Firestore (Serverless & Stateful)."""
    def __init__(self, db_path: str = None):
        self.collection_name = "checkpoints"
        self._db = None
        self.db_path = db_path
        
        import os
        import sys
        is_test = "pytest" in sys.modules or os.getenv("ENV") == "test"
        
        if db_path or is_test:
            self.mode = "sqlite"
            self.db_path = db_path or "checkpoints.db"
            self._init_sqlite()
            logger.info(f"Initialized SQLite CheckpointManager at {self.db_path}")
        elif firestore:
            try:
                self.mode = "firestore"
                # Use sync Client since all methods are synchronous
                self._db = firestore.Client()
                logger.info("Initialized Firestore CheckpointManager")
            except Exception as e:
                logger.warning(f"Failed to initialize Firestore: {e}. Falling back to SQLite.")
                self.mode = "sqlite"
                self.db_path = "checkpoints.db"
                self._init_sqlite()
        else:
            self.mode = "sqlite"
            self.db_path = "checkpoints.db"
            self._init_sqlite()
            logger.info(f"Initialized SQLite CheckpointManager at {self.db_path}")

    def _init_sqlite(self):
        conn = sqlite3.connect(self.db_path)
        try:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS checkpoints (
                    task_id TEXT PRIMARY KEY,
                    step_index INTEGER,
                    state TEXT,
                    created_at TEXT,
                    resumed INTEGER DEFAULT 0
                )
            """)
            conn.commit()
        finally:
            conn.close()

    def save(self, task_id: str, step_index: int, state: Dict[str, Any]) -> bool:
        if self.mode == "sqlite":
            try:
                conn = sqlite3.connect(self.db_path)
                cursor = conn.cursor()
                cursor.execute("SELECT resumed FROM checkpoints WHERE task_id = ?", (task_id,))
                row = cursor.fetchone()
                resumed = row[0] if row else 0
                
                cursor.execute("""
                    INSERT OR REPLACE INTO checkpoints (task_id, step_index, state, created_at, resumed)
                    VALUES (?, ?, ?, ?, ?)
                """, (task_id, step_index, json.dumps(state), datetime.now(timezone.utc).isoformat(), resumed))
                conn.commit()
                conn.close()
                return True
            except Exception as exc:
                logger.error(f"Failed to save SQLite checkpoint: {exc}")
                return False

        if not self._db: return False
        try:
            doc_ref = self._db.collection(self.collection_name).document(task_id)
            doc = doc_ref.get()
            resumed = doc.to_dict().get("resumed", False) if doc.exists else False
            
            doc_ref.set({
                "task_id": task_id,
                "step_index": step_index,
                "state": json.dumps(state),
                "created_at": datetime.now(timezone.utc).isoformat(),
                "resumed": resumed
            })
            logger.info(f"Firestore checkpoint saved for task_id={task_id} step={step_index}")
            return True
        except Exception as exc:
            logger.error(f"Failed to save Firestore checkpoint: {exc}")
            return False

    def load(self, task_id: str) -> Optional[Checkpoint]:
        if self.mode == "sqlite":
            try:
                conn = sqlite3.connect(self.db_path)
                cursor = conn.cursor()
                cursor.execute("SELECT task_id, step_index, state, created_at, resumed FROM checkpoints WHERE task_id = ?", (task_id,))
                row = cursor.fetchone()
                if not row:
                    conn.close()
                    return None
                
                cp = Checkpoint(
                    task_id=row[0],
                    step_index=row[1],
                    state=json.loads(row[2]),
                    created_at=row[3],
                    resumed=bool(row[4])
                )
                cursor.execute("UPDATE checkpoints SET resumed = 1 WHERE task_id = ?", (task_id,))
                conn.commit()
                conn.close()
                return cp
            except Exception as exc:
                logger.error(f"Failed to load SQLite checkpoint: {exc}")
                return None

        if not self._db: return None
        try:
            doc_ref = self._db.collection(self.collection_name).document(task_id)
            doc = doc_ref.get()
            if not doc.exists:
                return None
                
            data = doc.to_dict()
            cp = Checkpoint(
                task_id=data["task_id"],
                step_index=data["step_index"],
                state=json.loads(data["state"]),
                created_at=data["created_at"],
                resumed=bool(data.get("resumed", False))
            )
            # Mark as resumed
            doc_ref.update({"resumed": True})
            return cp
        except Exception as exc:
            logger.error(f"Failed to load Firestore checkpoint: {exc}")
            return None

    def list_all(self) -> List[Dict[str, Any]]:
        if self.mode == "sqlite":
            try:
                conn = sqlite3.connect(self.db_path)
                cursor = conn.cursor()
                cursor.execute("SELECT task_id, step_index, created_at, resumed FROM checkpoints ORDER BY created_at DESC")
                rows = cursor.fetchall()
                conn.close()
                return [
                    {
                        "task_id": r[0],
                        "step_index": r[1],
                        "created_at": r[2],
                        "resumed": bool(r[3])
                    }
                    for r in rows
                ]
            except Exception as exc:
                logger.error(f"Failed to list SQLite checkpoints: {exc}")
                return []

        if not self._db: return []
        try:
            docs = self._db.collection(self.collection_name).order_by("created_at", direction=firestore.Query.DESCENDING).stream()
            return [
                {
                    "task_id": d.id,
                    "step_index": d.to_dict().get("step_index"),
                    "created_at": d.to_dict().get("created_at"),
                    "resumed": bool(d.to_dict().get("resumed", False))
                }
                for d in docs
            ]
        except Exception as exc:
            logger.error(f"Failed to list Firestore checkpoints: {exc}")
            return []

    def clear(self, task_id: str) -> bool:
        if self.mode == "sqlite":
            try:
                conn = sqlite3.connect(self.db_path)
                cursor = conn.cursor()
                cursor.execute("DELETE FROM checkpoints WHERE task_id = ?", (task_id,))
                conn.commit()
                conn.close()
                return True
            except Exception as exc:
                logger.error(f"Failed to clear SQLite checkpoint: {exc}")
                return False

        if not self._db: return False
        try:
            self._db.collection(self.collection_name).document(task_id).delete()
            return True
        except Exception as exc:
            logger.error(f"Failed to clear Firestore checkpoint: {exc}")
            return False
