import asyncio
import logging

from engine.embedding import embedding_service
from engine.vector_db import vector_db

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


async def main():
    logger.info("--- Testing Neural Memory (RAG) ---")

    dummy_text = "Fix index out of bounds error in python lists by checking length before access."

    try:
        logger.info(f"Generating embedding for: '{dummy_text}'")
        vector = await embedding_service.generate_embedding(dummy_text)

        logger.info("Saving experience to Vector DB (Pinecone)...")
        await vector_db.save_experience(
            vector=vector,
            metadata={
                "issue": "IndexError",
                "solution": dummy_text,
                "language": "python",
            },
        )

        query_text = "How to prevent list index out of range in python?"
        logger.info(f"Generating embedding for query: '{query_text}'")
        query_vector = await embedding_service.generate_embedding(query_text)

        logger.info("Searching for similar experiences...")
        results = await vector_db.find_similar_experiences(query_vector, top_k=1)

        if results:
            logger.info(f"✅ Found match! Score: {results[0].get('score')}")
            logger.info(f"Metadata: {results[0].get('metadata')}")
        else:
            logger.warning("No matches found.")

    except Exception as e:
        logger.error(f"Test failed. Make sure API keys are set correctly: {e!s}")


if __name__ == "__main__":
    asyncio.run(main())
