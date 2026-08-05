# backend/api/routes/agent.py
"""Autonomous Agent Execution Route.

Provides:
- /v1/agents/execute: Clean architecture route for autonomous agent tasks.
- Controller pattern with ErrorEventBus integration.
- Background task support for long-running operations.
"""

from __future__ import annotations

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, Request
from pydantic import BaseModel, Field

from api.dependencies import verify_autonomous_agent_token
from core.error_bus import with_error_bus
from core.messaging.event_bus import ErrorContext, ErrorEvent, error_event_bus

router = APIRouter(prefix="/api/v1/agents", tags=["Autonomous Agents"])


# Strict Pydantic Schema for Input Validation
class AgentTaskRequest(BaseModel):
    task_id: str = Field(..., description="Unique ID for the task")
    prompt: str = Field(..., min_length=10, max_length=5000)
    auto_execute: bool = Field(default=False)


class AgentTaskResponse(BaseModel):
    status: str
    result: str


@router.post("/execute", response_model=AgentTaskResponse)
@with_error_bus("execute_agent_task")
async def execute_agent_task(
    request: Request,
    payload: AgentTaskRequest,
    background_tasks: BackgroundTasks,
    user: dict = Depends(verify_autonomous_agent_token),
) -> AgentTaskResponse:
    """
    Triggers an autonomous agent task safely.

    বাংলা মন্তব্য: API রাউটারটি হবে একদম পরিষ্কার (Clean Architecture)।
    এটি সরাসরি লজিক এক্সিকিউট না করে সার্ভিসের কাছে কাজ ডেলিগেট করবে।
    """
    correlation_id = getattr(request.state, "correlation_id", "unknown")

    try:
        # Example of delegating to our hardened LLM Gateway
        # In production, this would call the actual LLM gateway service
        # response_text = await llm_gateway.generate_response(
        #     prompt=payload.prompt,
        #     model="gpt-4o"
        # )

        # For now, simulate a response
        response_text = f"Task {payload.task_id} executed successfully. Prompt processed: {payload.prompt[:100]}..."

        # If auto_execute is True, we can pass it to background tasks to prevent HTTP timeouts on Render
        if payload.auto_execute:
            # background_tasks.add_task(execute_code_safely, response_text)
            pass

        return AgentTaskResponse(status="success", result=response_text)

    except Exception as exc:
        # Route expected/unexpected errors to the ErrorBus and return safe HTTP response
        error_event_bus.emit(
            ErrorEvent(
                module="AgentExecutionRoute",
                error_type="TASK_EXECUTION_FAILED",
                message=str(exc)[:500],
                severity="ERROR",
                context={
                    "task_id": payload.task_id,
                    "correlation_id": correlation_id,
                    "user": user.get("sub", "unknown"),
                },
                structured_context=ErrorContext(
                    module="api.routes.agent",
                    request_id=correlation_id,
                    task_id=payload.task_id,
                    env="production",
                ),
            )
        )
        raise HTTPException(
            status_code=500,
            detail="Autonomous task failed. The system has logged the error for self-healing.",
        ) from exc
