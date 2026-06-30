from __future__ import annotations

import json
import re
import time

from fastapi.responses import JSONResponse
from loguru import logger


class HoneypotMiddleware:
    def __init__(self, app):
        self.app = app
        # পরিচিত অ্যাটাক সিগনেচার
        self.attack_signatures = [
            re.compile(r"(?i)(ignore previous instructions|system prompt)"),
            re.compile(r"(?i)(union select|1=1|--|drop table)"),
            re.compile(r"(?i)(<script>|javascript:)"),
        ]

    async def __call__(self, scope, receive, send):
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        import os
        import sys

        env = os.getenv("ENV", "").lower()
        if env == "test" or (
            "pytest" in sys.modules and env not in {"production", "prod"}
        ):
            await self.app(scope, receive, send)
            return

        client = scope.get("client")
        hacker_ip = client[0] if client else "unknown"

        # Check if the IP is already dynamically blocked by the RulesMutator
        from core.rules_mutator import RulesMutator

        if RulesMutator().is_ip_blocked(hacker_ip):
            logger.warning(
                f"Honeypot: Blocked request from blacklisted IP: {hacker_ip}"
            )
            response = JSONResponse(
                status_code=403,
                content={
                    "detail": "Forbidden: Access denied due to security policy violations."
                },
            )
            await response(scope, receive, send)
            return

        # রিকোয়েস্ট বডি রিড করা (Safely inside ASGI)
        body_bytes = b""
        messages = []

        if scope.get("method") in ("POST", "PUT", "PATCH"):
            more_body = True
            try:
                while more_body:
                    message = await receive()
                    messages.append(message)
                    body_bytes += message.get("body", b"")
                    more_body = message.get("more_body", False)
            except Exception:
                pass

        # Reconstruct receive channel for downstream handlers
        async def new_receive():
            if messages:
                return messages.pop(0)
            return {"type": "http.disconnect"}

        body_str = body_bytes.decode("utf-8", errors="ignore")
        query_str = scope.get("query_string", b"").decode("utf-8", errors="ignore")

        # Check query string and body for malicious signatures
        is_malicious = any(
            sig.search(body_str) or sig.search(query_str)
            for sig in self.attack_signatures
        )

        if is_malicious:
            # 🚨 হ্যাকার ডিটেক্টেড! তাকে ব্লক না করে Honeypot-এ রাউট করা হচ্ছে
            logger.warning(
                f"🕷️ Malicious payload from {hacker_ip}. Routing to Honeypot..."
            )

            # ডেটাবেসে হ্যাকারের প্যাটার্ন স্টাডি করার জন্য সেভ করা (Async Task)
            self._log_threat_intelligence(
                hacker_ip, body_str or query_str, scope.get("path", "")
            )

            # Increment threat level & block if threshold reached
            import core.app as app_mod

            if (
                hasattr(app_mod, "redis_queue")
                and app_mod.redis_queue
                and app_mod.redis_queue.configured
            ):
                try:
                    # Log attacker payload
                    log_entry = {
                        "ip": hacker_ip,
                        "url": f"{scope.get('scheme', 'http')}://{hacker_ip}{scope.get('path', '')}",
                        "method": scope.get("method", "GET"),
                        "timestamp": time.time(),
                    }
                    app_mod.redis_queue.set(
                        f"honeypot_attacker:{hacker_ip}:{int(time.time())}",
                        json.dumps(log_entry),
                        ex=86400,
                    )

                    threat_key = f"threat_level:{hacker_ip}"
                    hits = app_mod.redis_queue.incr(threat_key)
                    if hits == 1:
                        app_mod.redis_queue.expire(threat_key, 300)
                    elif hits and hits >= 3:
                        # Dynamically block IP using RulesMutator
                        RulesMutator().block_ip(
                            hacker_ip, reason="honeypot_threat_threshold_exceeded"
                        )
                except Exception as e:
                    logger.error(f"Redis operation failed in HoneypotMiddleware: {e}")

            # হ্যাকারকে ফেক সাকসেস রেসপন্স দেওয়া
            response = JSONResponse(
                status_code=200,
                content={
                    "status": "success",
                    "data": {
                        "role": "admin",
                        "access_granted": True,
                        "flag": "SupremeAI_Shadow_Env",
                    },
                },
                headers={"X-Server": "SupremeAI"},
            )
            await response(scope, new_receive, send)
            return

        # নরমাল ইউজার হলে রেগুলার ফ্লো
        if scope.get("method") in ("POST", "PUT", "PATCH"):
            await self.app(scope, new_receive, send)
        else:
            await self.app(scope, receive, send)

    def _log_threat_intelligence(self, ip: str, payload: str, endpoint: str):
        logger.info(f"Threat studied and recorded for IP {ip}")
        try:
            import asyncio

            loop = asyncio.get_running_loop()
            loop.run_in_executor(
                None, self._persist_threat_intel, ip, payload, endpoint
            )
        except RuntimeError:
            self._persist_threat_intel(ip, payload, endpoint)
        except Exception as exc:
            logger.debug(f"Failed to schedule threat intel persistence: {exc}")

    def _persist_threat_intel(self, ip: str, payload: str, endpoint: str):
        try:
            import firebase_admin
            from firebase_admin import firestore

            if not firebase_admin._apps:
                firebase_admin.initialize_app()
            db = firestore.client()
            db.collection("threat_intel").add(
                {
                    "ip": ip,
                    "payload": payload[:1000],
                    "endpoint": endpoint,
                    "timestamp": time.time(),
                }
            )
        except Exception as exc:
            logger.debug(f"Failed to persist threat intel to Firestore: {exc}")
