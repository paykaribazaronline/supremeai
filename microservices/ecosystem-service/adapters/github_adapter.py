"""GitHub API adapter — calls the real GitHub REST API.

বাংলা: GitHub REST API docs: https://docs.github.com/rest
test/demo মানের adapter — production-এ retry/rate-limit handling যোগ করতে হবে।
"""

from __future__ import annotations

from datetime import UTC, datetime
from typing import Any

import httpx

from config import settings
from ecosystem import BaseProviderAdapter
from ecosystem.health_model import HealthStatus


class GitHubAdapter(BaseProviderAdapter):
    """Calls https://api.github.com/repos/{owner}/{repo}."""

    BASE_URL = "https://api.github.com"
    provider = "github"  # type: ignore[assignment]

    def _headers(self) -> dict[str, str]:
        return {
            "Authorization": f"Bearer {settings.github_token}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
        }

    def _repo_url(self) -> str:
        return f"{self.BASE_URL}/repos/{settings.github_repo}"

    # -- Observe ops -------------------------------------------------------

    async def list_resources(self) -> list[dict[str, Any]]:
        """List recent workflow runs as 'child resources' (ROADMAP §37)."""
        try:
            async with httpx.AsyncClient(timeout=15) as client:
                resp = await client.get(
                    f"{self._repo_url()}/actions/runs",
                    headers=self._headers(),
                    params={"per_page": 10},
                )
                resp.raise_for_status()
                runs = resp.json().get("workflow_runs", [])
                return [
                    {
                        "id": r.get("id"),
                        "name": r.get("name"),
                        "status": r.get("status"),
                        "conclusion": r.get("conclusion"),
                        "head_sha": r.get("head_sha"),
                        "created_at": r.get("created_at"),
                    }
                    for r in runs
                ]
        except Exception as exc:  # noqa: BLE001
            return [{"error": str(exc)}]

    async def get_resource(self) -> dict[str, Any]:
        async with httpx.AsyncClient(timeout=15) as client:
            resp = await client.get(self._repo_url(), headers=self._headers())
            resp.raise_for_status()
            return resp.json()

    async def get_health(self) -> dict[str, Any]:
        """ROADMAP §41 — GitHub repo 'health' = reachable + not archived + pushes allowed."""
        try:
            data = await self.get_resource()
        except httpx.HTTPStatusError as exc:
            return {
                "resource_id": self.resource.resource_id,
                "status": str(HealthStatus.UNKNOWN),
                "error": f"http_{exc.response.status_code}",
                "captured_at": datetime.now(UTC).isoformat(),
            }
        except Exception as exc:  # noqa: BLE001
            return {
                "resource_id": self.resource.resource_id,
                "status": str(HealthStatus.UNKNOWN),
                "error": str(exc),
                "captured_at": datetime.now(UTC).isoformat(),
            }
        if data.get("archived"):
            status = HealthStatus.MAINTENANCE
        elif data.get("disabled"):
            status = HealthStatus.CRITICAL
        else:
            status = HealthStatus.HEALTHY
        return {
            "resource_id": self.resource.resource_id,
            "status": str(status),
            "raw_status": "archived" if data.get("archived") else "active",
            "version": (data.get("pushed_at") or "")[:10],
            "captured_at": datetime.now(UTC).isoformat(),
            "metadata": {
                "default_branch": data.get("default_branch"),
                "stars": data.get("stargazers_count"),
                "open_issues": data.get("open_issues_count"),
            },
        }

    async def get_metrics(self) -> dict[str, Any]:
        data = await self.get_resource()
        return {
            "stars": data.get("stargazers_count"),
            "forks": data.get("forks_count"),
            "open_issues": data.get("open_issues_count"),
            "open_prs": data.get("open_issues_count"),
            "size_kb": data.get("size"),
        }

    async def get_logs(
        self, *, limit: int = 100, level: str | None = None
    ) -> list[dict[str, Any]]:
        """ROADMAP §43 — recent workflow runs as lightweight log entries."""
        runs = await self.list_resources()
        return [
            {
                "level": "info" if r.get("conclusion") == "success" else "error",
                "message": f"workflow {r.get('name', '?')} status={r.get('status')} conclusion={r.get('conclusion')}",
                "timestamp": r.get("created_at"),
                "head_sha": r.get("head_sha"),
            }
            for r in runs[:limit]
        ]

    async def get_deployment(self) -> dict[str, Any] | None:
        try:
            async with httpx.AsyncClient(timeout=15) as client:
                resp = await client.get(
                    f"{self._repo_url()}/deployments",
                    headers=self._headers(),
                    params={"per_page": 1},
                )
                resp.raise_for_status()
                deps = resp.json()
        except Exception as exc:  # noqa: BLE001
            return {"error": str(exc)}
        return deps[0] if deps else None

    # -- Act ops (governance-gated) ---------------------------------------

    async def create_pr(self, *, head: str, base: str, title: str, body: str = "") -> dict[str, Any]:
        """ROADMAP §45 — trigger_kaggle / create_github_pr MCP op."""
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.post(
                f"{self._repo_url()}/pulls",
                headers=self._headers(),
                json={"head": head, "base": base, "title": title, "body": body},
            )
            if resp.status_code in (200, 201):
                return {"ok": True, "pr": resp.json()}
            return {"ok": False, "status_code": resp.status_code, "detail": resp.text[:200]}

    async def deploy(self, *, commit_sha: str | None = None, **_: Any) -> dict[str, Any]:
        """Create a GitHub deployment record (does not itself deploy)."""
        payload: dict[str, Any] = {"environment": "production"}
        if commit_sha:
            payload["ref"] = commit_sha
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.post(
                f"{self._repo_url()}/deployments",
                headers=self._headers(),
                json=payload,
            )
            if resp.status_code in (200, 201):
                return {"ok": True, "deployment": resp.json()}
            return {"ok": False, "status_code": resp.status_code, "detail": resp.text[:200]}


__all__ = ["GitHubAdapter"]
