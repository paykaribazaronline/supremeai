import os
import hmac
import hashlib
from typing import Optional, Dict, Any
from loguru import logger
from fastapi import APIRouter, Request, HTTPException, BackgroundTasks
from pydantic import BaseModel

router = APIRouter(prefix="/cdc", tags=["cdc"])

SUPABASE_WEBHOOK_SECRET = os.getenv("SUPABASE_WEBHOOK_SECRET", "")
PINECONE_API_KEY = os.getenv("PINECONE_API_KEY", "")
PINECONE_HOST = os.getenv("PINECONE_HOST", "")

class CDCEvent(BaseModel):
    type: str
    table: str
    record: Dict[str, Any]
    old_record: Optional[Dict[str, Any]] = None

async def _verify_webhook_signature(request: Request, body: bytes) -> bool:
    if not SUPABASE_WEBHOOK_SECRET:
        return True
    signature = request.headers.get("x-supabase-signature", "")
    if not signature:
        return False
    expected = hmac.new(
        SUPABASE_WEBHOOK_SECRET.encode(),
        body,
        hashlib.sha256,
    ).hexdigest()
    return hmac.compare_digest(f"sha256={expected}", signature)

async def _delete_from_vector_db(user_id: str, doc_id: typing.Optional[str] = None) -> None:
    if not PINECONE_API_KEY or not PINECONE_HOST:
        return
    try:
        import httpx
        if doc_id:
            vector_id = f"{user_id}:{doc_id}"
        else:
            vector_id = user_id
        
        async with httpx.AsyncClient(timeout=10.0) as client:
            await client.request(
                "DELETE",
                f"https://{PINECONE_HOST}/vectors/delete",
                headers={"Api-Key": PINECONE_API_KEY},
                json={"ids": [vector_id]},
            )
        logger.info(f"CDC: Deleted vector {vector_id} from Pinecone")
    except Exception as e:
        logger.error(f"CDC vector deletion failed: {e}")

@router.post("/webhook")
async def handle_cdc_webhook(request: Request, background_tasks: BackgroundTasks):
    body = await request.body()
    
    if not await _verify_webhook_signature(request, body):
        raise HTTPException(status_code=401, detail="Invalid webhook signature")
    
    try:
        import json
        event = json.loads(body)
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid JSON payload")
    
    event_type = event.get("type")
    table = event.get("table")
    record = event.get("record", {})
    old_record = event.get("old_record", {})
    
    logger.info(f"CDC Event received: {event_type} on {table}")
    
    if event_type in ("DELETE", "INSERT", "UPDATE", "DELETE"):
        user_id = record.get("user_id") or old_record.get("user_id")
        doc_id = record.get("id") or old_record.get("id")
        
        background_tasks.add_task(_delete_from_vector_db, user_id, doc_id)
    
    return {"status": "accepted"}

@router.get("/health")
async def cdc_health():
    return {
        "status": "ok",
        "events_processed": "active",
        "vector_sync": "enabled" if PINECONE_API_KEY else "disabled",
    }