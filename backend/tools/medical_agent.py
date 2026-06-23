from typing import Dict, Any
from loguru import logger


class MedicalAgent:
    async def analyze_symptoms(self, symptoms: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        logger.info(f"Analyzing medical symptoms: {symptoms}")
        try:
            from brain.model_router import ModelRouter
            router = ModelRouter()
            llm_prompt = (
                f"Analyze these symptoms from a clinical perspective: {symptoms}. "
                "Provide differential diagnosis, recommended tests, and urgency level. "
                "Include a clear disclaimer that this is not medical advice. "
                "Return only the analysis text."
            )
            result = router.async_route_and_generate(llm_prompt, task_type="general", max_cost=0.01)
            text = result.get("text", "") if isinstance(result, dict) else ""
            return {
                "status": "success",
                "symptoms": symptoms,
                "analysis": text or "Analysis unavailable.",
                "disclaimer": "This is not medical advice. Consult a qualified healthcare provider.",
            }
        except Exception as exc:
            logger.error(f"Medical analysis failed: {exc}")
            return {
                "status": "error",
                "symptoms": symptoms,
                "error": str(exc),
                "disclaimer": "This is not medical advice. Consult a qualified healthcare provider.",
            }
