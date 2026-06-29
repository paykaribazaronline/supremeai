import sys

import pytest

sys.path.append("../..")
from byoc.resource_manager import ResourceManager


class TestResourceManager:
    @pytest.mark.asyncio
    async def test_get_status(self):
        manager = ResourceManager()
        result = await manager.get_status("user1")
        assert result["user_id"] == "user1"
        assert "resources" in result
        assert "quota" in result

    @pytest.mark.asyncio
    async def test_list_resources(self):
        manager = ResourceManager()
        result = await manager.list_resources("user1")
        assert isinstance(result, list)
