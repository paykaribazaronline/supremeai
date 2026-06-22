from tools.github_agent import GitHubAgent

def test_github_agent_repo_connect():
    agent = GitHubAgent()
    res = agent.connect_repo("test-owner", "test-repo")
    assert res["status"] == "success"

def test_github_agent_analyze():
    agent = GitHubAgent()
    analysis = agent.analyze_repo("https://github.com/test/repo")
    assert "score" in analysis
    assert len(analysis["issues"]) > 0

def test_github_agent_pr_creation():
    agent = GitHubAgent()
    improvements = {"src/db.py": "Optimize pooling"}
    res = agent.create_improvement_pr("test/repo", improvements)
    assert res["status"] == "success"
    assert "supremeai-improvements" in res["branch"]
    assert "pr_url" in res
