import os
import re
import time
from typing import Any

from loguru import logger
from pydantic import BaseModel


MAX_AGENT_TOKENS = int(os.getenv("MAX_AGENT_TOKENS", "5000"))
MAX_AGENT_ITERATIONS = int(os.getenv("MAX_AGENT_ITERATIONS", "5"))
ADMIN_PERMISSIONS_REQUIRED = os.getenv("AGENT_ADMIN_PERMISSIONS_REQUIRED", "true").lower() == "true"

# [Antigravity 2026-06-22] Import free-tier tracker for budget-aware routing
try:
    from core.free_tier_tracker import FREE_PROVIDER_PRIORITY
    from core.free_tier_tracker import get_tracker

    _free_tier_available = True
except ImportError:
    _free_tier_available = False
    logger.warning("[Orchestrator] free_tier_tracker not available — budget-aware routing disabled")

TIER_KEYWORDS = {
    1: [
        "code",
        "function",
        "class",
        "debug",
        "refactor",
        "algorithm",
        "python",
        "javascript",
        "typescript",
        "react",
        "analyze",
        "logic",
        "reason",
        "math",
        "calculate",
        "prove",
        "optimize",
        "agent",
        "swarm",
        "workflow",
        "autonomous",
        "build",
        "create",
        "implement",
    ],
    2: [
        "search",
        "find",
        "research",
        "lookup",
        "query",
        "summarize",
        "translate",
        "sentiment",
    ],
    3: ["image", "photo", "picture", "visual", "ocr", "chart", "graph", "diagram"],
}


def _normalize(text: str) -> str:
    return re.sub(r"[^a-z0-9\s]", "", text.lower()).strip()


def _matches_any(prompt_lower: str, keywords: list[str]) -> bool:
    norm = _normalize(prompt_lower)
    return any(kw.lower() in norm for kw in keywords)


def route_request(prompt: str, task_type: str = "general") -> "SmartSemanticRouter":
    upper_task = (task_type or "general").upper()
    prompt_lower = prompt.lower()

    if upper_task in ("CODE", "CODING", "REASONING", "MATH"):
        intent = "coding" if "CODE" in upper_task else "reasoning"
        return SmartSemanticRouter(
            intent=intent,
            requires_expensive=True,
            tier=1,
            reasoning=f"Explicit task_type={task_type}",
        )

    if "VISION" in upper_task or any(ext in prompt_lower for ext in [".png", ".jpg", ".jpeg", ".pdf"]):
        return SmartSemanticRouter(
            intent="vision",
            requires_expensive=True,
            tier=3,
            reasoning="Vision file or task_type detected",
        )

    if _matches_any(prompt_lower, TIER_KEYWORDS[1]):
        intent = "coding" if _matches_any(prompt_lower, TIER_KEYWORDS[1][:10]) else "reasoning"
        return SmartSemanticRouter(
            intent=intent,
            requires_expensive=True,
            tier=1,
            reasoning="Keyword classification tier-1",
        )

    if _matches_any(prompt_lower, TIER_KEYWORDS[2]) or upper_task in (
        "TRANSLATION",
        "SENTIMENT",
        "SUMMARIES",
        "RAG",
        "SEARCH",
    ):
        return SmartSemanticRouter(
            intent="search",
            requires_expensive=False,
            tier=2,
            reasoning="Keyword classification tier-2",
        )

    if _matches_any(prompt_lower, TIER_KEYWORDS[3]) or upper_task in (
        "IMAGE",
        "VISION",
        "OCR",
    ):
        return SmartSemanticRouter(
            intent="vision",
            requires_expensive=True,
            tier=3,
            reasoning="Keyword classification tier-3",
        )

    return SmartSemanticRouter(
        intent="general",
        requires_expensive=False,
        tier=5,
        reasoning="Default fallback tier-5",
    )


class AgentCircuitBreaker:
    def __init__(self, agent_name: str):
        self.agent_name = agent_name
        self.max_tokens = MAX_AGENT_TOKENS
        self.max_iterations = MAX_AGENT_ITERATIONS
        self._iteration_count = 0
        self._token_count = 0
        self._locked = False
        self._lock_reason: str | None = None

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

    def check_limits(self, tokens: int = 0, iterations: int = 0) -> dict[str, Any]:
        if self._locked:
            return {"blocked": True, "reason": self._lock_reason}
        return {"blocked": False}

    def reset(self) -> None:
        self._iteration_count = 0
        self._token_count = 0
        self._locked = False
        self._lock_reason = None

    def get_status(self) -> dict[str, Any]:
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
        self._tasks: dict[str, dict[str, Any]] = {}

    def create_task(self, task_type: str, payload: dict[str, Any]) -> str:
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

    def _enqueue_task(self, task_id: str, task_type: str, payload: dict) -> None:
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

    def get_task(self, task_id: str) -> dict[str, Any] | None:
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

    def get_stats(self) -> dict[str, Any]:
        statuses = {"pending": 0, "processing": 0, "completed": 0, "failed": 0}
        for task in self._tasks.values():
            status_str = str(task.get("status", "failed"))
            statuses[status_str] = statuses.get(status_str, 0) + 1

        return {
            "total_tasks": len(self._tasks),
            "by_status": statuses,
        }


async_task_manager = AsyncTaskManager()


# ---------------------------------------------------------------------------
# [Antigravity 2026-06-22] Budget-aware routing helper
# ---------------------------------------------------------------------------


def budget_aware_route(
    prompt: str,
    task_type: str = "general",
    preferred_providers: list[str] | None = None,
) -> dict[str, Any]:
    """
    Extends route_request() with free-tier budget awareness.

    Returns the standard SmartSemanticRouter dict PLUS a 'best_provider' key
    that reflects real-time free-tier availability.

    Usage (in model_router or API handlers)::

        route = budget_aware_route(prompt, task_type="code")
        provider = route["best_provider"]  # e.g. "gemini" or "groq"
    """
    semantic_route = route_request(prompt, task_type)

    best_provider: str | None = None
    if _free_tier_available:
        try:
            tracker = get_tracker()
            candidates = preferred_providers or FREE_PROVIDER_PRIORITY
            best_provider = tracker.get_best_provider(candidates=candidates)
            if best_provider:
                logger.info(
                    f"[Orchestrator] budget_aware_route: intent={semantic_route.intent}, "
                    f"tier={semantic_route.tier}, best_free_provider={best_provider}"
                )
            else:
                logger.warning("[Orchestrator] budget_aware_route: all free providers exhausted")
        except Exception as exc:
            logger.warning(f"[Orchestrator] budget_aware_route failed: {exc}")

    return {
        "intent": semantic_route.intent,
        "tier": semantic_route.tier,
        "requires_expensive": semantic_route.requires_expensive,
        "reasoning": semantic_route.reasoning,
        "best_provider": best_provider,
    }
