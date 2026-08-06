from urllib.parse import urlencode

import httpx
from fastapi import APIRouter, Depends, Request
from fastapi.responses import RedirectResponse
from loguru import logger
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from api.dependencies import get_current_user_token
from core.config import settings
from core.security.security_vault import encrypt_token
from database.session import get_db_session
from models.integration import Integration

# বাংলা মন্তব্য: GitHub OAuth — রিয়েল ইউজার আইডি ও DB পার্সিস্টেন্স সহ সম্পূর্ণ ফ্লো
# আগের ভার্সনে user_id_placeholder = "test_user_id" হার্ডকোডেড ছিল এবং টোকেন DB-তে সেভ হতো না।
# এখন JWT থেকে প্রকৃত user_id নেওয়া হচ্ছে এবং encrypted token DB-তে সংরক্ষিত হচ্ছে।

router = APIRouter()


def _build_github_redirect_uri() -> str:
    """
    ডায়নামিক রিডাইরেক্ট URI তৈরি করে — প্রোডাকশনে settings.frontend_base_url ব্যবহার করবে,
    লোকালে ডিফল্ট localhost:8000।
    """
    base = settings.frontend_base_url
    return f"{base}/api/v1/integrations/github/callback"


@router.get("/integrations/github/link")
async def link_github():
    """
    ইউজারকে GitHub OAuth লগইন পেইজে রিডাইরেক্ট করে।
    redirect_uri এখন ডায়নামিক — settings.frontend_base_url থেকে নেওয়া হয়।
    """
    redirect_uri = _build_github_redirect_uri()
    params = {
        "client_id": settings.github_client_id,
        "scope": "repo user",
        "redirect_uri": redirect_uri,
    }
    github_auth_url = f"https://github.com/login/oauth/authorize?{urlencode(params)}"
    return RedirectResponse(url=github_auth_url)


@router.get("/integrations/github/callback")
async def github_callback(
    code: str,
    request: Request,
    token_payload: dict = Depends(get_current_user_token),
    db: AsyncSession = Depends(get_db_session),
):
    """
    GitHub OAuth কলব্যাক হ্যান্ডলার।
    কোড এক্সচেঞ্জ করে access_token নেয়, এনক্রিপ্ট করে, এবং DB-তে সংরক্ষণ করে।
    """
    # ১. JWT থেকে প্রকৃত user_id বের করা
    user_id = token_payload.get("sub")
    if not user_id:
        logger.error("GitHub OAuth callback: Token payload missing 'sub' claim.")
        return RedirectResponse(
            url=f"{settings.frontend_base_url}/integrations?status=error&message=Invalid token"
        )

    redirect_uri = _build_github_redirect_uri()
    token_url = "https://github.com/login/oauth/access_token"
    payload = {
        "client_id": settings.github_client_id,
        "client_secret": settings.github_client_secret,
        "code": code,
        "redirect_uri": redirect_uri,
    }
    headers = {"Accept": "application/json"}

    async with httpx.AsyncClient(timeout=15.0) as client:
        # ⏱️ FIX: explicit timeout — default timeout infinite হলে serverless function hang করে বিল বাড়ায়
        response = await client.post(token_url, json=payload, headers=headers, timeout=30.0)
        data = response.json()

    access_token = data.get("access_token")
    if not access_token:
        logger.warning(f"GitHub OAuth failed for user {user_id}: no access_token in response")
        return RedirectResponse(
            url=f"{settings.frontend_base_url}/integrations?status=error&message=Failed to get access token"
        )

    # ২. টোকেন এনক্রিপ্ট করা (AES-256 Fernet)
    encrypted_token = encrypt_token(access_token)

    # ৩. DB-তে ইন্টিগ্রেশন সেভ করা (upsert — একই user_id + provider-এ আপডেট)
    # ⚠️ FIX: SQLAlchemy AsyncSession.get() শুধুমাত্র primary key নেয়, dict ফিল্টার নয়।
    # তাই select() + where() ব্যবহার করতে হবে — নাহলে runtime ArgumentError থ্রো করবে।
    try:
        stmt = select(Integration).where(
            Integration.user_id == user_id,
            Integration.provider == "github",
        )
        result = await db.execute(stmt)
        existing = result.scalar_one_or_none()
        if existing:
            existing.encrypted_access_token = encrypted_token
        else:
            new_integration = Integration(
                user_id=user_id,
                provider="github",
                encrypted_access_token=encrypted_token,
            )
            db.add(new_integration)
        await db.commit()
        logger.info(f"✅ GitHub integration saved for user '{user_id}'")
    except Exception as exc:
        await db.rollback()
        logger.error(f"Failed to save GitHub integration for user '{user_id}': {exc}")
        return RedirectResponse(
            url=f"{settings.frontend_base_url}/integrations?status=error&message=Database error"
        )

    # ৪. ফ্রন্টএন্ডে রিডাইরেক্ট — ডায়নামিক URL
    frontend_base = settings.frontend_base_url
    return RedirectResponse(url=f"{frontend_base}/integrations?status=success")
