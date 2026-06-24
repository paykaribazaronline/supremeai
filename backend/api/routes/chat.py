from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse
import google.generativeai as genai
from pydantic import BaseModel
from api.dependencies import get_tenant_db
from loguru import logger

router = APIRouter(prefix="/api/chat", tags=["AI-Orchestration"])

class ChatPayload(BaseModel):
    prompt: str
    model_name: str = "gemini-1.5-pro"

# ⚡ ১. Fully Async Standard Completion
@router.post("/get_completion")
async def get_completion(payload: ChatPayload, db=Depends(get_tenant_db)):
    """Non-blocking Async LLM Completion"""
    logger.info(f"⚡ Async API Hit: Generating completion for tenant: {db.tenant_id}")
    try:
        model = genai.GenerativeModel(payload.model_name)
        # নেটিভ await কল (পুরো ইভেন্ট লুপ ফ্রি থাকবে)
        response = await model.generate_content_async(payload.prompt)
        return {"success": True, "response": response.text}
    except Exception as e:
        logger.error(f"Async LLM Error: {str(e)}")
        raise HTTPException(status_code=500, detail="AI Gateway Timeout.")

# ⚡ ২. Fully Async Streaming Generator
@router.post("/stream_chat")
async def stream_chat(payload: ChatPayload, db=Depends(get_tenant_db)):
    """High-Concurrency Async SSE Streamer"""
    logger.info(f"🌊 SSE Stream Initiated for tenant: {db.tenant_id}")
    
    async def async_generator():
        try:
            model = genai.GenerativeModel(payload.model_name)
            # অ্যাসিঙ্ক স্ট্রিমিং কল
            response = await model.generate_content_async(payload.prompt, stream=True)
            
            # ইটারেট করার জন্য async for লুপ
            async for chunk in response:
                if chunk.text:
                    # SSE (Server-Sent Events) স্ট্যান্ডার্ড ফরম্যাট
                    yield f"data: {chunk.text}\n\n"
            
            yield "data: [DONE]\n\n"
        except Exception as e:
            logger.error(f"Stream broken: {str(e)}")
            yield f"data: [ERROR] {str(e)}\n\n"

    # ইভেন্ট লুপ ব্লক না করে স্ট্রিমিং রেসপন্স থ্রো করা
    return StreamingResponse(async_generator(), media_type="text/event-stream")
