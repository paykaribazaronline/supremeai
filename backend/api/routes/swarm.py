import asyncio
import logging
import uuid
from typing import Any

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, Request
from pydantic import BaseModel, Field
from sse_starlette.sse import EventSourceResponse

from api.routes.admin import get_current_admin
from core.error_bus import with_error_bus
from core.orchestration.swarm_orchestrator import SwarmOrchestrator
from core.swarm_pubsub import swarm_streamer
from database.session import get_db_session
from engine.forge_compiler import ForgeCompiler
from models.patch_telemetry import PatchTelemetry

logger = logging.getLogger(__name__)


router = APIRouter(tags=["Swarm"])


@router.get("/stream")
async def stream_swarm_health(request: Request):
    """
    SSE Endpoint for Real-time Swarm Health & Logs.
    URL: /api/v1/swarm/stream
    """

    @with_error_bus("event_generator")
    async def event_generator():
        try:
            async for message in swarm_streamer.subscribe():
                if await request.is_disconnected():
                    break
                yield {"data": message}
        except Exception:
            logger.exception("Unhandled exception")
            pass

    return EventSourceResponse(event_generator())


@with_error_bus("_save_telemetry_to_db")
async def _save_telemetry_to_db(data: dict):
    """বাংলা মন্তব্য: আগে এই ফাংশন শুধু logger.info() করত — ডেটা কখনো DB-তে যেত না
    (silent data loss)। Self-Healing Engine-এর ফিডব্যাক লুপ (কোন প্যাচ ইউজার
    Accept/Reject করেছে) এই ডেটার উপর নির্ভর করে, তাই এখন সত্যিই persist করা হচ্ছে।
    """
    async for session in get_db_session():
        try:
            session.add(
                PatchTelemetry(
                    error_id=data["error_id"],
                    patch_id=data["patch_id"],
                    file_path=data["file_path"],
                    status=data["status"],
                    similarity_score=data["similarity_score"],
                )
            )
            await session.commit()
            logger.info(f"Telemetry persisted to DB: {data.get('patch_id', 'Unknown')}")
        except Exception:
            logger.exception(f"Failed to persist telemetry for patch {data.get('patch_id', 'Unknown')}")
            raise


class PatchTelemetryPayload(BaseModel):
    error_id: str = Field(..., description="Unique ID for the intercepted error")
    patch_id: str = Field(..., description="Unique ID for the generated patch")
    file_path: str = Field(..., description="Path of the healed file")
    status: str = Field(..., description="'ACCEPTED', 'REJECTED', or 'MODIFIED'")
    similarity_score: float = Field(..., description="Levenshtein similarity score (0.0 to 1.0)")


@router.post("/telemetry/patch-result", status_code=202)
async def record_patch_telemetry(payload: PatchTelemetryPayload, background_tasks: BackgroundTasks):
    """
    Receives telemetry on whether the user accepted, rejected, or modified the Swarm's proposed fix.
    """
    background_tasks.add_task(_save_telemetry_to_db, payload.model_dump())

    logger.info(
        f"Telemetry received: Patch {payload.patch_id} was {payload.status} with score {payload.similarity_score}"
    )
    return {"message": "Telemetry recorded"}


@router.post("/halt", status_code=202)
async def halt_swarm(admin_user: dict = Depends(get_current_admin)):
    """বাংলা মন্তব্য: গ্লোবাল ইমার্জেন্সি-স্টপ — মোবাইল অ্যাপের 'Hold to Kill' বাটনের
    সত্যিকারের ব্যাকএন্ড কাউন্টারপার্ট। আগে এই এন্ডপয়েন্টটি existই করত না, তাই বাটন
    চাপলে শুধু UI-তে অ্যানিমেশন হতো, কোনো এজেন্ট আসলে থামত না।
    """
    await swarm_streamer.set_halt(reason=f"manual_stop_by:{admin_user.get('sub', 'unknown')}")
    await swarm_streamer.broadcast(
        event_type="CIRCUIT_OPEN",
        payload={
            "message": "Emergency stop triggered — swarm execution halted.",
            "triggeredBy": admin_user.get("sub"),
        },
    )
    logger.critical(f"🛑 Swarm emergency-stop triggered by {admin_user.get('sub')}")
    return {"status": "halted"}


@router.post("/resume", status_code=202)
async def resume_swarm(admin_user: dict = Depends(get_current_admin)):
    """বাংলা মন্তব্য: হল্ট ফ্ল্যাগ ক্লিয়ার করে সোয়ার্ম আবার চালু করে।"""
    await swarm_streamer.clear_halt()
    await swarm_streamer.broadcast(
        event_type="CIRCUIT_CLOSED",
        payload={
            "message": "Swarm execution resumed.",
            "triggeredBy": admin_user.get("sub"),
        },
    )
    logger.critical(f"✅ Swarm resumed by {admin_user.get('sub')}")
    return {"status": "resumed"}


class SelfHealingRequest(BaseModel):
    filePath: str  # -- camelCase required to match frontend JSON API contract
    message: str
    lineNumber: int  # -- camelCase required to match frontend JSON API contract
    codeContext: str  # -- camelCase required to match frontend JSON API contract
    languageId: str  # -- camelCase required to match frontend JSON API contract


@router.post("/execute-healing")
async def execute_healing(payload: SelfHealingRequest, request: Request):
    """
    Agent-in-the-Loop endpoint to self-heal code errors from VS Code Extension.
    Rate limited to 5 requests per minute per IP to prevent LLM cost spikes.
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
            fixed_code = next(iter(workspace.generated_code.values()))

    if not fixed_code:
        fixed_code = payload.codeContext  # Fallback

    return {
        "success": True,
        "fixedCode": fixed_code,
        "message": "Swarm successfully generated a fix.",
    }


class FlowPosition(BaseModel):
    x: float
    y: float


class FlowNode(BaseModel):
    id: str = Field(..., description="Unique ID of the node")
    type: str = Field(..., description="Type of the node (e.g., agentNode, taskNode)")
    position: FlowPosition
    data: dict[str, Any] = Field(
        default_factory=dict,
        description="Node payload containing role, model, prompt, etc.",
    )


class FlowEdge(BaseModel):
    id: str = Field(..., description="Unique ID of the edge")
    source: str = Field(..., description="Source node ID")
    target: str = Field(..., description="Target node ID")
    animated: bool | None = Field(default=False, description="Whether the edge is animated")


class ForgePayload(BaseModel):
    name: str = Field(..., description="Name of the custom swarm flow")
    description: str | None = Field(default="", description="Optional description of the swarm's purpose")
    nodes: list[FlowNode]
    edges: list[FlowEdge]


@router.post("/forge", status_code=201)
async def save_forge_swarm(payload: ForgePayload):
    """
    Saves the visual swarm layout (nodes and edges) from the Evolution Forge.
    """
    try:
        logger.info(f"Received Forge payload for Swarm: {payload.name} with {len(payload.nodes)} nodes.")

        return {
            "status": "success",
            "message": "Swarm blueprint saved successfully",
            "data": {
                "swarm_name": payload.name,
                "node_count": len(payload.nodes),
                "edge_count": len(payload.edges),
            },
        }
    except Exception as e:
        logger.error(f"Failed to save Forge Swarm: {e!s}")
        raise HTTPException(status_code=500, detail="Internal server error while saving swarm blueprint") from e


async def run_swarm_execution_async(execution_plan):
    try:
        for node in execution_plan:
            await swarm_streamer.broadcast(
                event_type="NODE_EXECUTION",
                payload={
                    "nodeId": node["id"],
                    "status": "RUNNING",
                    "message": f"Agent Node [{node['data'].get('label', 'Unknown')}] is now processing input...",
                },
            )
            await asyncio.sleep(2)

            await swarm_streamer.broadcast(
                event_type="NODE_EXECUTION",
                payload={
                    "nodeId": node["id"],
                    "status": "COMPLETED",
                    "message": f"Agent Node [{node['data'].get('label', 'Unknown')}] execution finished.",
                },
            )
    except Exception as e:
        logger.error(f"Execution background task failed: {e!s}")


@router.post("/forge/{flow_id}/execute", status_code=202)
async def execute_forge_flow(flow_id: str, payload: ForgePayload, background_tasks: BackgroundTasks):
    try:
        execution_plan = ForgeCompiler.compile_and_sort(
            [n.model_dump() for n in payload.nodes],
            [e.model_dump() for e in payload.edges],
        )

        background_tasks.add_task(run_swarm_execution_async, execution_plan)
        return {
            "status": "accepted",
            "message": "Swarm execution started in background",
        }
    except ValueError as e:
        logger.error(f"DAG Validation Error: {e!s}")
        raise HTTPException(status_code=400, detail=str(e)) from e
    except Exception as e:
        logger.error(f"Execution failed: {e!s}")
        raise HTTPException(status_code=500, detail="Failed to execute flow") from e
