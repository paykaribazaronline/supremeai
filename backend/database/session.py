import os
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy.pool import NullPool
from loguru import logger

# Cloud Run-এর জন্য PgBouncer Transaction Mode URL ব্যবহার করা বাধ্যতামূলক
DATABASE_URL = os.getenv("SUPABASE_DATABASE_URL_POOLER", "")

if not DATABASE_URL:
    logger.warning("SUPABASE_DATABASE_URL_POOLER is missing. Database operations will fail.")

# Pro Tip: PgBouncer যখন কানেকশন পুলিং করছে, তখন SQLAlchemy-কে নিজস্ব পুল মেইনটেইন করতে দেওয়াটা ডেডলকের কারণ হতে পারে। 
# তাই 'poolclass=NullPool' ব্যবহার করা হলো, যাতে প্রতিটি রিকোয়েস্ট সরাসরি PgBouncer থেকে কানেকশন নেয় এবং কাজ শেষে ছেড়ে দেয়।
engine = create_async_engine(
    DATABASE_URL.replace("postgres://", "postgresql+asyncpg://") if DATABASE_URL else "sqlite+aiosqlite:///:memory:",
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
