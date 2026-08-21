# backend/learning/__init__.py
"""SupremeAI Continuous Learning & Pattern Recognition Module."""

from learning.pattern_recognizer import (
    Pattern,
    PatternMatch,
    PatternRecognizer,
    PatternType,
    hamming_distance,
)

__all__ = [
    "Pattern",
    "PatternMatch",
    "PatternRecognizer",
    "PatternType",
    "hamming_distance",
]
