import asyncio

from tools.web_fallback_agent import WebFallbackAgent


def test_web_fallback():
    agent = WebFallbackAgent()
    task = {"action": "Convert PDF to Text"}
    res = asyncio.run(agent.use_web_version("iLovePDF", "https://www.ilovepdf.com", task))
    assert res["success"] is True
    assert len(res["steps_executed"]) > 0
    assert "automated" in res["result_summary"]
