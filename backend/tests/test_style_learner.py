from unittest.mock import AsyncMock, patch
import pytest

from tools.style_learner import StyleLearner
try:
    from brain.model_router import ModelRouter
except ImportError:
    from backend.brain.model_router import ModelRouter

# বাংলা মন্তব্য: স্টাইল লার্নার টুলটির ফাংশনালিটি যাচাই করার জন্য টেস্ট কেস লেখা হচ্ছে।

@pytest.mark.anyio
async def test_default_style_guidelines():
    # বাংলা মন্তব্য: যদি রিপোজিটরিতে কোনো কোড ফাইল না থাকে বা লার্নিং ব্যর্থ হয়, তাহলে স্ট্যান্ডার্ড ডিফল্ট গাইডলাইন ফেরত দিচ্ছে কিনা তা পরীক্ষা করা হচ্ছে।
    learner = StyleLearner()
    guidelines = learner._default_guidelines()
    
    assert "python" in guidelines
    assert "typescript" in guidelines
    assert guidelines["python"]["naming_convention"] == "snake_case"


@pytest.mark.anyio
async def test_extract_style_guidelines():
    # বাংলা মন্তব্য: মক এলএলএম রেসপন্স ব্যবহার করে স্টাইল গাইডলাইন এক্সট্র্যাক্ট করার টেস্ট।
    # বাংলা মন্তব্য: রিয়েল মডেল রাউটার কল বন্ধ করতে patch.object ব্যবহার করে ক্লাস মেথডটি মক করা হলো
    with patch.object(ModelRouter, "async_route_and_generate", new_callable=AsyncMock) as mock_generate:
        mock_generate.return_value = {
            "text": '{"python": {"naming_convention": "pep8"}, "typescript": {"quotes": "double"}, "general_patterns": ["Test pattern"]}'
        }
        
        learner = StyleLearner()
        # একটি অস্থায়ী ফোল্ডার পাথ দিয়ে টেস্ট রান করছি
        guidelines = await learner.extract_style_guidelines("backend/tools")
        
        assert guidelines["python"]["naming_convention"] == "pep8"
        assert guidelines["typescript"]["quotes"] == "double"
