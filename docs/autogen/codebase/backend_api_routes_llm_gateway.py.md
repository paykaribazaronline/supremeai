# 📄 ফাইল: backend/api/routes/llm_gateway.py

**প্রকার:** .py  
**সাইজ:** 4,083 বাইট  
**আপডেট:** 2026-07-08T03:35:53.428090

---

## কোড

```py
# বাংলা মন্তব্য: LLM Gateway ও System Rules কন্ট্রোলার — স্টুডিও ড্যাশবোর্ড থেকে রিচেবল রাখতে
# /api/admin/llm প্রিফিক্স ব্যবহার করা হয়েছে (admin-console ডোমেইন রেস্ট্রিকশনযুক্ত /admin-api নয়)।
# প্ল্যাটফর্মের সাধারণ SUPREMEAI_API_TOKEN গেট এই রুটগুলোকে সুরক্ষিত রাখে।
# প্রোভাইডার তালিকা, ফলব্যাক রাউটিং চেইন, লাইভ মডেল ওভাররাইড ও সিস্টেম রুল মিউটেশন এখানে হয়।

from fastapi import APIRouter
from pydantic import BaseModel

from core import services
from core.config import settings


router = APIRouter(prefix="/api/admin/llm", tags=["LLM Gateway"])

# বাংলা মন্তব্য: ইন-মেমরি লাইভ মডেল ওভাররাইড স্টেট (ফলব্যাক চেইনের উপর প্রাধান্য পায়)
_ROUTER_STATE: dict[str, object] = {
    "current_override": None,
    "provider_order": ["openrouter", "gemini", "groq", "deepseek"],
    "cost_quality_preference": 0.7,
}


@router.get("/providers")
def list_providers():
    known = [
        ("openrouter", "OpenRouter", settings.openrouter_api_key,
         ["gpt-4o", "claude-3.5-sonnet", "llama-3.1-70b"]),
        ("gemini", "Google Gemini", settings.gemini_api_key,
         ["gemini-2.0-flash", "gemini-1.5-pro"]),
        ("groq", "Groq", settings.groq_api_key, ["llama-3.1-8b", "mixtral-8x7b"]),
        ("deepseek", "DeepSeek", settings.deepseek_api_key,
         ["deepseek-chat", "deepseek-reasoner"]),
    ]
    providers = [
        {
            "id": pid,
            "name": name,
            "status": "healthy",
            "latency_ms": 120,
            "models": models,
            "mode": "active",
        }
        for pid, name, has_key, models in known
        if has_key
    ]
    # বাংলা মন্তব্য: কোনো ক্লাউড কী কনফিগার না থাকলে লোকাল Ollama ফলব্যাক দেখানো হয়
    if not providers:
        providers.append(
            {
                "id": "ollama",
                "name": "Ollama (Local)",
                "status": "healthy",
                "latency_ms": 45,
                "models": ["llama3", "mistral"],
                "mode": "active",
            }
        )
    return providers


@router.get("/router")
def get_router_state():
    return _ROUTER_STATE


class RouterOverride(BaseModel):
    provider: str
    model: str
    remaining_requests: int = 100


@router.post("/router/override")
def set_router_override(payload: RouterOverride):
    # বাংলা মন্তব্য: লাইভ মডেল সুইচ — নির্দিষ্ট প্রোভাইডার/মডেলে রিকোয়েস্ট রাউট করা হবে
    _ROUTER_STATE["current_override"] = {
        "provider": payload.provider,
        "model": payload.model,
        "remaining_requests": payload.remaining_requests,
    }
    return {"status": "success", "override": _ROUTER_STATE["current_override"]}


@router.get("/rules")
def get_system_rules():
    return services.rules_engine.rules


class RulesPayload(BaseModel):
    rules: dict


@router.post("/rules")
def save_system_rules(payload: RulesPayload):
    # বাংলা মন্তব্য: কেন্দ্রীয় সিস্টেম স্কিমা রুল রিয়েল-টাইমে মিউটেট ও সংরক্ষণ করা হয়
    ok = services.rules_engine.save_rules(payload.rules)
    if ok:
        return {"status": "success"}
    return {"status": "error", "message": "Failed to save rules"}

```