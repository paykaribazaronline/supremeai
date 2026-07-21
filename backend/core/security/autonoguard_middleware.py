"""AutonoGuard Middleware — FastAPI Security Enforcement Layer.

বাংলা মন্তব্য: AutonoGuard Engine-কে FastAPI-এর মধ্যে integrate করে।
JIT OTP Injection, AST Scanning, এবং Self-Healing-এর জন্য Middleware Layer।

This middleware ensures:
- Zero silent failures (all errors emit to Event Bus)
- Stateless distributed enforcement (Redis-backed)
- IP Churn detection for malware immunity
"""

from __future__ import annotations

import json
from typing import Any

from core.autonoguard_engine import (SENSITIVE_OPS, OperationContext,
                                     autonoguard_engine)
from fastapi import Request, Response
from fastapi.responses import JSONResponse
from loguru import logger
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.types import ASGIApp


class AutonoGuardMiddleware(BaseHTTPMiddleware):
    """
    FastAPI middleware that enforces autonomous security for sensitive endpoints.

    বাংলা: সংবেদনশীল এন্ডপইন্টে অটোনোমাস সিকিউরিটি এনফোর্স করে।
    """

    def __init__(self, app: ASGIApp) -> None:
        super().__init__(app)
        self._initialized: bool = False

    async def dispatch(self, request: Request, call_next: Any) -> Response:
        # Lazy-init on first request
        if not self._initialized:
            await autonoguard_engine.initialize()
            self._initialized = True

        path = request.url.path
        method = request.method

        # বাংলা মন্তব্য: public path-এ AutonoGuard এবং JIT OTP চেক এড়ানো হচ্ছে।
        # sensitive ops চেকের আগেই এটি skip করলে latency উল্লেখযোগ্যভাবে কমে।
        from core.config import settings as _settings

        if any(path.startswith(p) for p in _settings.supremeai_public_paths):
            return await call_next(request)

        # Check if this is a sensitive operation
        is_sensitive = any(path.startswith(op) for op in SENSITIVE_OPS)

        if not is_sensitive:
            return await call_next(request)

        # Extract admin identity (from JWT/auth middleware)
        user = getattr(request.state, "user", None)
        admin_id = "unknown"
        if isinstance(user, dict):
            admin_id = user.get("sub", "unknown")

        # Extract IP for churn detection
        client_ip = request.client.host if request.client else "unknown"

        # Extract OTP code from header (if provided)
        otp_code = request.headers.get("X-JIT-OTP") or request.headers.get("X-OTP")

        # Extract code to scan from body (for POST/PUT/PATCH) - capture body once
        code_to_scan: str | None = None
        raw_body: bytes = b""
        if method in {"POST", "PUT", "PATCH"}:
            try:
                raw_body = await request.body()
                if raw_body:
                    try:
                        payload = json.loads(raw_body)
                        code_to_scan = payload.get("code") or payload.get(
                            "generated_code"
                        )
                    except json.JSONDecodeError:
                        pass
            except Exception as exc:  # noqa: BLE001
                logger.debug(f"Failed to extract body for scanning: {exc}")

        # Enforce operation
        is_allowed, error_message = await autonoguard_engine.enforce_operation(
            admin_id=admin_id,
            ip=client_ip,
            otp_code=otp_code,
            path=path,
            method=method,
            code_to_scan=code_to_scan,
        )

        if not is_allowed:
            # Emit security event for audit trail
            await autonoguard_engine.heal_error(
                Exception(f"Security block: {error_message}"),
                OperationContext(
                    admin_id=admin_id,
                    ip_address=client_ip,
                    path=path,
                    method=method,
                    headers=dict(request.headers),
                    correlation_id=getattr(request.state, "correlation_id", None),
                ),
            )

            return JSONResponse(
                status_code=401,
                content={
                    "title": "Security Verification Required",
                    "detail": error_message or "OTP or security scan required",
                    "instance": path,
                    "requires_otp": "OTP sent — provide code via X-JIT-OTP header",
                },
            )

        # Rebuild request with body if we consumed it
        if raw_body:

            async def receive():
                return {"type": "http.request", "body": raw_body}

            request._receive = receive  # type: ignore[attr-defined]

        return await call_next(request)
