"""Tests to improve coverage for billing_api route (30.3% -> target 60%)."""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from fastapi import HTTPException


class TestBillingGetBalance:
    """Tests for get_balance endpoint."""

    def test_get_balance_returns_wallet_data(self):
        """Authenticated user should get wallet balance."""
        from api.routes.billing_api import get_balance

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "test-user"}

        mock_wallet = MagicMock()
        mock_wallet.balance = 100.0
        mock_wallet.currency = "USD"

        with patch("api.routes.billing_api.SupabaseDB") as MockDB:
            mock_db = MagicMock()
            MockDB.return_value = mock_db
            mock_db.get_wallet.return_value = mock_wallet
            result = get_balance(mock_request)

        assert result["balance"] == 100.0

    def test_get_balance_no_wallet_returns_zero(self):
        """User without wallet should return zero balance."""
        from api.routes.billing_api import get_balance

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "new-user"}

        with patch("api.routes.billing_api.SupabaseDB") as MockDB:
            mock_db = MagicMock()
            MockDB.return_value = mock_db
            mock_db.get_wallet.return_value = None
            result = get_balance(mock_request)

        assert result["balance"] == 0.0

    def test_get_balance_unauthorized(self):
        """Unauthenticated request should raise 401."""
        from api.routes.billing_api import get_balance

        mock_request = MagicMock()
        mock_request.state.user = None

        with pytest.raises(HTTPException) as exc_info:
            get_balance(mock_request)

        assert exc_info.value.status_code == 401


class TestBillingTopUp:
    """Tests for top_up endpoint."""

    def test_top_up_success(self):
        """Valid top-up should process payment."""
        from api.routes.billing_api import TopUpRequest, top_up

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "test-user"}

        payload = TopUpRequest(amount=50.0, currency="USD")

        with patch("api.routes.billing_api.stripe") as mock_stripe:
            mock_stripe.checkout.Session.create.return_value = MagicMock(
                url="https://checkout.stripe.com/test"
            )
            result = top_up(payload, mock_request)

        assert "checkout_url" in result

    def test_top_up_invalid_amount(self):
        """Invalid amount should raise 422."""
        from api.routes.billing_api import TopUpRequest, top_up

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "test-user"}

        payload = TopUpRequest(amount=-10.0, currency="USD")

        with pytest.raises(HTTPException) as exc_info:
            top_up(payload, mock_request)

        assert exc_info.value.status_code == 422

    def test_top_up_unauthorized(self):
        """Unauthenticated request should raise 401."""
        from api.routes.billing_api import TopUpRequest, top_up

        mock_request = MagicMock()
        mock_request.state.user = None

        payload = TopUpRequest(amount=50.0, currency="USD")

        with pytest.raises(HTTPException) as exc_info:
            top_up(payload, mock_request)

        assert exc_info.value.status_code == 401


class TestBillingWebhook:
    """Tests for stripe_webhook endpoint."""

    def test_webhook_valid_event(self):
        """Valid Stripe webhook should process event."""
        from api.routes.billing_api import stripe_webhook

        mock_request = MagicMock()
        mock_request.body = AsyncMock(
            return_value=b'{"type": "checkout.session.completed", "data": {"object": {"client_reference_id": "test-user", "amount_total": 5000}}}'
        )
        mock_request.headers = {"stripe-signature": "test-sig"}

        with patch(
            "api.routes.billing_api.stripe.Webhook.construct_event"
        ) as mock_construct:
            mock_event = MagicMock()
            mock_event.type = "checkout.session.completed"
            mock_event.data.object.client_reference_id = "test-user"
            mock_event.data.object.amount_total = 5000
            mock_construct.return_value = mock_event

            result = stripe_webhook(mock_request)

        assert result["status"] == "success"

    def test_webhook_invalid_signature(self):
        """Invalid webhook signature should raise 400."""
        from api.routes.billing_api import stripe_webhook
        from stripe.error import SignatureVerificationError

        mock_request = MagicMock()
        mock_request.body = AsyncMock(
            return_value=b'{"type": "checkout.session.completed"}'
        )
        mock_request.headers = {"stripe-signature": "bad-sig"}

        with patch(
            "api.routes.billing_api.stripe.Webhook.construct_event",
            side_effect=SignatureVerificationError("Bad signature", None),
        ):
            with pytest.raises(HTTPException) as exc_info:
                stripe_webhook(mock_request)

        assert exc_info.value.status_code == 400
