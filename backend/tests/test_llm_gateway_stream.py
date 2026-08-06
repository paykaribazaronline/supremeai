import pytest
from core.llm.llm_gateway import get_http_client, shutdown_http_client
from core.llm.provider_router import LatencyAwareWeightedRouter


@pytest.mark.asyncio
async def test_provider_router_selection():
    router = LatencyAwareWeightedRouter(providers={"openai": 5.0, "anthropic": 3.0})
    provider = await router.select_provider()
    assert provider in ["openai", "anthropic"]


@pytest.mark.asyncio
async def test_provider_router_circuit_breaker():
    router = LatencyAwareWeightedRouter(providers={"openai": 5.0})
    # Record multiple failures to trip circuit
    for _ in range(6):
        await router.record_result("openai", latency_ms=500.0, success=False)

    stats = router.stats["openai"]
    assert stats.is_circuit_open() is True


@pytest.mark.asyncio
async def test_http_client_singleton_lifecycle():
    client1 = get_http_client()
    client2 = get_http_client()
    assert client1 is client2
    await shutdown_http_client()
