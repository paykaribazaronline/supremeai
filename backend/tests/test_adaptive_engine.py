import pytest
from unittest.mock import MagicMock
from adaptive_engine.registry import PlatformRegistry, PlatformProfile
from adaptive_engine.intent_parser import IntentParser
from adaptive_engine.experience_db import ExperienceDatabase, Experience
from adaptive_engine.platform_learner import PlatformLearner
from brain.model_router import ModelRouter

def test_platform_registry():
    registry = PlatformRegistry()
    
    # Check preloaded
    github = registry.get_platform("github")
    assert github is not None
    assert github.display_name == "GitHub"
    assert "oauth2" in github.auth_methods
    
    # Register new
    new_profile = PlatformProfile(
        name="customcloud",
        display_name="Custom Cloud",
        category="cloud",
        auth_methods=["api_key"],
        capabilities=["compute"],
        deploy_methods=["api"]
    )
    registry.register_platform(new_profile)
    
    retrieved = registry.get_platform("customcloud")
    assert retrieved is not None
    assert retrieved.display_name == "Custom Cloud"


def test_intent_parser():
    fake_router = MagicMock()
    fake_router.route_and_generate.return_value = {
        "text": """
        {
          "app_type": "blog",
          "features": ["auth", "comments"],
          "tech_stack": {"frontend": "react", "backend": "fastapi"},
          "pages": ["home", "detail"],
          "integrations": [],
          "deployment_target": "vercel",
          "clarification_question": null
        }
        """
    }
    
    parser = IntentParser(fake_router)
    spec = parser.parse_intent("I want a react blog deployed to Vercel")
    
    assert spec.app_type == "blog"
    assert "auth" in spec.features
    assert spec.tech_stack["frontend"] == "react"
    assert spec.deployment_target == "vercel"


def test_experience_db(tmp_path):
    db_file = tmp_path / "test_experience.db"
    db = ExperienceDatabase(db_path=str(db_file))
    
    exp = Experience(
        user_id="user-123",
        request="I want a blog",
        context={"app_type": "blog"},
        action_taken="Code generation",
        result="success",
        what_worked=["parsed"],
        what_failed=[]
    )
    
    row_id = db.record_experience(exp)
    assert row_id > 0
    
    list_exp = db.get_experiences()
    assert len(list_exp) == 1
    assert list_exp[0].user_id == "user-123"
    assert list_exp[0].context["app_type"] == "blog"


@pytest.mark.anyio
async def test_platform_learner():
    fake_router = MagicMock()
    # Mock async_route_and_generate
    async def mock_async_route_and_generate(*args, **kwargs):
        return {
            "text": """
            {
              "display_name": "Cool Cloud",
              "category": "cloud",
              "auth_methods": ["oauth2"],
              "capabilities": ["hosting"],
              "deploy_methods": ["git_push"],
              "sdk_code": "class CoolCloudClient:\\n    pass",
              "api_endpoints": {"deploy": "/v1/deploy"}
            }
            """
        }
    fake_router.async_route_and_generate = mock_async_route_and_generate
    
    registry = PlatformRegistry()
    learner = PlatformLearner(fake_router, registry)
    
    profile = await learner.learn_from_docs("coolcloud", "https://docs.coolcloud.io")
    assert profile.display_name == "Cool Cloud"
    assert "oauth2" in profile.auth_methods
    assert "hosting" in profile.capabilities
    
    # Check it is in registry
    assert registry.get_platform("coolcloud") is not None
