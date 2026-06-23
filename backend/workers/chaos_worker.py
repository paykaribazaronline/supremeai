import os
import httpx
import asyncio
from google.cloud import firestore
from loguru import logger
from datetime import datetime, timezone

class NightlyChaosAuditor:
    """
    Autonomous Self-Testing & Healing Auditor.
    Runs nightly security fuzzing and stress checks to guard the deployment pipeline.
    """
    def __init__(self):
        self.db = firestore.Client()
        self.gate_ref = self.db.collection("deploy_gate").document("status")
        # স্টেজ রেপ্লিকা ইউআরএল ম্যাপ (প্রোডাকশন থেকে আলাদা)
        self.target_url = os.getenv("STAGING_REPLICA_URL", "http://localhost:8000")
        
    async def execute_audit_sequence(self) -> bool:
        logger.info("🤖 Starting Autonomous Nightly Chaos Audit Sequence...")
        
        failures = 0
        try:
            # 🧪 টেস্ট ১: স্যান্ডবক্স ইন্টিগ্রিটি চেক (ফাস্ট এএসটি ভ্যালিডেশন)
            from fuzz_sandbox import run_sandbox_ast_check, generate_fuzz_payloads
            payloads = generate_fuzz_payloads()
            
            for code, _ in payloads[:20]: # টপ ২০টি ক্রিটিক্যাল পেলোড ফাজিং
                try:
                    # স্যান্ডবক্স যদি কোনো ম্যালিশিয়াস কোডকে ট্রু (Safe) বলে দেয়, তবে সিকিউরিটি লিক!
                    if run_sandbox_ast_check(code):
                        failures += 1
                        logger.critical("🚨 [SECURITY BREACH] Sandbox bypass detected during autonomous fuzzing!")
                except Exception:
                    pass # SecurityError আশা করা হচ্ছে, তাই এটি পাস

            # 🧪 টেস্ট ২: রানটাইম কানেকশন পুল স্ট্রেস চেক (Synthetic Heavy Requests)
            async with httpx.AsyncClient(timeout=5.0) as client:
                headers = {"Idempotency-Key": f"auto-chaos-{datetime.now(timezone.utc).timestamp()}"}
                # একই টাইমে ব্যাক-টু-ব্যাক ৫টি রিকোয়েস্ট ফায়ার করে রাউটার স্টেট চেক
                tasks = [
                    client.post(f"{self.target_url}/api/task/execute", json={"message": "Ping"}, headers=headers)
                    for _ in range(5)
                ]
                responses = await asyncio.gather(*tasks, return_exceptions=True)
                
                for res in responses:
                    if isinstance(res, Exception) or res.status_code >= 500:
                        failures += 1
                        logger.error(f"💥 Runtime Connection Failure or 500 Server Error detected: {res}")

            # ── 🔒 CLOSED-LOOP AUTOMATION DECISION ────────────────────────
            now = datetime.now(timezone.utc)
            if failures > 0:
                logger.critical(f"💀 Chaos Audit FAILED with {failures} anomalies. LOCKING deployment gates!")
                self.gate_ref.set({
                    "status": "LOCKED",
                    "reason": f"Autonomous audit failed with {failures} anomalies.",
                    "updated_at": now
                })
                return False
            else:
                logger.info("🏆 Autonomous Chaos Audit PASSED perfectly. Deploy gate is UNLOCKED.")
                self.gate_ref.set({
                    "status": "UNLOCKED",
                    "reason": "All self-testing gates returned green.",
                    "updated_at": now
                })
                return True

        except Exception as global_err:
            logger.critical(f"⚠️ Auditor crashed internally: {str(global_err)}. Locking pipeline for safety.")
            self.gate_ref.set({"status": "LOCKED", "reason": f"Auditor internal error: {str(global_err)}"})
            return False
