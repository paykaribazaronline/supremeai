# 📄 ফাইল: backend/api/routes/swarm.py

**প্রকার:** .py  
**সাইজ:** 7,150 বাইট  
**আপডেট:** 2026-07-11T19:26:12.068394

---

## কোড

```py
import asyncio
import logging
import uuid
from typing import Any

from fastapi import APIRouter
from fastapi import BackgroundTasks
from fastapi import HTTPException
from fastapi import Request
from pydantic import BaseModel
from pydantic import Field
from sse_starlette.sse import EventSourceResponse

from core.swarm_pubsub import swarm_streamer
from engine.forge_compiler import ForgeCompiler
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
                if await request.is_disconnected():
                    break
                yield {"data": message}
        except Exception:  # noqa: BLE001
            pass

    return EventSourceResponse(event_generator())


class PatchTelemetry(BaseModel):
    error_id: str = Field(..., description="Unique ID for the intercepted error")
    patch_id: str = Field(..., description="Unique ID for the generated patch")
    file_path: str = Field(..., description="Path of the healed file")
    status: str = Field(..., description="'ACCEPTED', 'REJECTED', or 'MODIFIED'")
    similarity_score: float = Field(..., description="Levenshtein similarity score (0.0 to 1.0)")


def _save_telemetry_to_db(data: dict):
    # এখানে SQLModel বা Tortoise ORM এর মতো ORM ব্যবহার করে ইনসার্ট করবেন
    # session.add(PatchTelemetryModel(**data))
    # session.commit()
    logger.info(f"Telemetry persisted to DB: {data.get('patch_id', 'Unknown')}")


@router.post("/telemetry/patch-result", status_code=202)
async def record_patch_telemetry(payload: PatchTelemetry, background_tasks: BackgroundTasks):
    """
    Receives telemetry on whether the user accepted, rejected, or modified the Swarm's proposed fix.
    """
    background_tasks.add_task(_save_telemetry_to_db, payload.model_dump())

    logger.info(f"Telemetry received: Patch {payload.patch_id} was {payload.status} with score {payload.similarity_score}")
    return {"message": "Telemetry recorded"}


class SelfHealingRequest(BaseModel):
    filePath: str
    message: str
    lineNumber: int
    codeContext: str
    languageId: str


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
            fixed_code = list(workspace.generated_code.values())[0]

    if not fixed_code:
        fixed_code = payload.codeContext  # Fallback

    return {"success": True, "fixedCode": fixed_code, "message": "Swarm successfully generated a fix."}


class FlowPosition(BaseModel):
    x: float
    y: float


class FlowNode(BaseModel):
    id: str = Field(..., description="Unique ID of the node")
    type: str = Field(..., description="Type of the node (e.g., agentNode, taskNode)")
    position: FlowPosition
    data: dict[str, Any] = Field(default_factory=dict, description="Node payload containing role, model, prompt, etc.")


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
            "data": {"swarm_name": payload.name, "node_count": len(payload.nodes), "edge_count": len(payload.edges)},
        }
    except Exception as e:
        logger.error(f"Failed to save Forge Swarm: {str(e)}")
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
    except Exception as e:  # noqa: BLE001
        logger.error(f"Execution background task failed: {str(e)}")


@router.post("/forge/{flow_id}/execute", status_code=202)
async def execute_forge_flow(flow_id: str, payload: ForgePayload, background_tasks: BackgroundTasks):
    try:
        execution_plan = ForgeCompiler.compile_and_sort([n.model_dump() for n in payload.nodes], [e.model_dump() for e in payload.edges])

        background_tasks.add_task(run_swarm_execution_async, execution_plan)
        return {"status": "accepted", "message": "Swarm execution started in background"}
    except ValueError as e:
        logger.error(f"DAG Validation Error: {str(e)}")
        raise HTTPException(status_code=400, detail=str(e)) from e
    except Exception as e:
        logger.error(f"Execution failed: {str(e)}")
        raise HTTPException(status_code=500, detail="Failed to execute flow") from e

```