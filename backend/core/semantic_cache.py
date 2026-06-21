import os
import hashlib
import time
from typing import Optional, Dict, Any, List
from dataclasses import dataclass
from loguru import logger
import httpx
import numpy as np

@dataclass
class CacheEntry:
    prompt_hash: str
    prompt_text: str
    response: str
    provider: str
    model: str
    embedding: Optional[List[float]]
    created_at: float
    hit_count: int = 0

class SemanticCache:
    def __init__(self):
        self._redis_url = os.getenv("UPSTASH_REDIS_REST_URL", "")
        self._redis_token = os.getenv("UPSTASH_REDIS_REST_TOKEN", "")
        self._pinecone_api_key = os.getenv("PINECONE_API_KEY", "")
        self._pinecone_index_name = os.getenv("PINECONE_INDEX_NAME", "supremeai-semantic-cache")
        self._pinecone_host = os.getenv("PINECONE_HOST", "")
        self._ttl_seconds = int(os.getenv("SEMANTIC_CACHE_TTL", "3600"))
        self._similarity_threshold = float(os.getenv("SEMANTIC_SIMILARITY_THRESHOLD", "0.95"))
        self._client = httpx.Client(timeout=10.0) if self._redis_url and self._redis_token else None
    
    @property
    def is_configured(self) -> bool:
        return bool(self._client and self._pinecone_api_key)
    
    def _hash_prompt(self, prompt: str) -> str:
        return hashlib.sha256(prompt.encode()).hexdigest()
    
    def _get_cache_key(self, prompt: str) -> str:
        return f"semantic_cache:{self._hash_prompt(prompt)}"
    
    def _get_vector_key(self, prompt_hash: str) -> str:
        return f"semantic_vector:{prompt_hash}"
    
    def _redis_request(self, *args: Any) -> Dict[str, Any]:
        if not self._client:
            raise RuntimeError("Redis not configured")
        response = self._client.post(
            self._redis_url,
            headers={"Authorization": f"Bearer {self._redis_token}"},
            json=list(args),
        )
        response.raise_for_status()
        return response.json()
    
    async def get_embedding(self, text: str) -> Optional[List[float]]:
        if not self._pinecone_api_key:
            return None
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.post(
                    f"https://api.pinecone.io/embed",
                    headers={"Api-Key": self._pinecone_api_key},
                    json={"text": text, "model": "text-embedding-3-small"},
                )
                if response.status_code == 200:
                    data = response.json()
                    return data.get("embedding", [])
        except Exception as e:
            logger.debug(f"Failed to get embedding: {e}")
        return None
    
    async def query_similar(self, prompt: str) -> Optional[CacheEntry]:
        if not self.is_configured:
            return None
        
        query_embedding = await self.get_embedding(prompt)
        if not query_embedding:
            return await self._get_exact_match(prompt)
        
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.post(
                    f"https://{self._pinecone_host}/query",
                    headers={"Api-Key": self._pinecone_api_key},
                    json={
                        "vector": query_embedding,
                        "topK": 5,
                        "includeMetadata": True,
                    },
                )
                if response.status_code == 200:
                    matches = response.json().get("matches", [])
                    for match in matches:
                        if match.get("score", 0) >= self._similarity_threshold:
                            prompt_hash = match.get("id", "")
                            cached = await self._get_by_hash(prompt_hash)
                            if cached:
                                cached.hit_count += 1
                                await self._increment_hit(prompt_hash)
                                return cached
        except Exception as e:
            logger.debug(f"Pinecone vector search failed: {e}")
        
        return await self._get_exact_match(prompt)
    
    async def _get_exact_match(self, prompt: str) -> Optional[CacheEntry]:
        cache_key = self._get_cache_key(prompt)
        try:
            result = self._redis_request("GET", cache_key).get("result")
            if result:
                data = eval(result) if isinstance(result, str) else result
                return CacheEntry(
                    prompt_hash=self._hash_prompt(prompt),
                    prompt_text=prompt,
                    response=data.get("response", ""),
                    provider=data.get("provider", ""),
                    model=data.get("model", ""),
                    embedding=data.get("embedding"),
                    created_at=data.get("created_at", time.time()),
                )
        except Exception as e:
            logger.debug(f"Exact cache miss: {e}")
        return None
    
    async def set(self, prompt: str, response: str, provider: str, model: str) -> None:
        if not self._client:
            return
        
        cache_key = self._get_cache_key(prompt)
        prompt_hash = self._hash_prompt(prompt)
        
        embedding = await self.get_embedding(prompt)
        
        entry_data = {
            "response": response,
            "provider": provider,
            "model": model,
            "embedding": embedding,
            "created_at": time.time(),
        }
        
        try:
            self._redis_request("SET", cache_key, str(entry_data), "EX", self._ttl_seconds)
            
            if embedding and self._pinecone_api_key:
                await self._upsert_vector(prompt_hash, embedding, entry_data)
        except Exception as e:
            logger.error(f"Failed to set cache: {e}")
    
    async def _upsert_vector(self, prompt_hash: str, embedding: List[float], metadata: Dict) -> None:
        if not self._pinecone_api_key or not self._pinecone_host:
            return
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                await client.post(
                    f"https://{self._pinecone_host}/vectors/upsert",
                    headers={"Api-Key": self._pinecone_api_key},
                    json={
                        "vectors": [{
                            "id": prompt_hash,
                            "values": embedding,
                            "metadata": metadata,
                        }],
                    },
                )
        except Exception as e:
            logger.debug(f"Failed to upsert vector: {e}")
    
    async def _increment_hit(self, prompt_hash: str) -> None:
        if not self._client:
            return
        try:
            self._redis_request("HINCRBY", self._get_vector_key(prompt_hash), "hits", 1)
        except Exception:
            pass
    
    async def _get_by_hash(self, prompt_hash: str) -> Optional[CacheEntry]:
        try:
            matches = []
            for key in [f for f in self._list_cache_keys()[:100] if prompt_hash in f]:
                result = self._redis_request("GET", key).get("result")
                if result:
                    matches.append(result)
            if matches:
                data = eval(matches[0]) if isinstance(matches[0], str) else matches[0]
                return CacheEntry(
                    prompt_hash=prompt_hash,
                    prompt_text=data.get("prompt_text", ""),
                    response=data.get("response", ""),
                    provider=data.get("provider", ""),
                    model=data.get("model", ""),
                    embedding=data.get("embedding"),
                    created_at=data.get("created_at", time.time()),
                )
        except Exception:
            pass
        return None
    
    def _list_cache_keys(self) -> List[str]:
        try:
            keys = []
            cursor = 0
            while True:
                result = self._redis_request("SCAN", cursor, "MATCH", "semantic_cache:*")
                batch = result.get("result", [])
                keys.extend(batch)
                cursor = batch.get("cursor", 0) if isinstance(batch, dict) else 0
                if cursor == 0:
                    break
            return keys
        except Exception:
            return []
    
    def get_stats(self) -> Dict[str, Any]:
        if not self._client:
            return {"configured": False}
        try:
            hits = self._redis_request("DBSIZE").get("result", 0)
            return {
                "configured": True,
                "total_cached": hits,
                "similarity_threshold": self._similarity_threshold,
                "ttl_seconds": self._ttl_seconds,
            }
        except Exception as e:
            return {"configured": True, "error": str(e)}
    
    async def close(self) -> None:
        if self._client:
            self._client.close()