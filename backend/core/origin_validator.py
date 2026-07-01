# বাংলা কমেন্ট: সুপ্রিম-এআই এর ট্রাস্টেড অরিজিন ভ্যালিডেশন মিডলওয়্যার।
# এটি ওয়াইল্ডকার্ড CORS বাইপাস রোধ করে এবং শুধুমাত্র অনুমোদিত ডোমেইন থেকে এপিআই অ্যাক্সেস নিশ্চিত করে।

from fastapi import HTTPException
from fastapi import Request
from fastapi import status
from starlette.middleware.base import BaseHTTPMiddleware

from core.config import settings
from core.logging_config import logger


class TrustedOriginMiddleware(BaseHTTPMiddleware):
    def __init__(self, app):
        super().__init__(app)
        self.allowed_origins = set(settings.cors_origins)

    async def dispatch(self, request: Request, call_next):
        # বাংলা মন্তব্য: এপিআই রিকোয়েস্টের Origin এবং Host হেডার রিড করা হচ্ছে।
        origin = request.headers.get("Origin")
        
        # যদি রিকোয়েস্টে অরিজিন হেডার থাকে (যেমন ব্রাউজার বেসড রিকোয়েস্ট), তবে সেটি হোয়াইটলিস্টে থাকতে হবে
        if origin and origin not in self.allowed_origins:
                client_ip = request.client.host if request.client else "unknown"
                logger.critical(f"🔥 CSRF ALERT: Unauthorized Origin Access Blocked! Malicious Origin: {origin} from IP: {client_ip}")
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail="Cross-Origin Request Blocked. Device identity unauthorized."
                )
                
        # বাংলা মন্তব্য: যদি অরিজিন না থাকে (যেমন ডিরেক্ট কার্ল বা এক্সটেনশন রিকোয়েস্ট), তবে হোস্ট হেডার ভ্যালিডেশন
        # বাংলা মন্তব্য: ডাইনামিকলি সেটিংস থেকে অনুমোদিত হোস্ট চেক করা হচ্ছে (হার্ডকোড মুক্ত)
        is_allowed = any(allowed_host in host for allowed_host in settings.allowed_hosts) if host else True
        if host and not is_allowed:
            logger.critical(f"🚨 Security Intrusion: Host Header Tampering Detected -> {host}")
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Host verification failure."
            )

        # বাংলা কমেন্ট: ভ্যালিডেশন সাকসেসফুল হলে রিকোয়েস্ট পরবর্তী প্রসেসে পাস হবে
        response = await call_next(request)
        
        # জিরো-গ্যাপ CORS হেডার ইনজেকশন (ওয়াইল্ডকার্ড মুক্ত)
        if origin and origin in self.allowed_origins:
            response.headers["Access-Control-Allow-Origin"] = origin
            response.headers["Access-Control-Allow-Credentials"] = "true"
            response.headers["Access-Control-Allow-Methods"] = "GET, POST, PUT, DELETE, OPTIONS"
            response.headers["Access-Control-Allow-Headers"] = "Content-Type, Authorization, X-Requested-With"
            
        return response
