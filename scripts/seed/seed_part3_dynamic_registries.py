#!/usr/bin/env python3
"""
Part 3 — Dynamic Registries & Logic Externalization
Seeds Firestore with dynamic data to replace hardcoded values in:
  • ChatProcessingService (Search URL Registry)
  • SecurityConfig (CSRF Ignored Paths)
  • StubLocalProvider (Offline Triggers)
  • CodeFlowHandler (Error Patterns)
"""

import sys
import os

sys.path.insert(0, os.path.dirname(__file__))

from seed_lib import _learning, run_part

# ── ১. সার্চ ইউআরএল রেজিস্ট্রি (ChatProcessingService-এর জন্য) ──────────────
SEARCH_REGISTRY = {
    "google_search": {
        "name": "Google",
        "url_template": "https://www.google.com/search?q={query}",
        "priority": 99, # ডিফল্ট বা কম অগ্রাধিকার
        "category": "GENERAL",
        "keywords": ["google", "search", "web", "internet", "find", "look up"]
    },
    "stackoverflow": {
        "name": "Stack Overflow",
        "url_template": "https://stackoverflow.com/search?q={query}",
        "priority": 20,
        "category": "CODING",
        "keywords": ["stackoverflow", "code", "programming", "error", "bug", "solution", "java", "python", "javascript", "spring boot", "react", "flutter"]
    },
    "github_code": {
        "name": "GitHub Code",
        "url_template": "https://github.com/search?q={query}&type=code",
        "priority": 30,
        "category": "RESOURCES",
        "keywords": ["github", "repo", "repository", "source code", "project", "git"]
    }
}

# ── ২. সিকিউরিটি কনফিগারেশন (SecurityConfig-এর জন্য) ────────────────────────
SECURITY_CONFIGS = {
    "csrf_ignored_paths": {
        "paths": [
            "/api/auth/**",
            "/api/chat/**",
            "/api/admin/providers/**",
            "/api/public/**"
        ],
        "description": "এই পাথগুলো সিএসআরএফ প্রোটেকশন থেকে বাদ দেওয়া হয়েছে"
    }
}

# ── ৩. অফলাইন ট্রিগার এবং প্যাটার্ন (StubLocalProvider-এর জন্য) ──────────────
OFFLINE_PATTERNS = {
    "time_query": _learning(
        type_="PATTERN",
        category="LOCAL_RESPONSE_TRIGGER",
        content=r"\b(সময়|time|ঘড়ি|বাজে)\b",
        solutions=["বর্তমান সময় প্রদান করুন"],
        severity="LOW",
        context={"regex": True}
    ),
    "identity_query": _learning(
        type_="PATTERN",
        category="LOCAL_RESPONSE_TRIGGER",
        content=r"\b(কে তুমি|who are you|তোমার নাম)\b",
        solutions=["সুপ্রিমএআই অ্যাসিস্ট্যান্ট হিসেবে পরিচয় দিন"],
        severity="LOW",
        context={"regex": True}
    )
}

# ── ৪. ডাইনামিক এরর প্যাটার্ন (CodeFlowHandler-এর জন্য) ────────────────────────
ERROR_PATTERNS = {
    "java_npe": {
        "pattern": r"java\.lang\.NullPointerException",
        "language": "Java",
        "fix_suggestion": "অবজেক্টটি নাল কিনা চেক করুন অথবা Optional ব্যবহার করুন।"
    },
    "python_indentation": {
        "pattern": r"IndentationError",
        "language": "Python",
        "fix_suggestion": "ইন্ডেন্টেশন বা স্পেসগুলো চেক করুন।"
    },
    "js_undefined": {
        "pattern": r"Cannot read properties of undefined",
        "language": "JavaScript",
        "fix_suggestion": "ভ্যারিয়েবলটি ডিফাইন করা আছে কিনা নিশ্চিত করুন বা ঐচ্ছিক চেইনিং (?.) ব্যবহার করুন।"
    }
}

# ── ৫. সিস্টেম প্রম্পট (Dynamic CMS for Prompts) ──────────────────────────────
SYSTEM_PROMPTS = {
    "skill_extraction": (
        "Analyze this interaction.\nUser: {originalMessage}\nResponse: {responseContent}\n\n"
        "CRITICAL INSTRUCTION: Extract ONLY the core technical SKILL (the 'Why' and 'How') and "
        "the best web SOURCE/URL pattern to find this type of info. Do NOT memorize the exact "
        "factual answer. Format as a reusable principle/routing rule for future queries."
    ),
    "web_fallback_consensus": (
        "You are SupremeAI, an advanced agentic system.\n\n"
        "[PAST LEARNED SKILLS & ROUTING PATTERNS]\n{localData}\n\n"
        "[LIVE EXTERNAL DATA]\n{scrapedData}\n\n"
        "User Question: {message}\n\n"
        "CRITICAL INSTRUCTION: Do not rely on past factual memory. Use the PAST LEARNED SKILLS "
        "and the LIVE EXTERNAL DATA to answer the user's question directly. DO NOT explain your "
        "work process, DO NOT provide an architecture plan, and DO NOT explain how you found the "
        "answer. Just provide the perfect, up-to-date REAL answer concisely in their preferred language."
    )
}

# ============================================================================
# ENTRY POINT
# ============================================================================

if __name__ == "__main__":
    # system_learning কালেকশনে প্যাটার্নগুলো সেভ করা হচ্ছে
    collections = {
        "system_learning": OFFLINE_PATTERNS,
        "search_registries": SEARCH_REGISTRY,
        "system_configs": {
            "security": SECURITY_CONFIGS
        },
        "system_configs": {
            "security": SECURITY_CONFIGS,
            "prompts": SYSTEM_PROMPTS
        },
        "error_patterns": ERROR_PATTERNS
    }

    run_part(
        part_name="Part 3 — Dynamic Registries",
        collections=collections
    )