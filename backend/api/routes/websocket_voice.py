import asyncio
import json
import time

import httpx
from fastapi import APIRouter
from fastapi import Query
from fastapi import WebSocket
from fastapi import WebSocketDisconnect
from fastapi import status

from core.config import settings
from core.security import verify_token
from database.supabase_client import db
from models.voice_interaction import VoiceInteractionLog


router = APIRouter(prefix="/ws", tags=["Voice Engine Stream"])


class VoiceConnectionManager:
    def __init__(self):
        self.active_connections: list[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)
        print("🟢 [WS] Voice Client Connected to SupremeAI Nexus.")

    def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)
        print("🔴 [WS] Voice Client Disconnected.")

    async def _authenticate(self, websocket: WebSocket) -> dict | None:
        token = websocket.query_params.get("token")
        if not token:
            return None
        try:
            return verify_token(token)
        except Exception:
            return None

manager = VoiceConnectionManager()

async def process_audio_with_groq(audio_bytes: bytes) -> str:
    """
    Sends the audio buffer to Groq's Whisper API for ultra-fast STT.
    """
    if not settings.groq_api_key:
        return "Error: GROQ_API_KEY is missing."

    url = "https://api.groq.com/openai/v1/audio/transcriptions"
    headers = {"Authorization": f"Bearer {settings.groq_api_key}"}
    
    files = {
        "file": ("audio.webm", audio_bytes, "audio/webm")
    }
    data = {
        "model": "whisper-large-v3",
        "response_format": "json"
    }

    async with httpx.AsyncClient() as client:
        try:
            response = await client.post(url, headers=headers, files=files, data=data, timeout=10.0)
            response.raise_for_status()
            result = response.json()
            return result.get("text", "")
        except Exception as e:
            print(f"❌ [Groq STT Error]: {e}")
            return f"Error processing audio: {str(e)}"

async def handle_intent(transcript: str, websocket: WebSocket, start_time: float):
    # Intent Router
    transcript_clean = transcript.strip()
    
    # Check if it's a command
    if transcript_clean.startswith("/"):
        supremeai_response = f"Executing system command: {transcript_clean}... Authorization confirmed."
    else:
        # Natural Language Processing (Simulating conversational Groq/LLM)
        supremeai_response = (
            f"Hello! You said: '{transcript_clean}'. I am Aethel, "
            "your SupremeAI orchestrator. How can I assist you with the cluster today?"
        )
        
    # Log to database
    if db.client:
        latency_ms = int((time.time() - start_time) * 1000)
        log_entry = VoiceInteractionLog(
            user_id="admin-01",
            transcript=transcript_clean,
            supremeai_response=supremeai_response,
            latency_ms=latency_ms
        )
        try:
            db.client.table("voice_interactions").insert(log_entry.dict(exclude_none=True)).execute()
        except Exception as db_err:
            print(f"⚠️ [DB Logging Error]: {db_err}")
    
    # Stream text response back for Web Speech API TTS
    words = supremeai_response.split(" ")
    for word in words:
        await websocket.send_json({"type": "response_chunk", "text": word + " "})
        await asyncio.sleep(0.05)
    
    await websocket.send_json({"type": "response_complete"})

@router.websocket("/voice")
async def websocket_voice_endpoint(
    websocket: WebSocket,
    token: str | None = Query(default=None),
):
    """
    Real-time WebSocket for Voice-to-Voice Streaming.
    Receives binary audio chunks, uses Groq Whisper for STT, and returns the response.
    """
    auth_payload = await manager._authenticate(websocket)
    if not auth_payload:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
        return

    await manager.connect(websocket)

    # Store audio chunks in memory
    audio_buffer = bytearray()
    start_time = time.time()

    try:
        while True:
            # We accept both text (commands like {"action": "process"}) and binary (audio chunks)
            message = await websocket.receive()

            if "bytes" in message:
                # Append binary chunk to buffer
                audio_buffer.extend(message["bytes"])
            elif "text" in message:
                try:
                    payload = json.loads(message["text"])
                    action = payload.get("action")

                    if action == "process":
                        if len(audio_buffer) == 0:
                            await websocket.send_json({"error": "Empty audio buffer"})
                            continue

                        # 1. Process STT using Groq
                        print(f"🎙️ [WS] Processing audio buffer ({len(audio_buffer)} bytes)...")
                        transcript = await process_audio_with_groq(bytes(audio_buffer))
                        print(f"🗣️ [User Voice]: {transcript}")

                        # Clear buffer for next recording
                        audio_buffer.clear()

                        # Send transcript to UI
                        await websocket.send_json({"type": "transcript", "text": transcript})

                        # 2. Intent Router
                        await handle_intent(transcript, websocket, start_time)
                        start_time = time.time() # Reset timer

                    elif action == "text_chat":
                        transcript = payload.get("text", "")
                        print(f"💬 [User Text]: {transcript}")

                        # Process text intent directly
                        await handle_intent(transcript, websocket, start_time)
                        start_time = time.time() # Reset timer

                except json.JSONDecodeError:
                    print("⚠️ [WS] Received invalid text message.")

    except WebSocketDisconnect:
        manager.disconnect(websocket)
    except Exception as e:
        print(f"❌ [WS Voice Engine Error]: {e}")
        manager.disconnect(websocket)
