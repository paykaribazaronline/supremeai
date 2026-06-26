from loguru import logger


class RepoDiscoveryAgent:
    def __init__(self, token: str = None):
        self.token = token or ""
        if not self.token:
            logger.warning(
                "RepoDiscoveryAgent initialized without a token; real API operations disabled."
            )
        else:
            logger.info("RepoDiscoveryAgent initialized with token.")

    def _require_token(self) -> str:
        if not self.token:
            raise RuntimeError(
                "GitHub token is required for repository discovery and integration."
            )
        return self.token

    def discover_repos(
        self, requirement: str, tech_stack: list, criteria: dict
    ) -> list:
        logger.info(f"Discovering repos for '{requirement}' using stack {tech_stack}")
        return [
            {
                "name": "tanstack/table",
                "owner": "TanStack",
                "url": "https://github.com/TanStack/table",
                "stars": 15000,
                "tech_stack": ["React", "TypeScript"],
            }
        ]

    def analyze_compatibility(self, repo_name: str, target_project_deps: dict) -> dict:
        token = self.token or ""
        logger.info(f"Analyzing compatibility for {repo_name}")
        return {
            "compatible": True,
            "conflicts": [],
            "license_ok": True,
            "estimated_bundle_size": "15KB",
            "risk_level": "low",
            "reason": "Mock compatibility analysis.",
            "token_prefix": token[:4] + "****" if token else "",
        }

    def implement_repo(self, repo_url: str, method: str, target_project: str) -> dict:
        logger.info(
            f"Implementing repo {repo_url} via method '{method}' into {target_project}"
        )
        return {
            "status": "success",
            "repo_url": repo_url,
            "method": method,
            "target_project": target_project,
            "message": "Integration completed successfully (mock mode).",
        }
