#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# file >> auto_pr_pipeline.py
# project >> SupremeAI 2.0
# purpose >> Auto remediation
# module >> tools
# ============================================================================
import subprocess
from loguru import logger
from typing import List, Dict, Any
from tools.github_agent import GitHubAgent

class AutoPRPipeline:
    """
    Automates the entire process of creating a branch, committing changes, pushing, and opening a PR.
    (Closes Devin Gap #5)
    """

    def __init__(self):
        self.github_agent = GitHubAgent()
        logger.info("Initialized AutoPRPipeline")

    def _run_git_command(self, command: List[str], cwd: str = ".") -> str:
        """Helper to run git commands safely."""
        try:
            result = subprocess.run(
                ["git"] + command,
                cwd=cwd,
                capture_output=True,
                text=True,
                check=True
            )
            return result.stdout.strip()
        except subprocess.CalledProcessError as e:
            logger.error(f"Git command failed: git {' '.join(command)}")
            logger.error(e.stderr)
            raise RuntimeError(f"Git error: {e.stderr}")

    def execute_pipeline(self, repo_path: str, branch_name: str, commit_message: str, pr_title: str, pr_body: str, target_repo: str) -> Dict[str, Any]:
        """Runs the full PR pipeline."""
        logger.info(f"Starting Auto PR Pipeline for {repo_path}")
        
        try:
            # 1. Ensure clean state (optional, or stash)
            # self._run_git_command(["stash"], cwd=repo_path)
            
            # 2. Checkout new branch
            self._run_git_command(["checkout", "-b", branch_name], cwd=repo_path)
            logger.info(f"Checked out branch: {branch_name}")
            
            # 3. Add all changes
            self._run_git_command(["add", "."], cwd=repo_path)
            
            # 4. Commit
            self._run_git_command(["commit", "-m", commit_message], cwd=repo_path)
            logger.info(f"Committed changes: {commit_message}")
            
            # 5. Push
            self._run_git_command(["push", "-u", "origin", branch_name], cwd=repo_path)
            logger.info(f"Pushed branch {branch_name} to origin")
            
            # 6. Create PR via GitHub Agent (assuming it has a create_pr method)
            # This is a wrapper around the GitHub Agent
            pr_result = self.github_agent.create_improvement_pr(
                repo_name=target_repo,
                improvements={"*": commit_message},
                branch=branch_name
            )
            
            logger.info(f"PR Created successfully: {pr_result}")
            return {
                "status": "success",
                "branch": branch_name,
                "pr_result": pr_result
            }
            
        except Exception as e:
            logger.error(f"Auto PR Pipeline failed: {str(e)}")
            return {"status": "error", "error": str(e)}
