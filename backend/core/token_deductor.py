# Distributed billing token deductions and micro-transaction processor
# বাংলা মন্তব্য: ডিস্ট্রিবিউটেড এনভায়রনমেন্টে রেস কন্ডিশন এড়াতে রেডিস ডিস্ট্রিবিউটেড লক সহ ব্যালেন্স মডিউলেটর।

import asyncio  # বাংলা মন্তব্য: ফাইলের শুরুতে asyncio ইম্পোর্ট নেওয়া হলো
import json
import os
import uuid
from decimal import Decimal

from loguru import logger
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy.orm.exc import StaleDataError

from core.upstash_redis_queue import UpstashRedisQueue
from models.transaction_ledger import TransactionLedgerEntry
from models.wallet import UserWallet


redis_queue = UpstashRedisQueue()


class TokenDeductor:
    """
    Safely deducts credits from a user's wallet based on token consumption.
    Features Distributed Redis Locking to prevent double-spending race conditions.
    """
    def __init__(self):
        # Load token price config
        base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        config_path = os.path.join(base_dir, "config", "pricing_tiers.json")
        try:
            with open(config_path, encoding="utf-8") as f:
                self.config = json.load(f)
        except Exception:
            self.config = {
                "token_rates_usd_per_1k": {"input": 0.0015, "output": 0.0020},
                "byoc_deployment_fee_usd": 0.05
            }

    def _acquire_distributed_lock(self, lock_key: str, lock_value: str, ttl: int = 10) -> bool:
        """
        Acquires a distributed lock using Upstash Redis SET NX.
        """
        # বাংলা মন্তব্য: গ্লোবাল রেডিস কী লক সেট করা হচ্ছে ডাবল-স্পেন্ডিং ঠেকাতে।
        if not redis_queue.configured:
            # Fallback for local testing without active Upstash credentials
            return True
            
        try:
            # SET lock_key lock_value NX EX ttl
            return redis_queue.set_nx(lock_key, lock_value, ex=ttl)
        except Exception as e:
            logger.error(f"Failed to acquire distributed lock: {e}")
            return False

    def _release_distributed_lock(self, lock_key: str, lock_value: str):
        """
        Releases a distributed lock.
        """
        if not redis_queue.configured:
            return
        try:
            # বাংলা মন্তব্য: Lua স্ক্রিপ্ট ব্যবহার করে নিশ্চিত করা হচ্ছে যে লক সৃষ্টিকারী ওনার ছাড়া অন্য কেউ লক ডিলিট করতে পারবে না
            lua_script = """
            if redis.call("get", KEYS[1]) == ARGV[1] then
                return redis.call("del", KEYS[1])
            else
                return 0
            end
            """
            redis_queue.eval(lua_script, 1, lock_key, lock_value)
        except Exception as e:
            logger.error(f"Failed to release distributed lock: {e}")

    async def deduct_tokens(self, session: AsyncSession, user_id: str, input_tokens: int, output_tokens: int, model_name: str) -> bool:
        """
        Deducts credits based on inputs/outputs token counts with Zero-Gap Concurrency Control.
        """
        lock_key = f"lock:wallet:{user_id}"
        lock_value = str(uuid.uuid4())
        
        # Poll lock acquisition to avoid blocking
        acquired = False
        for _ in range(20):
            if self._acquire_distributed_lock(lock_key, lock_value, ttl=5):
                acquired = True
                break
            await asyncio.sleep(0.1)

        if not acquired:
            logger.error(f"Could not acquire distributed lock for user: {user_id}")
            return False

        try:
            # Calculate rates accurately and cast to Decimal for strict precision
            rates = self.config.get("token_rates_usd_per_1k", {"input": 0.0015, "output": 0.0020})
            cost_float = (input_tokens / 1000.0 * rates["input"]) + (output_tokens / 1000.0 * rates["output"])
            cost = Decimal(str(round(cost_float, 6)))

            # Atomic Transaction Block
            async with session.begin():
                # বাংলা কমেন্ট: .with_for_update() ব্যবহার করে ডাটাবেসের নির্দিষ্ট রো-টি লক করা হচ্ছে (Zero-Gap Concurrency)
                result = await session.execute(select(UserWallet).where(UserWallet.user_id == user_id).with_for_update())
                wallet = result.scalars().first()

                if not wallet:
                    logger.error(f"Wallet not found for user: {user_id}")
                    return False

                total_available = wallet.balance_usd + wallet.monthly_allowance_usd
                if total_available < cost:
                    logger.warning(f"Insufficient funds for user {user_id}: required {cost}, available {total_available}")
                    return False

                # Deduct from allowance first, then main balance
                if wallet.monthly_allowance_usd >= cost:
                    wallet.monthly_allowance_usd -= cost
                else:
                    remaining = cost - wallet.monthly_allowance_usd
                    wallet.monthly_allowance_usd = Decimal('0.000000')
                    wallet.balance_usd -= remaining

                # Record in Ledger
                tx_id = str(uuid.uuid4())
                entry = TransactionLedgerEntry(
                    transaction_id=tx_id,
                    user_id=user_id,
                    amount_usd=-cost,
                    transaction_type="token_usage",
                    description=f"Consumed {input_tokens}i/{output_tokens}o tokens on model: {model_name}"
                )
                session.add(entry)
            
            # session.begin() ব্লকের বাইরে আসার সাথে সাথে এটি অটোমেটিকভাবে কমিট হবে। 
            # যদি অন্য কোনো থ্রেড ইতমধ্যে ব্যালেন্স মডিফাই করে থাকে, তবে SQLAlchemy 'version' কলাম চেক করে StaleDataError থ্রো করবে।
            
            logger.success(f"Deducted ${cost} from user {user_id} for token usage.")
            return True

        except StaleDataError:
            logger.critical(f"Optimistic Concurrency Failure: Wallet modified by another transaction for user {user_id}")
            return False
        except Exception as e:
            logger.error(f"Transaction failed for {user_id}: {str(e)}")
            return False
        finally:
            self._release_distributed_lock(lock_key, lock_value)

    async def deduct_byoc_deployment(self, session: AsyncSession, user_id: str, skill_name: str) -> bool:
        """
        Deducts credit for spinning up BYOC Cloud Run services.
        """
        lock_key = f"lock:wallet:{user_id}"
        lock_value = str(uuid.uuid4())
        
        acquired = False
        for _ in range(20):
            if self._acquire_distributed_lock(lock_key, lock_value, ttl=5):
                acquired = True
                break
            await asyncio.sleep(0.1)

        if not acquired:
            return False

        try:
            cost_float = self.config.get("byoc_deployment_fee_usd", 0.05)
            cost = Decimal(str(round(cost_float, 6)))

            async with session.begin():
                # বাংলা কমেন্ট: .with_for_update() ব্যবহার করে ডাটাবেসের নির্দিষ্ট রো-টি লক করা হচ্ছে (Zero-Gap Concurrency)
                result = await session.execute(select(UserWallet).where(UserWallet.user_id == user_id).with_for_update())
                wallet = result.scalars().first()

                if not wallet:
                    return False

                total_available = wallet.balance_usd + wallet.monthly_allowance_usd
                if total_available < cost:
                    return False

                if wallet.monthly_allowance_usd >= cost:
                    wallet.monthly_allowance_usd -= cost
                else:
                    remaining = cost - wallet.monthly_allowance_usd
                    wallet.monthly_allowance_usd = Decimal('0.000000')
                    wallet.balance_usd -= remaining

                tx_id = str(uuid.uuid4())
                entry = TransactionLedgerEntry(
                    transaction_id=tx_id,
                    user_id=user_id,
                    amount_usd=-cost,
                    transaction_type="byoc_deployment",
                    description=f"BYOC deployment fee for skill: {skill_name}"
                )
                session.add(entry)
            
            logger.success(f"Deducted ${cost} deployment fee from user {user_id}.")
            return True
            
        except StaleDataError:
            logger.critical(f"Optimistic Concurrency Failure: Wallet modified by another transaction for user {user_id}")
            return False
        except Exception as e:
            logger.error(f"Transaction failed for {user_id}: {str(e)}")
            return False
        finally:
            self._release_distributed_lock(lock_key, lock_value)
