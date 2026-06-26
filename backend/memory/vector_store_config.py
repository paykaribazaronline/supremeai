from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass
class VectorStoreConfig:
    backend: str = "chroma"
    qdrant_url: str | None = None
    qdrant_api_key: str | None = None
    pinecone_api_key: str | None = None
    pinecone_index: str | None = None
    default_collection: str = "supremeai_default"
    embedding_model: str = "BAAI/bge-small-en-v1.5"
    distance: str = "cosine"


def get_vector_store_config() -> VectorStoreConfig:
    return VectorStoreConfig(
        backend=os.getenv("VECTOR_BACKEND", "chroma"),
        qdrant_url=os.getenv("QDRANT_URL"),
        qdrant_api_key=os.getenv("QDRANT_API_KEY"),
        pinecone_api_key=os.getenv("PINECONE_API_KEY"),
        pinecone_index=os.getenv("PINECONE_INDEX"),
    )
