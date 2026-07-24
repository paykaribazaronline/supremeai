from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager
from typing import Any

from core.config import settings
from loguru import logger
from sqlalchemy.ext.asyncio import (AsyncSession, async_sessionmaker,
                                    create_async_engine)
from sqlalchemy.pool import StaticPool

DATABASE_URL = settings.supabase_database_url

if not DATABASE_URL:
    logger.warning(
        "SUPABASE_DATABASE_URL_POOLER is missing. Database operations will fail."
    )


# বাংলা মন্তব্য: কানেকশন স্ট্রিংয়ে postgresql:// বা postgres:// থাকলে তা asyncpg-এর জন্য postgresql+asyncpg:// দিয়ে প্রতিস্থাপন করা হচ্ছে
def get_async_url(url: str) -> str:
    if not url:
        return "sqlite+aiosqlite:///:memory:"
    if url.startswith("postgresql://"):
        return url.replace("postgresql://", "postgresql+asyncpg://", 1)
    if url.startswith("postgres://"):
        return url.replace("postgres://", "postgresql+asyncpg://", 1)
    return url


_async_url = get_async_url(DATABASE_URL)

# বাংলা মন্তব্য: MyPy টাইপ ইনফারেন্সের সমস্যা সমাধানের জন্য টাইপ হিসেবে dict[str, Any] ব্যবহার করা হলো
engine_kwargs: dict[str, Any] = {
    "echo": False,
}
if _async_url.startswith("sqlite"):
    engine_kwargs["poolclass"] = StaticPool
    engine_kwargs["connect_args"] = {"check_same_thread": False}
if _async_url.startswith("postgresql"):
    # বাংলা মন্তব্য: User ও Admin — দুই আলাদা Render instance একই Supabase PgBouncer পুলে
    # কানেক্ট করে, তাই SERVICE_ROLE অনুযায়ী pool limit ভাগ করা হচ্ছে যাতে কোনো একটি
    # instance বাকিটার জন্য কানেকশন শেষ করে না ফেলে (pool exhaustion prevention)।
    # User: high-traffic client-facing, বেশি concurrency দরকার -> min=2, max=15 (pool_size + max_overflow)
    # Admin: low-traffic internal panel, সামান্য concurrency যথেষ্ট -> min=1, max=3
    _role = settings.service_role.lower()
    if _role == "admin":
        _pool_size, _max_overflow = 1, 2  # base(1) + overflow(2) = max 3 concurrent
    else:
        _pool_size, _max_overflow = 2, 13  # base(2) + overflow(13) = max 15 concurrent

    engine_kwargs.update(
        {
            "pool_size": _pool_size,
            "max_overflow": _max_overflow,
            "pool_timeout": 30,
            "pool_recycle": 1800,
            # বাংলা মন্তব্য: stateless API রুট থেকে কানেকশন যেন দ্রুত রিলিজ হয়, তাই pre_ping দিয়ে
            # স্টেল কানেকশন এড়ানো হচ্ছে (PgBouncer transaction-mode এ স্টেল হওয়া সাধারণ ঘটনা)।
            "pool_pre_ping": True,
            # বাংলা মন্তব্য: PgBouncer এর transaction pool মোডের সাথে সামঞ্জস্যের জন্য statement_cache_size=0 করা হলো
            "connect_args": {
                "command_timeout": 30,
                "server_settings": {"application_name": f"supremeai_2.0_{_role}"},
                "statement_cache_size": 0,
            },
        }
    )
    logger.info(
        f"🔌 DB pool configured for SERVICE_ROLE='{_role}': pool_size={_pool_size}, max_overflow={_max_overflow}"
    )

engine = create_async_engine(_async_url, **engine_kwargs)

AsyncSessionLocal = async_sessionmaker(
    bind=engine, class_=AsyncSession, expire_on_commit=False, autoflush=False
)


@asynccontextmanager
async def get_db_session_context() -> AsyncGenerator[AsyncSession, None]:
    """Context manager for backend tasks or non-FastAPI usages.

    বাংলা: FastAPI-এর বাইরে বা ব্যাকগ্রাউন্ড টাস্কে ডাটাবেস সেশন ব্যবহারের জন্য।
    """
    from fastapi import HTTPException
    from sqlalchemy.exc import TimeoutError as SATimeoutError

    try:
        async with AsyncSessionLocal() as session:
            try:
                yield session
            except Exception as e:
                await session.rollback()
                logger.error(f"Database transaction rolled back due to error: {e}")
                raise
    except (TimeoutError, SATimeoutError) as e:
        logger.error(f"Database pool exhausted: {e}")
        raise HTTPException(
            status_code=503,
            detail="Service temporarily unavailable due to high load (DB pool exhausted).",
        )


# FastAPI Dependency Injection (with safe rollback)
async def get_db_session() -> AsyncGenerator[AsyncSession, None]:
    """FastAPI Dependency for database sessions.

    বাংলা: FastAPI রুটগুলোর জন্য ডাটাবেস ডিপেন্ডেন্সি।
    """
    async with get_db_session_context() as session:
        yield session
