"""
SupremeAI Rate Limiting Middleware
===================================
Redis-backed rate limiting for API endpoints.
Protects against abuse and controls LLM costs.

Author: SuperAI Transformation Patch
Version: 1.0.0
"""

import time
import asyncio
import logging
from typing import Optional
from functools import wraps

from fastapi import Request, Response, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from starlette.middleware.base import BaseHTTPMiddleware
import redis.asyncio as aioredis

# Configure logging
logger = logging.getLogger(__name__)

security = HTTPBearer(auto_error=False)


class RateLimiter:
    """
    Token bucket rate limiter backed by Redis.
    
    Supports multiple rate limit tiers:
    - Anonymous: 10 requests/minute
    - Authenticated: 60 requests/minute  
    - Premium: 300 requests/minute
    - Admin: 1000 requests/minute
    """
    
    # Rate limit configurations (requests per minute)
    TIERS = {
        "anonymous": (10, 60),      # 10 req/min
        "authenticated": (60, 60),  # 60 req/min
        "premium": (300, 60),       # 300 req/min
        "admin": (1000, 60),        # 1000 req/min
    }
    
    # Endpoint-specific overrides (endpoint: (limit, window))
    ENDPOINT_OVERRIDES = {
        "/api/chat/stream": (30, 60),  # Streaming is expensive
        "/api/ai/generate": (20, 60),  # AI generation
        "/api/browser/scrape": (5, 60),  # Scraping is resource-intensive
    }
    
    def __init__(
        self,
        redis_url: str = "redis://localhost:6379",
        enabled: bool = True
    ):
        self.redis_url = redis_url
        self.enabled = enabled
        self._redis: Optional[aioredis.Redis] = None
    
    async def _get_redis(self) -> Optional[aioredis.Redis]:
        """Lazy Redis initialization."""
        from core.config import settings
        if getattr(settings, 'RATE_LIMIT_USE_SIMPLIFIED', False):
            return None
            
        if not self._redis:
            try:
                self._redis = aioredis.from_url(
                    self.redis_url,
                    decode_responses=True,
                    socket_timeout=1
                )
                await self._redis.ping()
            except Exception:
                self._redis = None
        return self._redis
    
    def _get_tier(self, request: Request) -> str:
        """Determine rate limit tier from request context."""
        # Check for admin role
        user = getattr(request.state, 'user', None)
        if user and getattr(user, 'role', None) == 'admin':
            return 'admin'
        
        # Check for premium subscription
        if user and getattr(user, 'is_premium', False):
            return 'premium'
        
        # Authenticated user
        if user:
            return 'authenticated'
        
        return 'anonymous'
    
    def _get_limits(self, endpoint: str, tier: str) -> tuple:
        """Get rate limits for endpoint/tier combination."""
        # Check endpoint-specific override first
        if endpoint in self.ENDPOINT_OVERRIDES:
            return self.ENDPOINT_OVERRIDES[endpoint]
        # Fall back to tier defaults
        return self.TIERS.get(tier, self.TIERS['anonymous'])
    
    async def is_allowed(
        self,
        key: str,
        limit: int,
        window: int
    ) -> tuple[bool, dict]:
        """
        Check if request is allowed under rate limit.
        
        Uses sliding window algorithm with Redis.
        
        Returns:
            Tuple of (allowed, metadata)
        """
        if not self.enabled:
            return True, {"remaining": limit, "reset": time.time() + window}
        
        redis = await self._get_redis()
        if not redis:
            # Fail open if Redis unavailable
            logger.warning("Rate limiter Redis unavailable, allowing request")
            return True, {"remaining": limit, "reset": time.time() + window}
        
        try:
            now = time.time()
            pipe = redis.pipeline(transaction=True)
            
            # Remove old entries outside window
            pipe.zremrangebyscore(key, 0, now - window)
            
            # Count current window requests
            pipe.zcard(key)
            
            # Add this request
            pipe.zadd(key, {str(now): now})
            
            # Set expiry on key
            pipe.expire(key, window)
            
            results = await pipe.execute()
            current_count = results[1]
            
            remaining = max(0, limit - current_count)
            reset_time = now + window
            
            if current_count >= limit:
                return False, {
                    "remaining": 0,
                    "reset": reset_time,
                    "current": current_count,
                    "limit": limit
                }
            
            return True, {
                "remaining": remaining,
                "reset": reset_time,
                "current": current_count,
                "limit": limit
            }
            
        except Exception as e:
            logger.error(f"Rate limit check error: {e}")
            # Fail open on errors
            return True, {"remaining": limit, "reset": time.time() + window}
    
    async def check_rate_limit(
        self,
        request: Request
    ) -> tuple[bool, dict]:
        """
        Main entry point for rate limiting.
        
        Returns:
            Tuple of (allowed, headers_to_set)
        """
        # Get client identifier
        client_id = self._get_client_id(request)
        endpoint = request.url.path
        tier = self._get_tier(request)
        limit, window = self._get_limits(endpoint, tier)
        
        # Build Redis key
        key = f"ratelimit:{client_id}:{endpoint}"
        
        allowed, meta = await self.is_allowed(key, limit, window)
        
        headers = {
            "X-RateLimit-Limit": str(limit),
            "X-RateLimit-Remaining": str(meta["remaining"]),
            "X-RateLimit-Reset": str(int(meta["reset"])),
            "X-RateLimit-Tier": tier
        }
        
        return allowed, headers
    
    def _get_client_id(self, request: Request) -> str:
        """Extract client identifier from request."""
        # Try user ID first
        user = getattr(request.state, 'user', None)
        if user and hasattr(user, 'id'):
            return f"user:{user.id}"
        
        # Fall back to IP address
        forwarded = request.headers.get("X-Forwarded-For")
        if forwarded:
            return f"ip:{forwarded.split(',')[0].strip()}"
        
        return f"ip:{request.client.host if request.client else 'unknown'}"


class RateLimitMiddleware(BaseHTTPMiddleware):
    """FastAPI middleware for automatic rate limiting."""
    
    def __init__(self, app, limiter: RateLimiter):
        super().__init__(app)
        self.limiter = limiter
    
    async def dispatch(self, request: Request, call_next):
        # Skip rate limiting for health checks and static assets
        if request.url.path in ["/health", "/ready", "/metrics"]:
            return await call_next(request)
        
        # Check rate limit
        allowed, headers = await self.limiter.check_rate_limit(request)
        
        response = await call_next(request)
        
        # Add rate limit headers to response
        for key, value in headers.items():
            response.headers[key] = value
        
        if not allowed:
            return JSONResponse(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                content={
                    "error": "Rate limit exceeded",
                    "message": "Too many requests. Please try again later.",
                    "retry_after": int(headers.get("X-RateLimit-Reset", 60))
                },
                headers=headers
            )
        
        return response


from fastapi.responses import JSONResponse


def rate_limit(limit: int = 60, window: int = 60):
    """
    Decorator for per-endpoint rate limiting.
    
    Usage:
        @app.post("/api/chat")
        @rate_limit(limit=30, window=60)
        async def chat_endpoint(request: Request):
            ...
    """
    def decorator(func):
        @wraps(func)
        async def wrapped_func(request: Request, *args, **kwargs):
            limiter = RateLimiter()
            client_id = limiter._get_client_id(request)
            key = f"decorator:{client_id}:{func.__name__}"
            
            allowed, meta = await limiter.is_allowed(key, limit, window)
            
            if not allowed:
                raise HTTPException(
                    status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                    detail={
                        "error": "Rate limit exceeded",
                        "retry_after": int(meta["reset"])
                    }
                )
            
            response = await func(request, *args, **kwargs)
            return response
        return wrapped_func
    return decorator


