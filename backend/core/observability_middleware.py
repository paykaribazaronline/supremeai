from __future__ import annotations

import time
import uuid
from datetime import datetime
from datetime import timezone

from api.routes.metrics import record_error
from api.routes.metrics import record_request
from api.routes.metrics import record_request_duration
from core.telemetry import setup_tracing
from core.telemetry import trace_span


class ObservabilityMiddleware:
    def __init__(self, app) -> None:
        self.app = app
        setup_tracing()

    async def __call__(self, scope, receive, send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        path = scope.get("path", "")
        if path == "/metrics":
            await self.app(scope, receive, send)
            return

        headers = scope.get("headers", [])
        trace_id = ""
        user_id = "anonymous_api_user"
        for k, v in headers:
            if k.lower() in (b"x-trace-id", b"traceparent"):
                trace_id = v.decode("utf-8")
            elif k.lower() == b"x-user-id":
                user_id = v.decode("utf-8")

        if not trace_id:
            trace_id = f"00-{uuid.uuid4().hex}-0000000000000001-01"

        method = scope.get("method", "GET")
        started = time.perf_counter()
        status_code = 500
        error_type = None

        async def custom_send(message):
            nonlocal status_code
            if message["type"] == "http.response.start":
                status_code = message["status"]
                start_headers = list(message.get("headers", []))
                start_headers.append((b"X-Trace-ID", trace_id.encode("utf-8")))
                start_headers.append((b"traceparent", trace_id.encode("utf-8")))
                message["headers"] = start_headers
            await send(message)

        try:
            with trace_span(
                f"{method} {path}",
                attributes={
                    "http.method": method,
                    "http.route": path,
                    "http.url": f"{scope.get('scheme', 'http')}://{scope.get('server', ('localhost', 80))[0]}{path}",
                    "trace_id": trace_id,
                },
                kind="server",
            ):
                await self.app(scope, receive, custom_send)
        except Exception as exc:
            error_type = type(exc).__name__
            record_error(error_type, path)
            raise
        finally:
            duration = time.perf_counter() - started
            record_request(method, path, status_code)
            record_request_duration(method, path, duration)
            try:
                from core.posthog_client import posthog_client

                posthog_client.capture(
                    distinct_id=user_id,
                    event="api_request",
                    properties={
                        "path": path,
                        "method": method,
                        "status_code": status_code,
                        "duration": duration,
                    },
                )
            except Exception:
                pass

            try:
                from database.supabase_client import db

                if db.client:
                    db.upsert_usage_metric(
                        {
                            "tenant_id": user_id,
                            "metric_name": f"api_request_{method.lower()}_{path.replace('/', '_')}",
                            "metric_value": duration,
                            "collected_at": datetime.now(timezone.utc).isoformat(),
                        }
                    )
            except Exception:
                pass
