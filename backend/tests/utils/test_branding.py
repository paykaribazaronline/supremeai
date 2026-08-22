from utils.branding import (
    MODEL_DISPLAY,
    PROVIDER_DISPLAY,
    _normalize,
    get_model_display_name,
    get_provider_display_name,
)


class TestNormalize:
    def test_none(self):
        assert _normalize(None) == ""

    def test_strips_and_lowercases(self):
        assert _normalize("  OpenAI ") == "openai"

    def test_empty_string(self):
        assert _normalize("") == ""


class TestGetModelDisplayName:
    def test_exact_match(self):
        assert get_model_display_name("gpt-4o") == "SupremeAI Core"

    def test_exact_match_anthropic(self):
        assert get_model_display_name("claude-3-opus") == "SupremeAI Reason Pro"

    def test_exact_match_google(self):
        assert get_model_display_name("gemini-1.5-pro") == "SupremeAI Vision"

    def test_partial_match_versioned(self):
        # gpt-4o-2024-05-13 should resolve via prefix to gpt-4o
        assert get_model_display_name("gpt-4o-2024-05-13") == "SupremeAI Core"

    def test_partial_match_prefix(self):
        assert get_model_display_name("gemini-2.0-flash") == "SupremeAI Vision"

    def test_case_insensitive(self):
        assert get_model_display_name("GPT-4O") == "SupremeAI Core"

    def test_whitespace_normalized(self):
        assert get_model_display_name("  claude-3-opus ") == "SupremeAI Reason Pro"

    def test_unknown_returns_default(self):
        assert get_model_display_name("some-unknown-model") == "SupremeAI Core"

    def test_none_returns_default(self):
        assert get_model_display_name(None) == "SupremeAI Core"

    def test_empty_returns_default(self):
        assert get_model_display_name("") == "SupremeAI Core"

    def test_all_registered_models_resolve(self):
        for key in MODEL_DISPLAY:
            assert get_model_display_name(key) == MODEL_DISPLAY[key]["label"]


class TestGetProviderDisplayName:
    def test_exact_match(self):
        assert get_provider_display_name("openai") == "SupremeAI Core"

    def test_exact_match_anthropic(self):
        assert get_provider_display_name("anthropic") == "SupremeAI Reason"

    def test_partial_contains(self):
        assert get_provider_display_name("openai-chat") == "SupremeAI Core"

    def test_case_insensitive(self):
        assert get_provider_display_name("Google") == "SupremeAI Vision"

    def test_whitespace_normalized(self):
        assert get_provider_display_name("  groq ") == "SupremeAI Llama"

    def test_unknown_returns_default(self):
        assert get_provider_display_name("unknown-provider") == "SupremeAI"

    def test_none_returns_default(self):
        assert get_provider_display_name(None) == "SupremeAI"

    def test_empty_returns_default(self):
        assert get_provider_display_name("") == "SupremeAI"

    def test_all_registered_providers_resolve(self):
        for key in PROVIDER_DISPLAY:
            assert get_provider_display_name(key) == PROVIDER_DISPLAY[key]
