"""Test-harness admin auth — simple Bearer token from env.

বাংলা: এটি শুধু STANDALONE TEST-এর জন্য। আসল production supremeai-তে
core.security.authentication.auth_middleware.verify_admin_session_fail_closed
ব্যবহার করা হবে (ecosystem_admin.py-তে try/except দিয়ে fallback আছে)।

⚠️ এই auth কখনোই production supremeai-তে ব্যবহার করবেন না।
"""

from __future__ import annotations

from fastapi import Depends, HTTPException, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from config import settings

_security = HTTPBearer(auto_error=False)


async def verify_admin_session_fail_closed(
    credentials: HTTPAuthorizationCredentials | None = Depends(_security),
) -> dict:
    """Drop-in replacement for the production admin auth dependency.

    Reads `ADMIN_TOKEN` from env. Returns a simple admin identity dict.
    """
    expected = settings.admin_token
    if not credentials or credentials.scheme.lower() != "bearer":
        raise HTTPException(
            status_code=401,
            detail="missing Bearer token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    if credentials.credentials != expected:
        raise HTTPException(
            status_code=403,
            detail="invalid admin token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return {"role": "admin", "source": "test-harness"}


__all__ = ["verify_admin_session_fail_closed"]
