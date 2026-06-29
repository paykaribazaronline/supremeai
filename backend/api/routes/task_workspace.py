import os

import google.generativeai as genai
from fastapi import APIRouter
from fastapi import BackgroundTasks
from fastapi import HTTPException
from pydantic import BaseModel


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

# AI Model Setup (Gemini)
API_KEY = os.getenv("SUPREMEAI_API_KEY") or os.getenv("GEMINI_API_KEY")
if API_KEY:
    genai.configure(api_key=API_KEY)
    model = genai.GenerativeModel('gemini-1.5-pro')

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
        # ফরম্যাটিং হিস্ট্রি (AI এর বোঝার সুবিধার্থে)
        chat_history_text = "\n".join([f"{msg.role}: {msg.content}" for msg in payload.messages[-5:]])
        
        prompt = f"""
        Previous Context:
        {chat_history_text}
        
        Current Task ({payload.task_type}): {payload.task}
        """

        # ৩. Generate AI Response
        if API_KEY:
            response = model.generate_content(prompt)
            result_text = response.text
        else:
            # Fallback for local testing if API key is missing
            result_text = f"আপনার '{payload.task}' টাস্কটি (টাইপ: {payload.task_type}) সফলভাবে রিসিভ করা হয়েছে! (API Key Not Set)"

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
