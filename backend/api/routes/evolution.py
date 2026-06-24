from fastapi import APIRouter, HTTPException, status, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel
from loguru import logger
from jose import jwt
from core.config import settings
from backend.evolution.auto_skill_creator import AutoSkillCreator
from backend.api.dependencies import get_tenant_db
from backend.core.tenant_db import TenantAwareFirestore
import os
import json
import secrets
from pathlib import Path

router = APIRouter(prefix="/api/evolution", tags=["self-evolution-engine"])

security = HTTPBearer()


def require_admin_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token = credentials.credentials
    try:
        jwt_secret = settings.jwt_secret
        decoded = jwt.decode(token, jwt_secret, algorithms=["HS256"])
        if decoded.get("role") != "admin":
            raise HTTPException(status_code=403, detail="Forbidden: User does not have admin role.")
        return decoded
    except Exception as e:
        expected = os.getenv("SUPREMEAI_API_TOKEN") or ""
        if expected and secrets.compare_digest(token, expected):
            return {"uid": "admin", "role": "admin"}
        raise HTTPException(status_code=401, detail=f"Invalid Admin Authorization Token: {str(e)}")


@router.get("/logs")
async def get_evolution_logs(admin: dict = Depends(require_admin_token)):
    base_dir = Path(__file__).resolve().parent.parent.parent
    log_path = base_dir / "backend" / "data" / "evolution_logs.jsonl"
    if not log_path.exists():
        return {"logs": []}
    try:
        with open(log_path, "r", encoding="utf-8") as f:
            lines = f.readlines()
        logs = [json.loads(line) for line in lines if line.strip()]
        return {"logs": logs}
    except Exception as e:
        logger.error(f"Failed to read evolution logs: {e}")
        raise HTTPException(status_code=500, detail="Failed to read evolution logs")


class EvolutionRequest(BaseModel):
    skill_name: str
    user_demand: str


@router.post("/forge")
async def forge_dynamic_skill(
    payload: EvolutionRequest,
    db: TenantAwareFirestore = Depends(get_tenant_db)
):
    """
    On-the-fly AI Skill Generation and Sandbox Deployed Gate.
    """
    creator = AutoSkillCreator(db=db)
    result = await creator.generate_and_deploy_skill(
        user_demand=payload.user_demand,
        skill_name=payload.skill_name
    )
    
    if not result["success"]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=result["error"]
        )
        
    return result
