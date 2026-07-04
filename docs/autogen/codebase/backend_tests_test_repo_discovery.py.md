# 📄 ফাইল: backend/tests/test_repo_discovery.py

**প্রকার:** .py  
**সাইজ:** 723 বাইট  
**আপডেট:** 2026-07-04T08:51:16.286909

---

## কোড

```py
from tools.repo_discovery_agent import RepoDiscoveryAgent


def test_discover_repos():
    agent = RepoDiscoveryAgent()
    repos = agent.discover_repos("table", ["React"], {"min_stars": 500})
    assert len(repos) > 0
    assert any(r["name"] == "tanstack/table" for r in repos)


def test_analyze_compatibility():
    agent = RepoDiscoveryAgent()
    res = agent.analyze_compatibility("tanstack/table", {})
    assert res["compatible"] is True
    assert res["risk_level"] == "low"


def test_implement_repo():
    agent = RepoDiscoveryAgent()
    res = agent.implement_repo(
        "https://github.com/TanStack/table", "npm", "customer-app"
    )
    assert res["status"] == "success"
    assert "npm" in res["method"]

```