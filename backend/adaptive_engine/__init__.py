#!/usr/bin/env python3
"""
adaptive_engine/__init__.py
===========================
SupremeAI 2.0 — Adaptive Engine Package Initialization

বাংলা মন্তব্য: অ্যাডাপ্টিভ ইঞ্জিন প্যাকেজ — স্বয়ংক্রিয় শেখা, প্ল্যাটফর্ম
অ্যাডাপ্টেশন, এবং অভিজ্ঞতা ভিত্তিক উন্নয়ন মডিউল।
"""

from __future__ import annotations

from typing import Any

from adaptive_engine.experience_db import Experience, ExperienceDatabase
from adaptive_engine.intent_parser import IntentParser
from adaptive_engine.learning_loop import (LearningCycleResult,
                                           LearningInsight, LearningLoop,
                                           create_learning_loop)
from adaptive_engine.platform_learner import PlatformLearner, PlatformProfile
from adaptive_engine.registry import PlatformRegistry

__all__ = [
    "Experience",
    "ExperienceDatabase",
    "IntentParser",
    "LearningCycleResult",
    "LearningInsight",
    "LearningLoop",
    "PlatformLearner",
    "PlatformProfile",
    "PlatformRegistry",
    "create_learning_loop",
]

# Version tracking for adaptive engine components
__version__ = "2.0.0"
__engine_build__ = "2026.07.20"


def get_engine_info() -> dict[str, Any]:
    """Return adaptive engine metadata."""
    return {
        "version": __version__,
        "build": __engine_build__,
        "components": sorted(__all__),
    }
