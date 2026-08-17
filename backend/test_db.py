import os
import asyncio
import logging
from dotenv import load_dotenv
from sqlalchemy.ext.asyncio import create_async_engine
from sqlalchemy import text

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("test_db")


async def check_db():
    try:
        url = os.getenv('SUPABASE_DATABASE_URL')
        if not url:
            logger.warning("No DB URL configured")
            return
        if url.startswith('postgres://'):
            url = url.replace('postgres://', 'postgresql+asyncpg://', 1)
        elif url.startswith('postgresql://'):
            url = url.replace('postgresql://', 'postgresql+asyncpg://', 1)
        logger.info("Connecting to %s", url)
        engine = create_async_engine(url, echo=False)
        async with engine.connect() as conn:
            res = await conn.execute(text('SELECT 1'))
            logger.info("Result: %s", res.scalar())
        await engine.dispose()
    except Exception as e:
        logger.error("DB Error: %s", e, exc_info=True)


if __name__ == "__main__":
    load_dotenv('.env')
    asyncio.run(check_db())

