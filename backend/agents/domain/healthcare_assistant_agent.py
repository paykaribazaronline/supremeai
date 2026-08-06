"""
SupremeAI — Healthcare Assistant Agent
=======================================
Medical information processing with privacy compliance.
Provides health data analysis, medication tracking, and wellness insights.
"""

from __future__ import annotations

import hashlib
import logging
import re
from dataclasses import dataclass
from datetime import UTC, datetime
from enum import StrEnum
from typing import Any

from core.cache import get_cache
from core.llm_router import LLMRouter

logger = logging.getLogger("supremeai.healthcare_assistant")

HEALTHCARE_CACHE_TTL = 600  # 10 minutes


class HealthMetricType(StrEnum):
    HEART_RATE = "heart_rate"
    BLOOD_PRESSURE = "blood_pressure"
    BLOOD_SUGAR = "blood_sugar"
    TEMPERATURE = "temperature"
    WEIGHT = "weight"
    SLEEP_HOURS = "sleep_hours"
    STEPS = "steps"
    OXYGEN_SATURATION = "oxygen_saturation"


class PrivacyLevel(StrEnum):
    PUBLIC = "public"
    INTERNAL = "internal"
    RESTRICTED = "restricted"
    PHI = "phi"  # Protected Health Information


@dataclass(frozen=True)
class HealthRecord:
    """Immutable health record."""

    user_id: str
    metric_type: HealthMetricType
    value: float
    unit: str
    timestamp: datetime
    privacy_level: PrivacyLevel
    notes: str | None


@dataclass(frozen=True)
class HealthInsight:
    """Immutable health insight."""

    metric: str
    current_value: float
    normal_range: tuple[float, float]
    status: str  # normal, attention, critical
    recommendation: str


# Normal ranges for common health metrics
NORMAL_RANGES = {
    HealthMetricType.HEART_RATE: (60, 100),
    HealthMetricType.BLOOD_PRESSURE: (90, 120),  # Systolic
    HealthMetricType.BLOOD_SUGAR: (70, 140),
    HealthMetricType.TEMPERATURE: (36.1, 37.2),
    HealthMetricType.OXYGEN_SATURATION: (95, 100),
    HealthMetricType.SLEEP_HOURS: (7, 9),
    HealthMetricType.STEPS: (5000, 10000),
}


class PHIScanner:
    """
    Scans data for Protected Health Information (PHI).
    """

    PHI_PATTERNS = {
        "ssn": r"\b\d{3}-\d{2}-\d{4}\b",
        "phone": r"\b\d{3}[-.]?\d{3}[-.]?\d{4}\b",
        "email": r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b",
        "medical_id": r"\b(?:MRN|PATIENT|MEDICAL)[-_]?\d{6,}\b",
        "date_of_birth": r"\b\d{2}[/-]\d{2}[/-]\d{4}\b",
    }

    @staticmethod
    def detect_phi(text: str) -> list[dict[str, Any]]:
        """Detect PHI in text content."""
        findings = []
        for phi_type, pattern in PHIScanner.PHI_PATTERNS.items():
            matches = re.finditer(pattern, text)
            for match in matches:
                findings.append(
                    {
                        "type": phi_type,
                        "position": match.start(),
                        "preview": text[max(0, match.start() - 20) : match.end() + 20],
                    }
                )
        return findings

    @staticmethod
    def redact_phi(text: str) -> str:
        """Redact PHI from text."""
        redacted = text
        for phi_type, pattern in PHIScanner.PHI_PATTERNS.items():
            redacted = re.sub(pattern, f"[REDACTED_{phi_type.upper()}]", redacted)
        return redacted


class HealthcareAssistantAgent:
    """
    Medical information processing with privacy compliance.
    """

    def __init__(self, llm_router: LLMRouter | None = None) -> None:
        self.llm = llm_router or LLMRouter()
        self.cache = get_cache()
        self.phi_scanner = PHIScanner()

    def _cache_key(self, prefix: str, identifier: str) -> str:
        raw = (
            f"healthcare:{prefix}:{identifier}:{datetime.now(UTC).strftime('%Y%m%d%H')}"
        )
        return f"healthcare:{hashlib.sha256(raw.encode()).hexdigest()[:16]}"

    def check_vitals(self, metric: HealthMetricType, value: float) -> HealthInsight:
        """Check if a vital sign is within normal range."""
        normal_range = NORMAL_RANGES.get(metric, (0, 999))
        lower, upper = normal_range

        if value < lower * 0.8 or value > upper * 1.2:
            status = "critical"
            recommendation = f"Immediate attention needed: {metric.value} is critically outside normal range ({lower}-{upper})"
        elif value < lower or value > upper:
            status = "attention"
            recommendation = f"Monitor {metric.value}: value ({value}) is outside normal range ({lower}-{upper})"
        else:
            status = "normal"
            recommendation = f"{metric.value} is within normal range"

        return HealthInsight(
            metric=metric.value,
            current_value=value,
            normal_range=normal_range,
            status=status,
            recommendation=recommendation,
        )

    def sanitize_health_data(self, data: dict[str, Any]) -> dict[str, Any]:
        """Remove PHI from health data while preserving analytical value."""
        sanitized = {}
        for key, value in data.items():
            if isinstance(value, str):
                sanitized[key] = self.phi_scanner.redact_phi(value)
            elif isinstance(value, dict):
                sanitized[key] = self.sanitize_health_data(value)
            else:
                sanitized[key] = value
        return sanitized

    async def generate_wellness_tip(self, metrics: list[HealthRecord]) -> str:
        """Generate personalized wellness tip based on health metrics."""
        context = "\n".join(
            f"{m.metric_type.value}: {m.value} {m.unit}" for m in metrics[-5:]
        )

        prompt = (
            f"Based on these health metrics, provide a brief wellness tip:\n\n{context}\n\n"
            f"Keep it concise, actionable, and positive. Focus on one key improvement area."
        )

        try:
            result = await self.llm.route(
                prompt=prompt,
                task_type="reasoning",
                max_tokens=200,
            )
            return result.get(
                "content",
                "Maintain a balanced lifestyle with regular exercise and proper nutrition.",
            )
        except Exception as e:
            logger.error("Failed to generate wellness tip: %s", e)
            return "Maintain a balanced lifestyle with regular exercise and proper nutrition."


# Singleton
_healthcare_instance: HealthcareAssistantAgent | None = None


def get_healthcare_assistant() -> HealthcareAssistantAgent:
    """Get or create the singleton HealthcareAssistantAgent."""
    global _healthcare_instance
    if _healthcare_instance is None:
        _healthcare_instance = HealthcareAssistantAgent()
    return _healthcare_instance
