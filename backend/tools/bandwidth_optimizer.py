import re
from typing import Dict, Any
from loguru import logger

class BandwidthOptimizer:
    def __init__(self):
        logger.info("Initialized BandwidthOptimizer")

    def compress_prompt(self, prompt: str, target_ratio: float = 0.5) -> str:
        original_len = len(prompt)
        compressed = prompt
        compressed = re.sub(r"\s+", " ", compressed)
        compressed = re.sub(r"[^\x00-\x7F]+", "", compressed)
        compressed = compressed.strip()
        new_len = len(compressed)
        if new_len > original_len * max(target_ratio, 0.1):
            truncated = compressed[: int(original_len * target_ratio)]
            compressed = truncated.rstrip() + "..."
        logger.debug(f"Compressed prompt from {original_len} to {len(compressed)} chars")
        return compressed

    def generate_delta_update(self, old_state: Dict[str, Any], new_state: Dict[str, Any]) -> Dict[str, Any]:
        delta: Dict[str, Any] = {}
        for k, v in new_state.items():
            if k not in old_state or old_state[k] != v:
                delta[k] = v
        return delta
