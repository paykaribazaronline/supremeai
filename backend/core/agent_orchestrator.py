import os
import re
from typing import Optional, Dict, Any
from pydantic import BaseModel
from loguru import logger
import time

MAX_AGENT_TOKENS = int(os.getenv("MAX_AGENT_TOKENS", "5000"))
MAX_AGENT_ITERATIONS = int(os.getenv("MAX_AGENT_ITERATIONS", "5"))
ADMIN_PERMISSIONS_REQUIRED = os.getenv("AGENT_ADMIN_PERMISSIONS_REQUIRED", "true").lower() == "true"

TIER_KEYWORDS = {
    1: [
        "code", "function", "class", "debug", "refactor", "algorithm", "python", "javascript",
        "typescript", "react", "analyze", "logic", "reason", "math", "calculate", "prove", "optimize",
        "agent", "swarm", "workflow", "autonomous", "build", "create", "implement"
    ],
    2: [
        "search", "find", "research", "lookup", "query", "summarize", "translate", "sentiment"
    ],
    3: [
        "image", "photo", "picture", "visual", "ocr", "chart", "graph", "diagram"
    ],
}

def _normalize(text: str) -> str:
    return re.sub(r'[^a-z0-9\s]', '', text.lower()).strip()

def _matches_any(prompt_lower: str, keywords: list[str]) -> bool:
    norm = _normalize(prompt_lower)
    return any(kw.lower() in norm for kw in keywords)

def route_request(prompt: str, task_type: str = "general") -> "SmartSemanticRouter":
    upper_task = (task_type or "general").upper()
    prompt_lower = prompt.lower()

    if upper_task in ("CODE", "CODING", "REASONING", "MATH"):
        intent = "coding" if "CODE" in upper_task else "reasoning"
        return SmartSemanticRouter(intent=intent, requires_expensive=True, tier=1, reasoning=f"Explicit task_type={task_type}")

    if "VISION" in upper_task or any(ext in prompt_lower for ext in [".png", ".jpg", ".jpeg", ".pdf"]):
        return SmartSemanticRouter(intent="vision", requires_expensive=True, tier=3, reasoning="Vision file or task_type detected")

    if _matches_any(prompt_lower, TIER_KEYWORDS[1]):
        intent = "coding" if _matches_any(prompt_lower, TIER_KEYWORDS[1][:10]) else "reasoning"
        return SmartSemanticRouter(intent=intent, requires_expensive=True, tier=1, reasoning="Keyword classification tier-1")

    if _matches_any(prompt_lower, TIER_KEYWORDS[2]) or upper_task in ("TRANSLATION", "SENTIMENT", "SUMMARIES", "RAG", "SEARCH"):
        return SmartSemanticRouter(intent="search", requires_expensive=False, tier=2, reasoning="Keyword classification tier-2")

    if _matches_any(prompt_lower, TIER_KEYWORDS[3]) or upper_task in ("IMAGE", "VISION", "OCR"):
        return SmartSemanticRouter(intent="vision", requires_expensive=True, tier=3, reasoning="Keyword classification tier-3")

    return SmartSemanticRouter(intent="general", requires_expensive=False, tier=5, reasoning="Default fallback tier-5")

class AgentCircuitBreaker:
    def __init__(self, agent_name: str):
        self.agent_name = agent_name
        self.max_tokens = MAX_AGENT_TOKENS
        self.max_iterations = MAX_AGENT_ITERATIONS
        self._iteration_count = 0
        self._token_count = 0
        self._locked = False
        self._lock_reason: Optional[str] = None
    
    def increment_iteration(self) -> bool:
        self._iteration_count += 1
        if self._iteration_count > self.max_iterations:
            self._locked = True
            self._lock_reason = f"Max iterations ({self.max_iterations}) exceeded"
            return False
        return True
    
    def add_tokens(self, count: int) -> bool:
        self._token_count += count
        if self._token_count > self.max_tokens:
            self._locked = True
            self._lock_reason = f"Max tokens ({self.max_tokens}) exceeded"
            return False
        return True
    
    def check_limits(self, tokens: int = 0, iterations: int = 0) -> Dict[str, Any]:
        if self._locked:
            return {"blocked": True, "reason": self._lock_reason}
        return {"blocked": False}
    
    def reset(self) -> None:
        self._iteration_count = 0
        self._token_count = 0
        self._locked = False
        self._lock_reason = None
    
    def get_status(self) -> Dict[str, Any]:
        return {
            "agent_name": self.agent_name,
            "iterations_used": self._iteration_count,
            "tokens_used": self._token_count,
            "max_iterations": self.max_iterations,
            "max_tokens": self.max_tokens,
            "locked": self._locked,
            "lock_reason": self._lock_reason,
        }

class SmartSemanticRouter(BaseModel):
    intent: str = "general"
    requires_expensive: bool = False
    tier: int = 5
    reasoning: str = ""

class AsyncTaskManager:
    def __init__(self):
        self._tasks: Dict[str, Dict[str, Any]] = {}
    
    def create_task(self, task_type: str, payload: Dict[str, Any]) -> str:
        import uuid
        task_id = str(uuid.uuid4())[:12]
        
        self._tasks[task_id] = {
            "id": task_id,
            "type": task_type,
            "payload": payload,
            "status": "pending",
            "created_at": time.time(),
            "progress": 0,
        }
        
        self._enqueue_task(task_id, task_type, payload)
        
        return task_id
    
    def _enqueue_task(self, task_id: str, task_type: str, payload: Dict) -> None:
        celery_url = os.getenv("CELERY_BROKER_URL", "")
        if celery_url:
            try:
                import httpx
                httpx.post(
                    f"{celery_url}/enqueue",
                    json={"task_id": task_id, "type": task_type, "payload": payload},
                    timeout=2.0,
                )
            except Exception as e:
                logger.debug(f"Celery enqueue failed: {e}")
        else:
            self._simulate_task(task_id, task_type)
    
    def _simulate_task(self, task_id: str, task_type: str) -> None:
        if task_type in ["video_generation", "image_generation", "long_running"]:
            self._tasks[task_id]["status"] = "processing"
            self._tasks[task_id]["progress"] = 50
    
    def get_task(self, task_id: str) -> Optional[Dict[str, Any]]:
        task = self._tasks.get(task_id)
        if task:
            return {
                "task_id": task["id"],
                "type": task["type"],
                "status": task["status"],
                "progress": task["progress"],
                "created_at": task["created_at"],
            }
        return None
    
    def get_stats(self) -> Dict[str, Any]:
        statuses = {"pending": 0, "processing": 0, "completed": 0, "failed": 0}
        for task in self._tasks.values():
            statuses[task.get("status", "failed")] = statuses.get(task.get("status"), 0) + 1
        
        return {
            "total_tasks": len(self._tasks),
            "by_status": statuses,
        }

async_task_manager = AsyncTaskManager()