"""Unit tests for the new localization, analytics, and self-evolution modules."""

# বাংলা মন্তব্য: নতুন লোকালিলাইজেশন, অ্যানালিটিক্স এবং ইভোলিউশন মডিউলগুলোর জন্য ইউনিট টেস্টসমূহ।

from __future__ import annotations

from unittest.mock import AsyncMock, patch

import pytest
from sqlalchemy.ext.asyncio import AsyncSession

from core.evolution.agent_breeder import AgentBreeder, BreederConfig
from core.evolution.performance_oracle import OracleConfig, PerformanceOracle
from core.localization.bhasha_bot import BhashaBot
from core.localization.voice_didi import VoiceDidi
from tools.analytics.churn_prophet import ChurnProphet
from tools.analytics.insight_mage import InsightMage


class MockModelRouter:
    def route_and_generate(self, prompt: str, task_type: str) -> dict:
        if task_type == "translation":
            return {"success": True, "text": "Ami bhalo achi", "model": "mock-gemini"}
        elif task_type == "voice_processing":
            return {
                "success": True,
                "text": '{"corrected_text_bn": "আমি ভাত খাই", "intent": "order", "confidence": 0.95, "entities": [{"type": "product", "value": "rice"}], "suggested_action": "order_rice"}',
                "model": "mock-gemini",
            }
        return {"success": True, "text": "mocked response"}


@pytest.mark.asyncio
async def test_bhasha_bot_translation():
    """Test BhashaBot translation logic."""
    router = MockModelRouter()
    bot = BhashaBot(model_router=router)

    # Test Identity translation
    res1 = await bot.translate("hello", "en", "en")
    assert res1["translated_text"] == "hello"

    # Test Rule-based Banglish translation
    res2 = await bot.translate("আমি", "bn", "banglish")
    assert res2["translated_text"] == "ami"

    # Test LLM translation
    res3 = await bot.translate("I am fine", "en", "banglish")
    assert res3["translated_text"] == "Ami bhalo achi"
    assert res3["method_used"] == "llm_contextual"


@pytest.mark.asyncio
async def test_voice_didi_command():
    """Test VoiceDidi command parsing."""
    router = MockModelRouter()
    bot = BhashaBot(model_router=router)
    didi = VoiceDidi(model_router=router, bhasha_bot=bot)

    res = await didi.process_voice_command(audio_duration_ms=5000, transcript_hint="ami bhat khai")
    assert res["success"] is True
    assert res["intent"] == "order"
    assert res["confidence"] == 0.95


@pytest.mark.asyncio
async def test_agent_breeder():
    """Test genetic breeding (crossover and mutation)."""
    # বাংলা মন্তব্য: মক ডিবি সেশন পাস করে ব্রিডার টেস্ট
    db_mock = AsyncMock(spec=AsyncSession)
    config = BreederConfig(
        mutation_rate=0.1,
        crossover_rate=0.9,
        elite_ratio=0.1,
        tournament_size=2,
        max_generations=10,
        llm_temperature=0.3,
        llm_model_name="mock-gemini",
    )
    breeder = AgentBreeder(db_mock, config=config)

    p1 = {
        "prompt_dna": {"system_prompt": "Prompt A"},
        "tool_dna": {},
        "routing_dna": {},
    }
    p2 = {
        "prompt_dna": {"system_prompt": "Prompt B"},
        "tool_dna": {},
        "routing_dna": {},
    }

    child = await breeder._crossover.crossover(p1, p2)  # crossover strategy object call
    assert child is not None


def test_performance_oracle():
    """Test metrics oracle and alert triggering."""
    # বাংলা মন্তব্য: ওরাল মডিউলের জন্য মক ডিবি সেশন টেস্ট
    db_mock = AsyncMock(spec=AsyncSession)
    config = OracleConfig(
        weight_response_time=0.25,
        weight_accuracy=0.35,
        weight_cost=0.20,
        weight_error_rate=0.20,
        weak_link_threshold=0.40,
        retrain_threshold=0.50,
        replace_threshold=0.30,
        deprecate_threshold=0.15,
        lookback_hours=24,
        min_sample_size=10,
    )
    oracle = PerformanceOracle(db_mock, config=config)

    # rank_percentile computation helper test
    pct = oracle._rank_percentile(150.0, [100.0, 200.0, 150.0], lower_is_better=True)
    assert pct is not None
    assert 0.0 <= pct <= 1.0


@pytest.mark.asyncio
async def test_churn_prophet():
    """Test ChurnProphet prediction flow."""
    # বাংলা মন্তব্য: ChurnProphet এর এআই মেথড মক করে টেস্ট করা
    prophet = ChurnProphet()

    # Mock behavior signals from Firestore
    mock_signals = {
        "days_since_active": 30,
        "session_freq_change": -30.0,
        "feature_usage_change": -15.0,
        "support_tickets_recent": 5,
        "payment_delay_days": 14,
        "account_age_days": 80,
        "total_sessions": 15,
        "user_context": {"name": "Test User", "plan": "pro"},
    }

    mock_strategy = {
        "content": '{"churn_risk_score": 0.85, "risk_level": "critical", "factors": ["low logins"], "recommended_actions": ["send discount"]}'
    }

    with (
        patch.object(
            prophet,
            "_fetch_user_signals",
            new_callable=AsyncMock,
            return_value=mock_signals,
        ),
        patch(
            "services.llm.llm_router.LLMRouter.route",
            new_callable=AsyncMock,
            return_value=mock_strategy,
        ),
    ):
        res = await prophet.get_retention_strategy("tenant_123", "user_123")
        assert res is not None
        assert res.risk_level.value == "critical"
        assert len(res.strategies) > 0


@pytest.mark.asyncio
async def test_insight_mage():
    """Test InsightMage report generation."""
    # বাংলা মন্তব্য: InsightMage এর রিপোর্ট জেনারেশন মেথড মক করে টেস্ট করা
    mage = InsightMage()

    # Mock Firestore time-series fetch
    mock_time_series = ([100.0, 110.0, 105.0, 95.0, 150.0], [])

    mock_report = {"content": "## Summary\nThe sales are showing an upward trend of 15%."}

    with (
        patch.object(
            mage,
            "_fetch_time_series",
            new_callable=AsyncMock,
            return_value=mock_time_series,
        ),
        patch(
            "services.llm.llm_router.LLMRouter.route",
            new_callable=AsyncMock,
            return_value=mock_report,
        ),
    ):
        res = await mage.generate_report("tenant_123", "sales_data", "revenue", force_refresh=True)
        assert res is not None
        assert "Summary" in res.sections[0]["title"]
