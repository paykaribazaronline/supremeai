from fastapi import APIRouter
from pydantic import BaseModel
from brain.user_digital_twin import get_twin_manager

router = APIRouter(prefix="/twin", tags=["digital_twin"])

class PreferenceRequest(BaseModel):
    category: str
    value: float

@router.get("/{user_id}/profile")
async def get_profile(user_id: str):
    twin = get_twin_manager().get_or_create(user_id)
    return {
        "hashed_id": twin.hashed_id,
        "style_dna": twin.style_dna,
        "preferences": twin.preferences,
        "capabilities": twin.capabilities
    }

@router.get("/{user_id}/predictions")
async def get_predictions(user_id: str):
    twin = get_twin_manager().get_or_create(user_id)
    predictions = twin.predict_next_actions()
    return {
        "predictions": [{"description": p.description, "confidence": p.confidence} for p in predictions]
    }

@router.post("/{user_id}/preferences")
async def set_preference(user_id: str, req: PreferenceRequest):
    twin = get_twin_manager().get_or_create(user_id)
    twin.preferences[req.category] = req.value
    return {"status": "updated", "preferences": twin.preferences}
