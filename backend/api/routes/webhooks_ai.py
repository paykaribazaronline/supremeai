# backend/api/routes/webhooks_ai.py
"""
SupremeAI Interactive Telegram/Slack AI Webhook Router
Handles outgoing alerts with inline approval buttons and incoming callbacks.
"""

from typing import Any

from core.config import settings
from fastapi import APIRouter, HTTPException, status
from loguru import logger
from pydantic import BaseModel

router = APIRouter(prefix="/api/v1/webhooks/telegram", tags=["Webhooks AI"])


class AlertPayload(BaseModel):
    title: str
    description: str
    patch_code: str | None = None
    pr_id: str | None = None
    severity: str = "WARNING"


class CallbackQuery(BaseModel):
    callback_id: str
    user_id: str
    action: str  # "approve_pr" or "reject_pr"
    pr_id: str


@router.post("/send-alert")
async def send_telegram_alert(payload: AlertPayload) -> dict[str, Any]:
    """
    Send formatted alert message with inline buttons to Telegram chat.
    """
    inline_keyboard = [
        [
            {
                "text": "✅ Approve PR & Merge",
                "callback_data": f"approve_pr:{payload.pr_id or 'PR-001'}",
            },
            {
                "text": "❌ Reject",
                "callback_data": f"reject_pr:{payload.pr_id or 'PR-001'}",
            },
        ]
    ]

    message_text = (
        f"⚠️ <b>{payload.severity}: {payload.title}</b>\n\n"
        f"{payload.description}\n\n"
    )
    if payload.patch_code:
        message_text += (
            f"<b>Suggested Fix:</b>\n<code>{payload.patch_code[:500]}</code>\n"
        )

    logger.info(
        f"📢 [Telegram Webhook] Outgoing alert: {payload.title} (PR: {payload.pr_id})"
    )

    # Mock response or actual httpx call to Telegram Bot API
    return {
        "status": "sent",
        "chat_id": getattr(settings, "TELEGRAM_CHAT_ID", "mock_chat_123"),
        "message": message_text,
        "inline_keyboard": inline_keyboard,
    }


@router.post("/callback")
async def handle_telegram_callback(query: CallbackQuery) -> dict[str, Any]:
    """
    Handle user interaction on Telegram inline keyboard buttons.
    """
    logger.info(
        f"📥 [Telegram Callback] Action '{query.action}' by User '{query.user_id}' on PR '{query.pr_id}'"
    )

    if query.action == "approve_pr":
        # Trigger PR Pipeline / Auto-merge logic
        result = {
            "status": "approved",
            "pr_id": query.pr_id,
            "message": f"✅ PR {query.pr_id} approved by user {query.user_id}. Auto-merge initiated.",
        }
    elif query.action == "reject_pr":
        result = {
            "status": "rejected",
            "pr_id": query.pr_id,
            "message": f"❌ PR {query.pr_id} rejected by user {query.user_id}.",
        }
    else:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unknown action: {query.action}",
        )

    return result
