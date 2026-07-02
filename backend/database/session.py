import os

from loguru import logger
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.ext.asyncio import async_sessionmaker
from sqlalchemy.ext.asyncio import create_async_engine
from sqlalchemy.pool import NullPool


DATABASE_URL = os.getenv("SUPABASE_DATABASE_URL_POOLER", "")

if not DATABASE_URL:
    logger.warning("SUPABASE_DATABASE_URL_POOLER is missing. Database operations will fail.")

# বাংলা মন্তব্য: কানেকশন স্ট্রিংয়ে postgresql:// বা postgres:// থাকলে তা asyncpg-এর জন্য postgresql+asyncpg:// দিয়ে প্রতিস্থাপন করা হচ্ছে
def get_async_url(url: str) -> str:
    if not url:
        return "sqlite+aiosqlite:///:memory:"
    if url.startswith("postgresql://"):
        return url.replace("postgresql://", "postgresql+asyncpg://", 1)
    if url.startswith("postgres://"):
        return url.replace("postgres://", "postgresql+asyncpg://", 1)
    return url

engine = create_async_engine(
    get_async_url(DATABASE_URL),
    poolclass=NullPool,
    echo=False
)

AsyncSessionLocal = async_sessionmaker(
    bind=engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autoflush=False
)

# FastAPI Dependency Injection (with safe rollback)
async def get_db_session():
    async with AsyncSessionLocal() as session:
        try:
            yield session
        except Exception as e:
            await session.rollback()
            logger.error(f"Database transaction rolled back due to error: {e}")
            raise
        finally:
            await session.close()
