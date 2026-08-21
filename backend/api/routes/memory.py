from __future__ import annotations

from typing import Any

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field

from api.dependencies import get_current_user_token
from memory.checkpoint_resume import CheckpointResume
from memory.sliding_window import SlidingWindowConfig, SlidingWindowMemory

router = APIRouter(
    prefix="/memory",
    tags=["memory"],
    dependencies=[Depends(get_current_user_token)],
)

_checkpoint: CheckpointResume | None = None
_window: SlidingWindowMemory | None = None


def get_checkpoint() -> CheckpointResume:
    global _checkpoint
    if _checkpoint is None:
        _checkpoint = CheckpointResume()
    return _checkpoint


def get_window() -> SlidingWindowMemory:
    global _window
    if _window is None:
        _window = SlidingWindowMemory()
    return _window


class CheckpointSaveRequest(BaseModel):
    task_id: str = Field(..., description="Unique task identifier")
    step_index: int = Field(..., ge=0)
    state: dict[str, Any] = Field(default_factory=dict)


class CheckpointResponse(BaseModel):
    task_id: str
    step_index: int
    state: dict[str, Any]
    resumed: bool


class ChunkRequest(BaseModel):
    text: str
    session_id: str = "default"
    max_tokens: int = 4000
    overlap_ratio: float = 0.15


class ChunkResponse(BaseModel):
    session_id: str
    windows: list[dict[str, Any]]


class ContextRequest(BaseModel):
    documents: list[str] = Field(default_factory=list)
    query: str = ""
    session_id: str = "default"
    budget: int | None = None


class ContextResponse(BaseModel):
    session_id: str
    context: str


@router.post("/checkpoint", response_model=CheckpointResponse)
def save_checkpoint(payload: CheckpointSaveRequest):
    store = get_checkpoint()
    ok = store.save(payload.task_id, payload.step_index, payload.state)
    if not ok:
        raise HTTPException(status_code=500, detail="Failed to save checkpoint")
    return CheckpointResponse(
        task_id=payload.task_id,
        step_index=payload.step_index,
        state=payload.state,
        resumed=False,
    )


@router.get("/checkpoint/{task_id}", response_model=CheckpointResponse | None)
def load_checkpoint(task_id: str):
    store = get_checkpoint()
    result = store.load(task_id)
    if result is None:
        return None
    return CheckpointResponse(**result)


@router.get("/checkpoints", response_model=list[dict[str, Any]])
def list_checkpoints():
    store = get_checkpoint()
    return store.list_all()


@router.delete("/checkpoint/{task_id}")
def clear_checkpoint(task_id: str):
    store = get_checkpoint()
    ok = store.clear(task_id)
    if not ok:
        raise HTTPException(status_code=500, detail="Failed to clear checkpoint")
    return {"status": "ok", "task_id": task_id}


@router.post("/chunk", response_model=ChunkResponse)
def chunk_text(payload: ChunkRequest):
    config = SlidingWindowConfig(max_tokens=payload.max_tokens, overlap_ratio=payload.overlap_ratio)
    memory = SlidingWindowMemory(config=config)
    windows = memory.chunk(payload.text, session_id=payload.session_id)
    return ChunkResponse(session_id=payload.session_id, windows=windows)


@router.post("/context", response_model=ContextResponse)
def build_context(payload: ContextRequest):
    config = SlidingWindowConfig()
    memory = SlidingWindowMemory(config=config)
    budget = payload.budget or config.max_tokens
    context = memory.build_context(payload.documents, payload.query, payload.session_id, budget)
    return ContextResponse(session_id=payload.session_id, context=context)


@router.get("/recall", response_model=list[dict[str, Any]])
def recall_memory(session_id: str = "default", limit: int = 20):
    memory = get_window()
    return memory.recall(session_id, limit=limit)


@router.delete("/recall")
def clear_memory(session_id: str = "default"):
    memory = get_window()
    ok = memory.clear(session_id)
    if not ok:
        raise HTTPException(status_code=500, detail="Failed to clear memory")
    return {"status": "ok", "session_id": session_id}


# ---------------------------------------------------------------------------
# Vector Memory (Eternal Brain) API Schemas & Endpoints
# ---------------------------------------------------------------------------


class VectorRecallRequest(BaseModel):
    task_description: str = Field(..., min_length=1, description="Task or prompt to recall context for")
    limit: int = Field(default=5, ge=1, le=20)
    threshold: float = Field(default=0.6, ge=0.0, le=1.0)


class VectorSaveRequest(BaseModel):
    session_id: str = Field(..., min_length=1)
    summary: str = Field(..., min_length=1)
    task_type: str = Field(default="general")
    agent_type: str = Field(default="main")
    metadata: dict[str, Any] | None = None


class SessionSaveRequest(BaseModel):
    session_id: str = Field(..., min_length=1)
    messages: list[dict[str, Any]] = Field(..., min_length=1)
    task_type: str = Field(default="general")


@router.post("/recall")
async def vector_recall(req: VectorRecallRequest):
    """Semantic-search the Eternal Brain for relevant past memories."""
    from services.memory_service import recall_memories
    memories = await recall_memories(
        task_description=req.task_description,
        limit=req.limit,
        threshold=req.threshold,
    )
    return {"success": True, "memories": memories, "count": len(memories)}


@router.post("/save")
async def vector_save(req: VectorSaveRequest):
    """Store a vector memory entry into Supabase/pgvector or cascade fallback."""
    from services.memory_service import save_memory
    result = await save_memory(
        session_id=req.session_id,
        summary=req.summary,
        task_type=req.task_type,
        agent_type=req.agent_type,
        metadata=req.metadata,
    )
    return result


@router.post("/session")
async def save_session(req: SessionSaveRequest):
    """Summarize a full chat session via LLM, then save as vector memory."""
    from services.memory_service import summarize_and_save_session
    result = await summarize_and_save_session(
        session_id=req.session_id,
        messages=req.messages,
        task_type=req.task_type,
    )
    return result
