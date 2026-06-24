import os
import jwt
from datetime import datetime, timedelta, timezone
from fastapi import HTTPException, status
from loguru import logger
from core.config import settings

# স্ট্রিক্ট এনভায়রনমেন্ট ভেরিয়েবল (কখনোই হার্ডকোড করা যাবে না)
SECRET_KEY = settings.jwt_secret
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60

# 🚫 Auto-Admin Grant বন্ধ করে স্ট্রিক্ট হোয়াইটলিস্ট
ADMIN_WHITELIST = os.getenv("ADMIN_EMAILS", "admin@supremeai.com").split(",")

if not SECRET_KEY:
    logger.critical("🚨 FATAL: JWT Secret is missing! Halting boot process to prevent vulnerabilities.")
    raise RuntimeError("Security misconfiguration: Missing JWT Secret.")

def create_access_token(data: dict) -> str:
    """ক্রিপ্টোগ্রাফিক সাইনড JWT জেনারেট করবে (Plain Password নয়)"""
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    
    # ইউজার অ্যাডমিন কি না, তা হোয়াইটলিস্ট দিয়ে ভেরিফাই হবে, len(email) দিয়ে নয়!
    user_email = to_encode.get("sub")
    role = "admin" if user_email in ADMIN_WHITELIST else "user"
    to_encode.update({"role": role})

    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt

def verify_token(token: str) -> dict:
    """টোকেন ডিকোড এবং ভেরিফাই করবে"""
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token has expired")
    except jwt.PyJWTError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")
