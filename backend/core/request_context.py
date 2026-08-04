from __future__ import annotations

import contextvars
import uuid

from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware

# ContextVar ডিফাইন করা হচ্ছে
correlation_id_var: contextvars.ContextVar[str] = contextvars.ContextVar(
    "correlation_id", default=""
)


def get_correlation_id() -> str:
    return correlation_id_var.get()


class RequestContextMiddleware(BaseHTTPMiddleware):
    """
    বাংলা মন্তব্য: প্রতিটি ইনকামিং রিকোয়েস্টের জন্য একটি ইউনিক correlation_id জেনারেট করে এবং contextvars এ সেট করে।
    """

    async def dispatch(self, request: Request, call_next):
        corr_id = request.headers.get("X-Correlation-ID") or str(uuid.uuid4())
        token = correlation_id_var.set(corr_id)
        request.state.correlation_id = corr_id
        try:
            response = await call_next(request)
            response.headers["X-Correlation-ID"] = corr_id
            return response
        finally:
            correlation_id_var.reset(token)
