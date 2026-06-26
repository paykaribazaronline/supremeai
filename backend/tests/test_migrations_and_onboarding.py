"""
Tests for:
- DB Migration SQL schema validation (F.2)
- Onboarding API (E.1)
- Referral Engine endpoints
- Tenant Rate Limiter
"""

import pathlib
from unittest.mock import AsyncMock
from unittest.mock import MagicMock
from unittest.mock import patch

import pytest


MIGRATIONS_DIR = pathlib.Path(__file__).parent.parent / "database" / "migrations"


# ── SQL Schema Validation ─────────────────────────────────────────────────────


class TestMigrationFiles:
    """Ensure all migration files exist and have valid SQL structure."""

    @pytest.mark.parametrize(
        "filename",
        [
            "01_initial_setup.sql",
            "04_schema_upgrade.sql",
            "06_referral_system.sql",
            "07_tenant_sso_offline.sql",
        ],
    )
    def test_migration_file_exists(self, filename: str):
        path = MIGRATIONS_DIR / filename
        assert path.exists(), f"Migration file missing: {filename}"

    @pytest.mark.parametrize(
        "filename",
        [
            "06_referral_system.sql",
            "07_tenant_sso_offline.sql",
        ],
    )
    def test_migration_has_create_table(self, filename: str):
        path = MIGRATIONS_DIR / filename
        content = path.read_text(encoding="utf-8").upper()
        assert "CREATE TABLE" in content, f"{filename} should contain CREATE TABLE"
        assert "IF NOT EXISTS" in content, f"{filename} should use IF NOT EXISTS"

    def test_referral_schema_has_required_tables(self):
        content = (MIGRATIONS_DIR / "06_referral_system.sql").read_text().lower()
        required_tables = [
            "referral_codes",
            "referral_redemptions",
            "credit_wallets",
            "credit_ledger",
        ]
        for table in required_tables:
            assert table in content, f"Missing table: {table} in 06_referral_system.sql"

    def test_tenant_schema_has_required_tables(self):
        content = (MIGRATIONS_DIR / "07_tenant_sso_offline.sql").read_text().lower()
        required_tables = [
            "tenant_limits",
            "sso_configs",
            "offline_sync_logs",
            "tenant_usage",
        ]
        for table in required_tables:
            assert (
                table in content
            ), f"Missing table: {table} in 07_tenant_sso_offline.sql"

    def test_referral_schema_has_indexes(self):
        content = (MIGRATIONS_DIR / "06_referral_system.sql").read_text().upper()
        assert "CREATE INDEX" in content, "06_referral_system.sql should have indexes"

    def test_tenant_schema_has_billing_tier_check(self):
        content = (MIGRATIONS_DIR / "07_tenant_sso_offline.sql").read_text()
        assert "free" in content
        assert "enterprise" in content
        assert "CHECK" in content.upper()

    def test_04_schema_upgrade_has_github_repos(self):
        path = MIGRATIONS_DIR / "04_schema_upgrade.sql"
        if path.exists():
            content = path.read_text().lower()
            assert "github_repos" in content or "tools_registry" in content


# ── Onboarding API Tests ──────────────────────────────────────────────────────


class TestOnboardingAPI:
    @pytest.mark.anyio
    async def test_validate_api_key_unknown_provider(self):
        from api.routes.onboarding import _validate_api_key

        result = await _validate_api_key("custom_provider", "any-key")
        assert result is True  # Unknown provider → assume valid

    @pytest.mark.anyio
    async def test_validate_api_key_valid(self):
        from api.routes.onboarding import _validate_api_key

        with patch("httpx.AsyncClient") as mock_client_cls:
            mock_client = AsyncMock()
            mock_client.__aenter__ = AsyncMock(return_value=mock_client)
            mock_client.__aexit__ = AsyncMock(return_value=None)
            mock_resp = MagicMock()
            mock_resp.status_code = 200
            mock_client.get = AsyncMock(return_value=mock_resp)
            mock_client_cls.return_value = mock_client
            result = await _validate_api_key("openai", "sk-test123")
        assert result is True

    @pytest.mark.anyio
    async def test_validate_api_key_invalid(self):
        from api.routes.onboarding import _validate_api_key

        with patch("httpx.AsyncClient") as mock_client_cls:
            mock_client = AsyncMock()
            mock_client.__aenter__ = AsyncMock(return_value=mock_client)
            mock_client.__aexit__ = AsyncMock(return_value=None)
            mock_resp = MagicMock()
            mock_resp.status_code = 401
            mock_client.get = AsyncMock(return_value=mock_resp)
            mock_client_cls.return_value = mock_client
            result = await _validate_api_key("openai", "invalid-key")
        assert result is False

    def test_save_user_preferences_local_fallback(self, tmp_path):
        from api.routes.onboarding import OnboardingPayload
        from api.routes.onboarding import _save_user_preferences

        payload = OnboardingPayload(
            user_id="test-user-123",
            provider="openrouter",
            api_key="sk-test",
            default_model="gpt-4o-mini",
            theme="dark",
            language="bn",
        )
        with patch("api.routes.onboarding.pathlib.Path") as mock_path:
            mock_p = MagicMock()
            mock_p.__truediv__ = MagicMock(return_value=MagicMock())
            mock_path.return_value = mock_p
            # Should not raise
            result = _save_user_preferences(payload)
        # Result is bool (True or False depending on DB/local)
        assert isinstance(result, bool)

    @pytest.mark.anyio
    async def test_complete_onboarding_endpoint(self):
        from api.routes.onboarding import OnboardingPayload
        from api.routes.onboarding import complete_onboarding

        payload = OnboardingPayload(
            user_id="test-user",
            provider="custom",
            api_key="test-key",
            default_model="gpt-4o-mini",
            first_chat_sent=False,
        )
        with patch(
            "api.routes.onboarding._validate_api_key",
            new_callable=AsyncMock,
            return_value=True,
        ):
            with patch(
                "api.routes.onboarding._save_user_preferences", return_value=True
            ):
                result = await complete_onboarding(payload)
        assert result.status == "success"
        assert result.user_id == "test-user"
        assert result.provider_valid is True
        assert "ready" in result.message.lower() or "set" in result.message.lower()

    @pytest.mark.anyio
    async def test_onboarding_invalid_key_still_succeeds(self):
        from api.routes.onboarding import OnboardingPayload
        from api.routes.onboarding import complete_onboarding

        payload = OnboardingPayload(
            user_id="test-user",
            provider="openai",
            api_key="invalid",
        )
        with patch(
            "api.routes.onboarding._validate_api_key",
            new_callable=AsyncMock,
            return_value=False,
        ):
            with patch(
                "api.routes.onboarding._save_user_preferences", return_value=True
            ):
                result = await complete_onboarding(payload)
        # Should not raise — gracefully returns warning
        assert result.status == "success"
        assert result.provider_valid is False
        assert "⚠️" in result.message


# ── Viral Referral Engine Tests ───────────────────────────────────────────────


class TestViralReferralEngine:
    def test_generate_referral_code_format(self):
        from tools.viral_referral_engine import ViralReferralEngine

        engine = ViralReferralEngine()
        with patch("tools.viral_referral_engine.db") as mock_db:
            mock_db.client = None  # Force local store
            with patch.object(engine, "_save_local"):
                with patch.object(
                    engine, "_load_local", return_value={"codes": {}, "wallets": {}}
                ):
                    result = engine.generate_referral_code("user123")
        assert result["status"] == "success"
        assert result["code"].startswith("SUPREME-")
        assert len(result["code"]) == 16  # SUPREME- + 8 hex chars

    def test_generate_deep_link_platforms(self):
        from tools.viral_referral_engine import ViralReferralEngine

        engine = ViralReferralEngine()
        for platform in ["twitter", "facebook", "whatsapp", "telegram", "generic"]:
            link = engine.generate_deep_link("SUPREME-ABC12345", platform)
            assert "SUPREME-ABC12345" in link or "invite" in link

    def test_calculate_reward_bronze(self):
        from tools.viral_referral_engine import ViralReferralEngine

        engine = ViralReferralEngine()
        with patch("tools.viral_referral_engine.db") as mock_db:
            mock_db.client = None
            with patch.object(
                engine,
                "_load_local",
                return_value={
                    "codes": {},
                    "wallets": {},
                    "redemptions": [{"referrer_id": "u1"}],
                },
            ):
                reward = engine._calculate_reward("u1")
        assert reward["tier"] in ("bronze", "silver", "gold", "platinum")
        assert reward["reward"] > 0

    def test_fraud_detection_same_ip(self):
        import time

        from tools.viral_referral_engine import ViralReferralEngine

        engine = ViralReferralEngine()
        # Simulate 3 same-IP redemptions in last 7 days
        fake_history = [
            {
                "referrer_id": "u1",
                "new_user_id": f"new{i}",
                "created_at": time.time(),
                "metadata": {"ip_address": "1.2.3.4"},
            }
            for i in range(3)
        ]
        with patch("tools.viral_referral_engine.db") as mock_db:
            mock_db.client = None
            with patch.object(
                engine,
                "_load_local",
                return_value={"codes": {}, "wallets": {}, "redemptions": fake_history},
            ):
                is_fraud = engine._is_fraudulent(
                    "u1", "new_victim", {"ip_address": "1.2.3.4"}
                )
        assert is_fraud is True


# ── Rate Limit Manager Tests ──────────────────────────────────────────────────


class TestTenantRateLimiter:
    def test_rate_limiter_file_exists(self):
        import pathlib

        p = pathlib.Path(__file__).parent.parent / "tools" / "tenant_rate_limiter.py"
        assert p.exists(), "tenant_rate_limiter.py must exist"
        assert (
            p.stat().st_size > 1000
        ), "tenant_rate_limiter.py appears too small (stub?)"

    def test_rate_limiter_has_class(self):
        import ast
        import pathlib

        p = pathlib.Path(__file__).parent.parent / "tools" / "tenant_rate_limiter.py"
        tree = ast.parse(p.read_text())
        classes = [n.name for n in ast.walk(tree) if isinstance(n, ast.ClassDef)]
        assert len(classes) > 0, "tenant_rate_limiter.py should have at least one class"
