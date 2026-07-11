# 📄 ফাইল: scripts/migrate_default_user.py

**প্রকার:** .py  
**সাইজ:** 1,552 বাইট  
**আপডেট:** 2026-07-11T13:28:08.915598

---

## কোড

```py
import asyncio
import sys
from pathlib import Path

# Add backend directory to sys.path to allow importing backend modules
backend_dir = Path(__file__).parent.parent / "backend"
sys.path.insert(0, str(backend_dir))

from database.session import async_session_maker
from models.wallet import UserWallet, TransactionLedgerEntry
from sqlalchemy.future import select
from loguru import logger

async def migrate_default_user(new_user_id: str):
    async with async_session_maker() as session:
        async with session.begin():
            # Update Wallets
            result = await session.execute(
                select(UserWallet).where(UserWallet.user_id == "default_user_session")
            )
            wallets = result.scalars().all()
            for wallet in wallets:
                logger.info(f"Migrating wallet {wallet.id} to new user {new_user_id}")
                wallet.user_id = new_user_id
            
            # Update Ledgers
            result = await session.execute(
                select(TransactionLedgerEntry).where(TransactionLedgerEntry.user_id == "default_user_session")
            )
            entries = result.scalars().all()
            for entry in entries:
                entry.user_id = new_user_id
            
            logger.info(f"Migrated {len(wallets)} wallets and {len(entries)} transaction entries.")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python migrate_default_user.py <new_user_id>")
        sys.exit(1)
    asyncio.run(migrate_default_user(sys.argv[1]))

```