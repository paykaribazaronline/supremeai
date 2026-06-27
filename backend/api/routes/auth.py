from __future__ import annotations

from datetime import datetime
from datetime import timedelta
from datetime import timezone

from fastapi import APIRouter
from fastapi import Depends
from fastapi import HTTPException
from fastapi import status
from fastapi.security import OAuth2PasswordBearer
from pydantic import BaseModel


try:
    from jose import JWTError
    from jose import jwt
except ImportError:
    JWTError = Exception  # type: ignore[misc,assignment]
    jwt = None  # type: ignore[assignment]

from core.config import settings
from core.rbac import UserContext


router = APIRouter(prefix="/auth", tags=["auth"])

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login", auto_error=False)

SECRET_KEY = settings.jwt_secret
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24


def create_access_token(data: dict, expires_delta: timedelta | None = None) -> str:
    if jwt is None:
        raise RuntimeError("python-jose[cryptography] is required for token issuance")
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + (expires_delta or timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES))
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)


async def optional_current_user(
    token: str | None = Depends(oauth2_scheme),
) -> UserContext | None:
    if not token or jwt is None:
        return None
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        user_id = payload.get("sub", "unknown")
        role = payload.get("role", "viewer")
        return UserContext(user_id=user_id, role=role)
    except Exception:
        return None


class LoginRequest(BaseModel):
    username: str
    password: str


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
async def login(body: LoginRequest):
    raise HTTPException(
        status_code=status.HTTP_501_NOT_IMPLEMENTED,
        detail="Direct login is not supported. Use the admin TOTP flow or an OAuth provider.",
    )


@router.get("/me", response_model=MeResponse)
async def me(current_user: UserContext | None = Depends(optional_current_user)):
    if current_user is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Not authenticated")
    return MeResponse(user_id=current_user.user_id, role=current_user.role, scopes=current_user.scopes)
