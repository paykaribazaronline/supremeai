"""Tests to improve coverage for billing_api route (30.3% -> target 60%)."""

from unittest.mock import AsyncMock, MagicMock

import pytest
from fastapi import HTTPException


class TestBillingGetBalance:
    """Tests for get_wallet_balance endpoint."""

    @pytest.mark.asyncio
    async def test_get_balance_returns_wallet_data(self):
        """Authenticated user should get wallet balance."""
        from api.routes.billing_api import get_wallet_balance

        mock_session = AsyncMock()
        mock_result = MagicMock()
        mock_wallet = MagicMock()
        mock_wallet.user_id = "test-user"
        mock_wallet.balance_usd = 100.0
        mock_wallet.monthly_allowance_usd = 0.0
        mock_result.scalars().first.return_value = mock_wallet
        mock_session.execute.return_value = mock_result

        token_payload = {"sub": "test-user"}
        result = await get_wallet_balance(
            session=mock_session, token_payload=token_payload
        )

        assert result["user_id"] == "test-user"
        assert result["balance_usd"] == 100.0

    @pytest.mark.asyncio
    async def test_get_balance_unauthorized(self):
        """Unauthenticated request should raise 401."""
        from api.routes.billing_api import get_wallet_balance

        mock_session = AsyncMock()
        token_payload = {}

        with pytest.raises(HTTPException) as exc_info:
            await get_wallet_balance(session=mock_session, token_payload=token_payload)

        assert exc_info.value.status_code == 401


class TestBillingTopUp:
    """Tests for add_funds endpoint."""

    @pytest.mark.asyncio
    async def test_top_up_success(self):
        """Valid top-up should return checkout_url."""
        from api.routes.billing_api import add_funds

        mock_session = AsyncMock()
        mock_result = MagicMock()
        mock_wallet = MagicMock()
        mock_result.scalars().first.return_value = mock_wallet
        mock_session.execute.return_value = mock_result

        mock_request = MagicMock()
        mock_request.headers.get.return_value = "http://localhost:3000"
        token_payload = {"sub": "test-user"}

        result = await add_funds(
            amount=50.0,
            request=mock_request,
            session=mock_session,
            token_payload=token_payload,
        )

        assert "checkout_url" in result
        assert result["status"] == "pending"

    @pytest.mark.asyncio
    async def test_top_up_invalid_amount(self):
        """Invalid amount should raise 400."""
        from api.routes.billing_api import add_funds

        mock_session = AsyncMock()
        mock_request = MagicMock()
        token_payload = {"sub": "test-user"}

        with pytest.raises(HTTPException) as exc_info:
            await add_funds(
                amount=-10.0,
                request=mock_request,
                session=mock_session,
                token_payload=token_payload,
            )

        assert exc_info.value.status_code == 400


class TestBillingWebhook:
    """Tests for stripe_webhook endpoint."""

    @pytest.mark.asyncio
    async def test_webhook_missing_secret(self):
        """Missing webhook secret should ignore request safely."""
        from api.routes.billing_api import stripe_webhook

        mock_request = MagicMock()
        mock_request.body = AsyncMock(return_value=b"{}")
        mock_request.headers = {}
        mock_session = AsyncMock()

        result = await stripe_webhook(request=mock_request, session=mock_session)

        assert result["status"] == "ignored"
