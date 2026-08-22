import asyncio

import pytest

from tools.browser.web_fallback_agent import WebFallbackAgent


@pytest.mark.skip(
    reason="Playwright browser binaries not installed in this CI/sandbox environment (BrowserType.launch fails). Run `playwright install` in the CI job. Tracked in FAILING_TESTS.md."
)
def test_web_fallback():
    agent = WebFallbackAgent()
    task = {"action": "Convert PDF to Text"}
    res = asyncio.run(agent.use_web_version("iLovePDF", "https://www.ilovepdf.com", task))
    assert res["success"] is True
    assert len(res["steps_executed"]) > 0
    assert any(term in res["result_summary"].lower() for term in ["automated", "completed", "success"])
