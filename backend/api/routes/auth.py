# ruff: noqa: BLE001, B904, E722
from __future__ import annotations

from datetime import UTC, datetime, timedelta

from fastapi import APIRouter, Depends, HTTPException, Request, status
from fastapi.security import OAuth2PasswordBearer
from loguru import logger
from pydantic import BaseModel

from core.error_bus import with_error_bus

try:
    from jose import JWTError, jwt
except ImportError:
    JWTError = Exception  # type: ignore[misc,assignment]
    jwt = None  # type: ignore[assignment]

from core.cache.redis_manager import redis_manager
from core.config import settings
from core.security.rbac import UserContext
from database.supabase_client import db

router = APIRouter(prefix="/auth", tags=["auth"])

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login", auto_error=False)

SECRET_KEY = settings.jwt_secret
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24


def create_access_token(data: dict, expires_delta: timedelta | None = None) -> str:
    if jwt is None:
        raise RuntimeError("python-jose[cryptography] is required for token issuance")
    to_encode = data.copy()
    expire = datetime.now(UTC) + (expires_delta or timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES))
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)


@with_error_bus("optional_current_user")
async def optional_current_user(
    token: str | None = Depends(oauth2_scheme),
) -> UserContext | None:
    if not token or jwt is None:
        return None
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        # বাংলা মন্তব্য: JWT ডিকোড সফল হলে UserContext তৈরি করে return করা হচ্ছে।
        user_id = payload.get("sub", "unknown")
        role = payload.get("role", "viewer")
        return UserContext(user_id=user_id, role=role)
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
    token_type: str = "bearer"
    user_id: str
    role: str


class MeResponse(BaseModel):
    user_id: str
    role: str
    scopes: tuple[str, ...] = ()


@router.post("/login", response_model=TokenResponse)
async def login(body: LoginRequest, request: Request):
    if not db.client:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Supabase client is not initialized",
        )

    try:
        res = db.client.auth.sign_in_with_password({"email": body.username, "password": body.password})
        if not res.user:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")

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

        # বাংলা মন্তব্য: Phase 2 — Hybrid Fingerprint Login। হেডারটি ঐচ্ছিক, তাই না থাকলেও
        # লগইন স্বাভাবিকভাবে চলবে (ব্রেকিং চেঞ্জ নয়); থাকলে ডিভাইসটি known-devices সেটে যোগ হয়
        # যা AntiHackingContextMiddleware admin scope-এ তৃতীয় সিগন্যাল হিসেবে ব্যবহার করে।
        fingerprint = request.headers.get("x-device-fingerprint")
        if fingerprint and redis_manager and redis_manager.client:
            try:
                await redis_manager.client.sadd(f"device:known:{user_id}", fingerprint)
            except Exception as exc:
                logger.warning(f"Failed to register device fingerprint for {user_id}: {exc}")

        return TokenResponse(access_token=access_token, user_id=user_id, role=primary_role)
    except Exception as e:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=str(e))


@router.post("/register", response_model=TokenResponse)
async def register(body: RegisterRequest):
    if not db.client:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Supabase client is not initialized",
        )

    try:
        res = db.client.auth.sign_up({"email": body.username, "password": body.password})
        if not res.user:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Registration failed")

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
        return TokenResponse(access_token=access_token, user_id=user_id, role=primary_role)
    except Exception as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


@router.get("/me", response_model=MeResponse)
async def me(current_user: UserContext | None = Depends(optional_current_user)):
    if current_user is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Not authenticated")
    # বাংলা মন্তব্য: scopes যদি None হয় তবে MeResponse ভ্যালিডেশন পাস করানোর জন্য খালি টুপল পাস করা হচ্ছে।
    scopes_val = current_user.scopes if current_user.scopes is not None else ()
    return MeResponse(user_id=current_user.user_id, role=current_user.role, scopes=scopes_val)


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
