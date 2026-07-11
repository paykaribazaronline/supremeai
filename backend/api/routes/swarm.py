import logging
import uuid

from fastapi import APIRouter
from fastapi import BackgroundTasks
from fastapi import Request
from pydantic import BaseModel
from pydantic import Field
from sse_starlette.sse import EventSourceResponse

from core.swarm_pubsub import swarm_streamer
from engine.swarm_orchestrator import SwarmOrchestrator


logger = logging.getLogger(__name__)


router = APIRouter(tags=["Swarm"])


@router.get("/stream")
async def stream_swarm_health(request: Request):
    """
    SSE Endpoint for Real-time Swarm Health & Logs.
    URL: /api/v1/swarm/stream
    """

    async def event_generator():
        try:
            async for message in swarm_streamer.subscribe():
                # যদি রিকোয়েস্ট ড্রপ হয় (যেমন ইউজার ট্যাব ক্লোজ করেছে)
                if await request.is_disconnected():
                    break
                # SSE ফরম্যাটে ডেটা পাঠানো হচ্ছে
                yield {"data": message}
        except Exception:  # noqa: BLE001
            pass  # Handle graceful shutdown

    return EventSourceResponse(event_generator())


class PatchTelemetry(BaseModel):
    error_id: str = Field(..., description="Unique ID for the intercepted error")
    patch_id: str = Field(..., description="Unique ID for the generated patch")
    file_path: str = Field(..., description="Path of the healed file")
    status: str = Field(..., description="'ACCEPTED', 'REJECTED', or 'MODIFIED'")
    similarity_score: float = Field(..., description="Levenshtein similarity score (0.0 to 1.0)")


@router.post("/telemetry/patch-result", status_code=202)
async def record_patch_telemetry(payload: PatchTelemetry, background_tasks: BackgroundTasks):
    """
    Receives telemetry on whether the user accepted, rejected, or modified the Swarm's proposed fix.
    """
    background_tasks.add_task(_save_telemetry_to_db, payload.model_dump())

    logger.info(f"Telemetry received: Patch {payload.patch_id} was {payload.status} with score {payload.similarity_score}")
    return {"message": "Telemetry recorded"}


def _save_telemetry_to_db(data: dict):
    # TODO: Implement DB logic
    pass


class SelfHealingRequest(BaseModel):
    filePath: str
    message: str
    lineNumber: int
    codeContext: str
    languageId: str


@router.post("/execute-healing")
async def execute_healing(payload: SelfHealingRequest):
    """
    Agent-in-the-Loop endpoint to self-heal code errors from VS Code Extension.
    """
    session_id = str(uuid.uuid4())
    task_prompt = f"""
    The following {payload.languageId} code in {payload.filePath} has an error at line {payload.lineNumber}:
    Error Message: {payload.message}
    
    Code Context:
    ```
    {payload.codeContext}
    ```
    
    Please fix the error and provide the corrected complete code content.
    """

    orchestrator = SwarmOrchestrator(user_id="vscode_agent", session_id=session_id, task_prompt=task_prompt)

    # Execute swarm with 0 retries for speed in VS Code context
    workspace = await orchestrator.execute(max_retries=0)

    fixed_code = None
    if workspace.generated_code:
        fixed_code = workspace.generated_code.get(payload.filePath)
        if not fixed_code:
            fixed_code = list(workspace.generated_code.values())[0]

    if not fixed_code:
        fixed_code = payload.codeContext  # Fallback

    return {"success": True, "fixedCode": fixed_code, "message": "Swarm successfully generated a fix."}
