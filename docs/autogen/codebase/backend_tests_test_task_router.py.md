# 📄 ফাইল: backend/tests/test_task_router.py

**প্রকার:** .py  
**সাইজ:** 3,264 বাইট  
**আপডেট:** 2026-07-11T11:05:10.216100

---

## কোড

```py
import os


os.environ.setdefault("OPENROUTER_API_KEY", "")
os.environ.setdefault("HF_API_KEY", "")
os.environ.setdefault("OLLAMA_URL", "http://127.0.0.1:11434")

import asyncio
from unittest.mock import MagicMock
from unittest.mock import patch

import pytest


@pytest.fixture
def router():
    from core.task_router import TaskRouter

    return TaskRouter()


class TestTaskRouterProcessRequirement:
    @pytest.mark.parametrize(
        "desc,expected_type",
        [
            ("write a python script to sort a list", "coding"),
            ("scrape data from example.com", "web_scraping_local"),
            ("run a system command to list files", "system_control"),
            ("generate an image of a sunset", "image_generation"),
            ("what is the weather today", "general"),
        ],
    )
    def test_task_type_detection(self, router, desc, expected_type):
        result = router.process_requirement(desc)
        assert result["task_type"] == expected_type

    def test_token_budget_small(self, router):
        result = router.process_requirement("hello", max_cost=0.01)
        assert result["token_budget"] == "small"

    def test_token_budget_medium(self, router):
        result = router.process_requirement("x" * 600, max_cost=0.01)
        assert result["token_budget"] == "medium"

    def test_token_budget_large(self, router):
        result = router.process_requirement("x" * 2100, max_cost=0.01)
        assert result["token_budget"] == "large"

    @pytest.mark.parametrize(
        "desc,expected_modality",
        [
            ("look at this image", "image"),
            ("watch a video", "multimodal"),
            ("speak this text", "text"),
            ("analyze a photo", "image"),
            ("just type some text", "text"),
        ],
    )
    def test_modality_detection(self, router, desc, expected_modality):
        result = router.process_requirement(desc)
        assert result["modality"] == expected_modality

    @pytest.mark.parametrize(
        "desc,expected_depth",
        [
            ("do some math homework", "high"),
            ("analyze this dataset", "high"),
            ("research the history of rome", "high"),
            ("look at this picture", "medium"),
            ("watch this video", "medium"),
            ("say hello", "low"),
        ],
    )
    def test_reasoning_depth(self, router, desc, expected_depth):
        result = router.process_requirement(desc)
        assert result["reasoning_depth"] == expected_depth

    def test_cost_limit_passed_through(self, router):
        result = router.process_requirement("code task", max_cost=0.05)
        assert result["cost_limit"] == 0.05

    def test_analyze_and_route_alias(self, router):
        result = router.analyze_and_route("test prompt", max_cost=0.02)
        assert result["cost_limit"] == 0.02

    def test_draw_keyword_triggers_image_generation(self, router):
        result = router.process_requirement("draw a cat")
        assert result["task_type"] == "image_generation"

    def test_contains_image_keywords(self, router):
        result = router.process_requirement("generate an image of a tree")
        assert result["task_type"] == "image_generation"
        assert result["modality"] == "image"

```