import asyncio
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from tools.devops.github_agent import create_autonomous_pr


@pytest.mark.asyncio
async def test_dry_run_pr(async_session):
    # Testing create_autonomous_pr in dry-run mode
    with patch("tools.devops.github_agent.httpx.AsyncClient") as mock_client_cls:
        mock_client = AsyncMock()
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
        mock_pr_response.json.return_value = {
            "html_url": "https://github.com/mock/pr/1",
            "number": 1,
        }

        mock_client.get.side_effect = [mock_repo_info, mock_ref_info]
        mock_client.post.side_effect = [mock_branch_res, mock_pr_response]
        mock_client.put.return_value = mock_commit_res

        mock_client.put.return_value = mock_commit_res

        with patch(
            "tools.devops.github_agent.get_user_github_token",
            return_value="mock_token",
        ):
            await create_autonomous_pr(
                user_id="test_user",
                repo_name="test/repo",
                file_path="test.py",
                code_content="print('hello')",
                commit_msg="Test dry run",
                db=async_session,
            )

        # Mock PR Dry Run Successful


if __name__ == "__main__":
    asyncio.run(test_dry_run_pr())
