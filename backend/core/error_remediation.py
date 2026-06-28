import os

from loguru import logger


try:
    from qdrant_client import QdrantClient
    HAS_QDRANT = True
except ImportError:
    HAS_QDRANT = False


class ErrorRemediation:
    def __init__(self) -> None:
        self.qdrant: QdrantClient | None = None
        if HAS_QDRANT:
            url = os.getenv("QDRANT_URL", "localhost")
            self.qdrant = QdrantClient(url=url, prefer_grpc=False)

    async def lookup_fix(self, error_sig: str) -> str | None:
        if not self.qdrant:
            return None
        try:
            results = self.qdrant.search(
                collection_name="error_patterns",
                query_vector=[0.0] * 384,
                limit=1,
            )
            if results:
                return results[0].payload.get("fix")
        except Exception as exc:
            logger.debug(f"Qdrant lookup failed: {exc}")
        return None
