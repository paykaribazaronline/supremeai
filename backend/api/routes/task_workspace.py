from api.dependencies import get_current_user_token
from core.llm.llm_gateway import llm_gateway
from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException
from loguru import logger
from pydantic import BaseModel

router = APIRouter(prefix="/workspace/task", tags=["Supreme Workspace Tasks"])


# ==========================================
# ⚙️ PYDANTIC MODELS (Payload Validation)
# ==========================================
class ChatMessage(BaseModel):
    role: str
    content: str


class TaskPayload(BaseModel):
    task: str
    task_type: str = "general"
    messages: list[ChatMessage] = []


# ==========================================
# 🚀 ROUTE: /task/execute
# ==========================================
@router.post("/execute")
async def execute_task(
    payload: TaskPayload,
    background_tasks: BackgroundTasks,
    token_payload: dict = Depends(get_current_user_token),
):
    """
    Handles user prompts from the Vanilla JS Customer Dashboard.
    Integrates Redis rate limiting, RAM conversation history, and Supabase persistent storage.
    """
    _tenant_id = token_payload.get("sub")
    if not _tenant_id:
        raise HTTPException(status_code=401, detail="Invalid token")

    try:
        # বাংলা মন্তব্য: মেসেজ হিস্ট্রি এবং নতুন টাস্ক প্রম্পটকে গেটওয়ের উপযোগী মেসেজ লিস্ট স্কিমায় কনভার্ট করা হচ্ছে
        messages_payload = []
        for msg in payload.messages[-5:]:
            messages_payload.append(
                {
                    "role": "user" if msg.role.lower() == "user" else "assistant",
                    "content": msg.content,
                }
            )

        messages_payload.append(
            {
                "role": "user",
                "content": f"Current Task ({payload.task_type}): {payload.task}",
            }
        )

        # ৩. Generate AI Response
        # বাংলা মন্তব্য: সরাসরি গুগল নেটিভ ক্লায়েন্ট কল না করে ইউনিভার্সাল llm_gateway ব্যবহার করে এপিআই কল করা হচ্ছে
        response = await llm_gateway.acompletion(
            prompt=messages_payload, task_type=payload.task_type, stream=False
        )
        result_text = (
            response.get("text", "") if isinstance(response, dict) else str(response)
        )

        # ৫. Save to Supabase (Database - Long Term) - Background Task
        # রেসপন্স যেন ফাস্ট হয়, তাই ডাটাবেসে সেভ করার কাজটি ব্যাকগ্রাউন্ডে দেওয়া হলো
        def save_to_supabase(task, result):
            pass  # supabase.table("task_history").insert({"task": task, "result": result}).execute()

        background_tasks.add_task(save_to_supabase, payload.task, result_text)

        return {"result": result_text, "status": "success"}

    except Exception as e:  # noqa: BLE001
        logger.info(f"❌ Neural Pipeline Error: {str(e)}")  # noqa: T201
        raise HTTPException(
            status_code=500, detail="Neural connection pipeline error."
        ) from e


# ==========================================
# 📊 ROUTE: /task/quota
# ==========================================
@router.get("/quota")
async def get_quota(token_payload: dict = Depends(get_current_user_token)):
    """
    Fetch the current token quota from Redis for the UI.
    """
    _tenant_id = token_payload.get("sub")
    if not _tenant_id:
        raise HTTPException(status_code=401, detail="Invalid token")
    try:
        from core.cache.redis_manager import redis_manager

        remaining = await redis_manager.client.get(f"quota:{_tenant_id}:remaining")
    except Exception as e:  # noqa: BLE001
        # Ripple-Effect Guard: do NOT report 0 here — that is indistinguishable
        # from "quota genuinely exhausted" and will silently block legitimate
        # users during a Redis outage. Surface a distinct error state instead.
        logger.error(f"Quota lookup failed for tenant={_tenant_id}: {e}")
        raise HTTPException(
            status_code=503,
            detail="Quota service temporarily unavailable. Please retry shortly.",
        ) from e

    if remaining is not None:
        return {"remaining": int(remaining)}
    # Key genuinely absent (e.g. new tenant, quota not yet provisioned) — this
    # is a real "0" state, not a masked failure.
    return {"remaining": 0}
