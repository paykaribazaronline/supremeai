import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from tools.graph_service import GraphService

# বাংলা মন্তব্য: Neo4j নলেজ গ্রাফ সার্ভিসের লজিক টেস্ট করা হচ্ছে।

@pytest.mark.anyio
async def test_graph_service_dry_run():
    # বাংলা মন্তব্য: ক্রেডেনশিয়াল ছাড়া মক মোড ঠিকমতো কাজ করছে কিনা তার টেস্ট।
    with patch.dict("os.environ", {}, clear=True):
        service = GraphService()
        assert service.dry_run is True
        
        sync_result = await service.sync_skills_to_graph([{"id": "1", "name": "Python", "category": "Coding"}])
        assert sync_result is True

        rel_result = await service.create_relationship("1", "2", "DEPENDS_ON")
        assert rel_result is True

        path = await service.get_skill_path("Python", "FastAPI")
        assert "Dry-run Path Node 1" in path

@pytest.mark.anyio
async def test_graph_service_real_connection():
    # বাংলা মন্তব্য: ক্রেডেনশিয়াল থাকলে AsyncGraphDatabase ড্রাইভার কল হচ্ছে কিনা তা যাচাই করা।
    env_vars = {
        "NEO4J_URI": "bolt://mock-uri",
        "NEO4J_USER": "neo4j",
        "NEO4J_PASSWORD": "mock_password"
    }
    
    with patch.dict("os.environ", env_vars):
        with patch("tools.graph_service.AsyncGraphDatabase.driver") as mock_driver:
            mock_instance = AsyncMock()
            mock_driver.return_value = mock_instance
            
            service = GraphService()
            assert service.dry_run is False
            
            # সেশন এবং ট্রানজ্যাকশন মক করা
            mock_session = AsyncMock()
            mock_instance.session = MagicMock()
            mock_instance.session.return_value.__aenter__.return_value = mock_session
            
            await service.sync_skills_to_graph([
                {"id": "1", "name": "Python", "category": "Coding", "success_rate": 0.9}
            ])
            
            # চেক করা হচ্ছে যে session.run কল হয়েছে
            mock_session.run.assert_called_once()
            
            # ড্রাইভার ক্লোজ করা
            await service.close()
            mock_instance.close.assert_called_once()
