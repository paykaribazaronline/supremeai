import os
from datetime import datetime
from datetime import timezone
from typing import Any

import httpx
from loguru import logger


class GCPCloudFunctionClient:
    """HTTP trigger client for SupremeAI Google Cloud Functions."""

    def __init__(
        self,
        project_id: str | None = None,
        region: str | None = None,
        function_name: str | None = None,
        base_url: str | None = None,
        bearer_token: str | None = None,
        timeout: float = 30.0,
    ):
        self.project_id = (
            project_id
            or os.getenv("GCP_PROJECT_ID")
            or os.getenv("GOOGLE_CLOUD_PROJECT")
        )
        self.region = region or os.getenv("GCP_REGION", "us-central1")
        self.function_name = function_name or os.getenv("GCP_CLOUD_FUNCTION_NAME")
        self.base_url = (base_url or os.getenv("GCP_CLOUD_FUNCTION_URL", "")).rstrip(
            "/"
        )
        self.bearer_token = bearer_token or os.getenv("GCP_CLOUD_FUNCTION_BEARER_TOKEN")
        self.timeout = timeout

    @property
    def is_configured(self) -> bool:
        return bool(self.function_url)

    @property
    def function_url(self) -> str | None:
        if self.base_url:
            return self.base_url
        if self.project_id and self.function_name:
            return f"https://{self.region}-{self.project_id}.cloudfunctions.net/{self.function_name}"
        return None

    def trigger(
        self,
        payload: dict[str, Any],
        endpoint: str | None = None,
        method: str = "POST",
        timeout: float | None = None,
    ) -> dict[str, Any]:
        url = self._url_for(endpoint)
        if not url:
            return {
                "success": False,
                "provider": "gcp_cloud_functions",
                "error": "GCP Cloud Function URL is not configured",
            }

        headers = {"Content-Type": "application/json"}
        if self.bearer_token:
            headers["Authorization"] = f"Bearer {self.bearer_token}"

        started = datetime.now(timezone.utc)
        try:
            with httpx.Client(timeout=timeout or self.timeout) as client:
                response = client.request(method, url, json=payload, headers=headers)
            latency_ms = (datetime.now(timezone.utc) - started).total_seconds() * 1000
            return {
                "success": 200 <= response.status_code < 300,
                "provider": "gcp_cloud_functions",
                "status_code": response.status_code,
                "latency_ms": round(latency_ms, 2),
                "function_url": url,
                "data": self._safe_json(response),
            }
        except Exception as exc:
            logger.error(f"GCP Cloud Function trigger failed: {exc}")
            return {
                "success": False,
                "provider": "gcp_cloud_functions",
                "function_url": url,
                "error": str(exc),
            }

    def trigger_ocr(
        self, image_urls, project_id: str, user_id: str, languages=None
    ) -> dict[str, Any]:
        payload = {
            "imageUrls": image_urls,
            "projectId": project_id,
            "userId": user_id,
            "languages": languages or ["en", "bn"],
        }
        return self.trigger(payload, endpoint="processOCR")

    def get_config(self) -> dict[str, Any]:
        return {
            "provider": "gcp_cloud_functions",
            "project_id": self.project_id,
            "region": self.region,
            "function_name": self.function_name,
            "function_url": self.function_url,
            "configured": self.is_configured,
            "timeout": self.timeout,
        }

    def _url_for(self, endpoint: str | None) -> str | None:
        base = self.function_url
        if not base or not endpoint:
            return base
        return f"{base}/{endpoint.lstrip('/')}"

    @staticmethod
    def _safe_json(response: httpx.Response) -> Any:
        try:
            return response.json()
        except Exception:
            return {"text": response.text}
