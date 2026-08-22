import os

os.environ.setdefault("OPENROUTER_API_KEY", "")
os.environ.setdefault("HF_API_KEY", "")
os.environ.setdefault("OLLAMA_URL", "http://127.0.0.1:11434")


import pytest


@pytest.fixture
def router():
    from core.queue.task_router import TaskRouter

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


class TestSwarmLLMRouter:
    """বাংলা মন্তব্য: ৫টি কাস্টম HF ৩বি মডেলের রাউটিং এবং কী রোটেশন টেস্টের জন্য টেস্ট ক্লাস।"""

    def test_key_rotator_round_robin(self):
        from services.llm.llm_router import HFKeyRotator

        test_keys = ["key1", "key2", "key3"]
        rotator = HFKeyRotator(test_keys)
        assert rotator.get_key() == "key1"
        assert rotator.get_key() == "key2"
        assert rotator.get_key() == "key3"
        assert rotator.get_key() == "key1"

    def test_task_classification_routing(self):
        from services.llm.llm_router import HFSwarmRouter

        swarm_router = HFSwarmRouter()
        assert swarm_router.classify_task("Write a python function to parse JSON") == "coding"
        assert swarm_router.classify_task("Solve the equation x^2 + 5x + 6 = 0") == "reasoning"
        assert swarm_router.classify_task("Write a poem and creative story about space") == "creative"
        assert swarm_router.classify_task("Analyze this screenshot image of UI") == "vision"
        assert swarm_router.classify_task("Give me a fast draft answer") == "draft"
        assert swarm_router.classify_task("Step by step instructions " + "word " * 160) == "master"
        assert swarm_router.classify_task("Hello, how are you today?") == "general"

    def test_model_swarm_registry_mapping(self):
        from core.config import settings

        assert settings.MODEL_SWARM["coding"] == "njelit1/supreme-coder-3b"
        assert settings.MODEL_SWARM["reasoning"] == "njelitltd/supreme-reasoner-3b"
        assert settings.MODEL_SWARM["general"] == "ziaulhaq1/supreme-general-3b"
        assert settings.MODEL_SWARM["creative"] == "njelitltd2/supreme-creative-3b"
        assert settings.MODEL_SWARM["master"] == "njelitltd3/supreme-master-3b"
        assert settings.MODEL_SWARM["vision"] == "njelltd5/supreme-vision-3b"
        assert settings.MODEL_SWARM["draft"] == "njelltd4/supreme-draft-0.5b"
