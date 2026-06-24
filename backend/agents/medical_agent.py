import re
from typing import Any, Dict, List, Optional
from loguru import logger

try:
    from tools.domain_adapter import DomainAdapter
    _DOMAIN_ADAPTER_AVAILABLE = True
except Exception:
    _DOMAIN_ADAPTER_AVAILABLE = False


class MedicalAgent:
    """
    Symptom analysis, drug interaction checker (disclaimer-first).
    Closes Gap #46
    """

    SYSTEM_PROMPT = (
        "You are a medical information assistant (NOT a doctor). "
        "You MUST begin every response with a strong disclaimer. "
        "Never diagnose, prescribe, or claim certainty. "
        "Provide general medical information with references to standard sources."
    )

    DRUG_INTERACTION_PATTERNS = [
        (r"warfarin", [r"aspirin", r"ibuprofen", r"nsaids"], "Increased bleeding risk with NSAIDs/antiplatelets"),
        (r"maoi\w*", [r"ssri", r"snri", r"tramadol", r"meperidine"], "Serotonin syndrome risk"),
        (r"ace\s+inhibitor", [r"potassium\s+supplement", r"spironolactone"], "Hyperkalemia risk"),
        (r"statins?", [r"gemfibrozil", r"clarithromycin", r"itraconazole"], "Increased myopathy/rhabdomyolysis risk"),
        (r"methotrexate", [r"nsaids", r"penicillin", r"sulfonamides"], "Enhanced toxicity risk"),
        (r"lithium", [r"ace\s+inhibitor", r"diuretic", r"nsaid"], "Lithium toxicity risk"),
    ]

    def __init__(self):
        self.domain_adapter = DomainAdapter() if _DOMAIN_ADAPTER_AVAILABLE else None
        logger.info("Initialized MedicalAgent (disclaimer-first)")

    def symptom_analysis(self, symptoms: str, age: Optional[int] = None,
                         medical_history: Optional[str] = None) -> Dict[str, Any]:
        context = f"Patient age: {age or 'unknown'}\nHistory: {medical_history or 'none provided'}"
        prompt = (
            "Given the following symptoms and context, provide a structured differential diagnosis "
            "with possible causes (general, non-diagnostic). Do not prescribe. "
            "Always recommend seeing a licensed physician.\n\n"
            f"Symptoms: {symptoms}\n{context}"
        )
        return self._generate(prompt, context=context, action="symptom_analysis")

    def drug_interaction(self, medications: List[str]) -> Dict[str, Any]:
        found_interactions: List[Dict[str, Any]] = []
        med_lower = [m.lower() for m in medications]
        for (drug_a, interacts_with, warning) in self.DRUG_INTERACTION_PATTERNS:
            for med in med_lower:
                if re.search(drug_a, med):
                    for group in interacts_with:
                        for other in med_lower:
                            if re.search(group, other) and other != med:
                                found_interactions.append({
                                    "drug_a": med,
                                    "drug_b": other,
                                    "risk": warning,
                                    "severity": "high",
                                })
        unique = {f"{i['drug_a']}+{i['drug_b']}": i for i in found_interactions}
        interactions = list(unique.values())
        prompt = (
            f"Review these medications for interactions: {', '.join(medications)}. "
            f"Rule-based findings: {json.dumps(interactions) if interactions else 'none detected'}. "
            "Expand with general pharmacological guidance. Always remind the patient to consult their doctor/pharmacist."
        )
        result = self._generate(prompt, context=f"Medications: {', '.join(medications)}", action="drug_interaction")
        result["interactions"] = interactions
        result["disclaimer"] = (
            "Disclaimer: This is not medical advice. Drug interactions are complex. "
            "Always consult a qualified healthcare provider or pharmacist before making changes."
        )
        return result

    def _generate(self, prompt: str, context: Optional[str] = None, action: str = "general") -> Dict[str, Any]:
        if self.domain_adapter:
            try:
                result = self.domain_adapter.adapt_request("medical", prompt, context=context)
                return {
                    "action": action,
                    "response": result.get("response", ""),
                    "model": result.get("model", "unknown"),
                    "provider": result.get("provider", "unknown"),
                    "disclaimer": result.get("disclaimer", self.SYSTEM_PROMPT),
                }
            except Exception as exc:
                logger.debug(f"MedicalAgent generation failed: {exc}")
        return {
            "action": action,
            "response": "[Local fallback] Medical domain adapter unavailable. Enable domain_adapter for LLM analysis.",
            "disclaimer": self.SYSTEM_PROMPT,
        }


import json
