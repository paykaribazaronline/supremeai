from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel
from backend.evolution.auto_skill_creator import AutoSkillCreator

router = APIRouter(prefix="/api/evolution", tags=["self-evolution-engine"])
creator = AutoSkillCreator()

class EvolutionRequest(BaseModel):
    skill_name: str
    user_demand: str

@router.post("/forge")
async def forge_dynamic_skill(payload: EvolutionRequest):
    """
    On-the-fly AI Skill Generation and Sandbox Deployed Gate.
    """
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
