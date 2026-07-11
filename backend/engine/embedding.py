import logging
import os
from typing import List

import litellm

logger = logging.getLogger(__name__)

class EmbeddingService:
    """
    Handles text-to-vector embedding generation using LiteLLM.
    Defaults to OpenAI's 'text-embedding-3-small' for high performance and low cost.
    """
    def __init__(self, model_name: str = "text-embedding-3-small"):
        self.model_name = model_name
        
    async def generate_embedding(self, text: str) -> List[float]:
        """
        Generates a vector embedding for a single text string.
        """
        try:
            response = await litellm.aembedding(
                model=self.model_name,
                input=text
            )
            # LiteLLM normalizes the response to match OpenAI's schema
            return response.data[0]['embedding']
        except Exception as e:
            logger.error(f"Failed to generate embedding: {str(e)}")
            raise

    async def generate_embeddings_batch(self, texts: List[str]) -> List[List[float]]:
        """
        Generates vector embeddings for a batch of texts.
        """
        try:
            response = await litellm.aembedding(
                model=self.model_name,
                input=texts
            )
            # Extract embeddings maintaining the original order
            return [item['embedding'] for item in response.data]
        except Exception as e:
            logger.error(f"Failed to generate batch embeddings: {str(e)}")
            raise

embedding_service = EmbeddingService()
