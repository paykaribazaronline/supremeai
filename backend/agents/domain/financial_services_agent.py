"""
SupremeAI — Financial Services Agent
=====================================
For fintech applications and financial analysis.
Provides transaction analysis, risk scoring, and financial insights.
"""

from __future__ import annotations

import hashlib
import logging
from dataclasses import dataclass
from datetime import UTC, datetime, timedelta
from enum import StrEnum
from typing import Any

from core.cache import get_cache
from core.tenant_db import TenantAwareFirestore

logger = logging.getLogger("supremeai.financial_services")

FINANCIAL_CACHE_TTL = 300  # 5 minutes


class TransactionType(StrEnum):
    CREDIT = "credit"
    DEBIT = "debit"
    REFUND = "refund"
    FEE = "fee"
    TRANSFER = "transfer"


class RiskCategory(StrEnum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    SUSPICIOUS = "suspicious"


@dataclass(frozen=True)
class Transaction:
    """Immutable transaction record."""

    id: str
    user_id: str
    amount: float
    currency: str
    type: TransactionType
    timestamp: datetime
    merchant: str | None
    metadata: dict[str, Any]


@dataclass(frozen=True)
class RiskAssessment:
    """Immutable risk assessment for a transaction."""

    transaction_id: str
    risk_category: RiskCategory
    risk_score: float
    flags: list[str]
    recommended_action: str


@dataclass(frozen=True)
class FinancialInsight:
    """Immutable financial insight."""

    user_id: str
    metric: str
    value: float
    change_percent: float
    benchmark: float
    recommendation: str


class FinancialServicesAgent:
    """
    For fintech applications and financial analysis.
    Handles transaction analysis, risk scoring, and financial insights.
    """

    def __init__(self, db: TenantAwareFirestore | None = None) -> None:
        self.db = db
        self.cache = get_cache()

    def _cache_key(self, prefix: str, identifier: str) -> str:
        raw = (
            f"financial:{prefix}:{identifier}:{datetime.now(UTC).strftime('%Y%m%d%H')}"
        )
        return f"financial:{hashlib.sha256(raw.encode()).hexdigest()[:16]}"

    async def assess_transaction_risk(
        self,
        transaction: Transaction,
        user_history: list[Transaction] | None = None,
    ) -> RiskAssessment:
        """Assess risk level of a financial transaction."""
        flags = []
        risk_score = 0.0

        # Amount-based risk
        if transaction.amount > 10000:
            flags.append("high_value_transaction")
            risk_score += 0.3
        elif transaction.amount > 5000:
            flags.append("above_average_value")
            risk_score += 0.15

        # Velocity check (if history provided)
        if user_history:
            recent_count = sum(
                1
                for t in user_history
                if t.timestamp > datetime.now(UTC) - timedelta(hours=1)
            )
            if recent_count > 5:
                flags.append("high_transaction_velocity")
                risk_score += 0.25

            # Duplicate check
            similar = [
                t
                for t in user_history[-10:]
                if abs(t.amount - transaction.amount) < 0.01
                and t.merchant == transaction.merchant
            ]
            if len(similar) > 2:
                flags.append("duplicate_transaction_pattern")
                risk_score += 0.2

        # Time-based risk
        hour = datetime.now(UTC).hour
        if hour < 6 or hour > 23:
            flags.append("off_hours_transaction")
            risk_score += 0.1

        # Category assignment
        if risk_score >= 0.6:
            category = RiskCategory.SUSPICIOUS
            action = "Block transaction and flag for manual review"
        elif risk_score >= 0.4:
            category = RiskCategory.HIGH
            action = "Require additional authentication"
        elif risk_score >= 0.2:
            category = RiskCategory.MEDIUM
            action = "Monitor transaction pattern"
        else:
            category = RiskCategory.LOW
            action = "Process normally"

        return RiskAssessment(
            transaction_id=transaction.id,
            risk_category=category,
            risk_score=round(risk_score, 2),
            flags=flags,
            recommended_action=action,
        )

    async def analyze_spending_patterns(
        self,
        user_id: str,
        transactions: list[Transaction],
    ) -> list[FinancialInsight]:
        """Analyze user spending patterns and generate insights."""
        insights = []

        if not transactions:
            return insights

        total_spent = sum(
            t.amount for t in transactions if t.type == TransactionType.DEBIT
        )
        total_income = sum(
            t.amount for t in transactions if t.type == TransactionType.CREDIT
        )

        # Spending vs income
        if total_income > 0:
            savings_rate = (total_income - total_spent) / total_income
            if savings_rate < 0.1:
                insights.append(
                    FinancialInsight(
                        user_id=user_id,
                        metric="savings_rate",
                        value=savings_rate,
                        change_percent=0.0,
                        benchmark=0.2,
                        recommendation="Consider reducing discretionary spending to improve savings rate",
                    )
                )

        # Average transaction value
        if transactions:
            avg_transaction = total_spent / len(transactions)
            insights.append(
                FinancialInsight(
                    user_id=user_id,
                    metric="avg_transaction_value",
                    value=avg_transaction,
                    change_percent=0.0,
                    benchmark=50.0,
                    recommendation=f"Average transaction is ${avg_transaction:.2f}",
                )
            )

        return insights

    async def get_currency_exchange_rate(
        self, from_currency: str, to_currency: str
    ) -> dict[str, Any]:
        """Get exchange rate (simulated - would use external API in production)."""
        # Simulated exchange rates for common pairs
        rates = {
            ("USD", "BDT"): 109.50,
            ("BDT", "USD"): 0.0091,
            ("USD", "EUR"): 0.92,
            ("EUR", "USD"): 1.09,
            ("USD", "GBP"): 0.79,
            ("GBP", "USD"): 1.27,
        }
        rate = rates.get((from_currency.upper(), to_currency.upper()), 1.0)
        return {
            "from": from_currency,
            "to": to_currency,
            "rate": rate,
            "timestamp": datetime.now(UTC).isoformat(),
        }


# Singleton
_financial_instance: FinancialServicesAgent | None = None


def get_financial_services() -> FinancialServicesAgent:
    """Get or create the singleton FinancialServicesAgent."""
    global _financial_instance
    if _financial_instance is None:
        _financial_instance = FinancialServicesAgent()
    return _financial_instance
