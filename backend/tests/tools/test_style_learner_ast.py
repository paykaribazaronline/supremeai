# বাংলা মন্তব্য: Style Learner-এর tree-sitter AST analysis ফাংশনালিটি টেস্ট।

from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from tools.style_learner import StyleLearner


@pytest.mark.anyio
async def test_analyze_codebase_ast():
    # বাংলা মন্তব্য: Codebase-এর AST-level pattern বিশ্লেষণ টেস্ট
    learner = StyleLearner()

    with patch("brain.model_router.ModelRouter.async_route_and_generate", new_callable=AsyncMock) as mock_generate:
        mock_generate.return_value = {"text": '{"naming_convention": "snake_case", "function_length": 20, "import_style": "isort"}'}
        )

        # Create a mock directory structure
        with patch("tools.style_learner.Path") as mock_path:
            mock_path_instance = MagicMock()
            mock_path_instance.rglob.return_value = [MagicMock(suffix=".py", read_text=MagicMock(return_value="def my_function():\n    pass"))]
            mock_path.return_value = mock_path_instance

            result = await learner.analyze_codebase("backend/tools")

    assert result is not None
    assert "naming_convention" in result


@pytest.mark.anyio
async def test_generate_with_style():
    # বাংলা মন্তব্য: ব্যবহারকারীর স্টাইল অনুযায়ী কোড জেনারেট করা হচ্ছে
    learner = StyleLearner()

    with patch("brain.model_router.ModelRouter.async_route_and_generate", new_callable=AsyncMock) as mock_generate:
        mock_generate.return_value = {"text": "def my_snake_case_function():\n    # Generated with user style\n    pass"}
        )

        result = await learner.generate_with_style("Create a function", "user_123")

    assert result is not None
    assert "def" in result


@pytest.mark.anyio
async def test_sync_team_style():
    # বাংলা মন্তব্য: টিম স্টাইল সিঙ্ক টেস্ট
    learner = StyleLearner()

    with patch("brain.model_router.ModelRouter.async_route_and_generate", new_callable=AsyncMock) as mock_generate:
        mock_generate.return_value = {"text": '{"naming_convention": "snake_case", "confidence": 0.95}'}
        )

        result = await learner._detect_naming_convention(code_samples)

    assert result["naming_convention"] == "snake_case"



