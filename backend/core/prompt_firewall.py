#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# file >> prompt_firewall.py
# project >> SupremeAI 2.0
# purpose >> Prompt firewall
# module >> core
# ============================================================================
import os
import re
from typing import Optional, Dict, Any, List
from loguru import logger
import httpx

class PromptFirewall:
    def __init__(self):
        self.llama_guard_url = os.getenv("LLAMA_GUARD_URL", "")
        self.nemo_guardrails_enabled = os.getenv("NEMO_GUARDRAILS_ENABLED", "true").lower() == "true"
        self._local_patterns = self._load_local_patterns()
    
    def _load_local_patterns(self) -> List[Dict[str, Any]]:
        return [
            {"name": "prompt_injection", "patterns": [
                r"ignore\s+(previous|all)\s+(instructions|rules|prompt)",
                r"disregard\s+(previous|all)",
                r"you\s+are\s+now\s+(in\s+)?developer\s+mode",
                r"jailbreak",
                r"DAN\s+mode",
                r"unfiltered\s+mode",
            ]},
            {"name": "sensitive_extraction", "patterns": [
                r"(password|api[_\s-]?key|secret|token)\s*(=|:)?\s*['\"]?[a-zA-Z0-9\-_]{20,}['\"]?",
                r"(BEGIN|END)\s+(RSA|PGP|OPENSSH)\s+KEY",
                r"(ssh-rsa|ssh-ed25519)\s+[A-Za-z0-9+/=]+",
            ]},
            {"name": "malicious_code", "patterns": [
                r"(?i)(rm\s+-rf|/bin/sh|chmod\s+777)",
                r"(?i)(curl|wget)\s+.*\|\s*(bash|sh)",
                r"(?i)base64\s+-d.*\|\s*(bash|sh|python)",
            ]},
        ]
    
    def _check_local_patterns(self, prompt: str) -> Optional[str]:
        prompt_lower = prompt.lower()
        for category in self._local_patterns:
            for pattern in category["patterns"]:
                if re.search(pattern, prompt_lower, re.IGNORECASE):
                    return f"Blocked: {category['name']} pattern detected"
        return None
    
    async def scan_with_llama_guard(self, prompt: str) -> Optional[str]:
        if not self.llama_guard_url:
            return None
        
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.post(
                    f"{self.llama_guard_url}/classify",
                    json={"text": prompt},
                )
                if response.status_code == 200:
                    result = response.json()
                    if result.get("safety_category"):
                        return f"Blocked by Llama Guard: {result.get('safety_category')}"
        except Exception as e:
            logger.debug(f"Llama Guard scan failed (non-fatal): {e}")
        return None
    
    async def pre_flight_check(self, prompt: str) -> Dict[str, Any]:
        local_violation = self._check_local_patterns(prompt)
        if local_violation:
            logger.warning(f"Local firewall block: {local_violation}")
            return {"allowed": False, "reason": local_violation, "provider": "local"}
        
        guard_violation = await self.scan_with_llama_guard(prompt)
        if guard_violation:
            logger.warning(f"Llama Guard block: {guard_violation}")
            return {"allowed": False, "reason": guard_violation, "provider": "llama_guard"}
        
        return {"allowed": True, "reason": "prompt_approved", "provider": "firewall"}
    
    async def classify_intent(self, prompt: str) -> Dict[str, Any]:
        complexity = "simple"
        requires_coding = any(kw in prompt.lower() for kw in ["code", "function", "class", "debug", "refactor", "algorithm"])
        requires_reasoning = any(kw in prompt.lower() for kw in ["reason", "logic", "analyze", "math", "calculate"])
        requires_vision = any(kw in prompt.lower() for kw in ["image", "photo", "picture", "visual", "ocr"])
        
        if requires_coding:
            complexity = "coding"
        elif requires_reasoning:
            complexity = "reasoning"
        elif requires_vision:
            complexity = "vision"
        
        return {
            "intent": complexity,
            "requires_expensive_model": complexity in ["coding", "reasoning"],
            "confidence": 0.85,
        }

firewall = PromptFirewall()

async def pre_flight_scan(prompt: str) -> Dict[str, Any]:
    return await firewall.pre_flight_check(prompt)

async def classify_intent(prompt: str) -> Dict[str, Any]:
    return await firewall.classify_intent(prompt)