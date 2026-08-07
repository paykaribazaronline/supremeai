"""LLM Gateway API routes with health monitoring."""

# বাংলা মন্তব্য: REGRESSION FIX — কমিট 985ed35 ভুলভাবে `from core.auth import get_current_user`
# import করেছিল যা কোডবেসে কখনোই ছিল না। প্রতিটি অন্য রাউটারের মতো
# `from api.dependencies import get_current_user_token` ব্যবহার করা হচ্ছে।

# বাংলা মন্তব্য: P0 STOP-THE-LINE FIX — অ্যাডমিন রুটগুলো `get_current_user_token` ব্যবহার করত,
# যার ফলে যেকোনো authenticated user (viewer role সহ) admin endpoints-এ access পেত।
# এখন `get_current_admin` ডিপেন্ডেন্সি যোগ করা হলো — role চেক বাধ্যতামূলক।

import os

from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import JSONResponse
from loguru import logger
from pydantic import BaseModel

from api.dependencies import get_current_user_token
from core.llm.free_tier_tracker import get_tracker
from core.llm.llm_gateway import get_llm_gateway
from core.resilience.circuit_breaker_manager import get_circuit_breaker_manager

router = APIRouter(prefix="/llm-gateway", tags=["llm-gateway"])


def _learning_enabled() -> bool:
    # বাংলা মন্তব্য: ENABLE_DAILY_LEARNER flag — default OFF (safe mode)।
    # চালু করলে learning engine embedding-similarity দিয়ে self-sufficient উত্তর দেয়,
    # নাহলে স্ট্যান্ডার্ড stateless orchestration (প্রডাকশন স্থিতিশীল)।
    return os.getenv("ENABLE_DAILY_LEARNER", "false").lower() in ("1", "true", "yes")


def get_current_admin(payload: dict = Depends(get_current_user_token)) -> dict:
    """Enforce admin role for sensitive LLM gateway admin routes."""
    if payload.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")
    return payload


@router.get("/health")
async def llm_gateway_health(current_user: dict = Depends(get_current_user_token)):
    """Health check for LLM Gateway with circuit breaker and tracker status."""
    gateway = get_llm_gateway()

    # Get circuit breaker states
    cb_manager = get_circuit_breaker_manager()
    circuit_breaker_states = cb_manager.get_all_states()

    # Get free tier tracker status
    tracker = get_tracker()
    tracker_status = tracker.get_all_status()

    return {
        "status": "healthy",
        "gateway_initialized": gateway is not None,
        "circuit_breakers": circuit_breaker_states,
        "free_tier_trackers": tracker_status,
        "providers_configured": True,
    }


@router.get("/admin/gateway/state")
async def get_gateway_state(admin_user: dict = Depends(get_current_admin)):
    """Get detailed state information for all gateways."""
    gateway = get_llm_gateway()

    # Get circuit breaker states
    cb_manager = get_circuit_breaker_manager()
    circuit_breaker_states = cb_manager.get_all_states()

    # Get free tier tracker status
    tracker = get_tracker()
    tracker_status = tracker.get_all_status()

    return {
        "llm_gateway": {
            "initialized": gateway is not None,
            "request_count": getattr(gateway, "_request_count", 0),
            "error_count": getattr(gateway, "_error_count", 0),
        },
        "circuit_breakers": circuit_breaker_states,
        "free_tier_trackers": tracker_status,
    }


@router.post("/admin/circuit-breaker/reset/{name}")
async def reset_circuit_breaker(name: str, admin_user: dict = Depends(get_current_admin)):
    """Reset a specific circuit breaker."""
    cb_manager = get_circuit_breaker_manager()
    success = cb_manager.reset_breaker(name)

    if success:
        return {"message": f"Circuit breaker {name} reset successfully"}
    else:
        return JSONResponse(status_code=404, content={"error": f"Circuit breaker {name} not found"})


@router.get("/admin/providers/fallback-chain")
async def get_fallback_chain(
    task_type: str = "chat",
    model: str | None = None,
    provider: str | None = None,
    admin_user: dict = Depends(get_current_admin),
):
    """Get the current fallback chain for a given task type."""
    gateway = get_llm_gateway()
    call_chain = gateway._build_call_chain(model, provider, task_type)

    return {"task_type": task_type, "fallback_chain": call_chain, "chain_length": len(call_chain)}


class CompletionRequest(BaseModel):
    """Chat completion request that can route through the learning engine."""

    model: str = "gpt-4o"
    messages: list[dict] = []
    task_type: str = "general"
    max_tokens: int = 1000
    temperature: float = 0.7


@router.post("/completion")
async def completion(
    req: CompletionRequest,
    current_user: dict = Depends(get_current_user_token),
):
    """Unified chat completion.

    বাংলা মন্তব্য: ENABLE_DAILY_LEARNER=true হলে LLMGatewayWithLearning দিয়ে চলে —
    যেটা embedding similarity দিয়ে self-sufficient উত্তর দিতে পারে এবং শেখে।
    Flag বন্ধ থাকলে স্ট্যান্ডার্ড LLMRouter.async_generate (safe fallback) ব্যবহার হয়।
    দুই ক্ষেত্রেই যেকোনো এরর হলে স্ট্যান্ডার্ড পাথে ফলব্যাক করে — প্রডাকশন ঝুঁকিমুক্ত।
    """
    user_query = req.messages[-1].get("content", "") if req.messages else ""

    if _learning_enabled():
        try:
            from core.llm.llm_gateway_with_learning import LLMGatewayWithLearning

            gateway = LLMGatewayWithLearning(min_confidence=0.75, learning_enabled=True)
            response = await gateway.acompletion(
                model=req.model,
                messages=req.messages,
                task_type=req.task_type,
                max_tokens=req.max_tokens,
                temperature=req.temperature,
            )
            return {"response": response, "source": "learning_engine"}
        except Exception as exc:  # noqa: BLE001 — safe fallback to standard path
            logger.warning(f"Learning gateway failed, falling back to standard router: {exc}")

    # STANDARD PATH (safe fallback / flag off)
    from core.llm_router import LLMRouter

    router = LLMRouter()
    gen = await router.async_generate(
        prompt=user_query,
        task_type=req.task_type,
        max_tokens=req.max_tokens,
        temperature=req.temperature,
        model_override=req.model,
    )
    text = gen.get("text", "") if isinstance(gen, dict) else str(gen)
    return {"response": text, "source": "standard"}
