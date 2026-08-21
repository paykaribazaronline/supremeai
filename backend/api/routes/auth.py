from __future__ import annotations

import asyncio
from datetime import UTC, datetime, timedelta

from fastapi import APIRouter, Depends, HTTPException, Request, status
from fastapi.security import OAuth2PasswordBearer
from loguru import logger
from pydantic import BaseModel

from core.error_bus import with_error_bus

import jwt
from jwt import PyJWTError as JWTError

from core.cache.redis_manager import redis_manager
from core.config import settings
from core.security.rbac import UserContext
from database.supabase_client import db

router = APIRouter(prefix="/auth", tags=["auth"])

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login", auto_error=False)

SECRET_KEY = settings.jwt_secret
ALGORITHM = "HS256"
# বাংলা: ২৪ ঘণ্টার টোকেন অনেক বেশি — অ্যাক্সেস টোকেন ১ ঘণ্টা, রিফ্রেশ টোকেন দীর্ঘ মেয়াদী।
ACCESS_TOKEN_EXPIRE_MINUTES = 60
# বাংলা: রিফ্রেশ টোকেন ৭ দিন — প্রোডাকশন স্ট্যান্ডার্ড।
REFRESH_TOKEN_EXPIRE_DAYS = 7


def create_access_token(data: dict, expires_delta: timedelta | None = None) -> str:
    if jwt is None:
        raise RuntimeError("PyJWT is required for token issuance")
    to_encode = data.copy()
    # বাংলা: iat (issued-at) ও jti (token id) — রিভোকেশন ও অডিট ট্রেইলিং এর জন্য আবশ্যক।
    import uuid as _uuid

    now = datetime.now(UTC)
    expire = now + (expires_delta or timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES))
    to_encode.update(
        {
            "exp": expire,
            "iat": now,
            "jti": to_encode.get("jti") or f"jti-{_uuid.uuid4().hex[:16]}",
            "type": "access",
        }
    )
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)


def create_refresh_token(data: dict) -> str:
    """বাংলা: রিফ্রেশ টোকেন — অ্যাক্সেস টোকেন পুনঃপ্রদানের জন্য দীর্ঘ মেয়াদী।"""
    if jwt is None:
        raise RuntimeError("PyJWT is required for token issuance")
    import uuid as _uuid

    to_encode = data.copy()
    now = datetime.now(UTC)
    expire = now + timedelta(days=REFRESH_TOKEN_EXPIRE_DAYS)
    to_encode.update(
        {
            "exp": expire,
            "iat": now,
            "jti": f"jti-{_uuid.uuid4().hex[:16]}",
            "type": "refresh",
        }
    )
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)


@with_error_bus("optional_current_user")
async def optional_current_user(
    token: str | None = Depends(oauth2_scheme),
) -> UserContext | None:
    if not token or jwt is None:
        return None
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        # বাংলা: type=access ছাড়া অন্য টোকেন (যেমন refresh) ব্যবহার রোধ।
        if payload.get("type") not in (None, "access"):
            return None
        # বাংলা মন্তব্য: JWT ডিকোড সফল হলে UserContext তৈরি করে return করা হচ্ছে।
        user_id = payload.get("sub", "unknown")
        role = payload.get("role", "viewer")
        return UserContext(
            user_id=user_id,
            role=role,
            email=payload.get("email") if isinstance(payload.get("email"), str) else None,
        )
    except Exception:
        logger.exception("Unhandled exception")
        return None


class LoginRequest(BaseModel):
    username: str
    password: str


class RegisterRequest(BaseModel):
    username: str
    password: str
    name: str | None = None


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str | None = None
    token_type: str = "bearer"
    user_id: str
    role: str


class MeResponse(BaseModel):
    user_id: str
    role: str
    scopes: tuple[str, ...] = ()
    email: str | None = None


class RefreshRequest(BaseModel):
    refresh_token: str


@router.post("/login", response_model=TokenResponse)
async def login(body: LoginRequest, request: Request):
    if not db.client:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Supabase client is not initialized",
        )

    # বাংলা: supabase-py এর auth.sign_in_with_password সিঙ্ক্রোনাস — সরাসরি কল করলে
    # event loop ব্লক হয়ে যায়। asyncio.to_thread দিয়ে thread pool-এ পাঠাচ্ছি।
    try:
        res = await asyncio.to_thread(
            db.client.auth.sign_in_with_password,
            {"email": body.username, "password": body.password},
        )
        if not res.user:
            # বাংলা: auth ফেইলিওরে generic message — internal detail লিক করছি না।
            logger.warning(f"Login failed for email={body.username!r}: no user returned")
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")

        user_id = res.user.id
        # বাংলা: ইমেইলটি settings.admin_emails তালিকায় আছে কি না তা দেখে রোল অ্যাসাইন করা হচ্ছে (ঝুঁকিপূর্ণ "admin" in username চেক প্রতিস্থাপিত)।
        is_admin = body.username and any(
            body.username.lower() == admin_email.lower() for admin_email in settings.admin_emails
        )
        primary_role = "admin" if is_admin else "user"
        token_data = {
            "sub": user_id,
            "role": primary_role,
            "email": body.username,
            "method": "supabase_auth",
        }
        access_token = create_access_token(token_data)
        refresh_token = create_refresh_token(token_data)

        # বাংলা মন্তব্য: Phase 2 — Hybrid Fingerprint Login। হেডারটি ঐচ্ছিক, তাই না থাকলেও
        # লগইন স্বাভাবিকভাবে চলবে (ব্রেকিং চেঞ্জ নয়); থাকলে ডিভাইসটি known-devices সেটে যোগ হয়
        # যা AntiHackingContextMiddleware admin scope-এ তৃতীয় সিগন্যাল হিসেবে ব্যবহার করে।
        fingerprint = request.headers.get("x-device-fingerprint")
        if fingerprint and redis_manager and redis_manager.client:
            try:
                await redis_manager.client.sadd(f"device:known:{user_id}", fingerprint)
            except Exception as exc:
                logger.warning(f"Failed to register device fingerprint for {user_id}: {exc}")

        return TokenResponse(
            access_token=access_token,
            refresh_token=refresh_token,
            user_id=user_id,
            role=primary_role,
        )
    except HTTPException:
        # বাংলা: নিজে রেইজ করা HTTPException পুনরায় রেইজ করি — বাকি সব exception 500।
        raise
    except Exception as e:
        # বাংলা: অন্য কোনো exception (network, DB, Supabase internal) — ক্লায়েন্টকে
        # generic বার্তা দেখাচ্ছি, কিন্তু server-side এ পূর্ণ stack লগ করছি।
        logger.exception(f"Unexpected login error for email={body.username!r}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Login service temporarily unavailable. Please try again.",
        ) from e


@router.post("/register", response_model=TokenResponse)
async def register(body: RegisterRequest):
    if not db.client:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Supabase client is not initialized",
        )

    try:
        # বাংলা: sign_up ও সিঙ্ক্রোনাস — to_thread দিয়ে wrap করা হলো।
        res = await asyncio.to_thread(
            db.client.auth.sign_up,
            {"email": body.username, "password": body.password},
        )
        if not res.user:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Registration failed")

        # বাংলা মন্তব্য: যদি ইমেইল ভেরিফিকেশন অন থাকে, তাহলে res.session None হবে। সেক্ষেত্রে ফেক টোকেন না দিয়ে ইউজারকে ভেরিফাই করতে বলব।
        if res.session is None:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Email confirmation required")

        user_id = res.user.id
        # বাংলা মন্তব্য: ইমেইলটি settings.admin_emails তালিকায় আছে কি না তা দেখে রোল অ্যাসাইন করা হচ্ছে (ঝুঁকিপূর্ণ "admin" in username চেক প্রতিস্থাপিত)।
        is_admin = body.username and any(
            body.username.lower() == admin_email.lower() for admin_email in settings.admin_emails
        )
        primary_role = "admin" if is_admin else "user"
        token_data = {
            "sub": user_id,
            "role": primary_role,
            "email": body.username,
            "method": "supabase_auth",
        }
        access_token = create_access_token(token_data)
        refresh_token = create_refresh_token(token_data)
        return TokenResponse(
            access_token=access_token,
            refresh_token=refresh_token,
            user_id=user_id,
            role=primary_role,
        )
    except HTTPException:
        raise
    except Exception as e:
        # বাংলা: registration-এর ক্ষেত্রেও internal error লিক করছি না।
        logger.exception(f"Unexpected registration error for email={body.username!r}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Registration service temporarily unavailable. Please try again.",
        ) from e


@router.post("/refresh", response_model=TokenResponse)
async def refresh_token_endpoint(body: RefreshRequest):
    """বাংলা: রিফ্রেশ টোকেন দিয়ে নতুন অ্যাক্সেস টোকেন প্রদান।

    type=refresh চেক করে access token রিফ্রেশে ব্যবহার রোধ করা হয় — token confusion প্রতিরোধ।
    """
    if jwt is None:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="JWT service unavailable")
    try:
        payload = jwt.decode(body.refresh_token, SECRET_KEY, algorithms=[ALGORITHM])
    except JWTError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid refresh token")

    if payload.get("type") != "refresh":
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token is not a refresh token")

    token_data = {
        "sub": payload.get("sub", "unknown"),
        "role": payload.get("role", "viewer"),
        "email": payload.get("email"),
        "method": payload.get("method", "supabase_auth"),
    }
    new_access = create_access_token(token_data)
    return TokenResponse(
        access_token=new_access,
        refresh_token=None,
        user_id=str(token_data["sub"]),
        role=str(token_data["role"]),
    )


@router.get("/me", response_model=MeResponse)
async def me(current_user: UserContext | None = Depends(optional_current_user)):
    if current_user is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Not authenticated")
    # বাংলা মন্তব্য: scopes যদি None হয় তবে MeResponse ভ্যালিডেশন পাস করানোর জন্য খালি টুপল পাস করা হচ্ছে।
    scopes_val = current_user.scopes if current_user.scopes is not None else ()
    return MeResponse(
        user_id=current_user.user_id,
        role=current_user.role,
        scopes=scopes_val,
        email=current_user.email,
    )


@router.get("/verify")
async def verify_token(request: Request):
    user = getattr(request.state, "user", None)
    if not user:
        raise HTTPException(status_code=401, detail="Missing or invalid token")
    return {
        "valid": True,
        "user_id": user.get("sub"),
        "role": user.get("role"),
        "message": "Authentication successful",
    }
