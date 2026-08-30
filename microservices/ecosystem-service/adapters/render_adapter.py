"""Render API adapter — calls the real Render control plane API.

বাংলা: Render API docs: https://api-docs.render.com/
এই adapter-টি test/demo মানের — basic health/metrics/logs/deploy/restart
দেয়। Production-এ retry/circuit-breaker/metrics যোগ করতে হবে।
"""

from __future__ import annotations

from datetime import UTC, datetime
from typing import Any

import httpx

from config import settings
from ecosystem import BaseProviderAdapter, UnifiedHealth
from ecosystem.health_model import HealthStatus


class RenderAdapter(BaseProviderAdapter):
    """Calls https://api.render.com/v1/services/{service_id}."""

    BASE_URL = "https://api.render.com/v1"
    provider = "render"  # type: ignore[assignment]

    def _headers(self) -> dict[str, str]:
        return {
            "Authorization": f"Bearer {settings.render_api_key}",
            "Accept": "application/json",
        }

    def _service_url(self) -> str:
        return f"{self.BASE_URL}/services/{settings.render_service_id}"

    # -- Observe ops -------------------------------------------------------

    async def list_resources(self) -> list[dict[str, Any]]:
        async with httpx.AsyncClient(timeout=15) as client:
            resp = await client.get(f"{self.BASE_URL}/services", headers=self._headers())
            resp.raise_for_status()
            return resp.json()

    async def get_resource(self) -> dict[str, Any]:
        async with httpx.AsyncClient(timeout=15) as client:
            resp = await client.get(self._service_url(), headers=self._headers())
            resp.raise_for_status()
            return resp.json()

    async def get_health(self) -> dict[str, Any]:
        """ROADMAP §41 — normalize Render's status to UnifiedHealth."""
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

        # Render status: created|suspended|update_failed|building|live|deactivated
        status_map = {
            "live": HealthStatus.HEALTHY,
            "building": HealthStatus.MAINTENANCE,
            "created": HealthStatus.UNKNOWN,
            "suspended": HealthStatus.CRITICAL,
            "update_failed": HealthStatus.CRITICAL,
            "deactivated": HealthStatus.CRITICAL,
        }
        svc_status = data.get("status", "unknown")
        health_status = status_map.get(svc_status, HealthStatus.UNKNOWN)
        return {
            "resource_id": self.resource.resource_id,
            "status": str(health_status),
            "raw_status": svc_status,
            "version": data.get("commit", {}).get("id"),
            "captured_at": datetime.now(UTC).isoformat(),
            "metadata": {"service_id": settings.render_service_id},
        }

    async def get_metrics(self) -> dict[str, Any]:
        """ROADMAP §41 — fetch metrics. Render's metrics endpoint is limited."""
        try:
            async with httpx.AsyncClient(timeout=15) as client:
                # Render exposes memory/CPU via the service object itself.
                resp = await client.get(self._service_url(), headers=self._headers())
                resp.raise_for_status()
                data = resp.json()
        except Exception as exc:  # noqa: BLE001
            return {"error": str(exc)}
        return {
            "service_id": settings.render_service_id,
            "status": data.get("status"),
            "commit": data.get("commit", {}).get("id"),
            "created_at": data.get("createdAt"),
            "type": data.get("type"),
        }

    async def get_logs(
        self, *, limit: int = 100, level: str | None = None
    ) -> list[dict[str, Any]]:
        """ROADMAP §43 — lightweight log metadata (raw log fetching needs Render's log stream)."""
        # Render's log streaming requires a websocket; for the test harness we
        # return the most recent deploy events as lightweight log entries.
        try:
            async with httpx.AsyncClient(timeout=15) as client:
                resp = await client.get(
                    f"{self._service_url()}/deploys",
                    headers=self._headers(),
                    params={"limit": min(limit, 20)},
                )
                resp.raise_for_status()
                deploys = resp.json()
        except Exception as exc:  # noqa: BLE001
            return [{"level": "error", "message": str(exc), "resource_id": self.resource.resource_id}]
        return [
            {
                "level": "info",
                "message": f"deploy {d.get('id', '?')[:12]} status={d.get('status')} commit={d.get('commit', {}).get('id', '?')[:8]}",
                "timestamp": d.get("createdAt"),
            }
            for d in (deploys or [])
        ][:limit]

    async def get_deployment(self) -> dict[str, Any] | None:
        try:
            async with httpx.AsyncClient(timeout=15) as client:
                resp = await client.get(
                    f"{self._service_url()}/deploys",
                    headers=self._headers(),
                    params={"limit": 1},
                )
                resp.raise_for_status()
                deploys = resp.json()
        except Exception as exc:  # noqa: BLE001
            return {"error": str(exc)}
        return deploys[0] if deploys else None

    # -- Act ops (low-risk) ------------------------------------------------

    async def restart(self) -> dict[str, Any]:
        """ROADMAP §28 — restart is a low-risk autonomous action."""
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.post(
                f"{self._service_url()}/restart", headers=self._headers()
            )
            return {"ok": resp.status_code in (200, 202), "status_code": resp.status_code}

    async def deploy(self, *, commit_sha: str | None = None, clear_cache: bool = False) -> dict[str, Any]:
        """Trigger a deploy on Render."""
        payload: dict[str, Any] = {"clearCache": "on" if clear_cache else "off"}
        if commit_sha:
            payload["commitId"] = commit_sha
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.post(
                f"{self._service_url()}/deploys",
                headers=self._headers(),
                json=payload,
            )
            if resp.status_code in (200, 201, 202):
                return {"ok": True, "deploy": resp.json()}
            return {"ok": False, "status_code": resp.status_code, "detail": resp.text[:200]}

    async def rollback(self, *, deployment_id: str) -> dict[str, Any]:
        """Render doesn't have a direct rollback endpoint — return guidance."""
        return {
            "ok": False,
            "error": "render_has_no_rollback_endpoint",
            "hint": f"redeploy a previous commit. target deploy={deployment_id}",
        }


__all__ = ["RenderAdapter"]
