from core.cache.autocache_proxy import AutoCacheProxy


def test_autocache_proxy_ttl_calculation():
    proxy = AutoCacheProxy()

    # Static docs prompt -> 86400s (24h)
    ttl_docs = proxy.calculate_dynamic_ttl("Show me the API documentation and guide")
    assert ttl_docs == 86400

    # Skills catalog -> 43200s (12h)
    ttl_skills = proxy.calculate_dynamic_ttl(
        "List all available tools and skills catalog"
    )
    assert ttl_skills == 43200

    # Code generation -> 3600s (1h)
    ttl_code = proxy.calculate_dynamic_ttl(
        "def async_generate(self, prompt: str): pass"
    )
    assert ttl_code == 3600

    # User dashboard -> 0s (Bypass cache)
    ttl_user = proxy.calculate_dynamic_ttl(
        "Show my account balance and profile dashboard"
    )
    assert ttl_user == 0

    # General chat -> 1800s (30m)
    ttl_chat = proxy.calculate_dynamic_ttl("What is the capital of France?")
    assert ttl_chat == 1800


def test_autocache_proxy_category_inference():
    proxy = AutoCacheProxy()
    assert proxy.infer_category_from_prompt("python code for tutorial") == "static_docs"
    assert proxy.infer_category_from_prompt("refactor function bug") == "code_gen"
    assert proxy.infer_category_from_prompt("hello how are you") == "ai_chat"
