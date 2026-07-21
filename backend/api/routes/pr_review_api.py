"""
PR Review API Routes
====================

GitHub Webhook রিসিভার ও রিভিউ স্ট্যাটাস এন্ডপয়েন্ট।

Endpoints:
  POST /api/v1/pr-review/webhook — GitHub webhook (PR opened/updated)
  GET  /api/v1/pr-review/{pr_id}/status — রিভিউ স্ট্যাটাস
"""

from __future__ import annotations

import hashlib
import hmac
import time
from typing import Any

from fastapi import APIRouter, HTTPException, Request
from loguru import logger
from pydantic import BaseModel

router = APIRouter(prefix="/api/v1/pr-review", tags=["pr-review"])

# বাংলা মন্তব্য: ইন-মেমরি রিভিউ স্ট্যাটাস স্টোর (পরবর্তীতে DB-তে পারসিস্ট করা যাবে)।
_review_status: dict[str, dict[str, Any]] = {}


class WebhookPayload(BaseModel):
    action: str | None = None
    repository: dict[str, Any] | None = None
    pull_request: dict[str, Any] | None = None


def _verify_signature(
    payload: bytes, signature: str | None, secret: str | None
) -> bool:
    """GitHub webhook signature যাচাই করে (HMAC-SHA256)।"""
    if not secret:
        return True  # বাংলা মন্তব্য: secret কনফিগ না থাকলে স্কিপ করা হচ্ছে (dev mode)।
    if not signature:
        return False
    expected = hmac.new(secret.encode("utf-8"), payload, hashlib.sha256).hexdigest()
    provided = signature.replace("sha256=", "")
    return hmac.compare_digest(expected, provided)


@router.post("/webhook")
async def github_webhook(request: Request):
    """GitHub থেকে PR webhook রিসিভ করে অটো-রিভিউ ট্রিগার করে।"""
    from core.config import settings

    body = await request.body()
    signature = request.headers.get("X-Hub-Signature-256")
    secret = getattr(settings, "github_webhook_secret", None)

    if not _verify_signature(body, signature, secret):
        raise HTTPException(status_code=401, detail="Invalid webhook signature")

    try:
        import json

        event = json.loads(body or b"{}")
    except Exception as e:  # noqa: BLE001
        raise HTTPException(status_code=400, detail=f"Invalid JSON: {e}") from e

    action = event.get("action", "")
    pr = event.get("pull_request")
    repo = event.get("repository")

    # বাংলা মন্তব্য: শুধুমাত্র PR opened/ready_for_review/synchronize ইভেন্টে রিভিউ করা হচ্ছে।
    if (
        action not in ("opened", "ready_for_review", "synchronize", "reopened")
        or not pr
        or not repo
    ):
        return {"status": "ignored", "action": action}

    repo_full_name = repo.get("full_name", "")
    pr_number = pr.get("number")

    try:
        from backend.tools.pr_reviewer import PRReviewer

        reviewer = PRReviewer()
        result = await reviewer.review_pr(repo_full_name, pr_number)
        status_key = f"{repo_full_name}#{pr_number}"
        _review_status[status_key] = {
            "status": result.get("status"),
            "action_taken": result.get("action_taken"),
            "comments_count": len(result.get("comments", [])),
            "timestamp": time.time(),
        }
        return {"status": "reviewed", "pr": status_key, "result": result}
    except Exception as e:  # noqa: BLE001
        logger.error(f"Webhook review failed: {e}")
        raise HTTPException(status_code=500, detail=str(e)) from e


@router.get("/{pr_id}/status")
async def get_review_status(pr_id: str) -> dict[str, Any]:
    """নির্দিষ্ট PR-এর রিভিউ স্ট্যাটাস ফেরত দেয়।"""
    status = _review_status.get(pr_id)
    if not status:
        raise HTTPException(status_code=404, detail=f"No review found for {pr_id}")
    return {"pr_id": pr_id, **status}
