import json
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
    """Persists task execution state in Google Cloud Firestore (Serverless & Stateful)."""
    def __init__(self, db_path: str = None):
        self.collection_name = "checkpoints"
        self._db = None
        if firestore:
            try:
                # If running on Cloud Run, it automatically picks up the default service account.
                self._db = firestore.AsyncClient() if hasattr(firestore, 'AsyncClient') else firestore.Client()
                logger.info("Initialized Firestore CheckpointManager")
            except Exception as e:
                logger.warning(f"Failed to initialize Firestore: {e}. Checkpoints will be disabled.")
        else:
            logger.warning("google-cloud-firestore not installed. CheckpointManager is disabled.")

    def save(self, task_id: str, step_index: int, state: Dict[str, Any]) -> bool:
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
        if not self._db: return False
        try:
            self._db.collection(self.collection_name).document(task_id).delete()
            return True
        except Exception as exc:
            logger.error(f"Failed to clear Firestore checkpoint: {exc}")
            return False
