# 📄 ফাইল: backend/tests/tools/test_viral_referral_engine.py

**প্রকার:** .py  
**সাইজ:** 19,115 বাইট  
**আপডেট:** 2026-07-11T19:51:42.240333

---

## কোড

```py
import asyncio
import json
import os
import time
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from tools.viral_referral_engine import FRAUD_INDICATOR_THRESHOLD, ViralReferralEngine


class TestViralReferralEngine:
    """Tests for tools/viral_referral_engine.py"""

    @pytest.fixture
    def engine(self, tmp_path, monkeypatch):
        monkeypatch.setattr("tools.viral_referral_engine.db.client", None)
        mock_settings = MagicMock()
        mock_settings.app_base_url = "https://supremeai.com"
        monkeypatch.setattr("tools.viral_referral_engine.settings", mock_settings)
        engine = ViralReferralEngine()
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        return engine

    @pytest.mark.anyio
    async def test_init(self, engine):
        assert engine is not None

    @pytest.mark.anyio
    async def test_local_store(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        path = engine._local_store()
        assert path.endswith("referrals.json")

    @pytest.mark.anyio
    async def test_load_local_empty(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "nonexistent", "referrals.json")
        data = engine._load_local()
        assert data == {"codes": {}, "wallets": {}}

    @pytest.mark.anyio
    async def test_load_local_existing(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        data = {"codes": {}, "wallets": {}}
        with open(engine._local_store(), "w", encoding="utf-8") as f:
            json.dump(data, f)
        result = engine._load_local()
        assert result == data

    @pytest.mark.anyio
    async def test_save_local(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        data = {"codes": {}, "wallets": {}}
        engine._save_local(data)
        assert os.path.exists(engine._local_store())
        with open(engine._local_store(), encoding="utf-8") as f:
            loaded = json.load(f)
        assert loaded == data

    @pytest.mark.anyio
    async def test_generate_referral_code_local(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        result = engine.generate_referral_code("user-123")
        assert result["status"] == "success"
        assert result["code"].startswith("SUPREME-")
        assert result["expires_at"] > time.time()

    @pytest.mark.anyio
    async def test_generate_referral_code_db(self, engine):
        mock_db = MagicMock()
        mock_table = MagicMock()
        mock_db.table.return_value = mock_table
        mock_table.upsert.return_value.execute.return_value = None

        with patch("tools.viral_referral_engine.db.client", mock_db), patch("tools.viral_referral_engine.settings") as mock_settings:
            mock_settings.app_base_url = "https://supremeai.com"
            result = engine.generate_referral_code("user-123")
        assert result["status"] == "success"
        assert result["code"].startswith("SUPREME-")
        mock_table.upsert.assert_called_once()

    @pytest.mark.anyio
    async def test_generate_referral_code_db_exception(self, engine):
        mock_db = MagicMock()
        mock_table = MagicMock()
        mock_db.table.return_value = mock_table
        mock_table.upsert.side_effect = Exception("DB error")

        with patch("tools.viral_referral_engine.db.client", mock_db), patch("tools.viral_referral_engine.settings") as mock_settings:
            mock_settings.app_base_url = "https://supremeai.com"
            result = engine.generate_referral_code("user-123")
        assert result["status"] == "success"

    @pytest.mark.anyio
    async def test_list_user_codes_local(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        code = engine.generate_referral_code("user-123")["code"]
        codes = engine.list_user_codes("user-123")
        assert len(codes) == 1
        assert codes[0]["code"] == code

    @pytest.mark.anyio
    async def test_list_user_codes_db(self, engine):
        mock_db = MagicMock()
        mock_table = MagicMock()
        mock_db.table.return_value = mock_table
        mock_table.select.return_value.eq.return_value.execute.return_value = MagicMock(data=[{"code": "SUPREME-ABC", "referrer_id": "user-456"}])

        with patch("tools.viral_referral_engine.db.client", mock_db):
            codes = engine.list_user_codes("user-456")
        assert len(codes) == 1
        assert codes[0]["code"] == "SUPREME-ABC"

    @pytest.mark.anyio
    async def test_list_user_codes_db_exception(self, engine):
        mock_db = MagicMock()
        mock_table = MagicMock()
        mock_db.table.return_value = mock_table
        mock_table.select.return_value.eq.return_value.execute.side_effect = Exception("DB error")

        with patch("tools.viral_referral_engine.db.client", mock_db):
            codes = engine.list_user_codes("user-456")
        assert codes == []

    @pytest.mark.anyio
    async def test_process_signup_invalid_code(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        result = await engine.process_signup("new-user-123", "INVALID-CODE", {})
        assert result["status"] == "skipped"
        assert result["reason"] == "invalid_code"

    @pytest.mark.anyio
    async def test_process_signup_valid_local(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        gen = engine.generate_referral_code("referrer-1")
        code = gen["code"]
        result = await engine.process_signup("new-user-123", code, {})
        assert result["status"] == "success"
        assert result["referrer_id"] == "referrer-1"
        assert "reward_applied" in result

    @pytest.mark.anyio
    async def test_process_signup_expired_code(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        gen = engine.generate_referral_code("referrer-1")
        code = gen["code"]
        data = engine._load_local()
        data["codes"][code]["expires_at"] = time.time() - 1
        engine._save_local(data)
        result = await engine.process_signup("new-user-123", code, {})
        assert result["status"] == "skipped"
        assert result["reason"] == "expired_code"

    @pytest.mark.anyio
    async def test_process_signup_fraudulent(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        gen = engine.generate_referral_code("referrer-1")
        code = gen["code"]
        meta = {"ip_address": "1.2.3.4", "device_fingerprint": "dev-abc"}
        with patch.object(engine, "_is_fraudulent", return_value=True):
            result = await engine.process_signup("new-user-123", code, meta)
        assert result["status"] == "skipped"
        assert result["reason"] == "fraud_detected"

    @pytest.mark.anyio
    async def test_process_signup_db(self, engine):
        mock_db = MagicMock()
        mock_table = MagicMock()
        mock_db.table.return_value = mock_table
        mock_table.select.return_value.eq.return_value.eq.return_value.execute.return_value = MagicMock(
            data=[{"code": "SUPREME-ABC", "referrer_id": "referrer-1", "status": "active", "redeemed_count": 0, "expires_at": time.time() + 1000}]
        )
        mock_table.insert.return_value.execute.return_value = None
        mock_table.update.return_value.eq.return_value.execute.return_value = None

        with patch("tools.viral_referral_engine.db.client", mock_db):
            with patch("tools.viral_referral_engine.settings") as mock_settings:
                mock_settings.app_base_url = "https://supremeai.com"
                with patch.object(engine, "_is_fraudulent", return_value=False):
                    with patch.object(engine, "_calculate_reward", return_value={"reward": 10.0, "credit_bonus": 50, "tier": "silver"}):
                        result = await engine.process_signup("new-user-123", "SUPREME-ABC", {})
        assert result["status"] == "success"
        assert result["referrer_id"] == "referrer-1"

    @pytest.mark.anyio
    async def test_is_fraudulent_not_fraudulent(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        engine.generate_referral_code("referrer-1")
        await engine.process_signup("new-user-1", engine.generate_referral_code("referrer-1")["code"], {"ip_address": "1.2.3.4"})
        result = engine._is_fraudulent("referrer-1", "new-user-2", {"ip_address": "5.6.7.8"})
        assert result is False

    @pytest.mark.anyio
    async def test_is_fraudulent_same_ip(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        ip = "1.2.3.4"
        for i in range(FRAUD_INDICATOR_THRESHOLD):
            code = engine.generate_referral_code("referrer-1")["code"]
            await engine.process_signup(f"new-user-{i}", code, {"ip_address": ip})
        result = engine._is_fraudulent("referrer-1", "new-user-new", {"ip_address": ip})
        assert result is True

    @pytest.mark.anyio
    async def test_is_fraudulent_same_device(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        device = "device-123"
        for i in range(FRAUD_INDICATOR_THRESHOLD):
            code = engine.generate_referral_code("referrer-1")["code"]
            await engine.process_signup(f"new-user-{i}", code, {"device_fingerprint": device})
        result = engine._is_fraudulent("referrer-1", "new-user-new", {"device_fingerprint": device})
        assert result is True

    @pytest.mark.anyio
    async def test_is_fraudulent_db(self, engine):
        mock_db = MagicMock()
        mock_table = MagicMock()
        mock_db.table.return_value = mock_table
        mock_table.select.return_value.eq.return_value.execute.return_value = MagicMock(
            data=[{"created_at": time.time(), "metadata": {"ip_address": "1.2.3.4"}}] * FRAUD_INDICATOR_THRESHOLD
        )

        with patch("tools.viral_referral_engine.db.client", mock_db):
            result = engine._is_fraudulent("referrer-1", "new-user", {"ip_address": "1.2.3.4"})
        assert result is True

    @pytest.mark.anyio
    async def test_calculate_reward_local(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        for i in range(55):
            code = engine.generate_referral_code("referrer-1")["code"]
            await engine.process_signup(f"new-user-{i}", code, {})
        reward = engine._calculate_reward("referrer-1")
        assert reward["tier"] == "platinum"

    @pytest.mark.anyio
    async def test_calculate_reward_db(self, engine):
        mock_db = MagicMock()
        mock_table = MagicMock()
        mock_db.table.return_value = mock_table
        mock_table.select.return_value.eq.return_value.execute.return_value = MagicMock(count=55)

        with patch("tools.viral_referral_engine.db.client", mock_db):
            reward = engine._calculate_reward("referrer-1")
        assert reward["tier"] == "platinum"
        assert reward["count"] == 55

    @pytest.mark.anyio
    async def test_calculate_reward_no_count_attr(self, engine):
        mock_db = MagicMock()
        mock_table = MagicMock()
        mock_db.table.return_value = mock_table
        res = MagicMock()
        del res.count
        res.data = [{"id": i} for i in range(55)]
        mock_table.select.return_value.eq.return_value.execute.return_value = res

        with patch("tools.viral_referral_engine.db.client", mock_db):
            reward = engine._calculate_reward("referrer-1")
        assert reward["tier"] == "platinum"
        assert reward["count"] == 55

    @pytest.mark.anyio
    async def test_credit_wallet_local(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        result = engine._credit_wallet("user-1", 10.0, "bonus")
        assert result["amount"] == 10.0
        assert result["balance"] == 10.0
        assert result["tx_id"] is not None

    @pytest.mark.anyio
    async def test_credit_wallet_db(self, engine):
        mock_db = MagicMock()
        mock_table = MagicMock()
        mock_db.table.return_value = mock_table
        mock_table.select.return_value.eq.return_value.execute.return_value = MagicMock(data=[{"user_id": "user-1", "balance": 100.0}])
        mock_table.insert.return_value.execute.return_value = None
        mock_table.upsert.return_value.execute.return_value = None

        with patch("tools.viral_referral_engine.db.client", mock_db):
            result = engine._credit_wallet("user-1", 50.0, "bonus")
        assert result["amount"] == 50.0
        assert result["balance"] == 150.0

    @pytest.mark.anyio
    async def test_get_wallet_local_new(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        wallet = engine._get_wallet("new-user")
        assert wallet["balance"] == 0.0
        assert wallet["user_id"] == "new-user"

    @pytest.mark.anyio
    async def test_get_wallet_local_existing(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        engine._credit_wallet("user-1", 25.0, "initial")
        wallet = engine._get_wallet("user-1")
        assert wallet["balance"] == 25.0

    @pytest.mark.anyio
    async def test_get_wallet_balance(self, engine):
        assert engine.get_wallet_balance("user-1") == {"user_id": "user-1", "balance": 0.0}

    @pytest.mark.anyio
    async def test_get_ledger_local(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        engine._credit_wallet("user-1", 10.0, "tx1")
        engine._credit_wallet("user-1", 20.0, "tx2")
        ledger = engine.get_ledger("user-1")
        assert len(ledger) == 2
        assert ledger[0]["amount"] == 10.0
        assert ledger[1]["amount"] == 20.0

    @pytest.mark.anyio
    async def test_generate_deep_link(self, engine):
        assert "supremeai.com/invite/" in engine.generate_deep_link("CODE-123")

    @pytest.mark.anyio
    async def test_generate_deep_link_twitter(self, engine):
        link = engine.generate_deep_link("CODE-123", "twitter")
        assert "twitter.com/intent/tweet" in link
        assert "CODE-123" in link

    @pytest.mark.anyio
    async def test_generate_deep_link_whatsapp(self, engine):
        link = engine.generate_deep_link("CODE-123", "whatsapp")
        assert "whatsapp.com" in link

    @pytest.mark.anyio
    async def test_generate_deep_link_telegram(self, engine):
        link = engine.generate_deep_link("CODE-123", "telegram")
        assert "t.me/share/url" in link

    @pytest.mark.anyio
    async def test_generate_deep_link_unknown_platform(self, engine):
        link = engine.generate_deep_link("CODE-123", "unknown")
        assert "CODE-123" in link

    @pytest.mark.anyio
    async def test_record_social_share_local(self, engine, tmp_path):
        engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
        result = engine.record_social_share("user-1", "CODE-123", "twitter", {})
        assert result["status"] == "success"
        assert "deep_link" in result

    @pytest.mark.anyio
    async def test_record_social_share_db(self, engine):
        mock_db = MagicMock()
        mock_table = MagicMock()
        mock_db.table.return_value = mock_table
        mock_table.insert.return_value.execute.return_value = None

        with patch("tools.viral_referral_engine.db.client", mock_db):
            result = engine.record_social_share("user-1", "CODE-123", "twitter", {})
        assert result["status"] == "success"

    @pytest.mark.anyio
    async def test_stripe_payout_not_configured(self, engine):
        with patch("tools.viral_referral_engine.settings") as mock_settings:
            mock_settings.stripe_api_key = None
            result = engine._stripe_payout("user-1", 5000)
        assert result["status"] == "skipped"
        assert result["reason"] == "stripe_not_configured"

    @pytest.mark.anyio
    async def test_stripe_payout_success(self, engine):
        mock_stripe = MagicMock()
        mock_payout = MagicMock()
        mock_payout.id = "po_123"
        mock_stripe.Payout.create.return_value = mock_payout

        with patch("tools.viral_referral_engine.settings") as mock_settings, patch.dict("sys.modules", {"stripe": mock_stripe}):
            mock_settings.stripe_api_key = "sk_test_123"
            result = engine._stripe_payout("user-1", 5000)
        assert result["status"] == "success"
        assert result["payout_id"] == "po_123"

    @pytest.mark.anyio
    async def test_stripe_payout_failure(self, engine):
        mock_stripe = MagicMock()
        mock_stripe.Payout.create.side_effect = Exception("Stripe error")

        with patch("tools.viral_referral_engine.settings") as mock_settings, patch.dict("sys.modules", {"stripe": mock_stripe}):
            mock_settings.stripe_api_key = "sk_test_123"
            result = engine._stripe_payout("user-1", 5000)
        assert result["status"] == "error"

    @pytest.mark.anyio
    async def test_credit_stripe_payout_below_threshold(self, engine):
        with (
            patch.object(engine, "_get_wallet", return_value={"user_id": "u1", "balance": 10.0}),
            patch.object(engine, "_credit_wallet", return_value={"balance": 15.0, "amount": 5.0}),
        ):
            result = engine._credit_stripe_payout("u1", {"reward": 5.0})
        assert result["status"] == "credited"

    @pytest.mark.anyio
    async def test_credit_stripe_payout_above_threshold(self, engine):
        with (
            patch.object(engine, "_get_wallet", return_value={"user_id": "u1", "balance": 50.0}),
            patch.object(engine, "_credit_wallet", return_value={"balance": 100.0, "amount": 50.0}),
            patch.object(engine, "_stripe_payout", return_value={"status": "success", "payout_id": "po_123"}) as mock_payout,
        ):
            result = engine._credit_stripe_payout("u1", {"reward": 50.0})
        assert result["status"] == "paid"
        assert result["payout"]["payout_id"] == "po_123"

    @pytest.mark.anyio
    async def test_credit_stripe_payout_stripe_failure(self, engine):
        with (
            patch.object(engine, "_get_wallet", return_value={"user_id": "u1", "balance": 50.0}),
            patch.object(engine, "_credit_wallet", return_value={"balance": 100.0, "amount": 50.0}),
            patch.object(engine, "_stripe_payout", return_value={"status": "error"}),
        ):
            result = engine._credit_stripe_payout("u1", {"reward": 50.0})
        assert result["status"] == "credited"

```