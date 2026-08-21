# backend/learning/experience.py
"""Experience Ledger and Record Primitives for Continual Learning."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
import json
import logging
import os
from pathlib import Path
from typing import Any, Dict, List, Optional
import uuid

logger = logging.getLogger("supremeai.learning.experience")


@dataclass
class ExperienceRecord:
    """Immutable record of an executed task used for outcome analysis and learning."""

    task_id: str
    goal: str
    experience_id: str = field(default_factory=lambda: f"exp_{uuid.uuid4().hex[:10]}")
    plan_steps_count: int = 0
    tools_used: List[str] = field(default_factory=list)
    providers_used: List[str] = field(default_factory=list)
    verified: bool = False
    verification_score: float = 0.0
    cost_usd: float = 0.0
    latency_ms: float = 0.0
    failures: List[str] = field(default_factory=list)
    user_feedback: Optional[str] = None
    lessons_extracted: List[str] = field(default_factory=list)
    timestamp: str = field(default_factory=lambda: datetime.now().isoformat())

    def to_dict(self) -> Dict[str, Any]:
        return {
            "experience_id": self.experience_id,
            "task_id": self.task_id,
            "goal": self.goal,
            "plan_steps_count": self.plan_steps_count,
            "tools_used": self.tools_used,
            "providers_used": self.providers_used,
            "verified": self.verified,
            "verification_score": self.verification_score,
            "cost_usd": self.cost_usd,
            "latency_ms": self.latency_ms,
            "failures": self.failures,
            "user_feedback": self.user_feedback,
            "lessons_extracted": self.lessons_extracted,
            "timestamp": self.timestamp,
        }


class ExperienceStore:
    """In-memory and file-persisted Experience Ledger."""

    def __init__(self, storage_dir: Optional[str] = None) -> None:
        self.storage_dir = Path(storage_dir or os.path.expanduser("~/.supremeai/experience"))
        self.storage_dir.mkdir(parents=True, exist_ok=True)
        self.records: List[ExperienceRecord] = []

    def record(self, experience: ExperienceRecord) -> None:
        self.records.append(experience)
        try:
            filepath = self.storage_dir / f"{experience.experience_id}.json"
            with open(filepath, "w", encoding="utf-8") as fh:
                json.dump(experience.to_dict(), fh, indent=2)
        except Exception as exc:
            logger.warning(f"Could not persist experience {experience.experience_id}: {exc}")

    def get_recent(self, limit: int = 50) -> List[ExperienceRecord]:
        return self.records[-limit:]


# Global Singleton
_experience_store: Optional[ExperienceStore] = None


def get_experience_store() -> ExperienceStore:
    global _experience_store
    if _experience_store is None:
        _experience_store = ExperienceStore()
    return _experience_store
