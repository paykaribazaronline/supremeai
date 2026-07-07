# 📄 ফাইল: test_pr_dry_run.py

**প্রকার:** .py  
**সাইজ:** 1,704 বাইট  
**আপডেট:** 2026-07-07T22:11:19.716830

---

## কোড

```py
import asyncio
from unittest.mock import patch, MagicMock
from backend.services.github_agent import create_autonomous_pr

async def test_dry_run_pr():
    print("Testing create_autonomous_pr in dry-run mode...")
    with patch('backend.services.github_agent.httpx.AsyncClient') as mock_client_cls:
        mock_client = MagicMock()
        mock_client_cls.return_value.__aenter__.return_value = mock_client
        
        # Setup mock responses
        mock_repo_info = MagicMock()
        mock_repo_info.status_code = 200
        mock_repo_info.json.return_value = {"default_branch": "main"}
        
        mock_ref_info = MagicMock()
        mock_ref_info.status_code = 200
        mock_ref_info.json.return_value = {"object": {"sha": "mock_sha_123"}}
        
        mock_branch_res = MagicMock()
        mock_branch_res.status_code = 201
        
        mock_commit_res = MagicMock()
        mock_commit_res.status_code = 201
        
        mock_pr_response = MagicMock()
        mock_pr_response.status_code = 201
        mock_pr_response.json.return_value = {"html_url": "https://github.com/mock/pr/1"}
        
        mock_client.get.side_effect = [mock_repo_info, mock_ref_info]
        mock_client.post.side_effect = [mock_branch_res, mock_pr_response]
        mock_client.put.return_value = mock_commit_res
        
        pr_url = await create_autonomous_pr(
            user_id="test_user",
            repo_name="test/repo",
            file_path="test.py",
            code_content="print('hello')",
            commit_msg="Test dry run"
        )
        
        print(f"PR Dry Run Success! Mock PR URL: {pr_url}")

if __name__ == "__main__":
    asyncio.run(test_dry_run_pr())

```