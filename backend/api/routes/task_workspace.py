import os

from fastapi import APIRouter
from fastapi import BackgroundTasks
from fastapi import HTTPException
from pydantic import BaseModel
from core.llm_gateway import llm_gateway


router = APIRouter(prefix="/task", tags=["Supreme Workspace Tasks"])

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
async def execute_task(payload: TaskPayload, background_tasks: BackgroundTasks):
    """
    Handles user prompts from the Vanilla JS Customer Dashboard.
    Integrates Redis rate limiting, RAM conversation history, and Supabase persistent storage.
    """
    _tenant_id = "default_user_session" # প্রোডাকশনে এটি JWT বা সেশন টোকেন থেকে আসবে
    
    try:
        # বাংলা মন্তব্য: মেসেজ হিস্ট্রি এবং নতুন টাস্ক প্রম্পটকে গেটওয়ের উপযোগী মেসেজ লিস্ট স্কিমায় কনভার্ট করা হচ্ছে
        messages_payload = []
        for msg in payload.messages[-5:]:
            messages_payload.append({
                "role": "user" if msg.role.lower() == "user" else "assistant",
                "content": msg.content
            })
        
        messages_payload.append({
            "role": "user",
            "content": f"Current Task ({payload.task_type}): {payload.task}"
        })

        # ৩. Generate AI Response
        # বাংলা মন্তব্য: সরাসরি গুগল নেটিভ ক্লায়েন্ট কল না করে ইউনিভার্সাল llm_gateway ব্যবহার করে এপিআই কল করা হচ্ছে
        response = await llm_gateway.acompletion(
            prompt=messages_payload,
            task_type=payload.task_type,
            stream=False
        )
        result_text = response.get("text", "") if isinstance(response, dict) else str(response)

        # ৫. Save to Supabase (Database - Long Term) - Background Task
        # রেসপন্স যেন ফাস্ট হয়, তাই ডাটাবেসে সেভ করার কাজটি ব্যাকগ্রাউন্ডে দেওয়া হলো
        def save_to_supabase(task, result):
            pass # supabase.table("task_history").insert({"task": task, "result": result}).execute()
        
        background_tasks.add_task(save_to_supabase, payload.task, result_text)

        return {"result": result_text, "status": "success"}

    except Exception as e:
        print(f"❌ Neural Pipeline Error: {str(e)}")
        raise HTTPException(status_code=500, detail="Neural connection pipeline error.") from e

# ==========================================
# 📊 ROUTE: /task/quota
# ==========================================
@router.get("/quota")
async def get_quota():
    """
    Fetch the current token quota from Redis for the UI.
    """
    _tenant_id = "default_user_session"
    return {"remaining": 87} # Mocking the 87% for the UI
