import os
import json
from typing import Dict, Any, List
from loguru import logger

class RLHFPipeline:
    def __init__(self, storage_dir: str = "data/rlhf"):
        self.preference_logs: List[Dict[str, Any]] = []
        self.storage_dir = storage_dir
        os.makedirs(self.storage_dir, exist_ok=True)
        logger.info("Initialized RLHFPipeline")

    def record_preference(self, prompt: str, chosen_response: str, rejected_response: str) -> Dict[str, Any]:
        logger.debug("Recording RLHF preference data point.")
        record = {
            "prompt": prompt,
            "chosen": chosen_response,
            "rejected": rejected_response,
            "timestamp": __import__('datetime').datetime.utcnow().isoformat() + "Z",
        }
        self.preference_logs.append(record)
        path = os.path.join(self.storage_dir, "preferences.jsonl")
        with open(path, "a", encoding="utf-8") as f:
            f.write(json.dumps(record) + "\n")
        return {"status": "success", "recorded": len(self.preference_logs)}

    async def export_dpo_dataset(self, output_path: str | None = None) -> Dict[str, Any]:
        if not self.preference_logs:
            logger.warning("No preference data to export.")
            return {"status": "error", "error": "No preference data to export."}
        output_path = output_path or os.path.join(self.storage_dir, "dpo_dataset.jsonl")
        logger.info(f"Exporting {len(self.preference_logs)} DPO records to {output_path}")
        try:
            with open(output_path, "w", encoding="utf-8") as f:
                for log in self.preference_logs:
                    f.write(json.dumps(log) + "\n")
            count = len(self.preference_logs)
            self.preference_logs.clear()
            return {"status": "success", "exported": count, "output_path": output_path}
        except Exception as exc:
            logger.error(f"DPO export failed: {exc}")
            return {"status": "error", "error": str(exc)}
