from enum import Enum
from typing import List, Dict, Any
import hashlib

class InteractionType(Enum):
    CODE_REQUEST = "code_request"
    QUESTION = "question"
    DEBUGGING = "debugging"

class ActionPrediction:
    def __init__(self, description: str, confidence: float):
        self.description = description
        self.confidence = confidence

class UserTwin:
    def __init__(self, user_id: str):
        self.hashed_id = hashlib.sha256(user_id.encode()).hexdigest()
        self.style_dna = [0.0] * 14
        self.preferences: Dict[str, float] = {}
        self.capabilities: Dict[str, float] = {}
        self.journey_paths: List[str] = []
        
    async def record_interaction(self, interaction_type: InteractionType, content: str, duration_ms: float, success: bool):
        self.journey_paths.append(interaction_type.value)
        if len(self.journey_paths) > 50:
            self.journey_paths.pop(0)

    def predict_next_actions(self) -> List[ActionPrediction]:
        return [
            ActionPrediction("Review recent test failures", 0.75),
            ActionPrediction("Optimize database query", 0.65)
        ]

class TwinManager:
    def __init__(self):
        self.twins: Dict[str, UserTwin] = {}
        
    def get_or_create(self, user_id: str) -> UserTwin:
        if user_id not in self.twins:
            self.twins[user_id] = UserTwin(user_id)
        return self.twins[user_id]

_twin_manager_instance = None

def get_twin_manager() -> TwinManager:
    global _twin_manager_instance
    if _twin_manager_instance is None:
        _twin_manager_instance = TwinManager()
    return _twin_manager_instance
