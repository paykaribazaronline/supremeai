from fastapi import APIRouter, Depends, HTTPException, Request
from pydantic import BaseModel
from typing import List, Dict, Any
from loguru import logger

router = APIRouter(prefix="/api/mobile/bff", tags=["mobile-bff"])

class MobileChatRequest(BaseModel):
    message: str
    history: List[Dict[str, str]] = []
    model_preference: str = "gemini-1.5-flash"

@router.post("/orchestrate")
async def proxy_mobile_ai_request(request: Request, payload: MobileChatRequest):
    """
    BFF (Backend for Frontend) Router for Flutter Mobile Client.
    Eliminates the need for hardcoded API keys in the mobile source code.
    """
    import core.app as app_mod
    model_router = app_mod.model_router
    
    logger.info(f"📱 Mobile BFF intercepting request. Preferred Model: {payload.model_preference}")
    
    # কনটেক্সট লস ঠেকাতে হিস্ট্রি থেকে ইউনিফাইড প্রম্পট বিল্ড করা হচ্ছে
    formatted_prompt = ""
    for msg in payload.history:
        role = "User" if msg.get("role") == "user" else "Assistant"
        formatted_prompt += f"{role}: {msg.get('content', '')}\n"
    formatted_prompt += f"User: {payload.message}\nAssistant:"

    try:
        # ব্যাকএন্ডের ভেতরে থাকা সিকিউর ক্লাউড সিক্রেট এবং ইনজেক্টেড কী ব্যবহার করা হচ্ছে
        raw_response = await model_router.async_route_and_generate(
            prompt=formatted_prompt,
            task_type="general",
            max_cost=0.01  # মোবাইল অ্যাপের জন্য বাজেট লিমিট লকড
        )
        
        if not raw_response.get("success"):
            logger.error(f"Upstream AI core failed: {raw_response.get('error')}")
            raise HTTPException(status_code=502, detail="Upstream AI Provider connection failure.")
            
        return {
            "success": True,
            "text": raw_response.get("text"),
            "provider": raw_response.get("provider")
        }
        
    except Exception as e:
        logger.error(f"❌ Mobile BFF Execution Error: {str(e)}")
        raise HTTPException(status_code=500, detail="Internal BFF Proxy Error.")
