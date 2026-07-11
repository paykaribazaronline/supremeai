# 📄 ফাইল: scripts/health/auto_health_check.py

**প্রকার:** .py  
**সাইজ:** 5,581 বাইট  
**আপডেট:** 2026-07-11T13:13:34.416726

---

## কোড

```py
#!/usr/bin/env python
"""
auto_health_check.py
====================
Automated health checker for the SupremeAI 2.0 backend.

# এই স্ক্রিপ্টটি ব্যাকএন্ডের হেলথ, ডাটাবেস এবং অন্যান্য ডিপেন্ডেন্সি ঠিকভাবে কাজ করছে কিনা তা যাচাই করার জন্য তৈরি করা হয়েছে।
# (This script is designed to verify that the backend health, databases, and other dependencies are working correctly.)
"""

import os
import sys
import asyncio
import logging
import httpx
from datetime import datetime

# Add the backend directory to the path so we can import from core if needed
from pathlib import Path
backend_dir = Path(__file__).parent.parent.parent / "backend"
sys.path.insert(0, str(backend_dir))

# কনফিগারেশন - কোন কোন সার্ভিস চেক করা হবে
SERVICES = {
    "API_Gateway": os.getenv("BACKEND_URL", "http://localhost:8000") + "/api/v1/health",
    # 필요하면 Redis, NATS आदि এখানে যোগ করা যাবে
    # "Redis": "http://localhost:6379",
    # "NATS_JetStream": "http://localhost:8222/healthz", 
}
TIMEOUT = 5

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# টেলিগ্রাম বটের মাধ্যমে অ্যালার্ট পাঠানোর ফাংশন
async def send_telegram_alert(message: str):
    """টেলিগ্রামে অ্যালার্ট মেসেজ পাঠানোর ফাংশন।"""
    bot_token = os.getenv("TELEGRAM_BOT_TOKEN")
    chat_id = os.getenv("TELEGRAM_CHAT_ID")
    
    if not bot_token or not chat_id:
        logging.warning("Telegram Bot Token বা Chat ID সেট করা নেই। নোটিফিকেশন পাঠানো যাচ্ছে না।")
        return

    url = f"https://api.telegram.org/bot{bot_token}/sendMessage"
    payload = {
        "chat_id": chat_id,
        "text": f"🚨 *SupremeAI Health Alert* 🚨\n\n{message}",
        "parse_mode": "Markdown"
    }
    
    try:
        # httpx ব্যবহার করা হয়েছে যাতে async সাপোর্ট থাকে
        async with httpx.AsyncClient(timeout=TIMEOUT) as client:
            response = await client.post(url, json=payload)
            if response.status_code == 200:
                logging.info("✅ Telegram alert sent successfully.")
            else:
                logging.error(f"❌ Failed to send Telegram alert: {response.text}")
    except Exception as e:
        logging.error(f"❌ Error sending Telegram alert: {e}")

async def check_service(name: str, url: str):
    """নির্দিষ্ট সার্ভিসের হেলথ চেক করার ফাংশন।"""
    try:
        async with httpx.AsyncClient(timeout=TIMEOUT) as client:
            response = await client.get(url)
            if response.status_code == 200:
                return True, "OK"
            return False, f"Status Code: {response.status_code}"
    except Exception as e:
        return False, str(e)

async def check_database_connection() -> tuple[bool, str]:
    """প্রাইমারি ডাটাবেস কানেকশন যাচাই করে।"""
    try:
        from sqlalchemy import text
        from core.database import SessionLocal

        db = SessionLocal()
        try:
            db.execute(text("SELECT 1"))
            return True, "Database connection successful."
        except Exception as e:
            return False, f"Database query failed: {e}"
        finally:
            db.close()
    except ImportError as e:
        return False, f"Missing database modules: {e}"
    except Exception as e:
        return False, f"Database check error: {e}"

async def run_health_check():
    logging.info("🚀 Starting SupremeAI 2.0 Backend Health Check...")
    
    all_healthy = True
    alert_messages = []
    
    # 1. API Services চেক করা
    for name, url in SERVICES.items():
        success, message = await check_service(name, url)
        if success:
            logging.info(f"✅ {name} is healthy.")
        else:
            all_healthy = False
            error_msg = f"{name} is down! Error: {message}"
            logging.error(f"❌ {error_msg}")
            alert_messages.append(error_msg)
            
    # 2. Database চেক করা (CI এনভায়রনমেন্টে ডাটাবেস চেক স্কিপ করা যায়)
    if os.getenv("CI") != "true":
        db_success, db_message = await check_database_connection()
        if db_success:
            logging.info(f"✅ Database is healthy.")
        else:
            all_healthy = False
            error_msg = f"Database is down! Error: {db_message}"
            logging.error(f"❌ {error_msg}")
            alert_messages.append(error_msg)

    # 3. যদি কোনো সার্ভিস ডাউন থাকে, তাহলে টেলিগ্রামে নোটিফিকেশন পাঠানো
    if not all_healthy:
        combined_alert = "\n".join(alert_messages)
        await send_telegram_alert(combined_alert)
        sys.exit(1)
    else:
        logging.info("🎉 All backend health checks passed successfully!")
        sys.exit(0)

if __name__ == "__main__":
    asyncio.run(run_health_check())

```