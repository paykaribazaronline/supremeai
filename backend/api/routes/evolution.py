import json
import os
import secrets
import shutil
import time
from datetime import datetime, timezone
from pathlib import Path

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import jwt
from loguru import logger
from pydantic import BaseModel, Field
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from api.dependencies import get_tenant_db
from core.config import settings
from core.tenant_db import TenantAwareFirestore
from evolution.auto_skill_creator import AutoSkillCreator
from evolution.fitness_engine import FitnessEngine
from database.session import get_db_session
from models.evolution import CodeProposal


router = APIRouter(prefix="/api/evolution", tags=["self-evolution-engine"])

security = HTTPBearer()


def require_admin_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token = credentials.credentials
    try:
        jwt_secret = settings.jwt_secret
        decoded = jwt.decode(token, jwt_secret, algorithms=["HS256"])
        if decoded.get("role") != "admin":
            raise HTTPException(
                status_code=403, detail="Forbidden: User does not have admin role."
            )
        return decoded
    except Exception as e:
        expected = os.getenv("SUPREMEAI_API_TOKEN") or ""
        if expected and secrets.compare_digest(token, expected):
            return {"uid": "admin", "role": "admin"}
        raise HTTPException(
            status_code=401, detail=f"Invalid Admin Authorization Token: {str(e)}"
        ) from e


@router.get("/logs")
async def get_evolution_logs(admin: dict = Depends(require_admin_token)):
    try:
        from database.supabase_client import db

        if db.client:
            logs = db.get_evolution_logs(limit=500)
            return {"logs": logs}
    except Exception:
        pass

    base_dir = Path(__file__).resolve().parent.parent.parent
    log_path = base_dir / "backend" / "data" / "evolution_logs.jsonl"
    if not log_path.exists():
        return {"logs": []}
    try:
        with open(log_path, encoding="utf-8") as f:
            lines = f.readlines()
        logs = [json.loads(line) for line in lines if line.strip()]
        return {"logs": logs}
    except Exception as e:
        logger.error(f"Failed to read evolution logs: {e}")
        raise HTTPException(
            status_code=500, detail="Failed to read evolution logs"
        ) from e


class EvolutionRequest(BaseModel):
    skill_name: str
    user_demand: str


@router.post("/forge")
async def forge_dynamic_skill(
    payload: EvolutionRequest, db: TenantAwareFirestore = Depends(get_tenant_db)
):
    """
    On-the-fly AI Skill Generation and Sandbox Deployed Gate.
    """
    creator = AutoSkillCreator(db=db)
    result = await creator.generate_and_deploy_skill(
        user_demand=payload.user_demand, skill_name=payload.skill_name
    )

    if not result["success"]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, detail=result["error"]
        )

    return result


class QuarantineRequest(BaseModel):
    skill_name: str = Field(..., min_length=1, max_length=200)


@router.post("/quarantine")
async def quarantine_skill(
    payload: QuarantineRequest,
    admin: dict = Depends(require_admin_token),
    fitness_engine: FitnessEngine | None = Depends(FitnessEngine),
):
    skill_name = payload.skill_name.strip()
    try:
        skill_data = fitness_engine.registry.get_skill(skill_name)
        if skill_data is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Skill not found in registry",
            )
        skill_data["status"] = "QUARANTINED"
        fitness_engine.registry.skills["skills"][skill_name] = skill_data
        with open(fitness_engine.registry.registry_path, "w", encoding="utf-8") as f:
            json.dump(fitness_engine.registry.skills, f, indent=4)
        base_dir = Path(__file__).resolve().parent.parent.parent
        src = base_dir / "skills" / "dynamic" / skill_name
        dst = base_dir / "skills" / "quarantine" / skill_name
        if src.exists():
            (base_dir / "skills" / "quarantine").mkdir(parents=True, exist_ok=True)
            if dst.exists():
                shutil.rmtree(dst)
            shutil.move(str(src), str(dst))
            logger.info(f"Skill '{skill_name}' quarantined: {src} -> {dst}")
        else:
            logger.info(
                f"Skill '{skill_name}' marked QUARANTINED in registry (no dynamic directory found)"
            )
        base_dir_for_logs = Path(__file__).resolve().parent.parent.parent
        log_path = base_dir_for_logs / "backend" / "data" / "evolution_logs.jsonl"
        try:
            from database.supabase_client import db as db_client

            if db_client.client:
                db_client.append_evolution_log(
                    {
                        "event": {
                            "action": "quarantine",
                            "skill_name": skill_name,
                            "admin_uid": admin.get("uid"),
                            "timestamp": time.time(),
                        },
                        "created_at": datetime.now(timezone.utc).isoformat(),
                    }
                )
        except Exception as db_err:
            logger.warning(f"Failed to log quarantine action to Supabase: {db_err}")

        try:
            with open(log_path, "a", encoding="utf-8") as f:
                f.write(
                    json.dumps(
                        {
                            "action": "quarantine",
                            "skill_name": skill_name,
                            "admin_uid": admin.get("uid"),
                            "timestamp": time.time(),
                        },
                        default=str,
                    )
                    + "\n"
                )
        except Exception as log_err:
            logger.warning(f"Failed to append quarantine log: {log_err}")
        return {"success": True, "skill_name": skill_name, "new_status": "QUARANTINED"}
    except HTTPException:
        raise
    except Exception as exc:
        logger.exception(f"Quarantine failed for '{skill_name}'")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Quarantine failed",
        ) from exc


# 🛑 ZERO-GAP: Admin Evolution Proposals API Routing
@router.get("/proposals")
async def list_proposals(
    admin: dict = Depends(require_admin_token),
    session: AsyncSession = Depends(get_db_session)
):
    """
    List all pending AI code proposals for admin review.
    """
    result = await session.execute(
        select(CodeProposal).order_by(CodeProposal.created_at.desc())
    )
    proposals = result.scalars().all()
    # Serialize to keep Pydantic serialization happy
    return [
        {
            "id": str(p.id),
            "proposal_id": p.proposal_id,
            "skill_name": p.skill_name,
            "generated_code": p.generated_code,
            "ast_validated": p.ast_validated,
            "ci_passed": p.ci_passed,
            "status": p.status,
            "metadata_json": p.metadata_json,
            "created_at": p.created_at.isoformat() if p.created_at else None
        }
        for p in proposals
    ]


@router.post("/proposals/{proposal_id}/approve")
async def approve_proposal(
    proposal_id: str,
    admin: dict = Depends(require_admin_token),
    session: AsyncSession = Depends(get_db_session)
):
    """
    Manually approve a proposal after security review.
    """
    async with session.begin():
        result = await session.execute(
            select(CodeProposal).where(CodeProposal.proposal_id == proposal_id)
        )
        proposal = result.scalars().first()
        if not proposal:
            raise HTTPException(status_code=404, detail="Proposal not found")
        
        proposal.status = "approved"
        # এখানে ভবিষ্যতে আমাদের অটোনোমাস মার্জ লজিক বা GitOps ট্রিগার কল হবে।
        
    return {"status": "success", "message": f"Proposal {proposal_id} approved."}
