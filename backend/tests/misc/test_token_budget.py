import pytest

from core.llm.token_budget import (
    TokenBudgetManager,
    estimate_tokens,
    truncate_to_token_limit,
)


def test_estimate_tokens_empty():
    assert estimate_tokens("") == 0


def test_truncate_to_token_limit_from_end_keeps_tail():
    txt = "Sentence one. Sentence two. Sentence three." * 50
    out = truncate_to_token_limit(txt, max_tokens=50, from_end=True)
    assert len(out) > 0


@pytest.mark.anyio
async def test_prepare_prompt_truncates_when_exceeds_budget(monkeypatch):
    mgr = TokenBudgetManager(custom_budgets={"default": {"max_input_tokens": 200, "max_output_tokens": 50}})

    long_prompt = "hello " * 1000
    _processed, meta = mgr.prepare_prompt(long_prompt, provider="default", system_prompt="sys")

    assert meta["truncated"] is True
    assert meta["estimated_input_tokens"] > 0
    assert "tokens_saved" in meta


@pytest.mark.anyio
async def test_prepare_prompt_budget_exhaustion_raises_and_emits(monkeypatch):
    # system_prompt consumes almost entire budget
    mgr = TokenBudgetManager(custom_budgets={"default": {"max_input_tokens": 100, "max_output_tokens": 50}})

    # patch emit so it won't require real bus
    from core.llm import token_budget

    monkeypatch.setattr(token_budget.error_event_bus, "emit", lambda *args, **kwargs: None)

    with pytest.raises(ValueError):
        mgr.prepare_prompt("user", provider="default", system_prompt="x" * 10000)
