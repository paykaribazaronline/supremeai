#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# file >> observability_middleware.py
# project >> SupremeAI 2.0
# purpose >> Observability
# module >> core
# ============================================================================
from __future__ import annotations

import time

from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware

from api.routes.metrics import record_error, record_request, record_request_duration
from core.telemetry import setup_tracing, trace_span


class ObservabilityMiddleware(BaseHTTPMiddleware):
    def __init__(self, app) -> None:
        super().__init__(app)
        setup_tracing()

    async def dispatch(self, request: Request, call_next):
        path = request.url.path
        if path == "/metrics":
            return await call_next(request)

        import uuid
        
        # Extract existing trace ID or generate a new one
        trace_id = request.headers.get("x-trace-id") or request.headers.get("traceparent")
        if not trace_id:
            trace_id = f"00-{uuid.uuid4().hex}-0000000000000001-01"  # Matches OTel traceparent spec
        
        method = request.method
        started = time.perf_counter()
        status_code = 500
        error_type = None

        try:
            with trace_span(
                f"{method} {path}",
                attributes={
                    "http.method": method,
                    "http.route": path,
                    "http.url": str(request.url),
                    "trace_id": trace_id,
                },
                kind="server",
            ):
                response = await call_next(request)
                status_code = response.status_code
                response.headers["X-Trace-ID"] = trace_id
                response.headers["traceparent"] = trace_id
                return response
        except Exception as exc:  # pylint: disable=broad-except
            error_type = type(exc).__name__
            record_error(error_type, path)
            raise
        finally:
            duration = time.perf_counter() - started
            record_request(method, path, status_code)
            record_request_duration(method, path, duration)
            try:
                from core.posthog_client import posthog_client
                user_id = request.headers.get("x-user-id") or "anonymous_api_user"
                posthog_client.capture(
                    distinct_id=user_id,
                    event="api_request",
                    properties={
                        "path": path,
                        "method": method,
                        "status_code": status_code,
                        "duration": duration,
                    }
                )
            except Exception:
                pass
