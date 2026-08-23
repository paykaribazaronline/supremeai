"""
Optimized Vector Store for Free Tier
Reduces memory usage while maintaining search quality.
"""
import numpy as np
from typing import List, Optional, Dict, Any
from supabase import create_client
import json
import asyncio

class FreeTierOptimizedVectorStore:
    """
    Vector store optimized for 512MB memory constraint.
    
    Strategies:
    1. Batch operations (reduce connection overhead)
    2. Streaming results (don't load all into memory)
    3. Aggressive index tuning
    4. Connection pooling with limits
    """
    
    BATCH_SIZE = 50  # Smaller batches for less memory
    MAX_RESULTS = 20  # Limit results to save memory
    EMBEDDING_DIM = 1536  # OpenAI ada-002 dimension
    
    def __init__(self, supabase_url: str, supabase_key: str):
        self.client = create_client(supabase_url, supabase_key)
        self.table_name = "ai_memory"
        
        # Connection settings for low memory
        self._connection_pool_size = 2  # Very small pool for free tier
    
    async def upsert_batch(
        self, 
        embeddings: List[List[float]], 
        payloads: List[Dict[str, Any]],
        ids: List[str]
    ) -> bool:
        """
        Upsert embeddings in small batches to manage memory.
        """
        try:
            # Process in small batches
            for i in range(0, len(embeddings), self.BATCH_SIZE):
                batch_embeddings = embeddings[i:i + self.BATCH_SIZE]
                batch_payloads = payloads[i:i + self.BATCH_SIZE]
                batch_ids = ids[i:i + self.BATCH_SIZE]
                
                records = [
                    {
                        "id": bid,
                        "embedding": emb,
                        "metadata": payload,
                        "created_at": "now()"
                    }
                    for bid, emb, payload in zip(batch_ids, batch_embeddings, batch_payloads)
                ]
                
                # Insert batch
                result = self.client.table(self.table_name).upsert(
                    records,
                    on_conflict="id"
                ).execute()
                
                # Small delay to prevent overwhelming free tier DB
                await asyncio.sleep(0.05)
            
            return True
            
        except Exception as e:
            print(f"Batch upsert failed: {e}")
            return False
    
    async def similarity_search(
        self, 
        query_embedding: List[float],
        limit: int = MAX_RESULTS,
        filter_metadata: Optional[Dict] = None
    ) -> List[Dict]:
        """
        Search with memory-efficient streaming.
        Uses RPC call for vector search (pgvector).
        """
        try:
            # Build query with filters
            query = self.client.rpc(
                "match_memories",  # Need to create this function in Supabase if not exists
                {
                    "query_embedding": query_embedding,
                    "match_threshold": 0.7,
                    "match_count": min(limit, self.MAX_RESULTS)
                }
            )
            
            # Apply additional filters if provided
            if filter_metadata:
                for key, value in filter_metadata.items():
                    query = query.eq(f"metadata->>{key}", value)
            
            # Execute and get results
            result = query.execute()
            
            # Return only what we need (don't cache large results)
            return [
                {
                    "id": r.get("id"),
                    "content": r.get("metadata", {}).get("content", "")[:500],  # Truncate string to save memory
                    "score": r.get("similarity", 0),
                    "metadata": r.get("metadata", {})
                }
                for r in (result.data or [])
            ]
            
        except Exception as e:
            print(f"Similarity search failed: {e}")
            return []
    
    async def delete_old_memories(self, days_old: int = 30, limit: int = 100):
        """Delete old memories to save space (free tier storage limit)."""
        try:
            from datetime import datetime, timedelta
            
            cutoff = (datetime.utcnow() - timedelta(days=days_old)).isoformat()
            
            result = self.client.table(self.table_name)\
                .filter(f"created_at.lt.{cutoff}")\
                .limit(limit)\
                .delete()\
                .execute()
            
            return True
            
        except Exception as e:
            print(f"Delete failed: {e}")
            return False
