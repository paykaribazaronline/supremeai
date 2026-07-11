# 📄 ফাইল: backend/tests/core/test_cost_guard_coverage.py

**প্রকার:** .py  
**সাইজ:** 13,741 বাইট  
**আপডেট:** 2026-07-11T17:37:52.641358

---

## কোড

```py
# backend/tests/core/test_cost_guard_coverage.py
# বাংলা মন্তব্য: CostGuard-এর জন্য comprehensive unit tests।
# Database এবং HTTP exceptions mock করা হয়েছে।

import asyncio
from unittest.mock import AsyncMock
from unittest.mock import MagicMock
from unittest.mock import patch

import pytest
from fastapi import HTTPException

from core.cost_guard import CostGuard


# -------------------- Fixtures --------------------


@pytest.fixture
def cost_guard():
    """CostGuard ইনস্ট্যান্স ফেরত দেয়।"""
    return CostGuard()


@pytest.fixture
def cost_guard_with_db():
    """Database সহ CostGuard ইনস্ট্যান্স ফেরত দেয়।"""
    mock_db = MagicMock()
    return CostGuard(db=mock_db), mock_db


@pytest.fixture
def mock_budget_doc():
    """Mock budget document snapshot।"""
    doc = MagicMock()
    doc.exists = True
    doc.to_dict.return_value = {
        "monthly_limit": 100.0,
        "spent_amount": 50.0,
    }
    return doc


# -------------------- Tests: __init__ --------------------


class TestCostGuardInit:
    """বাংলা মন্তব্য: Initialization এবং tier limits টেস্ট।"""

    def test_default_initialization(self, cost_guard):
        """বাংলা মন্তব্য: Default initialization with no DB।"""
        assert cost_guard._db is None
        assert cost_guard.tier_limits["free"] == 0.0
        assert cost_guard.tier_limits["economy"] == 0.02
        assert cost_guard.tier_limits["premium"] == 0.50

    def test_initialization_with_db(self, cost_guard_with_db):
        """বাংলা মন্তব্য: DB দিয়ে initialization।"""
        cost_guard, mock_db = cost_guard_with_db
        assert cost_guard._db is mock_db


# -------------------- Tests: check_budget --------------------


class TestCheckBudget:
    """বাংলা মন্তব্য: check_budget() method টেস্ট।"""

    @pytest.mark.asyncio
    async def test_check_budget_without_db(self, cost_guard):
        """বাংলা মন্তব্য: DB না থাকলে budget check bypass হয়।"""
        result = await cost_guard.check_budget("tenant-123", 0.01)
        assert result is True

    @pytest.mark.asyncio
    async def test_check_budget_within_limit(self, cost_guard_with_db, mock_budget_doc):
        """বাংলা মন্তব্য: Budget limit-এর ভিতরে থাকলে True return হয়।"""
        cost_guard, mock_db = cost_guard_with_db
        mock_db.collection.return_value.document.return_value.get.return_value = mock_budget_doc

        result = await cost_guard.check_budget("tenant-123", 0.01)

        assert result is True

    @pytest.mark.asyncio
    async def test_check_budget_exceeds_limit(self, cost_guard_with_db):
        """বাংলা মন্তব্য: Budget exceed করলে HTTPException raise হয়।"""
        cost_guard, mock_db = cost_guard_with_db

        # Create a doc that exceeds budget
        doc = MagicMock()
        doc.exists = True
        doc.to_dict.return_value = {
            "monthly_limit": 100.0,
            "spent_amount": 99.99,  # Almost at limit
        }

        mock_db.collection.return_value.document.return_value.get.return_value = doc

        with pytest.raises(HTTPException) as exc_info:
            await cost_guard.check_budget("tenant-123", 0.02)  # Would exceed

        assert exc_info.value.status_code == 402
        assert "Budget Exceeded" in str(exc_info.value.detail)

    @pytest.mark.asyncio
    async def test_check_budget_no_budget_configured(self, cost_guard_with_db):
        """বাংলা মন্তব্য: Budget না থাকলে HTTPException raise হয়।"""
        cost_guard, mock_db = cost_guard_with_db

        doc = MagicMock()
        doc.exists = False

        mock_db.collection.return_value.document.return_value.get.return_value = doc

        with pytest.raises(HTTPException) as exc_info:
            await cost_guard.check_budget("tenant-123", 0.01)

        assert exc_info.value.status_code == 402
        assert "No budget configured" in str(exc_info.value.detail)

    @pytest.mark.asyncio
    async def test_check_budget_with_zero_limit(self, cost_guard_with_db):
        """বাংলা মন্তব্য: Zero budget limit-এ any cost exceed হবে।"""
        cost_guard, mock_db = cost_guard_with_db

        doc = MagicMock()
        doc.exists = True
        doc.to_dict.return_value = {
            "monthly_limit": 0.0,
            "spent_amount": 0.0,
        }

        mock_db.collection.return_value.document.return_value.get.return_value = doc

        with pytest.raises(HTTPException) as exc_info:
            await cost_guard.check_budget("tenant-123", 0.01)

        assert exc_info.value.status_code == 402

    @pytest.mark.asyncio
    async def test_check_budget_exact_limit(self, cost_guard_with_db):
        """বাংলা মন্তব্য: Exact budget limit-এ success হয়।"""
        cost_guard, mock_db = cost_guard_with_db

        doc = MagicMock()
        doc.exists = True
        doc.to_dict.return_value = {
            "monthly_limit": 100.0,
            "spent_amount": 99.98,
        }

        mock_db.collection.return_value.document.return_value.get.return_value = doc

        # 99.98 + 0.02 = 100.0 (exact limit, should succeed)
        result = await cost_guard.check_budget("tenant-123", 0.02)
        assert result is True

    @pytest.mark.asyncio
    async def test_check_budget_with_async_get(self, cost_guard_with_db, mock_budget_doc):
        """বাংলা মন্তব্য: Async get() method handle করে।"""
        cost_guard, mock_db = cost_guard_with_db

        # Make get() async
        async_get = AsyncMock(return_value=mock_budget_doc)
        mock_db.collection.return_value.document.return_value.get = async_get

        result = await cost_guard.check_budget("tenant-123", 0.01)
        assert result is True

    @pytest.mark.asyncio
    async def test_check_budget_with_sync_get(self, cost_guard_with_db, mock_budget_doc):
        """বাংলা মন্তব্য: Sync get() method handle করে।"""
        cost_guard, mock_db = cost_guard_with_db

        # get() is already sync (MagicMock)
        mock_db.collection.return_value.document.return_value.get.return_value = mock_budget_doc

        result = await cost_guard.check_budget("tenant-123", 0.01)
        assert result is True

    @pytest.mark.asyncio
    async def test_check_budget_handles_general_exception(self, cost_guard_with_db):
        """বাংলা মন্তব্য: General exception handle করে RuntimeError raise হয়।"""
        cost_guard, mock_db = cost_guard_with_db

        mock_db.collection.return_value.document.return_value.get.side_effect = RuntimeError("DB error")

        with pytest.raises(RuntimeError) as exc_info:
            await cost_guard.check_budget("tenant-123", 0.01)

        assert "CostGuard failed to verify budget" in str(exc_info.value)

    @pytest.mark.asyncio
    async def test_check_budget_reraises_http_exception(self, cost_guard_with_db):
        """বাংলা মন্তব্য: HTTPException directly re-raise হয়।"""
        cost_guard, mock_db = cost_guard_with_db

        mock_db.collection.return_value.document.return_value.get.side_effect = HTTPException(status_code=402, detail="Custom error")

        with pytest.raises(HTTPException) as exc_info:
            await cost_guard.check_budget("tenant-123", 0.01)

        assert exc_info.value.status_code == 402
        assert "Custom error" in str(exc_info.value.detail)

    @pytest.mark.asyncio
    async def test_check_budget_with_missing_fields(self, cost_guard_with_db):
        """বাংলা মন্তব্য: Missing fields default values দিয়ে handle হয়।"""
        cost_guard, mock_db = cost_guard_with_db

        doc = MagicMock()
        doc.exists = True
        doc.to_dict.return_value = {}  # No fields

        mock_db.collection.return_value.document.return_value.get.return_value = doc

        # Should use defaults: monthly_limit=0.0, spent_amount=0.0
        # 0.0 + 0.01 > 0.0, so should raise
        with pytest.raises(HTTPException) as exc_info:
            await cost_guard.check_budget("tenant-123", 0.01)

        assert exc_info.value.status_code == 402


# -------------------- Tests: validate_budget --------------------


class TestValidateBudget:
    """বাংলা মন্তব্য: validate_budget() method টেস্ট।"""

    def test_validate_free_tier(self, cost_guard):
        """বাংলা মন্তব্য: Free tier validate হয়।"""
        result = cost_guard.validate_budget("free")
        assert result is True

    def test_validate_economy_tier(self, cost_guard):
        """বাংলা মন্তব্য: Economy tier validate হয়।"""
        result = cost_guard.validate_budget("economy")
        assert result is True

    def test_validate_premium_tier(self, cost_guard):
        """বাংলা মন্তব্য: Premium tier validate হয়।"""
        result = cost_guard.validate_budget("premium")
        assert result is True

    def test_validate_unknown_tier(self, cost_guard):
        """বাংলা মন্তব্য: Unknown tier-ও True return করে (bypass mode)।"""
        result = cost_guard.validate_budget("enterprise")
        assert result is True

    def test_validate_logs_tier(self, cost_guard):
        """বাংলা মন্তব্য: Tier validation log হয়।"""
        with patch("core.cost_guard.logger") as mock_logger:
            cost_guard.validate_budget("premium")
            mock_logger.info.assert_called_once()
            log_msg = mock_logger.info.call_args[0][0]
            assert "Validating execution safety gate for AI tier: 'premium'" in log_msg


# -------------------- Tests: Global Instance --------------------


class TestGlobalInstance:
    """বাংলা মন্তব্য: Global cost_guard instance টেস্ট।"""

    def test_global_instance_exists(self):
        """বাংলা মন্তব্য: Global instance create করা আছে।"""
        from core.cost_guard import cost_guard

        assert isinstance(cost_guard, CostGuard)

    def test_global_instance_default_config(self):
        """বাংলা মন্তব্য: Global instance default configuration দিয়ে create করা আছে।"""
        from core.cost_guard import cost_guard

        assert cost_guard._db is None  # Default no DB
        assert "free" in cost_guard.tier_limits
        assert "economy" in cost_guard.tier_limits
        assert "premium" in cost_guard.tier_limits


# -------------------- Tests: Integration --------------------


class TestCostGuardIntegration:
    """বাংলা মন্তব্য: Integration-style tests for realistic scenarios।"""

    @pytest.mark.asyncio
    async def test_full_budget_check_workflow(self, cost_guard_with_db, mock_budget_doc):
        """বাংলা মন্তব্য: সম্পূর্ণ budget check workflow।"""
        cost_guard, mock_db = cost_guard_with_db
        mock_db.collection.return_value.document.return_value.get.return_value = mock_budget_doc

        # Check budget for a small cost
        result = await cost_guard.check_budget("tenant-123", 0.01)
        assert result is True

        # Verify DB was queried correctly
        mock_db.collection.assert_called_once_with("tenants/tenant-123/budget")
        mock_db.collection.return_value.document.assert_called_once_with("status")

    @pytest.mark.asyncio
    async def test_budget_exhaustion_scenario(self, cost_guard_with_db):
        """বাংলা মন্তব্য: Budget exhaustion scenario।"""
        cost_guard, mock_db = cost_guard_with_db

        # First check - within budget
        doc1 = MagicMock()
        doc1.exists = True
        doc1.to_dict.return_value = {
            "monthly_limit": 100.0,
            "spent_amount": 50.0,
        }
        mock_db.collection.return_value.document.return_value.get.return_value = doc1

        result = await cost_guard.check_budget("tenant-123", 0.01)
        assert result is True

        # Second check - budget exhausted
        doc2 = MagicMock()
        doc2.exists = True
        doc2.to_dict.return_value = {
            "monthly_limit": 100.0,
            "spent_amount": 100.01,
        }
        mock_db.collection.return_value.document.return_value.get.return_value = doc2

        with pytest.raises(HTTPException) as exc_info:
            await cost_guard.check_budget("tenant-123", 0.01)

        assert exc_info.value.status_code == 402

    @pytest.mark.asyncio
    async def test_tier_validation_workflow(self, cost_guard):
        """বাংলা মন্তব্য: Tier validation workflow।"""
        # All tiers should validate successfully
        tiers = ["free", "economy", "premium"]
        for tier in tiers:
            result = cost_guard.validate_budget(tier)
            assert result is True

    @pytest.mark.asyncio
    async def test_bypass_mode_without_db(self, cost_guard):
        """বাংলা মন্তব্য: No DB means bypass mode।"""
        # Without DB, all budget checks should pass
        result = await cost_guard.check_budget("any-tenant", 1000.0)
        assert result is True

```