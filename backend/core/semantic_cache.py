import math
import os

from loguru import logger


try:
    import google.generativeai as genai
    from google.cloud import firestore

    HAS_GOOGLE_DEPS = True
except ImportError:
    HAS_GOOGLE_DEPS = False


class VectorSemanticCache:
    """
    Enterprise Vector Semantic Cache Engine.
    Saves up to 90% of AI Token costs by matching prompt meanings instead of exact strings.
    """

    def __init__(self):
        if HAS_GOOGLE_DEPS:
            self.db = firestore.Client()
            self.collection = self.db.collection("supreme_semantic_cache")
            # Gemini API Config
            genai.configure(api_key=os.getenv("GEMINI_API_KEY"))
        else:
            self.db = None
            self.collection = None

    def _cosine_similarity(self, vec1: list[float], vec2: list[float]) -> float:
        """দুটি ভেক্টরের মধ্যে সিমিলারিটি স্কোর পরিমাপ করার বিশুদ্ধ গাণিতিক লজিক (Zero Dependencies)"""
        dot_product = sum(a * b for a, b in zip(vec1, vec2, strict=False))
        magnitude1 = math.sqrt(sum(a * a for a in vec1))
        magnitude2 = math.sqrt(sum(b * b for b in vec2))
        if not magnitude1 or not magnitude2:
            return 0.0
        return dot_product / (magnitude1 * magnitude2)

    async def get_cached_inference(
        self, prompt: str, model_name: str, threshold: float = 0.95
    ) -> str | None:
        """প্রম্পটের অর্থ বিশ্লেষণ করে ৯৫% ম্যাচিং পেলে ক্যাশড রেসপন্স রিটার্ন করে"""
        try:
            # ১. Gemini Embedding API দিয়ে প্রম্পটের ভেক্টর জেনারেট করা (Lightweight & High-Accuracy)
            response = genai.embed_content(
                model="models/text-embedding-004",
                content=prompt,
                task_type="retrieval_document",
            )
            query_vector = response.get("embedding")
            if not query_vector:
                return None

            # ২. ফায়ারস্টোর থেকে ওই নির্দিষ্ট মডেলের ক্যাশড ডাটা রিড করা
            cache_docs = self.collection.where("model", "==", model_name).stream()

            for doc in cache_docs:
                data = doc.to_dict()
                cached_vector = data.get("embedding")

                if cached_vector:
                    # ৩. কসাইন সিমিলারিটি ক্যালকুলেট করা
                    score = self._cosine_similarity(query_vector, cached_vector)
                    if score >= threshold:
                        logger.info(
                            f"⚡ [SEMANTIC CACHE HIT] Score: {score:.4f}. Token saved for model {model_name}!"
                        )
                        return data.get("response_text")

            return None
        except Exception as e:
            logger.error(f"⚠️ Semantic cache lookup failed silently: {str(e)}")
            return None

    async def set_cache_inference(
        self, prompt: str, model_name: str, response_text: str
    ):
        """ভবিষ্যতের ম্যাচিংয়ের জন্য রেসপন্স টেক্সট ভেক্টরসহ সেভ করে রাখা"""
        try:
            response = genai.embed_content(
                model="models/text-embedding-004",
                content=prompt,
                task_type="retrieval_document",
            )
            embedding = response.get("embedding")
            if not embedding:
                return

            self.collection.add(
                {
                    "prompt_example": prompt,
                    "model": model_name,
                    "embedding": embedding,
                    "response_text": response_text,
                    "created_at": firestore.SERVER_TIMESTAMP,
                }
            )
            logger.info(
                f"💾 Successfully vectorized and cached new semantic context for {model_name}."
            )
        except Exception as e:
            logger.error(
                f"❌ Failed to write vector semantic cache to Firestore: {str(e)}"
            )


class CacheEntry:
    def __init__(self, provider: str, model: str, response: str):
        self.provider = provider
        self.model = model
        self.response = response


class SemanticCache:
    def __init__(self):
        self.is_configured = os.getenv("GEMINI_API_KEY") is not None
        if self.is_configured:
            try:
                self._vector_cache = VectorSemanticCache()
            except Exception as e:
                logger.warning(f"Failed to initialize VectorSemanticCache: {e}")
                self.is_configured = False
                self._vector_cache = None
        else:
            self._vector_cache = None

    async def query_similar(self, prompt: str) -> CacheEntry | None:
        if not self.is_configured or not self._vector_cache:
            return None
        try:
            response = genai.embed_content(
                model="models/text-embedding-004",
                content=prompt,
                task_type="retrieval_document",
            )
            query_vector = response.get("embedding")
            if not query_vector:
                return None

            cache_docs = self._vector_cache.collection.stream()
            best_score = 0.0
            best_doc = None
            threshold = 0.95

            for doc in cache_docs:
                data = doc.to_dict()
                cached_vector = data.get("embedding")
                if cached_vector:
                    score = self._vector_cache._cosine_similarity(
                        query_vector, cached_vector
                    )
                    if score >= threshold and score > best_score:
                        best_score = score
                        best_doc = data

            if best_doc:
                logger.info(f"⚡ [SEMANTIC CACHE HIT] Score: {best_score:.4f}.")
                return CacheEntry(
                    provider=best_doc.get("provider", "gemini"),
                    model=best_doc.get("model", "unknown"),
                    response=best_doc.get("response_text", ""),
                )
            return None
        except Exception as e:
            logger.error(f"⚠️ SemanticCache query_similar failed: {e}")
            return None

    async def set(self, prompt: str, response: str, provider: str, model: str) -> None:
        if not self.is_configured or not self._vector_cache:
            return
        try:
            response_embed = genai.embed_content(
                model="models/text-embedding-004",
                content=prompt,
                task_type="retrieval_document",
            )
            embedding = response_embed.get("embedding")
            if not embedding:
                return

            self._vector_cache.collection.add(
                {
                    "prompt_example": prompt,
                    "model": model,
                    "provider": provider,
                    "embedding": embedding,
                    "response_text": response,
                    "created_at": firestore.SERVER_TIMESTAMP,
                }
            )
            logger.info(
                f"💾 Successfully cached new semantic context for {model} (provider={provider})."
            )
        except Exception as e:
            logger.error(f"❌ SemanticCache set failed: {e}")

