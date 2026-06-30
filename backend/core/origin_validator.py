# বাংলা কমেন্ট: সুপ্রিম-এআই এর ট্রাস্টেড অরিজিন ভ্যালিডেশন মিডলওয়্যার।
# এটি ওয়াইল্ডকার্ড CORS বাইপাস রোধ করে এবং শুধুমাত্র অনুমোদিত ডোমেইন থেকে এপিআই অ্যাক্সেস নিশ্চিত করে।

from fastapi import Request, HTTPException, status
from starlette.middleware.base import BaseHTTPMiddleware
from core.config import settings
from core.logging_config import logger

class TrustedOriginMiddleware(BaseHTTPMiddleware):
    def __init__(self, app):
        super().__init__(app)
        # বাংলা কমেন্ট: শুধুমাত্র আমাদের অনুমোদিত প্রোডাকশন ডোমেইন এবং ডেভেলপমেন্ট লোকালহোস্টের হোয়াইটলিস্ট
        self.allowed_origins = {
            "https://supremeai.njel.com.bd",
            "https://studio.njel.com.bd",
            "http://localhost:5173",  # Web Chat Client পোর্ট
            "http://localhost:5174"   # Studio Client পোর্ট
        }

    async def dispatch(self, request: Request, call_next):
        # বাংলা কমেন্ট: এপিআই রিকোয়েস্টের Origin এবং Host হেডার রিড করা হচ্ছে।
        origin = request.headers.get("Origin")
        
        # যদি রিকোয়েস্টে অরিজিন হেডার থাকে (যেমন ব্রাউজার বেসড রিকোয়েস্ট), তবে সেটি হোয়াইটলিস্টে থাকতে হবে
        if origin:
            if origin not in self.allowed_origins:
                logger.critical(f"🔥 CSRF ALERT: Unauthorized Origin Access Blocked! Malicious Origin: {origin} from IP: {request.client.host}")
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail="Cross-Origin Request Blocked. Device identity unauthorized."
                )
                
        # বাংলা কমেন্ট: যদি অরিজিন না থাকে (যেমন ডিরেক্ট কার্ল বা এক্সটেনশন রিকোয়েস্ট), তবে হোস্ট হেডার ভ্যালিডেশন
        host = request.headers.get("Host")
        if host and "localhost" not in host and "njel.com.bd" not in host:
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
