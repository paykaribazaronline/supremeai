import sys


sys.path.append("../..")
from byoc.cloud_connector import CloudStatus, list_resources, ping


class TestCloudConnector:
    def test_ping(self):
        import asyncio
        result = asyncio.run(ping())
        assert isinstance(result, CloudStatus)
        assert result.provider == "gcp"
        assert result.connected is False

    def test_list_resources(self):
        import asyncio
        result = asyncio.run(list_resources())
        assert isinstance(result, list)
        assert len(result) == 0
