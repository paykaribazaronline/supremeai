import asyncio
import subprocess
from typing import Any

from loguru import logger

from tools.github_agent import GitHubAgent


class AutoPRPipeline:
    """
    Automates the entire process of creating a branch, committing changes, pushing, and opening a PR.
    (Closes Devin Gap #5)
    """

    def __init__(self):
        self.github_agent = GitHubAgent()
        logger.info("Initialized AutoPRPipeline")

    async def _run_git_command(self, command: list[str], cwd: str = ".") -> str:
        """Helper to run git commands safely."""
        try:
            process = await asyncio.create_subprocess_exec(
                "git", *command, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.PIPE
            )
            stdout, stderr = await process.communicate()

            if process.returncode != 0:
                logger.error(f"Git command failed: git {' '.join(command)}")
                logger.error(stderr.decode())
                raise RuntimeError(f"Git error: {stderr.decode()}")

            return stdout.decode().strip()
        except FileNotFoundError:
            logger.error(
                "Git command not found. Ensure git is installed and in your PATH."
            )
            raise

    async def create_pr(
        self, repo: str, branch: str, title: str, body: str, files: dict[str, str]
    ) -> dict[str, Any]:
        """
        A more robust pipeline that handles file changes, branching, committing, and PR creation.
        """
        original_branch = await self._run_git_command(
            ["rev-parse", "--abbrev-ref", "HEAD"], cwd=repo
        )
        logger.info(f"Starting from branch: {original_branch}")

        try:
            # 1. Create and checkout a new branch
            await self._run_git_command(["checkout", "-b", branch], cwd=repo)
            logger.info(f"Checked out new branch: {branch}")

            # 2. Write file changes and add them to staging
            for file_path, content in files.items():
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(content)
                await self._run_git_command(["add", file_path], cwd=repo)
            logger.info(f"Staged {len(files)} file(s).")

            # 3. Commit changes
            commit_message = f"{title}\n\n{body}"
            await self._run_git_command(["commit", "-m", commit_message], cwd=repo)
            logger.info("Committed changes.")

            # 4. Push the new branch
            await self._run_git_command(["push", "-u", "origin", branch], cwd=repo)
            logger.info(f"Pushed branch '{branch}' to origin.")

            # 5. Create the pull request
            pr_result = self.github_agent.create_pr(
                repo_name=repo.split("/")[-2]
                + "/"
                + repo.rsplit("/", maxsplit=1)[-1],  # Assumes repo is a path like 'owner/name'
                title=title,
                body=body,
                head_branch=branch,
                base_branch=original_branch,
            )
            logger.info(f"Successfully created PR: {pr_result.get('pr_url')}")

            return {
                "status": "success",
                "pr_url": pr_result.get("pr_url"),
                "branch": branch,
            }

        except Exception as e:
            logger.error(f"Auto PR Pipeline failed: {e}")
            # Cleanup: checkout original branch and delete the new one
            logger.info("Cleaning up failed PR attempt...")
            await self._run_git_command(["checkout", original_branch], cwd=repo)
            await self._run_git_command(["branch", "-D", branch], cwd=repo)
            return {"status": "error", "error": str(e), "cleaned_up": True}

    async def execute_pipeline(
        self,
        repo_path: str,
        branch_name: str,
        commit_message: str,
        pr_title: str,
        pr_body: str,
        target_repo: str,
        base_branch: str | None = "main",
    ) -> dict[str, Any]:
        """Runs the full PR pipeline asynchronously."""
        logger.info(f"Starting Auto PR Pipeline for {repo_path}")
        original_branch = await self._run_git_command(
            ["rev-parse", "--abbrev-ref", "HEAD"], cwd=repo_path
        )

        try:
            # 1. Checkout new branch
            await self._run_git_command(["checkout", "-b", branch_name], cwd=repo_path)
            logger.info(f"Checked out branch: {branch_name}")

            # 2. Add all changes (assumes changes are already made)
            await self._run_git_command(["add", "."], cwd=repo_path)

            # 3. Commit
            await self._run_git_command(["commit", "-m", commit_message], cwd=repo_path)
            logger.info(f"Committed changes: {commit_message}")

            # 4. Push
            await self._run_git_command(
                ["push", "-u", "origin", branch_name], cwd=repo_path
            )
            logger.info(f"Pushed branch {branch_name} to origin")

            # 5. Create PR via GitHub Agent
            pr_result = self.github_agent.create_pr(
                repo_name=target_repo,
                title=pr_title,
                body=pr_body,
                head_branch=branch_name,
                base_branch=base_branch,
            )

            logger.info(f"PR Created successfully: {pr_result.get('pr_url')}")
            return {
                "status": "success",
                "branch": branch_name,
                "pr_url": pr_result.get("pr_url"),
            }

        except Exception as e:
            logger.error(f"Auto PR Pipeline failed: {str(e)}")
            # Cleanup failed branch
            await self._run_git_command(["checkout", original_branch], cwd=repo_path)
            await self._run_git_command(["branch", "-D", branch_name], cwd=repo_path)
            return {"status": "error", "error": str(e), "cleaned_up": True}
