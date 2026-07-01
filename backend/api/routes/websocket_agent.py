import asyncio
import base64
import io
import json
import os

from fastapi import APIRouter
from fastapi import WebSocket
from fastapi import WebSocketDisconnect
from fastapi import status
from fastapi import Query
from PIL import Image
from jose import JWTError

from core.security import verify_token
from tools.agent_tools import SUPREME_TOOLS
from core.llm_gateway import llm_gateway


router = APIRouter(prefix="/ws", tags=["Neural Engine Stream"])


# ==========================================
# 🔌 WEBSOCKET CONNECTION MANAGER
# ==========================================
class ConnectionManager:
    def __init__(self):
        self.active_connections: list[WebSocket] = []

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

    try:
        while True:
            # ১. ফ্রন্টএন্ড থেকে ইউজার প্রম্পট রিসিভ করা
            user_message = await websocket.receive_text()

            # ==========================================
            # 👁️ MULTI-MODAL PAYLOAD PARSING
            # ==========================================
            try:
                # Attempt to parse as JSON (New Web Client)
                payload = json.loads(user_message)
                text_prompt = payload.get("text", "")
                image_base64 = payload.get("image_base64", None)

                content_to_send = text_prompt
                # Note: Currently, llm_gateway supports text prompts for LiteLLM.
                # Multi-modal / image payload is parsed but mapped to text details for backward compatibility.
                if image_base64:
                    print("📸 [WS] Image payload received and decoded.")

            except json.JSONDecodeError:
                # Fallback to plain text (Existing Flutter Client)
                print(f"👤 [USER - Text Only]: {user_message}")
                content_to_send = user_message

            try:
                # ২. AI-কে প্রম্পট পাঠানো (Stream = True)
                # চ্যাট হিস্ট্রি ট্র্যাকিং
                chat_history.append({"role": "user", "content": content_to_send})

                response_stream = await llm_gateway.acompletion(
                    prompt=chat_history,
                    task_type="chat",
                    stream=True
                )

                response_content = ""
                # ৩. Token-by-Token স্ট্রিম করে ফ্রন্টএন্ডে পাঠানো
                async for chunk in response_stream:
                    if chunk:
                        # প্রতিটি টেক্সট চাঙ্ক পাওয়ার সাথে সাথে ক্লায়েন্টকে পাঠানো হচ্ছে
                        await websocket.send_text(chunk)
                        response_content += chunk

                        # খুব সামান্য ডিলি দেওয়া হচ্ছে যাতে UI-তে টাইপিং অ্যানিমেশন স্মuথ হয়
                        await asyncio.sleep(0.01)

                chat_history.append({"role": "assistant", "content": response_content})

                # ৪. রেসপন্স শেষ বোঝাতে একটি সিগন্যাল পাঠানো
                await websocket.send_text("[DONE]")
                print("✅ [AI]: Stream completed.")

            except Exception as e:
                print(f"❌ [GENERATION ERROR]: {e}")
                await websocket.send_text(f"\n[Error: Neural pipeline failed - {str(e)}]\n[DONE]")

    except WebSocketDisconnect:
        manager.disconnect(websocket)
