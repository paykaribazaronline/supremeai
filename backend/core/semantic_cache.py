# Vector Semantic Cache Engine for SupremeAI 2.0
# বাংলা মন্তব্য: এটি ফায়ারস্টোর বাদ দিয়ে সরাসরি experience_db.py (ChromaDB/Qdrant) ব্যবহার করে এবং ডাইনামিক থ্রেশহোল্ড সেট করে।

from loguru import logger

from adaptive_engine.experience_db import Experience
from adaptive_engine.experience_db import ExperienceDatabase
from core.config import settings


# বাংলা মন্তব্য: ক্যাশ পলিসি — task_type-ভিত্তিক থ্রেশহোল্ড
# এখন থেকে এগুলো settings/supabase-config থেকে ওভাররাইড করা যাবে।
# ডিফল্ট মান: কোড টাস্কের জন্য ৯৫%, জেনারেল টাস্কের জন্য ৮৫%।
# প্রোডাকশনে A/B টেস্টের জন্য থ্রেশহোল্ড কোড ডিপ্লয় ছাড়াই পরিবর্তন করতে হবে।
DEFAULT_CACHE_THRESHOLDS: dict[str, float] = {
    "code": 0.95,
    "generation": 0.95,
    "general": 0.85,
    "qa": 0.85,
    "reasoning": 0.80,
}


def get_cache_threshold(task_type: str) -> float:
    """
    task_type অনুযায়ী ক্যাশ থ্রেশহোল্ড রিটার্ন করে।
    settings থেকে কাস্টম থ্রেশহোল্ড ওভাররাইড নেওয়া যেতে পারে
    (যদি settings.cache_thresholds থাকে), অন্যথায় DEFAULT_CACHE_THRESHOLDS ব্যবহার করে।
    """
    # settings-এ cache_policies টেবিল থেকে ডাইনামিক থ্রেশহোল্ড নেওয়ার সুযোগ
    custom_thresholds: dict[str, float] | None = getattr(
        settings, "cache_thresholds", None
    )
    thresholds = custom_thresholds or DEFAULT_CACHE_THRESHOLDS

    task_lower = task_type.lower()
    for key, threshold in thresholds.items():
        if key in task_lower:
            return threshold
    return thresholds.get("general", 0.85)


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
            threshold = get_cache_threshold(task_type)

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
