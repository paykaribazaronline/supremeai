import json
import secrets
from datetime import UTC, datetime
from pathlib import Path

from fastapi import APIRouter, Depends, HTTPException
from loguru import logger
from pydantic import BaseModel

from admin.god import AdminGodLayer  # Your existing god.py
from api.dependencies import get_current_admin
from core.cache.redis_manager import redis_manager
from core.health.self_healer import SelfHealerService
from utils.firestore_helpers import get_firestore_db

router = APIRouter(
    prefix="/api/admin",
    tags=["Core Admin"],
    dependencies=[Depends(get_current_admin)],
)
_db_path = str(Path(__file__).resolve().parent.parent.parent / "data" / "admin_rules.db")
god_layer = AdminGodLayer(db_path=_db_path)


def get_healer_service() -> SelfHealerService:
    db = get_firestore_db()
    if not db:
        raise HTTPException(status_code=503, detail="Database unavailable")
    return SelfHealerService(db)


class RuleUpdate(BaseModel):
    key: str
    value: str


@router.post("/rules")
async def update_constitutional_rule(payload: RuleUpdate, admin_user: dict = Depends(get_current_admin)):
    """Update God.py constitutional rules directly from the Command Center UI"""
    try:
        god_layer.set_rule(payload.key, payload.value)
        logger.critical(
            f"🔒 Constitutional rule '{payload.key}' changed to '{payload.value}' by {admin_user.get('sub')}"
        )
        return {
            "status": "success",
            "message": f"Rule {payload.key} updated to {payload.value}",
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e)) from e


@router.post("/actions/{action_type}")
async def trigger_quick_action(action_type: str, admin_user: dict = Depends(get_current_admin)):
    """Trigger 1-click Quick Actions from Dashboard"""
    # Verify if admin actions are currently allowed by god.py
    god_layer.enforce("admin_action")
    logger.critical(f"🔒 Admin quick-action '{action_type}' requested by {admin_user.get('sub')}")

    # বাংলা মন্তব্য: প্রতিটি কুইক অ্যাকশনের জন্য রিয়েল ইমপ্লিমেন্টেশন করা হয়েছে
    if action_type == "cache":
        redis_client = redis_manager.client
        if redis_client:
            # সেশন ও ওটিপি কী সুরক্ষিত রাখতে শুধুমাত্র সাধারণ ক্যাশ প্যাটার্নগুলো স্ক্যান করে ডিলেট করা হচ্ছে
            patterns = [
                "bhasha_bot:*",
                "user_profile:*",
                "user_session:*",
                "semantic_cache:*",
                "cache:*",
                "health:*",
            ]
            total_deleted = 0
            for pattern in patterns:
                keys = await redis_client.keys(pattern)
                if keys:
                    await redis_client.delete(*keys)
                    total_deleted += len(keys)
            logger.info(f"Successfully cleared {total_deleted} cache keys from Redis.")
            return {
                "status": "success",
                "message": f"Selective cache cleared. Deleted {total_deleted} keys.",
            }
        else:
            raise HTTPException(status_code=503, detail="Redis client unavailable")

    elif action_type == "backup":
        # বাংলা মন্তব্য: ডাটাবেস টেবিল স্ক্যান করে JSON ব্যাকআপ ফাইল তৈরি করার ব্যাকগ্রাউন্ড টাস্ক
        try:
            import re

            from sqlalchemy import text

            from database.session import get_db_session

            # বাংলা মন্তব্য: টেবিল নামের বৈধতা যাচাই করতে রেগুলার এক্সপ্রেশন প্যাটার্ন ডিফাইন করা হলো।
            _VALID_TABLE_PATTERN = re.compile(r"^[A-Za-z0-9_]+$")

            backup_data = {}
            async for session in get_db_session():
                result = await session.execute(
                    text("SELECT table_name FROM information_schema.tables WHERE table_schema='public'")
                )
                tables = [row[0] for row in result.fetchall()]
                for table in tables:
                    if not _VALID_TABLE_PATTERN.match(table):
                        logger.warning(f"Skipping table '{table}' due to invalid naming pattern.")
                        continue
                    rows_res = await session.execute(text(f"SELECT * FROM {table}"))
                    columns = rows_res.keys()
                    rows = [dict(zip(columns, row, strict=False)) for row in rows_res.fetchall()]
                    for row in rows:
                        for k, v in row.items():
                            if hasattr(v, "isoformat"):
                                row[k] = v.isoformat()
                    backup_data[table] = rows

            backend_dir = Path(__file__).resolve().parent.parent.parent
            backup_dir = backend_dir / "backup"
            backup_dir.mkdir(parents=True, exist_ok=True)
            backup_path = backup_dir / f"db_backup_{int(datetime.now(UTC).timestamp())}.json"

            with open(backup_path, "w", encoding="utf-8") as f:
                json.dump(backup_data, f, indent=2)

            logger.info(f"Database backup saved successfully to {backup_path}")
            return {
                "status": "success",
                "message": f"Database backup saved successfully to {backup_path.name}",
            }
        except Exception as e:
            logger.error(f"Database backup failed: {e}")
            raise HTTPException(status_code=500, detail=f"Database backup failed: {e}") from e

    elif action_type == "rollback":
        # বাংলা মন্তব্য: Alembic প্রোগ্রামাটিক রোলব্যাক মেকানিজম
        try:
            from alembic import command
            from alembic.config import Config

            alembic_cfg = Config("backend/alembic.ini")
            alembic_cfg.set_main_option("script_location", "backend/alembic")
            command.downgrade(alembic_cfg, "-1")

            logger.info("Alembic rollback to previous revision completed successfully.")
            return {
                "status": "success",
                "message": "Database rollback to previous revision executed successfully.",
            }
        except Exception as e:
            logger.error(f"Rollback failed: {e}")
            raise HTTPException(status_code=500, detail=f"Rollback operation failed: {e}") from e

    else:
        raise HTTPException(status_code=404, detail="Action not found")


@router.get("/fixes")
async def get_fixes(
    tenant_id: str = "default",
    status: str = "pending_review",
    admin_user: dict = Depends(get_current_admin),
    healer: SelfHealerService = Depends(get_healer_service),
):
    """Fetch all fixes for a tenant with a specific status."""
    db = get_firestore_db()
    fixes_ref = db.collection("tenants").document(tenant_id).collection("fixes")
    query = fixes_ref.where("status", "==", status)

    try:
        results = await query.get()
    except TypeError:
        # Fallback for sync mock
        results = query.get()

    fixes = []
    for doc in results:
        fix_data = doc.to_dict()
        fix_data["id"] = doc.id
        fixes.append(fix_data)

    return {"fixes": fixes}


@router.post("/fixes/{fix_id}/approve")
async def approve_fix(
    fix_id: str,
    tenant_id: str = "default",
    admin_user: dict = Depends(get_current_admin),
    healer: SelfHealerService = Depends(get_healer_service),
):
    """Approve a pending fix."""
    admin_id = admin_user.get("sub", "unknown_admin")
    logger.info(f"Admin {admin_id} approving fix {fix_id} for tenant {tenant_id}")

    success = await healer.apply_fix(tenant_id, fix_id, admin_id)
    if not success:
        raise HTTPException(
            status_code=400,
            detail="Failed to apply fix. It may not exist or is already processed.",
        )

    return {"status": "success", "fix_id": fix_id}


@router.post("/fixes/{fix_id}/reject")
async def reject_fix(
    fix_id: str,
    tenant_id: str = "default",
    admin_user: dict = Depends(get_current_admin),
):
    """Reject a pending fix."""
    admin_id = admin_user.get("sub", "unknown_admin")
    logger.info(f"Admin {admin_id} rejecting fix {fix_id} for tenant {tenant_id}")

    db = get_firestore_db()
    doc_ref = db.collection("tenants").document(tenant_id).collection("fixes").document(fix_id)

    update_data = {
        "status": "rejected",
        "reviewed_by": admin_id,
        "applied_at": datetime.now(UTC).isoformat(),
    }

    try:
        await doc_ref.update(update_data)
    except TypeError:
        doc_ref.update(update_data)

    return {"status": "success", "fix_id": fix_id}


class VerifyOtpRequest(BaseModel):
    code: str


@router.post("/verify-otp")
async def verify_otp(payload: VerifyOtpRequest, admin_user: dict = Depends(get_current_admin)):
    """Validate a JIT OTP issued by AntiHackingContextMiddleware and promote the
    pending (mismatched) context to trusted, so the admin isn't re-challenged
    on their next request from this IP/fingerprint.

    বাংলা: অ্যাডমিন OTP সাবমিট করলে এখানে ভ্যালিডেট হয় এবং সফল হলে Redis-এ
    ট্রাস্টেড কনটেক্সট (last_context) আপডেট হয়ে যায়।
    """
    admin_id = admin_user.get("sub", "unknown_admin")

    if not redis_manager or not redis_manager.client:
        raise HTTPException(status_code=503, detail="Security store unavailable")

    pending_key = f"security:otp_pending:{admin_id}"
    raw_pending = await redis_manager.get_cache(pending_key)
    if not raw_pending:
        raise HTTPException(
            status_code=400,
            detail="No pending verification for this admin, or it has expired",
        )

    pending = json.loads(raw_pending)

    if not secrets.compare_digest(str(pending["code"]), str(payload.code)):
        logger.warning(f"❌ Failed OTP verification attempt for admin {admin_id}")
        raise HTTPException(status_code=401, detail="Invalid code")

    # বাংলা: সফল ভেরিফিকেশনে বর্তমান (আগে মিসম্যাচড) সিগন্যালকেই নতুন ট্রাস্টেড কনটেক্সট হিসেবে সেট করা হচ্ছে
    await redis_manager.set_cache(
        f"security:last_context:{admin_id}",
        json.dumps(pending["signal"]),
        ex_seconds=86400,
    )
    await redis_manager.client.delete(pending_key)

    logger.info(f"✅ Admin {admin_id} passed OTP verification — context promoted to trusted")
    return {"status": "verified"}
