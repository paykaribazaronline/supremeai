import pytest
import asyncio
from datetime import datetime, timezone
from fastapi.testclient import TestClient
from core.app import app

client = TestClient(app)

@pytest.fixture
def clean_billing_db():
    from api.routes.billing_api import wallets_db, ledger_db
    wallets_db.clear()
    ledger_db.clear()

def test_fetch_wallet_pre_seeds_bonus(clean_billing_db):
    resp = client.get("/api/billing/wallet")
    assert resp.status_code == 200
    data = resp.json()
    assert data["user_id"] == "default_user_session"
    assert data["balance_usd"] == 5.00  # SignUp Bonus
    assert data["subscription_tier"] == "free"

@pytest.mark.anyio
async def test_token_deductor_deducts_main_balance(clean_billing_db):
    from api.routes.billing_api import wallets_db, ledger_db, deductor
    from models.wallet import UserWallet

    user_id = "default_user_session"
    wallets_db[user_id] = UserWallet(
        user_id=user_id,
        balance_usd=5.00,
        subscription_tier="free",
        monthly_allowance_usd=0.00,
        last_renewed_at=datetime.now(timezone.utc)
    )

    # 1000 input, 1000 output. Pricing tiers: input: 0.0015, output: 0.0020. Total: 0.0035 USD
    res = await deductor.deduct_tokens(user_id, 1000, 1000, "gemini-1.5-pro")
    assert res is True
    assert wallets_db[user_id].balance_usd == 4.9965
    assert len(ledger_db) == 1

@pytest.mark.anyio
async def test_token_deductor_insufficient_funds(clean_billing_db):
    from api.routes.billing_api import wallets_db, deductor
    from models.wallet import UserWallet

    user_id = "default_user_session"
    wallets_db[user_id] = UserWallet(
        user_id=user_id,
        balance_usd=0.00,
        subscription_tier="free",
        monthly_allowance_usd=0.00,
        last_renewed_at=datetime.now(timezone.utc)
    )

    res = await deductor.deduct_tokens(user_id, 1000, 1000, "gemini-1.5-pro")
    assert res is False

def test_stripe_webhook_adds_credit(clean_billing_db):
    # Simulated Stripe completed session hook payload
    stripe_payload = {
        "type": "checkout.session.completed",
        "data": {
            "object": {
                "id": "cs_test_12345",
                "amount_total": 1000,  # 1000 cents = $10.00
                "metadata": {
                    "user_id": "default_user_session"
                }
            }
        }
    }
    
    resp = client.post("/api/billing/webhook/stripe", json=stripe_payload)
    assert resp.status_code == 200
    assert resp.json()["status"] == "processed"

    # Verify wallet topped up
    wallet_resp = client.get("/api/billing/wallet")
    # SignUp bonus ($5) + Topped-up credit ($10) = $15.00
    assert wallet_resp.json()["balance_usd"] == 15.00

def test_sslcommerz_webhook_adds_credit(clean_billing_db):
    ssl_payload = {
        "status": "VALID",
        "amount": 1000.0,  # 1000 BDT * 0.0085 = $8.50
        "value_a": "default_user_session"
    }

    resp = client.post("/api/billing/webhook/sslcommerz", json=ssl_payload)
    assert resp.status_code == 200
    assert resp.json()["status"] == "processed"

    # Verify BDT converted value credited
    wallet_resp = client.get("/api/billing/wallet")
    # SignUp bonus ($5) + Topped-up BDT credit ($8.50) = $13.50
    assert wallet_resp.json()["balance_usd"] == 13.50
