from tools.marketplace_agent import MarketplaceAgent

def test_marketplace_search():
    agent = MarketplaceAgent()
    results = agent.search_marketplaces("pdf", categories=["npm"])
    assert len(results) > 0
    assert all(r["marketplace"] == "npm" for r in results)

def test_marketplace_search_filters():
    agent = MarketplaceAgent()
    filters = {"min_stars": 1000}
    results = agent.search_marketplaces("pdf", filters=filters)
    assert all(r["stars"] >= 1000 for r in results)

def test_marketplace_install():
    agent = MarketplaceAgent()
    res = agent.install_tool("npm:pdf-parse", "supremeai-worker-01", sandbox=True)
    assert res["success"] is True
    assert res["sandboxed"] is True
