"""
Unified Security Pipeline & Conditional Middleware Loader.

Centrally manages security middleware registration (Origin Validation, Rate Limiting,
Prompt Firewall, Security Headers) to prevent redundant overhead and guarantee clean,
conditional loading based on application configuration.
"""

from __future__ import annotations

import time
from collections.abc import Callable
from typing import Any

from fastapi import FastAPI, Request, Response
from loguru import logger
from starlette.middleware.base import BaseHTTPMiddleware


class SupremeSecurityHeadersMiddleware(BaseHTTPMiddleware):
    """
    Applies strict, modern security headers to all outbound HTTP responses.
    Includes X-Content-Type-Options, X-Frame-Options, X-XSS-Protection, and Correlation ID.
    """

    async def dispatch(self, request: Request, call_next: Callable[[Request], Any]) -> Response:
        start_time = time.perf_counter()

        correlation_id = request.headers.get("X-Correlation-ID") or request.headers.get("X-Trace-Id")
        if not correlation_id:
            correlation_id = f"trace-{int(time.time() * 1000)}"

        response = await call_next(request)

        # Apply standard security headers
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["X-XSS-Protection"] = "1; mode=block"
        response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
        response.headers["X-Trace-Id"] = correlation_id
        response.headers["X-Process-Time"] = f"{(time.perf_counter() - start_time) * 1000:.2f}ms"

        return response


class SecurityPipelineManager:
    """
    Central manager for conditionally applying security middlewares to FastAPI app.
    """

    @staticmethod
    def register_security_pipeline(
        app: FastAPI,
        enable_headers: bool = True,
        enable_origin_validation: bool = False,
        enable_rate_limiter: bool = False,
    ) -> None:
        """
        Registers active security middlewares cleanly.
        """
        if enable_headers:
            app.add_middleware(SupremeSecurityHeadersMiddleware)
            logger.info("Security Pipeline: SupremeSecurityHeadersMiddleware enabled.")

        # Conditional loaders prevent unnecessary pipeline traversal
        if enable_origin_validation:
            try:
                from core.security.origin_validator import OriginValidatorMiddleware
                app.add_middleware(OriginValidatorMiddleware)
                logger.info("Security Pipeline: OriginValidatorMiddleware enabled.")
            except ImportError as exc:
                logger.warning(f"Could not load OriginValidatorMiddleware: {exc}")

        if enable_rate_limiter:
            try:
                from core.security.api_key_limiter import APIKeyLimiter
                app.add_middleware(APIKeyLimiter)
                logger.info("Security Pipeline: APIKeyLimiter enabled.")
            except ImportError as exc:
                logger.warning(f"Could not load APIKeyLimiter: {exc}")
