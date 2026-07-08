from typing import Any


try:
    from sentence_transformers import SentenceTransformer

    HAS_ST = True
except ImportError:
    HAS_ST = False


class KnowledgeExtractor:
    def __init__(self) -> None:
        if HAS_ST:
            self.model = SentenceTransformer("all-MiniLM-L6-v2")

    async def extract(self, content: str) -> list[dict[str, Any]]:
        if not HAS_ST:
            return []
        return [{"text": content, "embedding": self.model.encode(content).tolist()}]
