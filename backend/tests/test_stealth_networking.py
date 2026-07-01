import pytest
import os
from unittest.mock import patch
from core.config import settings
from tools.cloud_sandbox_orchestrator import CloudSandboxOrchestrator
from tools.proxy_manager import ProxyManager
from tools.stealth_http_client import StealthHTTPClient

def test_production_sandbox_fails_without_docker():
    # Enforce production mode
    old_env = settings.env
    settings.env = "production"
    
    orchestrator = CloudSandboxOrchestrator()
    # Mock docker to fail
    with patch("docker.from_env", side_effect=Exception("Docker daemon down")):
        res = orchestrator.run_code("print('hello')")
        
    assert res["success"] is False
    assert "SecurityException" in res["stderr"]
    
    # Restore settings environment
    settings.env = old_env

def test_proxy_manager_rotates_proxies():
    # Setup custom env proxies
    with patch.dict(os.environ, {"SUPREMEAI_PROXIES": "http://proxy1:8080,http://proxy2:8080"}):
        mgr = ProxyManager()
        p1 = mgr.get_next_proxy()
        p2 = mgr.get_next_proxy()
        p3 = mgr.get_next_proxy()
        
        assert p1 == "http://proxy1:8080"
        assert p2 == "http://proxy2:8080"
        assert p3 == "http://proxy1:8080"  # rotates back

@pytest.mark.anyio
async def test_stealth_http_client_adds_headers_and_rotates():
    with patch.dict(os.environ, {"SUPREMEAI_PROXIES": "http://proxy1:8080"}):
        client = StealthHTTPClient()
        
        async def mock_request(*args, **kwargs):
            # Assert headers spoofing was populated
            assert "User-Agent" in kwargs["headers"]
            assert kwargs["proxy"] == "http://proxy1:8080"
            
            class MockResponse:
                def raise_for_status(self):
                    pass
            return MockResponse()

        with patch("httpx.AsyncClient.request", new=mock_request):
            res = await client.get("http://example.com")
            assert res is not None
