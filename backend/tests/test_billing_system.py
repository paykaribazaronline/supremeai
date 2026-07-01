import pytest
import os

# Set mock environment variables for encryption key and stripe configuration before importing core modules
# বাংলা মন্তব্য: রানিং টেস্টে ক্লাউড কানেকশন ড্রাইভার ফাস্ট-ফেইল আটকাতে মক এনক্রিপশন কী সেট করা হলো
os.environ["SUPREMEAI_ENCRYPTION_KEY"] = "4vW8yO_tWn8_bM6W_vW7LDw8qddv6QRw2wKKyJue7sE="
os.environ["STRIPE_SECRET_KEY"] = "sk_test_key"
os.environ["STRIPE_WEBHOOK_SECRET"] = "whsec_test"

from decimal import Decimal
from fastapi.testclient import TestClient
from unittest.mock import patch

from core.app import app
from models.wallet import UserWallet
from database.session import get_db_session

client = TestClient(app)

# Mock DB Session for testing billing
class MockAsyncSession:
    def __init__(self):
        self._wallet = UserWallet(
            user_id="default_user_session",
            balance_usd=Decimal("5.000000"),
            monthly_allowance_usd=Decimal("0.000000"),
            version=1
        )
        self.added = []

    async def execute(self, statement):
        class MockResult:
            def __init__(self, val):
                self.val = val
            def scalars(self):
                class MockScalars:
                    def __init__(self, val):
                        self.val = val
                    def first(self):
                        return self.val
                    def all(self):
                        return [self.val]
                return MockScalars(self.val)
        return MockResult(self._wallet)

    def add(self, obj):
        self.added.append(obj)

    async def commit(self):
        pass

    async def close(self):
        pass

    async def rollback(self):
        pass

    # Support context manager
    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        pass

    def begin(self):
        return self


@pytest.fixture
def mock_db_session():
    session = MockAsyncSession()
    # Override FastAPI dependency
    app.dependency_overrides[get_db_session] = lambda: session
    yield session
    app.dependency_overrides.clear()


def test_fetch_wallet_pre_seeds_bonus(mock_db_session):
    resp = client.get("/api/billing/wallet")
    assert resp.status_code == 200
    data = resp.json()
    assert data["user_id"] == "default_user_session"
    assert data["balance_usd"] == 5.00  # SignUp Bonus


@pytest.mark.anyio
async def test_token_deductor_deducts_main_balance(mock_db_session):
    from api.routes.billing_api import token_deductor

    # 1000 input, 1000 output. Pricing tiers: input: 0.0015, output: 0.0020. Total: 0.0035 USD
    res = await token_deductor.deduct_tokens(mock_db_session, "default_user_session", 1000, 1000, "gemini-1.5-pro")
    assert res is True
    assert mock_db_session._wallet.balance_usd == Decimal("4.996500")
    assert len(mock_db_session.added) == 1


@pytest.mark.anyio
async def test_token_deductor_insufficient_funds(mock_db_session):
    from api.routes.billing_api import token_deductor

    mock_db_session._wallet.balance_usd = Decimal("0.000000")
    res = await token_deductor.deduct_tokens(mock_db_session, "default_user_session", 1000, 1000, "gemini-1.5-pro")
    assert res is False


def test_stripe_webhook_adds_credit(mock_db_session):
    # Mock stripe constructor to bypass network/signature validation
    with patch("stripe.Webhook.construct_event") as mock_construct:
        mock_construct.return_value = {
            "type": "payment_intent.succeeded",
            "data": {
                "object": {
                    "id": "pi_test_12345",
                    "amount_received": 1000,  # 1000 cents = $10.00
                    "metadata": {
                        "user_id": "default_user_session"
                    }
                }
            }
        }
        with patch("stripe.api_key", "sk_test_key"):
            with patch("api.routes.billing_api.STRIPE_WEBHOOK_SECRET", "whsec_test"):
                resp = client.post(
                    "/api/billing/webhook/stripe",
                    json={"type": "payment_intent.succeeded"},
                    headers={"Stripe-Signature": "t=123,v1=abc"}
                )
                assert resp.status_code == 200
                assert resp.json() == {"status": "success"}
                assert mock_db_session._wallet.balance_usd == Decimal("15.000000")


def test_sslcommerz_webhook_adds_credit(mock_db_session):
    ssl_payload = {
        "status": "VALID",
        "amount": 1000.0,  # 1000 BDT * 0.0085 = $8.50
        "value_a": "default_user_session"
    }

    resp = client.post("/api/billing/webhook/sslcommerz", json=ssl_payload)
    assert resp.status_code == 200
    assert resp.json()["status"] == "processed"
    assert mock_db_session._wallet.balance_usd == Decimal("13.500000")
