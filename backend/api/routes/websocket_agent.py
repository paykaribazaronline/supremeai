import asyncio
import base64
import io
import json
import os

import google.generativeai as genai
from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from PIL import Image

from tools.agent_tools import SUPREME_TOOLS

router = APIRouter(prefix="/ws", tags=["Neural Engine Stream"])

# ==========================================
# ⚙️ GEMINI AI & TOOLS CONFIGURATION
# ==========================================
API_KEY = os.getenv("SUPREMEAI_API_KEY") or os.getenv("GEMINI_API_KEY")
if API_KEY:
    genai.configure(api_key=API_KEY)
    
# মডেল ইনিশিয়ালাইজ করা এবং টুলস যুক্ত করা
model = genai.GenerativeModel(
    model_name='gemini-1.5-pro',
    tools=SUPREME_TOOLS, # AI এখন এই ফাংশনগুলোর অস্তিত্ব জানে!
)

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

manager = ConnectionManager()

# ==========================================
# 🚀 ROUTE: /ws/chat
# ==========================================
@router.websocket("/chat")
async def websocket_chat_endpoint(websocket: WebSocket):
    """
    Real-time bidirectional WebSocket for Token-by-Token streaming and Agentic Tool execution.
    Supports both plain text (Flutter) and JSON payloads with base64 images (Web Chat).
    """
    await manager.connect(websocket)
    
    # সেশন হিস্ট্রি মেইনটেইন করার জন্য চ্যাট অবজেক্ট তৈরি করা
    chat_session = model.start_chat(enable_automatic_function_calling=True)

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
                
                content_to_send = [text_prompt] if text_prompt else []
                
                if image_base64:
                    # Strip the data URL prefix if it exists (e.g., "data:image/jpeg;base64,")
                    if "," in image_base64:
                        image_base64 = image_base64.split(",")[1]
                        
                    image_data = base64.b64decode(image_base64)
                    image_obj = Image.open(io.BytesIO(image_data))
                    content_to_send.append(image_obj)
                    print("📸 [WS] Image payload received and decoded.")
                    
                # If only text was sent in JSON
                if len(content_to_send) == 1 and isinstance(content_to_send[0], str):
                    content_to_send = content_to_send[0]

            except json.JSONDecodeError:
                # Fallback to plain text (Existing Flutter Client)
                print(f"👤 [USER - Text Only]: {user_message}")
                content_to_send = user_message

            if not API_KEY:
                await websocket.send_text("⚠️ API Key is missing! Cannot process request.\n[DONE]")
                continue

            try:
                # ২. AI-কে প্রম্পট পাঠানো (Stream = True)
                response = await chat_session.send_message_async(
                    content_to_send, 
                    stream=True
                )

                # ৩. Token-by-Token স্ট্রিম করে ফ্রন্টএন্ডে পাঠানো
                async for chunk in response:
                    if chunk.text:
                        # প্রতিটি টেক্সট চাঙ্ক পাওয়ার সাথে সাথে ক্লায়েন্টকে পাঠানো হচ্ছে
                        await websocket.send_text(chunk.text)
                        
                        # খুব সামান্য ডিলি দেওয়া হচ্ছে যাতে UI-তে টাইপিং অ্যানিমেশন স্মুথ হয়
                        await asyncio.sleep(0.01) 
                
                # ৪. রেসপন্স শেষ বোঝাতে একটি সিগন্যাল পাঠানো
                await websocket.send_text("[DONE]")
                print("✅ [AI]: Stream completed.")

            except Exception as e:
                print(f"❌ [GENERATION ERROR]: {e}")
                await websocket.send_text(f"\n[Error: Neural pipeline failed - {str(e)}]\n[DONE]")

    except WebSocketDisconnect:
        manager.disconnect(websocket)
