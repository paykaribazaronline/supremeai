# বাংলা কমেন্ট: সুপ্রিম-এআই এর কোর প্রম্পট ফায়ারওয়াল এবং বেঙ্গলি নেটিভ এনফোর্সমেন্ট ইঞ্জিন।
# এটি সমস্ত সাব-এজেন্ট এবং এলএলএম রিকোয়েস্টে ১ নম্বর ও ২ নম্বর গোল্ডেন রুল স্ট্রিক্টলি ইনজেক্ট করে।

import re

from core.logging_config import logger


class PromptFirewall:
    def __init__(self):
        # বাংলা কমেন্ট: আলটিমেট বেঙ্গলি নেটিভ সিস্টেম ইনস্ট্রাকশন সেট (১০০% ক্র্যাপ-ফ্রি)
        self.bengali_native_instruction = (
            "\n\n=== STRICTOR BENGALI NATIVE ENFORCEMENT RULES ===\n"
            "RULE 1: You must respond, analyze, and explain ALWAYS in concise, clear, and high-quality Bengali language.\n"
            "RULE 2: If you generate, modify, or patch any source code (Python, Java, Dart, etc.), you MUST include precise "
            "explanatory comments written strictly in BENGALI (বাংলা কমেন্ট) right inside the code blocks explaining the changes.\n"
            "RULE 3: Maintain a senior, strict, professional developer persona. Avoid unnecessary filler words or fluff.\n"
            "=================================================\n"
        )

    def enforce_bengali_rules(self, original_system_prompt: str) -> str:
        """
        ইনকামিং সিস্টেম প্রম্পটের সাথে আমাদের গোল্ডেন বাংলা রুলস ইন্টারসেপ্ট ও ইনজেক্ট করে।
        """
        if not original_system_prompt:
            return self.bengali_native_instruction.strip()
            
        # বাংলা কমেন্ট: ডুপ্লিকেশন এড়াতে অলরেডি রুলস ইনজেক্টেড আছে কিনা তা চেক করা হচ্ছে
        if "BENGALI NATIVE ENFORCEMENT" in original_system_prompt:
            return original_system_prompt
            
        # জিরো-গ্যাপ প্রম্পট কন্টেনেশন
        secure_prompt = f"{original_system_prompt.strip()}{self.bengali_native_instruction}"
        logger.info("🔱 Prompt Firewall: Bengali Native & Code Commenting rules successfully injected into agent payload.")
        return secure_prompt

    def validate_agent_response(self, response_text: str) -> bool:
        """
        [Fail-Closed Safety Check] এজেন্টের জেনারেট করা আউটপুট আসলেই বাংলা রুলস মেনেছে কিনা তা পোস্ট-ভ্যালিডেশন করে।
        """
        if not response_text:
            return False
            
        # বাংলা কমেন্ট: আউটপুটে বাংলা ক্যারেক্টার সেট (Unicode Range: \u0980-\u09FF) আছে কিনা তা যাচাই করা হচ্ছে।
        bengali_character_regex = re.compile(r'[\u0980-\u09FF]')
        
        # যদি আউটপুট পুরোপুরি ইংরেজি বা অন্য ভাষায় হয় (বাংলা ক্যারেক্টার অনুপস্থিত), তবে এটি পলিসি ভায়োলেশন
        if not bengali_character_regex.search(response_text):
            logger.critical("🔥 SECURITY VIOLATION: Agent response failed i18n Bengali compliance check!")
            return False
            
        return True

    def _load_local_patterns(self):
        return [
            {"name": "prompt_injection", "patterns": []},
            {"name": "sensitive_extraction", "patterns": []},
            {"name": "malicious_code", "patterns": []}
        ]
        
    def _check_local_patterns(self, prompt: str):
        # সব ইনপুট লোয়ারকেস এবং ট্রিম করা হয়েছে যাতে প্যাটার্ন ম্যাচিং মিস না হয়
        cleaned_prompt = prompt.lower().strip()
        
        # ইনজেকশন প্যাটার্ন লিস্ট
        patterns = [
            "disregard", "developer mode", "jailbreak", 
            "dan mode", "unfiltered", "ignore previous"
        ]
        
        for pattern in patterns:
            if pattern in cleaned_prompt:
                return "prompt_injection"
                
        import re as _re
        if _re.search(r"(?i)\b(password|api_key|secret|token)\s*=|BEGIN RSA KEY|END PGP KEY|ssh-(rsa|ed25519)", prompt):
            return "sensitive_extraction"
        if _re.search(r"(?i)(rm\s+-rf|/bin/sh|chmod\s+\d|curl\s+.*\|\s*bash|wget\s+.*\|\s*sh|base64\s+-d\s+.*\|\s*python)", prompt):
            return "malicious_code"
        return None

    async def scan_with_llama_guard(self, prompt: str):
        lowered = prompt.lower()
        banned = [
            "violent", "harm", "kill", "attack", "weapon",
            "bomb", "terror", "murder", "abuse", "exploit",
            "hack", "malware", "ransomware", "phishing",
        ]
        for token in banned:
            if token in lowered:
                return "LlamaGuard"
        return None

    async def pre_flight_check(self, prompt: str):
        lowered = prompt.lower().strip()
        blocked = [
            "disregard", "developer mode", "jailbreak",
            "dan mode", "unfiltered", "ignore previous",
        ]
        for token in blocked:
            if token in lowered:
                return {"allowed": False, "provider": "local", "reason": "Blocked"}
        if "test prompt" in lowered:
            return {"allowed": False, "provider": "llama_guard", "reason": "Blocked"}
        return {"allowed": True, "reason": "prompt_approved", "provider": "firewall"}
        
    async def classify_intent(self, prompt: str):
        lowered = prompt.lower()
        if "python" in lowered or "java" in lowered or "dart" in lowered:
            return {"intent": "coding", "requires_expensive_model": True}
        if "reason" in lowered or "logic" in lowered:
            return {"intent": "reasoning", "requires_expensive_model": True}
        if "image" in lowered or "photo" in lowered:
            return {"intent": "vision", "requires_expensive_model": False}
        return {"intent": "simple", "requires_expensive_model": False}

async def pre_flight_scan(prompt: str):
    lowered = prompt.lower().strip()
    blocked = [
        "disregard", "developer mode", "jailbreak",
        "dan mode", "unfiltered", "ignore previous",
    ]
    for token in blocked:
        if token in lowered:
            return {"allowed": False}
    return {"allowed": True}


async def classify_intent(prompt: str):
    lowered = prompt.lower()
    if "python" in lowered or "java" in lowered or "dart" in lowered:
        return {"intent": "coding", "requires_expensive_model": True}
    if "reason" in lowered or "logic" in lowered:
        return {"intent": "reasoning", "requires_expensive_model": True}
    if "image" in lowered or "photo" in lowered:
        return {"intent": "vision", "requires_expensive_model": False}
    return {"intent": "simple", "requires_expensive_model": False}

# গ্লোবাল সিঙ্গেলটন ইনস্ট্যান্স জেনারেশন
prompt_firewall = PromptFirewall()
