# 📄 ফাইল: backend/utils/firestore_helpers.py

**প্রকার:** .py  
**সাইজ:** 3,493 বাইট  
**আপডেট:** 2026-07-04T03:48:57.234068

---

## কোড

```py
"""
Firestore ক্লায়েন্ট ইনিশিয়ালাইজেশন ইউটিলিটি — ডুপ্লিকেট `firestore.Client()` কল
এবং ইম্পোর্ট গার্ড প্যাটার্ন দূর করতে কেন্দ্রীয় ফ্যাক্টরি।

আগে প্রতিটি মডিউলে আলাদাভাবে try/except দিয়ে firestore ইম্পোর্ট করা হতো
এবং প্রতিবার `firestore.Client()` কল করা হতো। এখন এই একটি মডিউল থেকে
`get_firestore_db()` কল করলেই সঠিকভাবে কনফিগার করা ক্লায়েন্ট পাওয়া যাবে।
"""

import os
from typing import Any

from loguru import logger

from utils.environment import is_test_environment


# Firestore SDK প্রাপ্যতা যাচাই — একবারই চেক হয়, বারবার try/except লাগে না
try:
    from google.cloud import firestore  # type: ignore[import-untyped]

    FIRESTORE_AVAILABLE = True
except ImportError:
    firestore = None  # type: ignore[assignment]
    FIRESTORE_AVAILABLE = False


# সিঙ্গলটন ক্যাশ — একই প্রজেক্টের জন্য বারবার নতুন Client তৈরি হবে না
_client_cache: dict[str, Any] = {}


def get_firestore_db(project_id: str | None = None) -> Any | None:
    """Firestore ক্লায়েন্ট রিটার্ন করে, টেস্ট এনভায়রনমেন্টে None দেয়।

    সিঙ্গলটন প্যাটার্ন ব্যবহার করে — একই project_id-র জন্য একটিই Client তৈরি হয়।
    Firestore SDK না থাকলে বা কানেকশন ফেইল করলে None রিটার্ন করে।

    Args:
        project_id: GCP প্রজেক্ট আইডি। না দিলে এনভায়রনমেন্ট ভ্যারিয়েবল থেকে নেয়।

    Returns:
        firestore.Client অথবা None।
    """
    # টেস্ট এনভায়রনমেন্টে কখনো রিয়েল Firestore ক্লায়েন্ট তৈরি করা উচিত না
    if is_test_environment():
        return None

    if not FIRESTORE_AVAILABLE:
        return None

    resolved_project = (
        project_id
        or os.getenv("GCP_PROJECT_ID")
        or os.getenv("GOOGLE_CLOUD_PROJECT")
        or "supremeai-a"
    )

    # ক্যাশ চেক — আগেই তৈরি থাকলে সেটাই রিটার্ন
    if resolved_project in _client_cache:
        return _client_cache[resolved_project]

    try:
        client = firestore.Client(project=resolved_project)
        _client_cache[resolved_project] = client
        logger.info(f"Firestore client initialized for project: {resolved_project}")
        return client
    except Exception as exc:
        logger.warning(f"Firestore client initialization failed: {exc}")
        return None


def is_firestore_available() -> bool:
    """Firestore SDK ইন্সটল আছে কিনা তা রিটার্ন করে।"""
    return FIRESTORE_AVAILABLE

```