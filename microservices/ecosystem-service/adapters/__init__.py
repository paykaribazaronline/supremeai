"""Real working provider adapters for the standalone test harness.

বাংলা: এই adapter গুলো সত্যিই Render/GitHub/Supabase API call করবে।
প্রতিটি শুধু তখনই active হবে যখন সংশ্লিষ্ট env credential set করা আছে।

⚠️ এই adapter-গুলো test/demo মানের — production supremeai-তে আরও robust
retry/circuit-breaker/metrics যোগ করতে হবে। কিন্তু real API call + normalize
health → UnifiedHealth প্যাটার্ন দেখানোর জন্য যথেষ্ট।
"""

from __future__ import annotations

from adapters.github_adapter import GitHubAdapter
from adapters.render_adapter import RenderAdapter
from adapters.supabase_adapter import SupabaseAdapter
from config import settings
from ecosystem import ProviderKind, ResourceRegistry


def register_all_adapters(registry: ResourceRegistry) -> None:
    """ROADMAP §37 — register the real adapters with the resource registry.

    বাংলা: প্রতিটি adapter শুধু তখনই register হবে যখন তার credential env-এ আছে।
    """
    if settings.has_render():
        registry.register_adapter(
            ProviderKind.RENDER, "adapters.render_adapter.RenderAdapter"
        )
        print(">>> registered RenderAdapter", flush=True)
    if settings.has_github():
        registry.register_adapter(
            ProviderKind.GITHUB, "adapters.github_adapter.GitHubAdapter"
        )
        print(">>> registered GitHubAdapter", flush=True)
    if settings.has_supabase():
        registry.register_adapter(
            ProviderKind.SUPABASE, "adapters.supabase_adapter.SupabaseAdapter"
        )
        print(">>> registered SupabaseAdapter", flush=True)


__all__ = [
    "RenderAdapter",
    "GitHubAdapter",
    "SupabaseAdapter",
    "register_all_adapters",
]
