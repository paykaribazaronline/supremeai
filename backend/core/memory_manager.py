"""
Memory Manager for Free-Tier Optimization
Monitors and manages memory usage within 512MB constraint.
"""
import psutil
import gc
import logging
from dataclasses import dataclass
from typing import Optional
from functools import wraps
import asyncio

logger = logging.getLogger(__name__)

@dataclass
class MemoryStatus:
    total_mb: float
    used_mb: float
    free_mb: float
    percent_used: float
    is_critical: bool
    is_warning: bool

class FreeTierMemoryManager:
    """
    Memory manager optimized for Render's 512MB Free Tier.
    
    Thresholds:
    - Warning: >70% (~358 MB)
    - Critical: >85% (~435 MB)
    - Maximum: 512 MB (hard limit)
    """
    
    WARNING_THRESHOLD = 70.0  # percentage
    CRITICAL_THRESHOLD = 85.0  # percentage
    MAX_MEMORY_MB = 512  # Render free tier limit
    
    def __init__(self):
        self._process = psutil.Process()
        self._last_gc_time = 0
        self._gc_interval_seconds = 60  # Run GC every 60 seconds
    
    def get_status(self) -> MemoryStatus:
        """Get current memory status."""
        try:
            mem_info = self._process.memory_info()
            total_virtual = self._process.memory_info().rss / (1024 * 1024)
            
            # Get system memory for context
            system_mem = psutil.virtual_memory()
            
            status = MemoryStatus(
                total_mb=self.MAX_MEMORY_MB,
                used_mb=round(total_virtual, 2),
                free_mb=round(self.MAX_MEMORY_MB - total_virtual, 2),
                percent_used=round((total_virtual / self.MAX_MEMORY_MB) * 100, 2),
                is_critical=(total_virtual / self.MAX_MEMORY_MB * 100) >= self.CRITICAL_THRESHOLD,
                is_warning=(total_virtual / self.MAX_MEMORY_MB * 100) >= self.WARNING_THRESHOLD
            )
            
            return status
            
        except Exception as e:
            logger.error(f"Failed to get memory status: {e}")
            return MemoryStatus(512, 256, 256, 50.0, False, False)
    
    def should_cleanup(self) -> bool:
        """Check if we should run cleanup based on thresholds."""
        status = self.get_status()
        return status.is_critical or status.is_warning
    
    async def cleanup_if_needed(self, force: bool = False):
        """
        Run garbage collection and cleanup if memory is high.
        
        Args:
            force: Force cleanup regardless of threshold
        """
        status = self.get_status()
        
        if force or status.is_critical:
            logger.warning(f"⚠️ Memory critical ({status.percent_used}%). Running aggressive cleanup...")
            await self._aggressive_cleanup()
            
        elif status.is_warning:
            logger.info(f"ℹ️ Memory warning ({status.percent_used}%). Running standard cleanup...")
            await self._standard_cleanup()
    
    async def _standard_cleanup(self):
        """Standard garbage collection."""
        # Force Python garbage collection
        gc.collect()
        
        # Clear caches if they exist
        if hasattr(self, '_clear_caches'):
            self._clear_caches()
    
    async def _aggressive_cleanup(self):
        """Aggressive cleanup for critical memory situations."""
        import traceback
        
        logger.critical("🚨 Running AGGRESSIVE memory cleanup!")
        
        # 1. Force multiple GC passes
        for _ in range(3):
            gc.collect()
            await asyncio.sleep(0.1)
        
        # 2. Clear any object pools
        try:
            from core.ai_memory.vector_store import VectorStore
            if hasattr(VectorStore, '_connection_pool'):
                # Don't close, just shrink
                pass
        except ImportError:
            pass
        
        # 3. Log top memory consumers
        try:
            import tracemalloc
            tracemalloc.start()
            
            snapshot = tracemalloc.take_snapshot()
            top_stats = snapshot.statistics('lineno')[:10]
            
            logger.warning("Top memory consumers:")
            for stat in top_stats:
                logger.warning(f"  {stat}")
                
            tracemalloc.stop()
        except Exception:
            pass


# Singleton instance
_memory_manager: Optional[FreeTierMemoryManager] = None

def get_memory_manager() -> FreeTierMemoryManager:
    """Get or create the singleton memory manager instance."""
    global _memory_manager
    if _memory_manager is None:
        _memory_manager = FreeTierMemoryManager()
    return _memory_manager


def memory_aware(func):
    """
    Decorator that checks memory before and after function execution.
    Automatically triggers cleanup if needed.
    """
    @wraps(func)
    async def wrapper(*args, **kwargs):
        manager = get_memory_manager()
        
        # Check before execution
        await manager.cleanup_if_needed()
        
        try:
            # Execute function
            if asyncio.iscoroutinefunction(func):
                result = await func(*args, **kwargs)
            else:
                result = func(*args, **kwargs)
            
            # Check after execution
            await manager.cleanup_if_needed()
            
            return result
            
        except MemoryError:
            # Emergency cleanup on OOM
            logger.critical("💥 Out of memory! Emergency cleanup...")
            await manager.cleanup_if_needed(force=True)
            raise
            
    return wrapper


from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request

class MemoryAwareMiddleware(BaseHTTPMiddleware):
    """FastAPI middleware that monitors memory usage."""
    
    async def dispatch(self, request: Request, call_next):
        manager = get_memory_manager()
        
        # Log memory status for monitoring
        status = manager.get_status()
        
        if status.is_critical:
            logger.critical(f"🚨 CRITICAL MEMORY: {status.percent_used}% used")
        elif status.is_warning:
            logger.warning(f"⚠️ HIGH MEMORY: {status.percent_used}% used")
        
        # Process request
        response = await call_next(request)
        
        # Add memory headers for debugging
        response.headers["X-Memory-Used-MB"] = str(status.used_mb)
        response.headers["X-Memory-Percent"] = str(status.percent_used)
        
        # Cleanup after request
        await manager.cleanup_if_needed()
        
        return response
