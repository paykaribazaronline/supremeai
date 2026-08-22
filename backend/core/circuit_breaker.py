import time
import logging
from dataclasses import dataclass, field
from enum import Enum

logger = logging.getLogger("supremeai.fallback")

class CircuitState(Enum):
    CLOSED = "closed"        # Normal, external source is used
    OPEN = "open"             # External source down, use fallback directly
    HALF_OPEN = "half_open"   # Test if recovered

@dataclass
class CircuitBreaker:
    failure_threshold: int = 3
    recovery_timeout: float = 30.0
    state: CircuitState = field(default=CircuitState.CLOSED)
    failure_count: int = 0
    opened_at: float = 0.0

    async def record_success(self):
        self.failure_count = 0
        self.state = CircuitState.CLOSED

    async def record_failure(self):
        self.failure_count += 1
        if self.failure_count >= self.failure_threshold:
            self.state = CircuitState.OPEN
            self.opened_at = time.monotonic()
            logger.warning("Circuit OPEN — external source failed %d times, entering fallback mode", self.failure_count)

    async def should_attempt_external(self) -> bool:
        if self.state == CircuitState.CLOSED:
            return True
        if self.state == CircuitState.OPEN:
            if time.monotonic() - self.opened_at >= self.recovery_timeout:
                self.state = CircuitState.HALF_OPEN
                logger.info("Circuit HALF_OPEN — testing external source recovery")
                return True
            return False
        # HALF_OPEN
        return True

class RedisCircuitBreaker(CircuitBreaker):
    """
    A Centralized Circuit Breaker using Redis for tracking state across multiple workers.
    Falls back to the in-memory base class logic if Redis is unreachable.
    """
    def __init__(self, name: str = "default", failure_threshold: int = 3, recovery_timeout: float = 30.0):
        super().__init__(failure_threshold=failure_threshold, recovery_timeout=recovery_timeout)
        self.name = name
        self.prefix = f"circuit_breaker:{name}"
    
    async def _get_redis_client(self):
        from core.cache.redis_manager import redis_manager
        return await redis_manager.get_client_async()
        
    async def record_success(self):
        client = await self._get_redis_client()
        if not client:
            return await super().record_success()
            
        try:
            await client.set(f"{self.prefix}:state", CircuitState.CLOSED.value)
            await client.set(f"{self.prefix}:failures", 0)
        except Exception as e:
            logger.error(f"RedisCircuitBreaker record_success failed: {e}")
            await super().record_success()

    async def record_failure(self):
        client = await self._get_redis_client()
        if not client:
            return await super().record_failure()
            
        try:
            failures = await client.incr(f"{self.prefix}:failures")
            if failures >= self.failure_threshold:
                # Set state to OPEN
                current_state = await client.get(f"{self.prefix}:state")
                if current_state != CircuitState.OPEN.value:
                    await client.set(f"{self.prefix}:state", CircuitState.OPEN.value)
                    await client.set(f"{self.prefix}:opened_at", time.time())
                    logger.warning(f"[{self.name}] Circuit OPEN — external source failed {failures} times, entering fallback mode")
        except Exception as e:
            logger.error(f"RedisCircuitBreaker record_failure failed: {e}")
            await super().record_failure()

    async def should_attempt_external(self) -> bool:
        client = await self._get_redis_client()
        if not client:
            return await super().should_attempt_external()
            
        try:
            state_val = await client.get(f"{self.prefix}:state")
            if not state_val:
                return True
                
            if state_val == CircuitState.CLOSED.value:
                return True
                
            if state_val == CircuitState.OPEN.value:
                opened_at = await client.get(f"{self.prefix}:opened_at")
                if opened_at:
                    elapsed = time.time() - float(opened_at)
                    if elapsed >= self.recovery_timeout:
                        await client.set(f"{self.prefix}:state", CircuitState.HALF_OPEN.value)
                        logger.info(f"[{self.name}] Circuit HALF_OPEN — testing external source recovery")
                        return True
                return False
                
            # HALF_OPEN
            return True
        except Exception as e:
            logger.error(f"RedisCircuitBreaker should_attempt_external failed: {e}")
            return await super().should_attempt_external()
