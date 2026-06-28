import json
import subprocess
import time
from typing import Any

from loguru import logger


class DependencyManagerAgent:
    """
    An agent to automatically manage project dependencies.
    - Scans for outdated packages.
    - Checks for known security vulnerabilities.
    - Can attempt to auto-update packages and create a PR.
    """

    def __init__(self):
        logger.info("Initialized DependencyManagerAgent")
        try:
            from tools.auto_pr_pipeline import AutoPRPipeline

            self.pr_pipeline = AutoPRPipeline()
        except ImportError:
            self.pr_pipeline = None

    def _run_command(
        self, command: list[str], check_exit_code: bool = True
    ) -> dict[str, Any]:
        """Runs a command and returns its JSON output."""
        try:
            result = subprocess.run(
                command,
                capture_output=True,
                text=True,
                check=check_exit_code,
            )
            # For commands that return non-zero on success with findings (like npm audit)
            # we might get here with check_exit_code=False. The output could be on stdout or stderr.
            output_to_parse = result.stdout if result.stdout.strip() else result.stderr
            if not output_to_parse.strip():
                return {}  # No output to parse, return empty dict

            return json.loads(output_to_parse)
        except FileNotFoundError:
            logger.error(f"Command not found: {command[0]}. Is it installed?")
            return {"error": f"{command[0]} not found."}
        except subprocess.CalledProcessError as e:
            logger.error(f"Command failed: {e.stderr}")
            return {"error": e.stderr}
        except json.JSONDecodeError:
            logger.error("Failed to parse command output as JSON.")
            return {"error": "Invalid JSON output."}
        except Exception as e:
            logger.error(f"An unexpected error occurred: {e}")
            return {"error": str(e)}

    def check_npm_dependencies(self, project_path: str) -> dict[str, Any]:
        """
        Checks for outdated npm packages in a project.
        """
        logger.info(f"Checking npm dependencies for {project_path}")
        outdated_command = ["npm", "outdated", "--json", "--prefix", project_path]

        outdated_data = self._run_command(outdated_command)

        if "error" in outdated_data:
            return {"success": False, "error": outdated_data["error"]}

        return {
            "success": True,
            "outdated_packages": outdated_data,
            "count": len(outdated_data),
            "recommendation": "Run 'npm update' or use 'npm-check-updates' to upgrade packages.",
        }

    def check_pip_dependencies(self) -> dict[str, Any]:
        """
        Checks for outdated pip packages in the current environment.
        """
        logger.info("Checking pip dependencies")
        # Using --format json is crucial for parsing
        outdated_command = ["pip", "list", "--outdated", "--format", "json"]

        outdated_data = self._run_command(outdated_command)

        if "error" in outdated_data:
            return {"success": False, "error": outdated_data["error"]}

        return {
            "success": True,
            "outdated_packages": outdated_data,
            "count": len(outdated_data),
            "recommendation": "Run 'pip install --upgrade <package>' for each outdated package.",
        }

    def check_pip_vulnerabilities(self) -> dict[str, Any]:
        """
        Scans for vulnerabilities in pip packages using pip-audit.
        """
        logger.info("Scanning pip dependencies for vulnerabilities with pip-audit...")
        # Requires `pip install pip-audit`
        vuln_command = ["pip-audit", "--format", "json"]

        # pip-audit exits with 1 if vulnerabilities are found, so we don't check exit code
        vuln_data = self._run_command(vuln_command, check_exit_code=False)

        if "error" in vuln_data:
            return {"success": False, "error": vuln_data["error"]}

        return {
            "success": True,
            "vulnerabilities": vuln_data.get("vulnerabilities", []),
            "count": len(vuln_data.get("vulnerabilities", [])),
        }

    def check_npm_vulnerabilities(self, project_path: str) -> dict[str, Any]:
        """
        Scans for vulnerabilities in npm packages using npm audit.
        """
        logger.info(f"Scanning npm dependencies for vulnerabilities in {project_path}")
        vuln_command = ["npm", "audit", "--json", "--prefix", project_path]

        # npm audit exits with 1 if vulnerabilities are found
        vuln_data = self._run_command(vuln_command, check_exit_code=False)

        if "error" in vuln_data:
            return {"success": False, "error": vuln_data["error"]}

        # The summary is in the 'metadata' or 'summary' field
        return {"success": True, "audit_results": vuln_data}

    async def auto_update_and_pr(
        self, repo_path: str, package_name: str, package_manager: str = "pip"
    ):
        """Automates updating a dependency and creating a PR."""
        if not self.pr_pipeline:
            logger.error("AutoPRPipeline is not available. Cannot create PR.")
            return {"status": "error", "message": "AutoPRPipeline not found."}

        logger.info(
            f"Attempting to auto-update '{package_name}' using {package_manager} in {repo_path}"
        )

        # Define update command
        if package_manager == "pip":
            update_command = ["pip", "install", "--upgrade", package_name]
        elif package_manager == "npm":
            update_command = [
                "npm",
                "install",
                f"{package_name}@latest",
                "--prefix",
                repo_path,
            ]
        else:
            return {
                "status": "error",
                "message": f"Unsupported package manager: {package_manager}",
            }

        # Run the update
        update_result = self._run_command(update_command)
        if "error" in update_result:
            return {
                "status": "error",
                "message": f"Failed to update package: {update_result['error']}",
            }

        # Create a PR with the changes
        branch_name = f"chore/update-{package_name}-{int(time.time())}"
        title = f"chore: Update {package_manager} dependency {package_name}"
        body = f"Automatically updated `{package_name}` to the latest version."

        # This assumes the CI runner has git configured and write access to the repo
        return await self.pr_pipeline.execute_pipeline(
            repo_path=repo_path,
            branch_name=branch_name,
            commit_message=title,
            pr_title=title,
            pr_body=body,
            target_repo="your-org/your-repo-name",  # Replace with your repo name
        )
