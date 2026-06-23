import os
import math
import google.generativeai as genai
from google.cloud import firestore
from loguru import logger
from typing import Optional, List

class VectorSemanticCache:
    """
    Enterprise Vector Semantic Cache Engine.
    Saves up to 90% of AI Token costs by matching prompt meanings instead of exact strings.
    """
    def __init__(self):
        self.db = firestore.Client()
        self.collection = self.db.collection("supreme_semantic_cache")
        # Gemini API Config
        genai.configure(api_key=os.getenv("GEMINI_API_KEY"))

    def _cosine_similarity(self, vec1: List[float], vec2: List[float]) -> float:
        """দুটি ভেক্টরের মধ্যে সিমিলারিটি স্কোর পরিমাপ করার বিশুদ্ধ গাণিতিক লজিক (Zero Dependencies)"""
        dot_product = sum(a * b for a, b in zip(vec1, vec2))
        magnitude1 = math.sqrt(sum(a * a for a in vec1))
        magnitude2 = math.sqrt(sum(b * b for b in vec2))
        if not magnitude1 or not magnitude2:
            return 0.0
        return dot_product / (magnitude1 * magnitude2)

    async def get_cached_inference(self, prompt: str, model_name: str, threshold: float = 0.95) -> Optional[str]:
        """প্রম্পটের অর্থ বিশ্লেষণ করে ৯৫% ম্যাচিং পেলে ক্যাশড রেসপন্স রিটার্ন করে"""
        try:
            # ১. Gemini Embedding API দিয়ে প্রম্পটের ভেক্টর জেনারেট করা (Lightweight & High-Accuracy)
            response = genai.embed_content(
                model="models/text-embedding-004",
                content=prompt,
                task_type="retrieval_document"
            )
            query_vector = response.get('embedding')
            if not query_vector: return None

            # ২. ফায়ারস্টোর থেকে ওই নির্দিষ্ট মডেলের ক্যাশড ডাটা রিড করা
            cache_docs = self.collection.where("model", "==", model_name).stream()
            
            for doc in cache_docs:
                data = doc.to_dict()
                cached_vector = data.get("embedding")
                
                if cached_vector:
                    # ৩. কসাইন সিমিলারিটি ক্যালকুলেট করা
                    score = self._cosine_similarity(query_vector, cached_vector)
                    if score >= threshold:
                        logger.info(f"⚡ [SEMANTIC CACHE HIT] Score: {score:.4f}. Token saved for model {model_name}!")
                        return data.get("response_text")
                        
            return None
        except Exception as e:
            logger.error(f"⚠️ Semantic cache lookup failed silently: {str(e)}")
            return None

    async def set_cache_inference(self, prompt: str, model_name: str, response_text: str):
        """ভবিষ্যতের ম্যাচিংয়ের জন্য রেসপন্স টেক্সট ভেক্টরসহ সেভ করে রাখা"""
        try:
            response = genai.embed_content(
                model="models/text-embedding-004",
                content=prompt,
                task_type="retrieval_document"
            )
            embedding = response.get('embedding')
            if not embedding: return

            self.collection.add({
                "prompt_example": prompt,
                "model": model_name,
                "embedding": embedding,
                "response_text": response_text,
                "created_at": firestore.SERVER_TIMESTAMP
            })
            logger.info(f"💾 Successfully vectorized and cached new semantic context for {model_name}.")
        except Exception as e:
            logger.error(f"❌ Failed to write vector semantic cache to Firestore: {str(e)}")
