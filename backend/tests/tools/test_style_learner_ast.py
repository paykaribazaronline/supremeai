# বাংলা মন্তব্য: Style Learner-এর tree-sitter AST analysis ফাংশনালিটি টেস্ট।

from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from tools.style_learner import StyleLearner


@pytest.mark.anyio
async def test_analyze_codebase_ast():
    # বাংলা মন্তব্য: Codebase-এর AST-level pattern বিশ্লেষণ টেস্ট
    learner = StyleLearner()

        with patch.object(learner, "_get_model_router") as mock_router:
            mock_router.return_value.async_route_and_generate = AsyncMock(
                return_value={"text": '{"naming_convention": "snake_case", "function_length": 20, "import_style": "isort"}'}
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

        with patch.object(learner, "_get_model_router") as mock_router:
            mock_router.return_value.async_route_and_generate = AsyncMock(
                return_value={"text": "def my_snake_case_function():\n    # Generated with user style\n    pass"}
            )

            result = await learner.generate_with_style("Create a function", "user_123")

    assert result is not None
    assert "def" in result


@pytest.mark.anyio
async def test_sync_team_style():
    # বাংলা মন্তব্য: টিম স্টাইল সিঙ্ক টেস্ট
    learner = StyleLearner()

        with patch.object(learner, "_get_model_router") as mock_router:
            mock_router.return_value.async_route_and_generate = AsyncMock(return_value={"text": '{"team_id": "team_123", "style": "consistent"}'})

            with patch("tools.style_learner.Path") as mock_path:
                mock_path_instance = MagicMock()
                mock_path_instance.rglob.return_value = [MagicMock(suffix=".py", read_text=MagicMock(return_value="def team_function():\n    pass"))]
                mock_path.return_value = mock_path_instance

                result = await learner.sync_team_style("backend/tools", "team_123")

    assert result is not None
    assert "team_id" in result


@pytest.mark.anyio
async def test_naming_convention_detection():
    # বাংলা মন্তব্য: Variable naming convention শনাক্ত করা হচ্ছে
    learner = StyleLearner()

    code_samples = [
        "def my_function():\n    my_variable = 1\n    return my_variable",
        "def another_function():\n    another_var = 2\n    return another_var",
    ]

        with patch.object(learner, "_get_model_router") as mock_router:
            mock_router.return_value.async_route_and_generate = AsyncMock(
                return_value={"text": '{"naming_convention": "snake_case", "confidence": 0.95}'}
            )

            result = await learner._detect_naming_convention(code_samples)

    assert result["naming_convention"] == "snake_case"


@pytest.mark.anyio
async def test_function_length_preference():
    # বাংলা মন্তব্য: Function length preference শনাক্ত করা হচ্ছে
    learner = StyleLearner()

    code_samples = ["def short_func():\n    return 1", "def medium_func():\n    a = 1\n    b = 2\n    return a + b"]

        with patch.object(learner, "_get_model_router") as mock_router:
            mock_router.return_value.async_route_and_generate = AsyncMock(return_value={"text": '{"avg_function_length": 2, "max_lines": 10}'})

            result = await learner._detect_function_length_preference(code_samples)

    assert "avg_function_length" in result


@pytest.mark.anyio
async def test_import_ordering_style():
    # বাংলা মন্তব্য: Import ordering style শনাক্ত করা হচ্ছে
    learner = StyleLearner()

    code_samples = ["import os\nimport sys\nfrom typing import List", "import json\nimport re"]

        with patch.object(learner, "_get_model_router") as mock_router:
            mock_router.return_value.async_route_and_generate = AsyncMock(
                return_value={"text": '{"import_order": "stdlib_first", "line_length": 88}'}
            )

            result = await learner._detect_import_ordering(code_samples)

    assert "import_order" in result
