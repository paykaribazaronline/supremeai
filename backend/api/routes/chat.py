from fastapi import APIRouter
from fastapi import Depends
from fastapi import HTTPException
from fastapi import Request
from fastapi.responses import StreamingResponse
from loguru import logger
from pydantic import BaseModel

from api.dependencies import get_tenant_db
from core.llm_gateway import llm_gateway
from core.multi_layer_cache import multi_layer_cache


router = APIRouter(prefix="/api/chat", tags=["AI-Orchestration"])


class ChatPayload(BaseModel):
    prompt: str
    model_name: str = "gemini-1.5-pro"


# ⚡ ১. Fully Async Standard Completion with Multi-Layer Caching
@router.post("/get_completion")
async def get_completion(
    request: Request, payload: ChatPayload, db=Depends(get_tenant_db)
):
    """Non-blocking Async LLM Completion with 5-Layer Caching"""
    logger.info(f"⚡ Async API Hit: Generating completion for tenant: {db.tenant_id}")

    # Extract session ID from headers for session-based caching
    session_id = request.headers.get("X-Session-ID")

    # Check multi-layer cache first
    cached_result = await multi_layer_cache.get(
        prompt=payload.prompt, model_name=payload.model_name, session_id=session_id
    )

    if cached_result:
        logger.info(f"🚀 CACHE HIT: {cached_result['source']}")
        return {
            "success": True,
            "response": cached_result["response"],
            "cached": True,
            "cache_source": cached_result["source"],
            "latency_ms": cached_result.get("latency_ms", 0),
        }

    # Cache miss - generate response from AI model
    logger.info("❌ CACHE MISS: Generating new response from AI model")
    try:
        # বাংলা মন্তব্য: সরাসরি গুগল নেটিভ ক্লায়েন্ট কল না করে ইউনিভার্সাল llm_gateway ব্যবহার করে এপিআই কল করা হচ্ছে
        response = await llm_gateway.acompletion(
            prompt=payload.prompt,
            task_type="chat",
            stream=False
        )
        response_text = response.get("text", "") if isinstance(response, dict) else str(response)

        # Store response in multi-layer cache for future requests
        await multi_layer_cache.set(
            prompt=payload.prompt,
            response=response_text,
            model_name=payload.model_name,
            session_id=session_id,
        )

        return {
            "success": True,
            "response": response_text,
            "cached": False,
            "cache_source": "L5_AI_MODEL",
        }
    except Exception as e:
        logger.error(f"Async LLM Error: {str(e)}")
        raise HTTPException(status_code=500, detail="AI Gateway Timeout.") from e


# ⚡ ২. Fully Async Streaming Generator
@router.post("/stream_chat")
async def stream_chat(payload: ChatPayload, db=Depends(get_tenant_db)):
    """High-Concurrency Async SSE Streamer"""
    logger.info(f"🌊 SSE Stream Initiated for tenant: {db.tenant_id}")

    async def async_generator():
        try:
            # বাংলা মন্তব্য: ইউনিভার্সাল llm_gateway ব্যবহার করে স্ট্রিমিং সম্পন্ন করা হচ্ছে
            response_stream = await llm_gateway.acompletion(
                prompt=payload.prompt,
                task_type="chat",
                stream=True
            )

            async for chunk in response_stream:
                if chunk:
                    # SSE (Server-Sent Events) স্ট্যান্ডার্ড ফরম্যাট
                    yield f"data: {chunk}\n\n"

            yield "data: [DONE]\n\n"
        except Exception as e:
            logger.error(f"Stream broken: {str(e)}")
            yield f"data: [ERROR] {str(e)}\n\n"

    # ইভেন্ট লুপ ব্লক না করে স্ট্রিমিং রেসপন্স থ্রো করা
    return StreamingResponse(async_generator(), media_type="text/event-stream")
