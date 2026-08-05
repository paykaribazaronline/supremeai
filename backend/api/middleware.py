# backend/api/middleware.py
"""API-level middleware for SupremeAI.

Provides:
- SupremeContextMiddleware: Correlation ID injection with ErrorEventBus integration.
- RequestIdMiddleware: Inject X-Request-ID into every response for distributed tracing.
- TenantExtractionMiddleware: extracts tenant context from headers/JWT and attaches to request.state.
- ResponseStandardizationMiddleware: ensures all non-JSON responses follow the standard envelope.
- ChaosInjectorMiddleware: Enterprise Fault Injection & Chaos Engine for local testing.
- IdempotencyMiddleware: Redis-based distributed idempotency for POST paths.
"""

from __future__ import annotations

import asyncio
import json
import os
import random
import time
import uuid

from fastapi import HTTPException, Request
from fastapi.responses import JSONResponse
from loguru import logger
from starlette.middleware.base import BaseHTTPMiddleware

from core.config import settings
from core.error_bus import with_error_bus
from core.messaging.event_bus import ErrorContext, ErrorEvent, error_event_bus


class SupremeContextMiddleware(BaseHTTPMiddleware):
    """Injects Correlation ID for end-to-end observability and handles global failures."""

    @with_error_bus("dispatch")
    async def dispatch(self, request: Request, call_next):
        correlation_id = request.headers.get("X-Correlation-ID", str(uuid.uuid4()))
        request.state.correlation_id = correlation_id
        start_time = time.time()

        # বাংলা মন্তব্য: লগার কনটেক্সটে correlation_id বাইন্ড করা হচ্ছে যাতে সমস্ত সংশ্লিষ্ট লগে এটি দৃশ্যমান হয়
        with logger.contextualize(correlation_id=correlation_id):
            try:
                response = await call_next(request)

                response.headers["X-Correlation-ID"] = correlation_id
                response.headers["X-Content-Type-Options"] = "nosniff"
                response.headers["X-Frame-Options"] = "DENY"

                process_time = time.time() - start_time
                response.headers["X-Process-Time"] = f"{process_time:.4f}"

                return response

            except Exception as exc:
                error_event_bus.emit(
                    ErrorEvent(
                        module="GlobalMiddleware",
                        error_type="REQUEST_FAILURE",
                        message=str(exc)[:500],
                        severity="ERROR",
                        context={
                            "method": request.method,
                            "url": str(request.url),
                            "correlation_id": correlation_id,
                        },
                        structured_context=ErrorContext(
                            module="api.middleware",
                            request_id=correlation_id,
                            env=settings.env,
                        ),
                    )
                )
                raise


class RequestIdMiddleware(BaseHTTPMiddleware):
    """Inject X-Request-ID into every response for distributed tracing."""

    async def dispatch(self, request: Request, call_next):
        request_id = request.headers.get("X-Request-ID", str(uuid.uuid4()))
        request.state.request_id = request_id
        response = await call_next(request)
        response.headers["X-Request-ID"] = request_id
        return response


class TenantExtractionMiddleware(BaseHTTPMiddleware):
    """Attach tenant_id to request.state from X-Tenant-ID header or JWT."""

    async def dispatch(self, request: Request, call_next):
        tenant_id = request.headers.get("X-Tenant-ID")
        if not tenant_id:
            user = getattr(request.state, "user", None)
            if user:
                tenant_id = user.get("sub", "anonymous")
            else:
                tenant_id = "anonymous"
        request.state.tenant_id = tenant_id
        return await call_next(request)


class ResponseStandardizationMiddleware(BaseHTTPMiddleware):
    """Wrap non-JSON error responses into the standard API error envelope."""

    async def dispatch(self, request: Request, call_next):
        response = await call_next(request)
        if response.status_code >= 400 and response.headers.get("content-type") != "application/json":
            description = getattr(response, "description", "Unknown error")
            body_content = ""
            if hasattr(response, "body") and getattr(response, "body", b""):
                body_content = response.body.decode()
            body = {"error": {"title": description, "detail": body_content}}
            return JSONResponse(status_code=response.status_code, content=body)
        return response


class ChaosInjectorMiddleware(BaseHTTPMiddleware):
    """Enterprise Fault Injection & Chaos Engine.
    Simulates real-world network degradation, packet loss, and latency spikes.
    Active ONLY when LOCAL_CHAOS_MODE=true.
    """

    def __init__(self, app):
        super().__init__(app)
        self.chaos_enabled = (
            os.getenv("LOCAL_CHAOS_MODE", "false").lower() == "true" and settings.env.lower() != "production"
        )
        self.packet_drop_rate = float(os.getenv("CHAOS_PACKET_DROP_RATE", "0.20"))
        self.max_latency_spike = float(os.getenv("CHAOS_MAX_LATENCY_SPIKE", "3.5"))
        self.latency_spike_chance = float(os.getenv("CHAOS_LATENCY_SPIKE_CHANCE", "0.30"))

    async def dispatch(self, request: Request, call_next):
        if not self.chaos_enabled:
            return await call_next(request)

        if random.random() < self.latency_spike_chance:
            delay = random.uniform(0.5, self.max_latency_spike)
            logger.warning(f"[CHAOS ENGINE] Injecting artificial network lag: {delay:.2f}s on {request.url.path}")
            await asyncio.sleep(delay)

        if random.random() < self.packet_drop_rate:
            logger.critical(f"[CHAOS ENGINE] Simulated Packet Drop! Severing connection for {request.url.path}")
            return JSONResponse(
                status_code=504,
                content={
                    "title": "Gateway Timeout (Chaos Simulated)",
                    "detail": "Upstream connection dropped due to artificial network degradation.",
                    "instance": request.url.path,
                },
            )

        return await call_next(request)


IDEMPOTENCY_TTL_SECONDS = 120
IDEMPOTENCY_PATHS = (
    "/api/task",
    "/api/github",
    "/api/auth/callback",
    "/api/pr",
    "/api/agent",
)


class IdempotencyMiddleware(BaseHTTPMiddleware):
    """Redis-based distributed idempotency for POST paths."""

    async def dispatch(self, request: Request, call_next):
        path = request.url.path
        if request.method != "POST" or not any(path.startswith(p) for p in IDEMPOTENCY_PATHS):
            return await call_next(request)

        idempotency_key = request.headers.get("Idempotency-Key")
        if not idempotency_key:
            return JSONResponse(
                status_code=400,
                content={
                    "error": "Bad Request: 'Idempotency-Key' header is required for mutating operations.",
                    "hint": "Provide a unique UUID as 'Idempotency-Key' header.",
                },
            )

        try:
            from core.cache.redis_manager import (
                acquire_idempotency_lock,
                cache_response_and_release_lock,
                redis_manager,
                release_idempotency_lock,
            )
        except ImportError:
            logger.warning("[Idempotency] Failed to import redis_manager — skipping check (fail-open)")
            return await call_next(request)

        if redis_manager.client is not None:
            try:
                cached_key = f"idempotency:response:{idempotency_key}"
                cached = await redis_manager.client.get(cached_key)
                if cached:
                    logger.info(f"Idempotency Hit: serving cached response for key {idempotency_key}")
                    cached_data = json.loads(cached)
                    return JSONResponse(
                        status_code=cached_data.get("status_code", 200),
                        content=cached_data.get("body", {}),
                        headers={"X-Cache-Lookup": "HIT - Idempotency Lock"},
                    )
            except Exception as e:
                logger.warning(f"[Idempotency] Cache read failed — continuing: {e}")

        acquired = await acquire_idempotency_lock(idempotency_key, IDEMPOTENCY_TTL_SECONDS)
        if not acquired:
            logger.warning(f"Idempotency Block: {idempotency_key} is already being processed.")
            raise HTTPException(
                status_code=409,
                detail="Conflict: Request is already being processed. Duplicate execution blocked.",
            )

        try:
            response = await call_next(request)

            if response.status_code == 200 and redis_manager.client is not None:
                # বাংলা মন্তব্য: স্ট্রিমিং ও নন-স্ট্রিমিং উভয় রেসপন্সের জন্য রোবাস্ট বডি ক্যাপচার
                body_bytes = b""
                if hasattr(response, "body_iterator"):
                    try:
                        response_body = [section async for section in response.body_iterator]
                        body_bytes = b"".join(response_body)
                    except (RuntimeError, StopAsyncIteration) as stream_err:
                        logger.warning(f"[Idempotency] Body iterator exhausted or failed: {stream_err}")
                        body_bytes = b"{}"
                elif hasattr(response, "body"):
                    body_bytes = response.body if response.body else b"{}"
                else:
                    body_bytes = b"{}"

                # বাংলা মন্তব্য: স্ট্রিমিং রেসপন্সের জন্য পুনরায় Response অবজেক্ট তৈরি
                if hasattr(response, "body_iterator"):
                    from starlette.responses import Response as StarletteResponse

                    response = StarletteResponse(
                        content=body_bytes,
                        status_code=response.status_code,
                        headers=dict(response.headers),
                        media_type=response.media_type,
                    )

                try:
                    body_str = body_bytes.decode("utf-8")
                    cache_data = json.dumps({"status_code": 200, "body": json.loads(body_str)})
                    await cache_response_and_release_lock(idempotency_key, cache_data, IDEMPOTENCY_TTL_SECONDS * 5)
                except (json.JSONDecodeError, UnicodeDecodeError) as parse_err:
                    logger.warning(f"[Idempotency] Response body not JSON-serializable (non-blocking): {parse_err}")
                    await release_idempotency_lock(idempotency_key)
                except Exception as cache_err:
                    logger.warning(f"[Idempotency] Response caching failed (non-blocking): {cache_err}")
                    await release_idempotency_lock(idempotency_key)
            else:
                await release_idempotency_lock(idempotency_key)

            return response

        except Exception as e:
            await release_idempotency_lock(idempotency_key)
            logger.error(f"Execution failed inside Idempotency block: {e!s}")
            raise


__all__ = [
    "SupremeContextMiddleware",
    "RequestIdMiddleware",
    "TenantExtractionMiddleware",
    "ResponseStandardizationMiddleware",
    "ChaosInjectorMiddleware",
    "IdempotencyMiddleware",
]
