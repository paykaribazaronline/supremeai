import logging

logger = logging.getLogger(__name__)


class AutonomousProviderRouter:
    """
    শুধুমাত্র ১০০% জিরো-কস্ট এবং ক্লাউড-নেটিভ ফ্রি-টিয়ার প্রোভাইডারদের (যেমন Moonshot Kimi, Gemini Free Tier, Groq, GitHub Models, Hugging Face, OpenRouter)
    ম্যানেজ করার জন্য সেন্ট্রাল রাউটার।
    (DeepSeek, Together AI এবং Local Ollama সম্পূর্ণ রিমুভ করা হয়েছে)
    """

    def __init__(self):
        # আমরা শুধু একদম ফ্রি এবং ভার্চুয়াল ক্লাউড রিসোর্স ট্র্যাক করছি
        self.provider_token_usage: dict[str, float] = {
            "moonshot": 0.0,
            "gemini": 0.0,
            "groq": 0.0,
            "github_models": 0.0,
            "huggingface": 0.0,
            "openrouter": 0.0,
        }
        self.quota_limit = 0.80  # ৮০% ডেইলি ফ্রি কোটা লিমিট ট্র্যাকিং

    def get_optimal_provider(self, task_type: str, fallback_active: bool = False) -> str:
        """
        টাস্ক টাইপ অনুযায়ী শুধুমাত্র ফ্রি ক্লাউড প্রোভাইডারদের মধ্যে রাউটিং করে।
        (কোনো লোকাল রিসোর্স বা পেইড এপিআই কল করা হবে না)
        """
        # বাংলা ভাষা বা ক্রিয়েটিভ রাইটিং-এর কাজের জন্য Moonshot Kimi (PSI-001)
        if (task_type == "BANGLA_SPECIFIC" or task_type == "CREATIVE") and not fallback_active:
            if self.provider_token_usage["moonshot"] < self.quota_limit:
                return "moonshot"
            elif self.provider_token_usage["huggingface"] < self.quota_limit:
                # Hugging Face এর কিছু মডেল বাংলা সাপোর্ট করে
                return "huggingface"
            else:
                # কোটা শেষ হলে সরাসরি Gemini Free Tier-এ ফলব্যাক
                logger.warning("Moonshot quota near limit, falling back to Gemini Free Tier.")
                return "gemini"

        # কোডিং বা টেকনিক্যাল কাজের জন্য Groq বা GitHub Models
        if task_type == "CODING" or task_type == "TECHNICAL":
            if self.provider_token_usage["groq"] < self.quota_limit:
                return "groq"
            elif self.provider_token_usage["github_models"] < self.quota_limit:
                return "github_models"
            elif self.provider_token_usage["gemini"] < self.quota_limit:
                return "gemini"
            else:
                # সবচেয়ে দ্রুত হিসেবে OpenRouter এর কিছু ফ্রি মডেল
                return "openrouter"

        # জেনারেল অ্যানালিটিক্যাল কাজের জন্য Gemini Free Tier
        if self.provider_token_usage["gemini"] < self.quota_limit:
            return "gemini"

        # ডিফল্ট হিসেবে অন্যান্য ফ্রি প্রোভাইডারদের মধ্যে সুষম ভাবে রাউটিং
        for provider in self.provider_token_usage:
            if self.provider_token_usage[provider] < self.quota_limit:
                return provider

        # চরম অবস্থায় যেকোনো প্রোভাইডার ব্যবহার করতে হবে
        return "moonshot"

    def record_usage(self, provider: str, tokens_used: int):
        """
        ফ্রি প্রোভাইডারগুলোর টোকেন ব্যবহার ট্র্যাকিং (ZCO-002)
        """
        if provider in self.provider_token_usage:
            # টোকেন ব্যবহার আপডেট করা হচ্ছে
            self.provider_token_usage[provider] += tokens_used / 100000.0
            # বাংলায় ডেভেলপমেন্ট লগিং রাখা হয়েছে সহজে বোঝার সুবিধার্থে
            logger.info(
                f"ফ্রি প্রোভাইডার {provider}-এর বর্তমান কোটা ব্যবহার: {self.provider_token_usage[provider]:.2%}"
            )
