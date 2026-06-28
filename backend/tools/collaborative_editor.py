import json
import asyncio
import os
import redis.asyncio as redis

from fastapi import APIRouter
from fastapi import WebSocket
from fastapi import WebSocketDisconnect
from loguru import logger

router = APIRouter(prefix="/collaborate", tags=["collaborative-editor"])


class CollaborativeEditor:
    def __init__(self):
        # বাংলা মন্তব্য: লোকাল কন্টেইনারে কানেক্ট হওয়া সকেট এবং তাদের ব্যাকগ্রাউন্ড লিসেনার টাস্ক ট্র্যাক করার ডিকশনারি
        self.local_sessions: dict[str, dict[str, WebSocket]] = {}
        self.redis_listeners: dict[str, asyncio.Task] = {}

        # বাংলা মন্তব্য: Redis কানেকশন সেটআপ (Upstash বা Local)
        redis_url = os.getenv("REDIS_URL", "redis://localhost:6379")
        self.redis = redis.from_url(redis_url, decode_responses=True)
        logger.info(
            "Initialized CollaborativeEditor with Redis Pub/Sub and State Persistence"
        )

    async def get_session_state(self, session_id: str) -> dict:
        """বাংলা মন্তব্য: Redis থেকে সেশনের বর্তমান ডকুমেন্ট স্টেট এবং AI কার্সরের অবস্থান নিয়ে আসবে।"""
        state_key = f"supremeai:state:{session_id}"
        state = await self.redis.hgetall(state_key)

        if not state:
            return {
                "document_state": "",
                "ai_cursor": {"position": 0, "status": "idle"},
            }

        return {
            "document_state": state.get("document_state", ""),
            "ai_cursor": json.loads(
                state.get("ai_cursor", '{"position": 0, "status": "idle"}')
            ),
        }

    async def update_session_state(self, session_id: str, updates: dict):
        """বাংলা মন্তব্য: Redis হ্যাশে সেশন স্টেট আপডেট করবে (State Persistence)।"""
        state_key = f"supremeai:state:{session_id}"
        await self.redis.hset(state_key, mapping=updates)

    async def connect_client(
        self, session_id: str, client_id: str, websocket: WebSocket
    ):
        await websocket.accept()

        if session_id not in self.local_sessions:
            self.local_sessions[session_id] = {}
            # বাংলা মন্তব্য: নতুন সেশনের জন্য একটি ব্যাকগ্রাউন্ড Redis Pub/Sub লিসেনার টাস্ক চালু করা হচ্ছে
            listener_task = asyncio.create_task(self._redis_listener(session_id))
            self.redis_listeners[session_id] = listener_task

        self.local_sessions[session_id][client_id] = websocket
        logger.info(f"Client {client_id} connected locally to session {session_id}")

        # বাংলা মন্তব্য: ইউজার কানেক্ট হওয়ার সাথে সাথে Redis থেকে সর্বশেষ স্টেট ফেচ করে তাকে পাঠানো
        current_state = await self.get_session_state(session_id)
        await websocket.send_text(
            json.dumps({"type": "sync_state", "state": current_state})
        )

    async def disconnect_client(self, session_id: str, client_id: str):
        if session_id in self.local_sessions:
            if client_id in self.local_sessions[session_id]:
                del self.local_sessions[session_id][client_id]
                logger.info(
                    f"Client {client_id} disconnected from local session {session_id}"
                )

            # বাংলা মন্তব্য: এই কন্টেইনারে সেশনের আর কোনো ইউজার না থাকলে লিসেনার বন্ধ করে মেমোরি বাঁচানো হবে
            if not self.local_sessions[session_id]:
                del self.local_sessions[session_id]
                if session_id in self.redis_listeners:
                    self.redis_listeners[session_id].cancel()
                    del self.redis_listeners[session_id]
                    logger.info(
                        f"Stopped Redis listener for session {session_id} on this instance"
                    )

    async def broadcast(self, session_id: str, message: dict, sender_id: str = None):
        """বাংলা মন্তব্য: মেসেজটি সরাসরি লোকাল সকেটে না পাঠিয়ে, Redis চ্যানেলে পাবলিশ করা হচ্ছে।"""
        if sender_id:
            message["sender_id"] = sender_id

        channel = f"supremeai:collab:{session_id}"
        await self.redis.publish(channel, json.dumps(message))

    async def broadcast_delta(
        self, session_id: str, delta: dict, sender_id: str = None
    ):
        """বাংলা মন্তব্য: CRDT মার্জিং লজিক এবং স্টেট পারসিস্টেন্স"""
        current_state = await self.get_session_state(session_id)
        doc_state = current_state["document_state"]
        ai_cursor = current_state["ai_cursor"]

        pos = delta.get("position", 0)
        insert_text = delta.get("insert", "")

        # ডকুমেন্ট আপডেট করা
        new_doc_state = doc_state[:pos] + insert_text + doc_state[pos:]

        # AI কার্সর শিফট লজিক (Operational Transformation Simulation)
        if pos <= ai_cursor.get("position", 0):
            ai_cursor["position"] += len(insert_text)

        # আপডেট হওয়া স্টেট Redis এ সেভ করা
        await self.update_session_state(
            session_id,
            {"document_state": new_doc_state, "ai_cursor": json.dumps(ai_cursor)},
        )

        message = {
            "type": "delta",
            "delta": delta,
            "document_state": new_doc_state,
            "ai_cursor": ai_cursor,
        }
        await self.broadcast(session_id, message, sender_id)

    async def trigger_ai_edit(self, session_id: str, prompt: str, client_id: str):
        # ১. AI কার্সরের স্ট্যাটাস আপডেট করা
        current_state = await self.get_session_state(session_id)
        ai_cursor = current_state.get("ai_cursor", {"position": 0})
        ai_cursor["status"] = "processing"
        await self.update_session_state(
            session_id, {"ai_cursor": json.dumps(ai_cursor)}
        )

        # ২. ফ্রন্টএন্ডে "AI is typing..." অ্যানিমেশন চালু করার সিগন্যাল পাঠানো
        message = {"type": "ai_response", "prompt": prompt, "status": "processing"}
        await self.broadcast(session_id, message, client_id)

        # ৩. মেইন থ্রেড ব্লক না করে ব্যাকগ্রাউন্ডে AI টাস্ক চালু করা
        asyncio.create_task(self._process_ai_request(session_id, prompt))

    async def _process_ai_request(self, session_id: str, prompt: str):
        """বাংলা মন্তব্য: Freebuff বা AI মডেলকে দিয়ে কোড লিখিয়ে এডিটরে পুশ করা হবে।"""
        try:
            # আমরা এখানে CloudSandboxOrchestrator ব্যবহার করছি Freebuff কে কল করার জন্য
            from tools.cloud_sandbox_orchestrator import CloudSandboxOrchestrator

            orchestrator = CloudSandboxOrchestrator()

            logger.info(f"Asking Freebuff/AI to generate code for: {prompt}")

            # AI কে দিয়ে কোড জেনারেট করানো (আপাতত আপনার Freebuff ইন্টিগ্রেশন মেথড কল করছি)
            response = await orchestrator.delegate_to_freebuff(
                prompt=f"Write python code for: {prompt}"
            )

            # জেনারেট হওয়া কোড এক্সট্র্যাক্ট করা
            if response.get("status") == "success":
                ai_generated_code = f"\n\n# --- AI Generated Code ---\n# Prompt: {prompt}\n{response.get('output', '')}\n"
            else:
                # ফলব্যাক (যদি Freebuff কাজ না করে)
                ai_generated_code = f"\n\n# --- AI Response ---\n# Executed Prompt: {prompt}\ndef auto_generated_feature():\n    print('Hello from SupremeAI!')\n"

            # বর্তমান স্টেট ফেচ করে শেষে কোড যুক্ত করা
            current_state = await self.get_session_state(session_id)
            doc_state = current_state["document_state"]
            insert_position = len(doc_state)

            # ডেল্টা তৈরি করা (কোড এডিটরে পুশ করার জন্য)
            delta = {"insert": ai_generated_code, "position": insert_position}

            # এডিটরে কোড ব্রডকাস্ট করা (সব ইউজারের কাছে চলে যাবে)
            await self.broadcast_delta(session_id, delta, sender_id="supreme-ai-agent")

        except Exception as e:
            logger.error(f"Error processing AI request: {e}")
        finally:
            # কাজ শেষ, "AI is typing..." অ্যানিমেশন বন্ধ করার সিগন্যাল পাঠানো
            await self.broadcast(
                session_id,
                {"type": "ai_response", "status": "idle"},
                sender_id="supreme-ai-agent",
            )

    async def _redis_listener(self, session_id: str):
        """বাংলা মন্তব্য: Redis চ্যানেল থেকে মেসেজ রিসিভ করে লোকাল ক্লায়েন্টদের কাছে পাঠাবে।"""
        channel = f"supremeai:collab:{session_id}"
        pubsub = self.redis.pubsub()
        await pubsub.subscribe(channel)
        logger.info(f"Subscribed to Redis channel: {channel}")

        try:
            async for message in pubsub.listen():
                if message["type"] == "message":
                    data = message["data"]
                    msg_obj = json.loads(data)
                    sender_id = msg_obj.get("sender_id")

                    if session_id in self.local_sessions:
                        for client_id, ws in self.local_sessions[session_id].items():
                            # যে মেসেজ পাঠিয়েছে তাকে ইকো না করা
                            if client_id != sender_id:
                                try:
                                    await ws.send_text(data)
                                except Exception as e:
                                    logger.error(
                                        f"Error sending to local client {client_id}: {e}"
                                    )
        except asyncio.CancelledError:
            await pubsub.unsubscribe(channel)
            logger.info(f"Unsubscribed from Redis channel: {channel}")


editor_manager = CollaborativeEditor()


@router.websocket("/ws/{session_id}/{client_id}")
async def websocket_collab(websocket: WebSocket, session_id: str, client_id: str):
    await editor_manager.connect_client(session_id, client_id, websocket)
    try:
        while True:
            data = await websocket.receive_text()
            try:
                message = json.loads(data)
                msg_type = message.get("type")

                if msg_type == "delta":
                    await editor_manager.broadcast_delta(
                        session_id, message.get("delta", {}), client_id
                    )
                elif msg_type == "ai_request":
                    prompt = message.get("prompt", "")
                    await editor_manager.trigger_ai_edit(session_id, prompt, client_id)
                elif msg_type == "cursor":
                    await editor_manager.broadcast(
                        session_id,
                        {
                            "type": "cursor",
                            "client_id": client_id,
                            "position": message.get("position", {}),
                        },
                        client_id,
                    )
            except json.JSONDecodeError:
                logger.warning(f"Invalid JSON received from client {client_id}")
    except WebSocketDisconnect:
        await editor_manager.disconnect_client(session_id, client_id)
    except Exception as e:
        logger.error(
            f"WebSocket error in session {session_id} for client {client_id}: {e}"
        )
        await editor_manager.disconnect_client(session_id, client_id)
