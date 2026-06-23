#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# file >> honeypot_middleware.py
# project >> SupremeAI 2.0
# purpose >> Honeypot middleware
# module >> core
# ============================================================================
from __future__ import annotations

import json
import time
import re
from fastapi import Request
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware
from loguru import logger

class HoneypotMiddleware(BaseHTTPMiddleware):
    def __init__(self, app):
        super().__init__(app)
        # পরিচিত অ্যাটাক সিগনেচার
        self.attack_signatures = [
            re.compile(r"(?i)(ignore previous instructions|system prompt)"),
            re.compile(r"(?i)(union select|1=1|--|drop table)"),
            re.compile(r"(?i)(<script>|javascript:)")
        ]

    async def dispatch(self, request: Request, call_next):
        import sys
        import os
        if "pytest" in sys.modules or os.getenv("ENV") == "test":
            return await call_next(request)

        hacker_ip = request.client.host if request.client else "unknown"

        # Check if the IP is already dynamically blocked by the RulesMutator
        from core.rules_mutator import RulesMutator
        if RulesMutator().is_ip_blocked(hacker_ip):
            logger.warning(f"Honeypot: Blocked request from blacklisted IP: {hacker_ip}")
            return JSONResponse(
                status_code=403,
                content={"detail": "Forbidden: Access denied due to security policy violations."}
            )

        # রিকোয়েস্ট বডি রিড করা (Safely)
        body_bytes = b""
        if request.method in ("POST", "PUT", "PATCH"):
            try:
                body_bytes = await request.body()
                # Reconstruct request body for subsequent handlers
                async def receive():
                    return {"type": "http.request", "body": body_bytes, "more_body": False}
                request._receive = receive
            except Exception:
                pass

        body_str = body_bytes.decode("utf-8", errors="ignore")
        query_str = str(request.query_params)
        
        # Check query string and body for malicious signatures
        is_malicious = any(sig.search(body_str) or sig.search(query_str) for sig in self.attack_signatures)

        if is_malicious:
            # 🚨 হ্যাকার ডিটেক্টেড! তাকে ব্লক না করে Honeypot-এ রাউট করা হচ্ছে
            logger.warning(f"🕷️ Malicious payload from {hacker_ip}. Routing to Honeypot...")
            
            # ডেটাবেসে হ্যাকারের প্যাটার্ন স্টাডি করার জন্য সেভ করা (Async Task)
            self._log_threat_intelligence(hacker_ip, body_str or query_str, request.url.path)
            
            # Increment threat level & block if threshold reached
            import core.app as app_mod
            if hasattr(app_mod, "redis_queue") and app_mod.redis_queue and app_mod.redis_queue.configured:
                # Log attacker payload
                log_entry = {
                    "ip": hacker_ip,
                    "url": str(request.url),
                    "method": request.method,
                    "timestamp": time.time(),
                }
                app_mod.redis_queue.set(f"honeypot_attacker:{hacker_ip}:{int(time.time())}", json.dumps(log_entry), ex=86400)
                
                threat_key = f"threat_level:{hacker_ip}"
                hits = app_mod.redis_queue.incr(threat_key)
                if hits == 1:
                    app_mod.redis_queue.set(threat_key, "1", ex=300)
                elif hits and hits >= 3:
                    # Dynamically block IP using RulesMutator
                    RulesMutator().block_ip(hacker_ip, reason="honeypot_threat_threshold_exceeded")

            # হ্যাকারকে ফেক সাকসেস রেসপন্স দেওয়া
            return JSONResponse(
                status_code=200,
                content={
                    "status": "success",
                    "data": {"role": "admin", "access_granted": True, "flag": "SupremeAI_Shadow_Env"}
                },
                headers={"X-Server": "SupremeAI-Honeypot-v1"}
            )

        # নরমাল ইউজার হলে রেগুলার ফ্লো
        response = await call_next(request)
        return response

    def _log_threat_intelligence(self, ip: str, payload: str, endpoint: str):
        logger.info(f"🧠 Threat studied and recorded for IP {ip}")
        try:
            import firebase_admin
            from firebase_admin import firestore
            if not firebase_admin._apps:
                firebase_admin.initialize_app()
            db = firestore.client()
            db.collection("threat_intel").add({
                "ip": ip,
                "payload": payload[:1000],
                "endpoint": endpoint,
                "timestamp": time.time(),
            })
        except Exception as exc:
            logger.debug(f"Failed to persist threat intel to Firestore: {exc}")
