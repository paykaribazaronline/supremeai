"""Canonical local-first embedding utility for SupremeAI.

বাংলা মন্তব্য: এই মডিউলটি সেন্টেন্স-ট্রান্সফরমার্স (all-MiniLM-L6-v2, ৩৮৪-ডাইম, অফলাইন, ফ্রি)
ব্যবহার করে এমবেডিং তৈরি করে। sentence-transformers ইনস্টল না থাকলে LiteLLM দিয়ে
OpenAI text-embedding-3-small (১৫৩৬-ডাইম, বিল্ড) ফলব্যাক করে।

Supabase-এর মতো ১৫৩৬-ডাইম pgvector কলামের সাথে সামঞ্জস্য রাখতে local ৩৮৪-ডাইম
ভেক্টরকে শূন্য-প্যাড (zero-pad) করা হয় — কসাইন সিমিলারিটি অপরিবর্তিত থাকে কারণ
শূন্য প্যাডিং ডট-প্রোডাক্ট বা নর্ম পরিবর্তন করে না। এতে লাইভ ডেটাবেজ মাইগ্রেশন
ছাড়াই $0 এমবেডিং সম্ভব।
"""
from __future__ import annotations

import importlib.util
import logging
import math
import os

logger = logging.getLogger(__name__)

LOW_MEMORY_MODE = os.getenv("LOW_MEMORY_MODE", "false").lower() == "true"
_HAS_SENTENCE_TRANSFORMERS = (not LOW_MEMORY_MODE) and importlib.util.find_spec("sentence_transformers") is not None

_LOCAL_MODEL_NAME = "all-MiniLM-L6-v2"
_LOCAL_DIM = 384
_REMOTE_MODEL = "text-embedding-3-small"
_REMOTE_DIM = 1536

_encoder = None


def get_local_encoder():
    """Lazy-load the local SentenceTransformer encoder (graceful on failure)."""
    global _encoder
    if _encoder is None and _HAS_SENTENCE_TRANSFORMERS:
        try:
            from sentence_transformers import SentenceTransformer

            logger.info(f"[embeddings] Loading local SentenceTransformer('{_LOCAL_MODEL_NAME}')...")
            _encoder = SentenceTransformer(_LOCAL_MODEL_NAME)
        except Exception as exc:
            logger.warning(f"[embeddings] Failed to load local encoder: {exc}")
    return _encoder


def hash_vectorize(text: str, size: int = _LOCAL_DIM) -> list[float]:
    """Pure-Python feature hashing fallback — zero-cost, fully offline."""
    vector = [0.0] * size
    words = [w.lower() for w in text.split() if len(w) > 1]
    if not words:
        vector[0] = 1.0
        return vector
    for word in words:
        h = abs(hash(word)) % size
        sign = 1 if (abs(hash(word)) // size) % 2 == 0 else -1
        vector[h] += sign
    norm = math.sqrt(sum(x * x for x in vector))
    if norm > 0:
        vector = [x / norm for x in vector]
    return vector


def local_embed(text: str) -> list[float] | None:
    """Return a 384-dim local embedding, or None if the encoder is unavailable."""
    enc = get_local_encoder()
    if enc is not None:
        try:
            return enc.encode(text).tolist()
        except Exception as exc:
            logger.warning(f"[embeddings] local encode failed: {exc}")
            return None
    return None


def _pad_to_dim(vec: list[float], dim: int) -> list[float]:
    if len(vec) == dim:
        return vec
    if len(vec) > dim:
        return vec[:dim]
    return vec + [0.0] * (dim - len(vec))


def embed_for_pgvector(text: str, pg_dim: int = _REMOTE_DIM) -> list[float] | None:
    """
    Local-first embedding padded to ``pg_dim``.

    - Local path (384) zero-padded to ``pg_dim`` — cosine similarity preserved.
    - Falls back to LiteLLM OpenAI (native ``pg_dim``) when local encoder unavailable.
    - Returns ``None`` only if both fail (callers degrade gracefully to substring search).
    """
    local = local_embed(text)
    if local is not None:
        return _pad_to_dim(local, pg_dim)
    try:
        import litellm

        resp = litellm.embedding(model=_REMOTE_MODEL, input=text)
        return resp.data[0]["embedding"]
    except Exception as exc:
        logger.warning(f"[embeddings] LiteLLM fallback embedding failed: {exc}")
        return None


def embed_query(text: str) -> list[float] | None:
    """Default 384-dim local embedding for in-process semantic search (ChromaDB/Qdrant)."""
    return local_embed(text) or hash_vectorize(text)


class EmbeddingEngine:
    """Singleton embedding engine for local-first zero-cost semantic search."""

    _instance: EmbeddingEngine | None = None

    @classmethod
    def get_instance(cls) -> EmbeddingEngine:
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    async def embed(self, text: str) -> list[float]:
        """Compute embedding vector asynchronously."""
        vec = embed_query(text)
        if vec is None:
            vec = hash_vectorize(text)
        return vec

    def cosine(self, v1: list[float], v2: list[float]) -> float:
        """Compute cosine similarity between two vectors."""
        if not v1 or not v2 or len(v1) != len(v2):
            return 0.0
        dot = sum(a * b for a, b in zip(v1, v2))
        norm1 = math.sqrt(sum(a * a for a in v1))
        norm2 = math.sqrt(sum(b * b for b in v2))
        if norm1 <= 0 or norm2 <= 0:
            return 0.0
        return dot / (norm1 * norm2)

    async def vector_search(self, query: str, corpus: list[dict], top_k: int = 5) -> list[dict]:
        """Search top-k matching documents using cosine similarity."""
        q_vec = await self.embed(query)
        scored = []
        for doc in corpus:
            doc_vec = doc.get("vector") or await self.embed(doc.get("text", ""))
            score = self.cosine(q_vec, doc_vec)
            scored.append((score, doc))
        scored.sort(key=lambda x: x[0], reverse=True)
        return [dict(doc, score=score) for score, doc in scored[:top_k]]

