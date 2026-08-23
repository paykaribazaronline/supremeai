"""
SuperAI Security Middleware
============================
Security headers, request validation, and attack prevention.

Author: SuperAI Transformation Patch
Version: 1.0.0
"""

import re
import logging
import time
from typing import Callable, Optional
from urllib.parse import urlparse

from fastapi import Request, Response
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint

# Configure logging
logger = logging.getLogger(__name__)


# Security header configuration
SECURITY_HEADERS = {
    "X-Content-Type-Options": "nosniff",
    "X-Frame-Options": "DENY",
    "X-XSS-Protection": "1; mode=block",
    "Strict-Transport-Security": "max-age=31536000; includeSubDomains",
    "Referrer-Policy": "strict-origin-when-cross-origin",
    "Permissions-Policy": "camera=(), microphone=(), geolocation=()",
    "Content-Security-Policy": (
        "default-src 'self'; "
        "script-src 'self' 'unsafe-inline'; "
        "style-src 'self' 'unsafe-inline'; "
        "img-src 'self' data: https:; "
        "connect-src 'self' https://*.supabase.co wss://*.supabase.co;"
    ),
}

# Patterns for SQL injection detection
SQL_INJECTION_PATTERNS = [
    r"(\%27)|(\')|(\-\-)|(\%23)|(#)",  # Basic SQL meta-characters
    r"((\%3D)|(=))[^\n]*((\%27)|(\')|(\-\-)|(\%3B)|(;))",  # SQL injection basics
    r"\w*((\%27)|(\'))((\%6F)|o|(\%4F))((\%72)|r|(\%52))",  # SQL 'OR' injection
    r"((\%27)|(\'))union",  # UNION injection
    r"exec(\s|\+)+(s|x)p\w+",  # SQL Server exec
]

# XSS detection patterns
XSS_PATTERNS = [
    r"<script[^>]*>.*?</script>",
    r"javascript\s*:",
    r"on\w+\s*=",
    r"<iframe",
    r"<object",
    r"<embed",
]

# Additional Dangerous Patterns
DANGEROUS_PATTERNS = [
    r"\.\./",             # Path traversal
    r"\$\{",               # Template injection
]


class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    """Add security headers to all responses."""
    
    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        response = await call_next(request)
        
        # Add security headers
        for header, value in SECURITY_HEADERS.items():
            response.headers[header] = value
        
        # Remove server signature
        del_response_header(response.headers, "Server")
        del_response_header(response.headers, "X-Powered-By")
        
        return response


class RequestValidationMiddleware(BaseHTTPMiddleware):
    """Validate requests for common attack patterns."""
    
    # Maximum request sizes (bytes)
    MAX_BODY_SIZE = 10 * 1024 * 1024  # 10MB
    MAX_QUERY_LENGTH = 2048
    MAX_HEADER_SIZE = 8192
    
    # Default Rate limiting per IP
    REQUEST_LOG: dict = {}
    RATE_LIMIT = 100  # Requests per minute
    RATE_WINDOW = 60  # Seconds
    
    # Path-specific rate limits (critical paths)
    SIMPLE_RATE_LIMITS = {
        "/api/v1/auth/login": {"requests": 5, "window": 600},      # 5 per 10 min
        "/api/v1/auth/register": {"requests": 3, "window": 3600},    # 3 per hour
        "/api/v1/scraper/scrape": {"requests": 20, "window": 3600}, # 20 per hour
        "/api/v1/kaggle/submit": {"requests": 10, "window": 3600},  # 10 per hour
    }
    
    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        client_ip = self._get_client_ip(request)
        
        # Check request size
        content_length = request.headers.get("content-length")
        if content_length and int(content_length) > self.MAX_BODY_SIZE:
            logger.warning(f"Oversized request from {client_ip}: {content_length} bytes")
            return Response(
                status_code=413,
                content=b'{"error": "Request entity too large"}',
                media_type="application/json"
            )
        
        # Check query string length
        if len(str(request.query_params)) > self.MAX_QUERY_LENGTH:
            return Response(
                status_code=414,
                content=b'{"error": "URI too long"}',
                media_type="application/json"
            )
        
        # Simple rate limiting (backup for Redis-based limiter)
        if not await self._check_rate_limit(client_ip, request.url.path):
            return Response(
                status_code=429,
                content=b'{"error": "Too many requests"}',
                media_type="application/json"
            )
        
        # Scan for SQL injection in query params
        query_string = str(request.query_params)
        if self._detect_sql_injection(query_string):
            logger.warning(f"SQL injection attempt from {client_ip}")
            return Response(
                status_code=400,
                content=b'{"error": "Invalid request"}',
                media_type="application/json"
            )
        
        # Scan for XSS in query params
        if self._detect_xss(query_string):
            logger.warning(f"XSS attempt from {client_ip}")
            return Response(
                status_code=400,
                content=b'{"error": "Invalid request"}',
                media_type="application/json"
            )
            
        # Scan for other dangerous patterns
        if self._detect_dangerous(query_string):
            logger.warning(f"Dangerous pattern attempt from {client_ip}")
            return Response(
                status_code=400,
                content=b'{"error": "Invalid request"}',
                media_type="application/json"
            )
        
        return await call_next(request)
    
    def _get_client_ip(self, request: Request) -> str:
        forwarded = request.headers.get("X-Forwarded-For")
        if forwarded:
            return forwarded.split(",")[0].strip()
        return request.client.host if request.client else "unknown"
    
    async def _check_rate_limit(self, client_ip: str, path: str) -> bool:
        """Simple in-memory rate limiting with path specificity."""
        now = time.time()
        
        # Determine applicable limits
        limit = self.RATE_LIMIT
        window = self.RATE_WINDOW
        
        for critical_path, config in self.SIMPLE_RATE_LIMITS.items():
            if path.startswith(critical_path):
                limit = config["requests"]
                window = config["window"]
                break
                
        # Use a combination of IP and path prefix for tracking critical paths
        # For general requests, just use IP to group them
        tracking_key = f"{client_ip}:{path}" if limit != self.RATE_LIMIT else client_ip
        
        # Clean old entries across the entire log periodically (simplified)
        self.REQUEST_LOG = {
            k: timestamps
            for k, timestamps in self.REQUEST_LOG.items()
            if any(ts > now - 3600 for ts in timestamps) # Keep at most 1 hour history
        }
        
        # Check current IP/Key
        if tracking_key not in self.REQUEST_LOG:
            self.REQUEST_LOG[tracking_key] = []
        
        recent_requests = [ts for ts in self.REQUEST_LOG[tracking_key] if ts > now - window]
        
        if len(recent_requests) >= limit:
            return False
        
        self.REQUEST_LOG[tracking_key] = recent_requests + [now]
        return True
    
    @staticmethod
    def _detect_sql_injection(input_string: str) -> bool:
        """Detect potential SQL injection attempts."""
        for pattern in SQL_INJECTION_PATTERNS:
            if re.search(pattern, input_string, re.IGNORECASE):
                return True
        return False
    
    @staticmethod
    def _detect_xss(input_string: str) -> bool:
        """Detect potential XSS attempts."""
        for pattern in XSS_PATTERNS:
            if re.search(pattern, input_string, re.IGNORECASE):
                return True
        return False
        
    @staticmethod
    def _detect_dangerous(input_string: str) -> bool:
        """Detect other dangerous patterns (Path traversal, Template injection)."""
        for pattern in DANGEROUS_PATTERNS:
            if re.search(pattern, input_string, re.IGNORECASE):
                return True
        return False


def del_response_header(headers, key: str):
    """Safely delete a response header."""
    try:
        del headers[key]
    except KeyError:
        pass


