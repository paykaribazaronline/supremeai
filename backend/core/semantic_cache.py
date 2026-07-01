# Vector Semantic Cache Engine for SupremeAI 2.0
# বাংলা মন্তব্য: এটি ফায়ারস্টোর বাদ দিয়ে সরাসরি experience_db.py (ChromaDB/Qdrant) ব্যবহার করে এবং ডাইনামিক থ্রেশহোল্ড সেট করে।

from loguru import logger

from adaptive_engine.experience_db import Experience
from adaptive_engine.experience_db import ExperienceDatabase


class CacheEntry:
    def __init__(self, provider: str, model: str, response: str):
        self.provider = provider
        self.model = model
        self.response = response

class SemanticCache:
    def __init__(self):
        # Initialize Experience Database as the vector backend
        self.db = ExperienceDatabase()
        logger.info("SemanticCache initialized using ExperienceDatabase vector backend")

    async def query_similar(self, prompt: str, task_type: str = "general") -> CacheEntry | None:
        try:
            # বাংলা মন্তব্য: কাজের ধরণের ওপর ভিত্তি করে ডাইনামিক থ্রেশহোল্ড সেট করা হচ্ছে
            if "code" in task_type.lower() or "generation" in task_type.lower():
                threshold = 0.95  # Code tasks require exact or very high semantic similarity (95%)
            else:
                threshold = 0.85  # Normal Q&A uses 85% similarity threshold

            hits = self.db.find_similar(prompt, limit=1, threshold=threshold)
            if hits:
                best_hit = hits[0]
                logger.info(
                    f"⚡ [SEMANTIC CACHE HIT] Task: {task_type} | Score: {best_hit['score']:.4f} | Source: {best_hit['source']}"
                )
                return CacheEntry(
                    provider=best_hit.get("source", "chroma"),
                    model="cached_semantic",
                    response=best_hit.get("response", "")
                )
            return None
        except Exception as e:
            logger.error(f"⚠️ SemanticCache lookup failed: {e}")
            return None

    async def set(self, prompt: str, response: str, task_type: str = "general") -> None:
        try:
            # বাংলা মন্তব্য: সফল ও ভেরিফাইড কোড/রেসপন্স এক্সপেরিয়েন্স ডেটাবেসে রাইট করা হচ্ছে
            exp = Experience(
                request=prompt,
                generated_code=response if "code" in task_type.lower() else None,
                action_taken=response if "code" not in task_type.lower() else "Code Generated",
                result="success"
            )
            self.db.record_experience(exp)
            logger.info(f"💾 Successfully recorded successful experience pattern for {task_type}")
        except Exception as e:
            logger.error(f"❌ Failed to save experience pattern: {e}")
