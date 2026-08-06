from __future__ import annotations

from core.error_bus import with_error_bus
from core.messaging.event_bus import ErrorContext

"""
SupremeAI 2.0 — Telegram Bot Handler (Production-Ready)

Features:
- Webhook support (recommended for production)
- Polling mode fallback (dev/local)
- /start, /help, /status, /admin commands
- Auto-routes to SupremeOrchestrator

Setup:
  1. Get token from @BotFather → /newbot
  2. Set TELEGRAM_BOT_TOKEN in .env
  3. For webhook: set TELEGRAM_WEBHOOK_URL = https://your-domain.com/telegram/webhook
  4. Register webhook:
     curl "https://api.telegram.org/bot<TOKEN>/setWebhook?url=<WEBHOOK_URL>"
"""


import asyncio
import contextlib

# বাংলা মন্তব্য: ওএস মডিউল ইম্পোর্ট করা হলো যাতে os.environ ঠিকমত কাজ করে
import os
from typing import Any, ClassVar

import httpx
from loguru import logger

from core.config import settings


class TelegramBotHandler:
    """
    Production Telegram Bot — handles messages via webhook payload.
    Integrates with SupremeOrchestrator for AI responses.
    """

    COMMANDS: ClassVar[dict[str, str]] = {
        "/start": "👋 Welcome to *SupremeAI 2.0*!\nSend any message and I'll respond with AI power.",
        "/help": (
            "📖 *Commands:*\n"
            "/start — Welcome\n"
            "/help  — Show this help\n"
            "/status — System health check\n"
            "/admin — Admin menu (authorized only)\n\n"
            "Or just type anything and I'll handle it!"
        ),
        "/admin": "🔐 *Admin menu:*\n/rules /limit /block /unblock",
        "/rules": "📜 Constitutional rules: 5 directions (North, South, East, West, Center)",
    }

    def __init__(self, task_processor_interface=None) -> None:
        # বাংলা মন্তব্য: settings থেকে না পাওয়া গেলে os.environ থেকে fallback রিড করা হবে, যা টেস্ট কেসগুলোর জন্য সুবিধাজনক।
        self.bot_token: str = getattr(settings, "telegram_bot_token", None) or os.environ.get("TELEGRAM_BOT_TOKEN", "")
        self.api_base: str = f"https://api.telegram.org/bot{self.bot_token}"
        self.processor = task_processor_interface

        if not self.bot_token:
            logger.warning("TELEGRAM_BOT_TOKEN not set — Telegram bot disabled.")

    @property
    def configured(self) -> bool:
        return bool(self.bot_token and self.bot_token != "mock_token")

    # ── Telegram API helpers ──────────────────────────────────────

    async def send_message(self, chat_id: int | str, text: str, parse_mode: str = "Markdown") -> bool:
        if not self.configured:
            return False
        try:
            async with httpx.AsyncClient(timeout=10) as client:
                resp = await client.post(
                    f"{self.api_base}/sendMessage",
                    json={"chat_id": chat_id, "text": text, "parse_mode": parse_mode},
                )
                resp.raise_for_status()
                return True
        except Exception as exc:
            logger.error(f"Telegram sendMessage failed: {exc}")
            return False

    async def send_typing(self, chat_id: int | str) -> None:
        if not self.configured:
            return
        try:
            async with httpx.AsyncClient(timeout=5) as client:
                await client.post(
                    f"{self.api_base}/sendChatAction",
                    json={"chat_id": chat_id, "action": "typing"},
                )
        except Exception as e:
            logger.error(f"Telegram sendTyping failed for chat_id {chat_id}: {e}")

    async def set_webhook(self, webhook_url: str) -> bool:
        """Register webhook URL with Telegram."""
        if not self.configured:
            return False
        try:
            async with httpx.AsyncClient(timeout=10) as client:
                resp = await client.post(
                    f"{self.api_base}/setWebhook",
                    json={
                        "url": webhook_url,
                        "allowed_updates": ["message", "callback_query"],
                    },
                )
                data = resp.json()
                if data.get("ok"):
                    logger.info(f"✅ Telegram webhook set: {webhook_url}")
                    return True
                logger.error(f"Webhook error: {data}")
                return False
        except Exception as exc:
            logger.error(f"set_webhook failed: {exc}")
            return False

    async def get_me(self) -> dict[str, Any] | None:
        """Verify bot token and get bot info."""
        if not self.configured:
            return None
        try:
            async with httpx.AsyncClient(timeout=10) as client:
                resp = await client.get(f"{self.api_base}/getMe")
                data = resp.json()
                return data.get("result") if data.get("ok") else None
        except Exception as exc:
            logger.error(f"getMe failed: {exc}")
            return None

    # ── Message handling ──────────────────────────────────────────

    # ── Synchronous convenience wrapper ───────────────────────────

    def handle_message(self, text: str, user_id: str = "user") -> str:
        """Synchronous message handler used by tests and scripts."""
        command = text.strip().split()[0].lower() if text.strip().startswith("/") else None
        if command and command in self.COMMANDS:
            return self.COMMANDS[command]
        try:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            return loop.run_until_complete(self._ai_response(text, user_id))
        finally:
            with contextlib.suppress(Exception):
                loop.close()

    async def handle_update(self, update: dict[str, Any]) -> None:
        """Process a Telegram update payload (from webhook or polling)."""
        message = update.get("message")
        if not message:
            return

        chat_id: int = message["chat"]["id"]
        text: str = message.get("text", "").strip()
        user_id: str = str(message["from"]["id"])
        username: str = message["from"].get("username", user_id)

        logger.info(f"Telegram message from @{username} ({user_id}): '{text}'")

        # Command handling
        command = text.split(maxsplit=1)[0].lower() if text.startswith("/") else None
        if command:
            reply = self.COMMANDS.get(command)
            if reply:
                await self.send_message(chat_id, reply)
                return
            if command == "/status":
                await self._handle_status(chat_id)
                return

        # AI fallback
        await self.send_typing(chat_id)
        ai_response = await self._ai_response(text, user_id)
        await self.send_message(chat_id, ai_response)

    async def _handle_status(self, chat_id: int | str) -> None:
        import httpx as _httpx

        gcp_url = getattr(settings, "gcp_cloud_run_url", "")
        status_lines = ["🔍 *System Status:*\n"]
        for name, url in [
            ("GCP", gcp_url),
            ("Railway", getattr(settings, "railway_url", "")),
            ("Render", getattr(settings, "render_url", "")),
        ]:
            if not url:
                status_lines.append(f"⚪ {name}: not configured")
                continue
            try:
                async with _httpx.AsyncClient(timeout=5) as c:
                    r = await c.get(url + "/health")
                    icon = "✅" if r.status_code == 200 else "⚠️"
                    status_lines.append(f"{icon} {name}: `{r.status_code}`")
            except Exception as e:
                logger.error(f"Health check failed for {name}: {e}")
                status_lines.append(f"❌ {name}: unreachable")
        await self.send_message(chat_id, "\n".join(status_lines))

    async def _ai_response(self, text: str, user_id: str) -> str:
        if self.processor:
            try:
                task_type = "coding" if any(k in text.lower() for k in ["code", "function", "script"]) else "general"
                loop = asyncio.get_event_loop()
                # Run synchronous orchestrator call in executor to prevent blocking the event loop
                result = await loop.run_in_executor(None, lambda: self.processor.execute_task(text, task_type))
                return result.get("result", "Sorry, I couldn't process that.")
            except Exception as exc:
                logger.error(f"Orchestrator error: {exc}")
                return "⚠️ Error processing request. Please try again."
        return "🤖 SupremeAI 2.0 is ready! (Orchestrator not connected)"

    # ── Polling mode (dev/local) ─────────────────────────────────

    async def run_polling(self) -> None:
        """Long-polling loop — use only in local/dev mode."""
        if not self.configured:
            logger.warning("Telegram bot not configured — skipping polling.")
            return

    @with_error_bus("start_webhook")
    async def start_webhook(self, webhook_url: str):
        """বাংলা মন্তব্য: while True: sleep() পোলিং লুপ বাদ দিয়ে Event-Driven Webhook মডেলে মাইগ্রেট করা হলো।"""
        if not self.bot_token:
            return

        url = f"https://api.telegram.org/bot{self.bot_token}/setWebhook"
        try:
            import httpx

            from core.messaging.event_bus import ErrorEvent, error_event_bus

            # সংশোধন: Explicit timeout যোগ করা হয়েছে
            async with httpx.AsyncClient(timeout=httpx.Timeout(10.0, connect=5.0)) as client:
                resp = await client.post(url, json={"url": webhook_url})
                if resp.status_code == 200:
                    logger.info(f"Successfully registered Telegram Webhook to {webhook_url}")
                else:
                    logger.error(f"Failed to register webhook: {resp.text}")
                    error_event_bus.emit(
                        ErrorEvent(
                            module="telegram_bot",
                            error_type="WEBHOOK_FAILED",
                            message=resp.text[:200],
                            severity="ERROR",
                            structured_context=ErrorContext(module="auto_fixed"),
                        )
                    )
        except Exception as e:
            logger.error(f"Webhook setup exception: {e}")
            from core.messaging.event_bus import ErrorEvent, error_event_bus

            error_event_bus.emit(
                ErrorEvent(
                    module="telegram_bot",
                    error_type="WEBHOOK_EXCEPTION",
                    message=str(e)[:200],
                    severity="ERROR",
                    structured_context=ErrorContext(module="auto_fixed"),
                )
            )
            raise RuntimeError("Failed to setup Telegram webhook.") from e


# ── FastAPI webhook endpoint helper ──────────────────────────────


def create_telegram_router(handler: TelegramBotHandler):
    """Returns a FastAPI router for Telegram webhook endpoint."""
    from fastapi import APIRouter, Request, Response

    router = APIRouter(prefix="/telegram", tags=["telegram"])

    @router.post("/webhook")
    async def telegram_webhook(request: Request):
        update = await request.json()
        from core.utils.background_tasks import track_task

        track_task(asyncio.create_task(handler.handle_update(update)))
        return Response(status_code=200)

    @router.get("/health")
    async def telegram_health():
        me = await handler.get_me()
        return {"configured": handler.configured, "bot": me}

    return router


# ── Standalone entrypoint ─────────────────────────────────────────

if __name__ == "__main__":
    handler = TelegramBotHandler()
    asyncio.run(handler.run_polling())
