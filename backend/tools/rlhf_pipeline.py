import asyncio
from typing import Dict, Any, List
from loguru import logger

class RLHFPipeline:
    """
    Reinforcement Learning from Human Feedback pipeline.
    Collects preference data and prepares DPO (Direct Preference Optimization) datasets.
    """

    def __init__(self):
        self.preference_logs: List[Dict[str, Any]] = []
        logger.info("Initialized RLHFPipeline")

    def record_preference(self, prompt: str, chosen_response: str, rejected_response: str):
        """Records a user preference for DPO training."""
        logger.debug("Recording RLHF preference data point.")
        self.preference_logs.append({
            "prompt": prompt,
            "chosen": chosen_response,
            "rejected": rejected_response
        })

    async def export_dpo_dataset(self, output_path: str) -> bool:
        """Exports collected preference logs as a JSONL dataset for DPO."""
        if not self.preference_logs:
            logger.warning("No preference data to export.")
            return False
            
        logger.info(f"Exporting {len(self.preference_logs)} DPO records to {output_path}")
        import json
        with open(output_path, "w") as f:
            for log in self.preference_logs:
                f.write(json.dumps(log) + "\n")
                
        self.preference_logs.clear()
        return True
