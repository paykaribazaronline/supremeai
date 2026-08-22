from typing import Dict, Any, Callable, Awaitable

class Prediction:
    def __init__(self, key: str, confidence: float, description: str):
        self.key = key
        self.confidence = confidence
        self.description = description

class PredictiveCacheEngine:
    def __init__(self):
        self.cache_client = None
        self.history: Dict[str, list] = {}
        
    async def initialize(self, cache_client: Any):
        self.cache_client = cache_client
        
    async def record_access(self, user_id: str, cache_key: str):
        if user_id not in self.history:
            self.history[user_id] = []
        self.history[user_id].append(cache_key)
        if len(self.history[user_id]) > 100:
            self.history[user_id].pop(0)
            
    def predict(self, user_id: str, current_key: str) -> list[Prediction]:
        predictions = []
        if user_id in self.history:
            predictions.append(Prediction(f"next_page:{current_key}", 0.8, "Probable next page visit"))
        return predictions
        
    async def schedule_prefetch(self, predictions: list[Prediction], compute_registry: Dict[str, Callable[[], Awaitable[Any]]]):
        decisions = []
        for pred in predictions:
            if pred.confidence > 0.7:
                for reg_key, compute_fn in compute_registry.items():
                    if reg_key.replace('*', '') in pred.key:
                        try:
                            val = await compute_fn()
                            await self.cache_client.set(pred.key, val)
                            decisions.append({"key": pred.key, "status": "prefetched"})
                        except Exception as e:
                            decisions.append({"key": pred.key, "status": f"error: {e}"})
        return decisions

_predictive_engine_instance = None

def get_predictive_engine() -> PredictiveCacheEngine:
    global _predictive_engine_instance
    if _predictive_engine_instance is None:
        _predictive_engine_instance = PredictiveCacheEngine()
    return _predictive_engine_instance
