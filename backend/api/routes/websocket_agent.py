import asyncio
import json

from fastapi import APIRouter
from fastapi import Query
from fastapi import WebSocket
from fastapi import WebSocketDisconnect
from fastapi import status

from core.llm_gateway import llm_gateway
from core.security import verify_token
from database.supabase_client import SupabaseDB


router = APIRouter(prefix="/ws", tags=["Neural Engine Stream"])

_pref_locks: dict[str, asyncio.Lock] = {}
_pref_locks_lock = asyncio.Lock()


# বাংলা মন্তব্য: ইউজারের রিকোয়ারমেন্ট এনালাইসিস করে তা ডাটাবেজে সেভ রাখার জন্য ব্যাকগ্রাউন্ড অ্যাসিনক্রোনাস টাস্ক
async def analyze_and_save_preferences(user_id: str, user_message: str):
    async with _pref_locks_lock:
        if user_id not in _pref_locks:
            _pref_locks[user_id] = asyncio.Lock()
        lock = _pref_locks[user_id]

    async with lock:
        db = SupabaseDB()
        existing = await asyncio.to_thread(db.get_user_preferences, user_id)
        existing = existing or {}
        existing_prefs = existing.get("preferences") or {}

        safe_message = user_message.replace('"', "'")

        analysis_prompt = f"""Analyze the user's message to extract their work profile, technical stack, and preferred answer style.
User Message: '{safe_message}'
Existing Profile: {json.dumps(existing_prefs)}

Return ONLY a valid JSON object matching this structure (merge with existing if relevant):
{{
  "preferred_stack": "e.g., Python/FastAPI, TypeScript/React, none",
  "answering_style": "e.g., direct code, step-by-step tutorial, concise",
  "work_type": "e.g., debugging, new feature design, general"
}}
JSON:"""

        try:
            response = await llm_gateway.acompletion(
                prompt=analysis_prompt,
                task_type="analysis",
                stream=False
            )
            text = response.get("text", "{}") if isinstance(response, dict) else str(response)

            if "```" in text:
                parts = text.split("```")
                if len(parts) >= 3:
                    text = parts[1]
                    if text.startswith("json"):
                        text = text[4:]
            new_prefs = json.loads(text.strip())
            if new_prefs:
                merged_prefs = {**existing_prefs, **new_prefs}
                await asyncio.to_thread(db.upsert_user_preferences, {
                    "user_id": user_id,
                    "preferences": merged_prefs
                })
                print(f"🤖 [WS] Updated user preferences for {user_id}: {merged_prefs}")
        except Exception:
            print("⚠️ [WS] Failed to analyze user preferences")


# ==========================================
# 🔌 WEBSOCKET CONNECTION MANAGER
# ==========================================
class ConnectionManager:
    def __init__(self):
        self.active_connections: list[WebSocket] = []
        self._pref_tasks: dict[str, set[asyncio.Task]] = {}

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)
        print("🟢 [WS] New Client Connected to Neural Engine.")

    def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)
        print("🔴 [WS] Client Disconnected.")

    async def _authenticate(self, websocket: WebSocket) -> dict | None:
        token = websocket.query_params.get("token")
        if not token:
            return None
        try:
            return verify_token(token)
        except Exception:
            return None

    def track_pref_task(self, user_id: str, task: asyncio.Task) -> None:
        self._pref_tasks.setdefault(user_id, set()).add(task)

    def cancel_pref_tasks(self, user_id: str) -> None:
        tasks = self._pref_tasks.get(user_id, set())
        for task in tasks:
            task.cancel()
        self._pref_tasks.pop(user_id, None)


manager = ConnectionManager()


@router.websocket("/chat")
async def websocket_chat_endpoint(
    websocket: WebSocket,
    token: str | None = Query(default=None),
):
    """
    Real-time bidirectional WebSocket for Token-by-Token streaming and Agentic Tool execution.
    Supports both plain text (Flutter) and JSON payloads with base64 images (Web Chat).
    """
    auth_payload = await manager._authenticate(websocket)
    if not auth_payload:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
        return

    await manager.connect(websocket)

    # সেশন হিস্ট্রি মেইনটেইন করার জন্য চ্যাট অবজেক্ট তৈরি করা
    chat_history = []

    # বাংলা মন্তব্য: কানেক্টেড ইউজারের পূর্ববর্তী প্রেফারেন্স ডাটাবেজ থেকে রিড করা হচ্ছে
    user_id = auth_payload.get("sub", "anonymous")
    db = SupabaseDB()
    user_pref_record = await asyncio.to_thread(db.get_user_preferences, user_id)
    user_pref_record = user_pref_record or {}
    user_prefs = user_pref_record.get("preferences") or {}

    try:
        while True:
            # ১. ফ্রন্টএন্ড থেকে ইউজার প্রম্পট রিসিভ করা
            user_message = await websocket.receive_text()

            # ==========================================
            # 👁️ MULTI-MODAL PAYLOAD PARSING
            # ==========================================
            try:
                payload = json.loads(user_message)
                text_prompt = payload.get("text", "")
                image_base64 = payload.get("image_base64", None)

                content_to_send = text_prompt
                if image_base64:
                    print("📸 [WS] Image payload received and decoded.")

            except json.JSONDecodeError:
                print(f"👤 [USER - Text Only]: {user_message}")
                content_to_send = user_message

            try:
                chat_history.append({"role": "user", "content": content_to_send})

                system_instructions = "You are SupremeAI, a personalized autonomous coding assistant."
                if user_prefs:
                    system_instructions += (
                        f" The user prefers: Answering Style: {user_prefs.get('answering_style', 'default')}, "
                        f"Preferred Stack: {user_prefs.get('preferred_stack', 'default')}, "
                        f"Work Type: {user_prefs.get('work_type', 'default')}."
                    )

                messages_payload = [{"role": "system", "content": system_instructions}] + chat_history

                response_stream = await llm_gateway.acompletion(
                    prompt=messages_payload,
                    task_type="chat",
                    stream=True
                )

                response_content = ""
                async for chunk in response_stream:
                    if chunk:
                        await websocket.send_text(chunk)
                        response_content += chunk
                        await asyncio.sleep(0.01)

                chat_history.append({"role": "assistant", "content": response_content})

                await websocket.send_text("[DONE]")
                print("✅ [AI]: Stream completed.")

                pref_task = asyncio.create_task(analyze_and_save_preferences(user_id, content_to_send))
                manager.track_pref_task(user_id, pref_task)

            except Exception:
                print("❌ [GENERATION ERROR]")
                await websocket.send_text("\n[Error: Neural pipeline failed]\n[DONE]")

    except WebSocketDisconnect:
        manager.disconnect(websocket)
        if user_id:
            manager.cancel_pref_tasks(user_id)
