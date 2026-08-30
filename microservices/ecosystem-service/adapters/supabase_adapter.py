"""Supabase adapter — pings your existing Supabase project's REST API.

বাংলা: Supabase REST API docs: https://supabase.com/docs/reference/api/introduction
test/demo মানের adapter — production-এ connection pool/pgbouncer যোগ করতে হবে।
"""

from __future__ import annotations

from datetime import UTC, datetime
from typing import Any

import httpx

from config import settings
from ecosystem import BaseProviderAdapter
from ecosystem.health_model import HealthStatus


class SupabaseAdapter(BaseProviderAdapter):
    """Calls https://{project}.supabase.co/rest/v1/ and the health endpoint."""

    provider = "supabase"  # type: ignore[assignment]

    def _headers(self) -> dict[str, str]:
        return {
            "apikey": settings.supabase_service_key,
            "Authorization": f"Bearer {settings.supabase_service_key}",
            "Accept": "application/json",
        }

    def _rest_url(self, path: str = "") -> str:
        base = settings.supabase_url.rstrip("/")
        return f"{base}/rest/v1/{path}"

    def _health_url(self) -> str:
        # Supabase exposes a public status endpoint per project.
        return f"{settings.supabase_url.rstrip('/')}/health"

    # -- Observe ops -------------------------------------------------------

    async def list_resources(self) -> list[dict[str, Any]]:
        """List tables visible via the REST API (roadmap §37)."""
        try:
            async with httpx.AsyncClient(timeout=15) as client:
                resp = await client.get(
                    f"{settings.supabase_url.rstrip('/')}/rest/v1/",
                    headers=self._headers(),
                )
                resp.raise_for_status()
                data = resp.json()
                # Supabase returns OpenAPI spec at root; extract paths.
                paths = list(data.get("paths", {}).keys())
                return [{"table": p.strip("/")} for p in paths if p.startswith("/")]
        except Exception as exc:  # noqa: BLE001
            return [{"error": str(exc)}]

    async def get_resource(self) -> dict[str, Any]:
        """Return the project descriptor."""
        return {
            "url": settings.supabase_url,
            "project_id": settings.supabase_url.split("//")[1].split(".")[0]
            if "//" in settings.supabase_url
            else "unknown",
        }

    async def get_health(self) -> dict[str, Any]:
        """ROADMAP §41 — ping the Supabase health endpoint."""
        try:
            async with httpx.AsyncClient(timeout=10) as client:
                resp = await client.get(self._health_url())
                if resp.status_code == 200:
                    status = HealthStatus.HEALTHY
                elif 500 <= resp.status_code < 600:
                    status = HealthStatus.CRITICAL
                else:
                    status = HealthStatus.UNKNOWN
                return {
                    "resource_id": self.resource.resource_id,
                    "status": str(status),
                    "http_status": resp.status_code,
                    "captured_at": datetime.now(UTC).isoformat(),
                }
        except Exception as exc:  # noqa: BLE001
            return {
                "resource_id": self.resource.resource_id,
                "status": str(HealthStatus.UNKNOWN),
                "error": str(exc),
                "captured_at": datetime.now(UTC).isoformat(),
            }

    async def get_metrics(self) -> dict[str, Any]:
        """Supabase metrics require the management API; here we return a basic probe."""
        try:
            async with httpx.AsyncClient(timeout=10) as client:
                resp = await client.get(self._health_url())
                return {
                    "http_status": resp.status_code,
                    "response_time_ms": int(resp.elapsed.total_seconds() * 1000),
                }
        except Exception as exc:  # noqa: BLE001
            return {"error": str(exc)}

    async def get_logs(
        self, *, limit: int = 100, level: str | None = None
    ) -> list[dict[str, Any]]:
        """ROADMAP §43 — Supabase logs require the management API; return a placeholder."""
        return [
            {
                "level": "info",
                "message": "supabase log streaming requires management API access",
                "resource_id": self.resource.resource_id,
            }
        ]

    async def get_deployment(self) -> dict[str, Any] | None:
        return None

    # -- Act ops ----------------------------------------------------------

    async def restart(self) -> dict[str, Any]:
        return {"ok": False, "error": "supabase_restart_not_supported_via_rest"}


__all__ = ["SupabaseAdapter"]
