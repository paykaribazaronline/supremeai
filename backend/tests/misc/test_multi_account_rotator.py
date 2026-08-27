import json
import os
from datetime import timedelta
from unittest.mock import AsyncMock, patch

import pytest

from core.utils.time_utils import utc_now
from tools.security_tools.multi_account_rotator import (
    Account,
    MultiAccountRotator,
    Provider,
    ProviderStatus,
    TaskType,
)


# বাংলা মন্তব্য: rotator ইনস্ট্যান্স তৈরি করার জন্য ফিক্সচার
@pytest.fixture
def rotator(tmp_path):
    """ক tasaful rotator ইনস্ট্যান্স với tmp_path config ফাইল।"""
    config_file = str(tmp_path / "rotation_config.json")
    rotator = MultiAccountRotator(config_file=config_file)
    rotator.providers.clear()
    rotator.task_preferences.clear()
    return rotator


@pytest.fixture
def sample_provider():
    """নির্দিষ্ট ডাটা সহ Provider ফিক্সচার।"""
    acc1 = Account(
        id="acc-1",
        provider="groq",
        email="test1@example.com",
        status=ProviderStatus.ACTIVE,
        quota_used=200,
        quota_limit=1000,
    )
    acc2 = Account(
        id="acc-2",
        provider="groq",
        email="test2@example.com",
        status=ProviderStatus.ACTIVE,
        quota_used=900,
        quota_limit=1000,
    )
    return Provider(
        name="groq",
        base_url="https://api.groq.com",
        models=["llama3-70b-8192"],
        rate_limit_rpm=60,
        rate_limit_tpm=1000000,
        accounts=[acc1, acc2],
    )


class TestAccount:
    """Account ডাটাক্লাসের টেস্টস।"""

    def test_is_available_when_active_and_under_quota(self):
        """সক্রিয় অ্যাকাউন্ট কোটা সীমার নিচে থাকলে available হবে।"""
        acc = Account(
            id="a1",
            provider="groq",
            email="a@b.com",
            status=ProviderStatus.ACTIVE,
            quota_used=100,
            quota_limit=1000,
        )
        assert acc.is_available() is True

    def test_is_available_when_inactive(self):
        """ইনঅ্যাক্টিভ অ্যাকাউন্ট available નહોય."""
        acc = Account(
            id="a2",
            provider="groq",
            email="a@b.com",
            status=ProviderStatus.INACTIVE,
        )
        assert acc.is_available() is False

    def test_is_available_when_quota_exhausted(self):
        """কোটা পূর্ণ হলে available નહોય."""
        acc = Account(
            id="a3",
            provider="groq",
            email="a@b.com",
            status=ProviderStatus.ACTIVE,
            quota_used=1000,
            quota_limit=1000,
        )
        assert acc.is_available() is False

    def test_is_available_when_rate_limited(self):
        """রেট লিমিটের reset_time এর আগে available નહોય."""
        acc = Account(
            id="a4",
            provider="groq",
            email="a@b.com",
            status=ProviderStatus.ACTIVE,
            reset_time=utc_now() + timedelta(minutes=1),
        )
        assert acc.is_available() is False

    def test_get_health_score_no_requests(self):
        """কোনো রিকোয়েস্ট নাও থাকলে স্কোর 100."""
        acc = Account(
            id="a5",
            provider="groq",
            email="a@b.com",
            quota_used=0,
            quota_limit=1000,
        )
        assert acc.get_health_score() == 100.0

    def test_get_health_score_penalizes_errors_and_quota(self):
        """এরর ate এবং কোটা ব্যবহার স্কোর কমায়."""
        acc = Account(
            id="a6",
            provider="groq",
            email="a@b.com",
            total_requests=100,
            failed_requests=50,
            quota_used=800,
            quota_limit=1000,
            rate_limit_hits=2,
        )
        score = acc.get_health_score()
        assert 0.0 <= score <= 100.0
        assert score < 100.0

    def test_record_request_increments_counters(self):
        """রিকোয়েস্ট রেকর্ড করার পর total_requests বাড়ে ও last_used সেট হয়."""
        acc = Account(
            id="a7",
            provider="groq",
            email="a@b.com",
            total_requests=0,
        )
        before = utc_now()
        acc.record_request(success=True)
        assert acc.total_requests == 1
        assert acc.last_used >= before

    def test_record_request_failure_increments_failed(self):
        """ব্যর্থ রিকোয়েস্ট failed_requests বাড়ায়."""
        acc = Account(
            id="a8",
            provider="groq",
            email="a@b.com",
            total_requests=0,
            failed_requests=0,
        )
        acc.record_request(success=False)
        assert acc.total_requests == 1
        assert acc.failed_requests == 1

    def test_record_rate_limit_sets_reset_time(self):
        """রেট লিমিট রেকর্ড হলে reset_time ১ মিনিট পরে সেট হয়."""
        acc = Account(
            id="a9",
            provider="groq",
            email="a@b.com",
            rate_limit_hits=0,
        )
        before = utc_now()
        acc.record_rate_limit()
        assert acc.rate_limit_hits == 1
        assert acc.reset_time >= before + timedelta(minutes=1)


class TestProvider:
    """Provider ডাটাক্লাসের টেস্টস।"""

    def test_get_available_accounts_filters_inactive(self, sample_provider):
        """get_available_accounts শুধুমাত্র active অ্যাকাউন্ট ফেরত দেয়."""
        sample_provider.accounts[0].status = ProviderStatus.INACTIVE
        available = sample_provider.get_available_accounts()
        assert len(available) == 1
        assert available[0].id == "acc-2"

    def test_get_best_account_returns_highest_health_score(self, sample_provider):
        """get_best_account সবচেয়ে ভালো হেলথ স্কোর বিশিষ্ট অ্যাকাউন্ট দেয়."""
        sample_provider.accounts[0].quota_used = 100
        sample_provider.accounts[1].quota_used = 900
        best = sample_provider.get_best_account()
        assert best is not None
        assert best.id == "acc-1"

    def test_get_best_account_returns_none_when_none_available(self):
        """available অ্যাকাউন্ট নেই álom None ফেরত দেয়."""
        provider = Provider(
            name="groq",
            base_url="https://api.groq.com",
            models=["llama3-70b-8192"],
            rate_limit_rpm=60,
            rate_limit_tpm=1000000,
            accounts=[],
        )
        assert provider.get_best_account() is None

    def test_add_account_appends_to_list(self, sample_provider):
        """add_account নতুন অ্যাকাউন্ট লিস্টে যোগ করে."""
        new_acc = Account(id="acc-3", provider="groq", email="new@example.com")
        sample_provider.add_account(new_acc)
        assert len(sample_provider.accounts) == 3
        assert new_acc in sample_provider.accounts


class TestMultiAccountRotator:
    """MultiAccountRotator ক্লাসের টেস্টস।"""

    def test_constructor_initializes_empty_state(self, rotator):
        """কনসট্রাকশন প্রোভাইডার ও টাস্ক প্রেফারেন্স शূন्य 상태 দেয়।"""
        assert rotator.providers == {}
        assert rotator.task_preferences == {}
        assert os.path.isfile(rotator.config_file)

    def test_constructor_creates_default_config_when_missing(self, tmp_path):
        """কনফিগ ফাইল না থাকলে ডিফল্ট সিকেলেটন তৈরি হয়।"""
        config_file = str(tmp_path / "new_rotator_config.json")
        assert not os.path.exists(config_file)
        MultiAccountRotator(config_file=config_file)
        assert os.path.exists(config_file)
        with open(config_file) as f:
            data = json.load(f)
        assert "providers" in data
        assert "task_preferences" in data

    def test_meets_requirements_cost_pass(self, rotator):
        """কস্ট রিকোয়্যারমেন্ট মেট হলে true ফেরত দেয়।"""
        provider = Provider(
            name="groq",
            base_url="https://api.groq.com",
            models=["llama3-70b-8192"],
            rate_limit_rpm=60,
            rate_limit_tpm=1000000,
            cost_per_token=0.0001,
        )
        acc = Account(id="a1", provider="groq", email="a@b.com")
        reqs = {"max_cost_per_token": 0.001}
        assert rotator._meets_requirements(provider, acc, reqs) is True

    def test_meets_requirements_cost_fail(self, rotator):
        """কস্ট রিকোয়্যারমেন্ট মিট না করলে false ফেরত দেয়।"""
        provider = Provider(
            name="groq",
            base_url="https://api.groq.com",
            models=["llama3-70b-8192"],
            rate_limit_rpm=60,
            rate_limit_tpm=1000000,
            cost_per_token=0.01,
        )
        acc = Account(id="a1", provider="groq", email="a@b.com")
        reqs = {"max_cost_per_token": 0.001}
        assert rotator._meets_requirements(provider, acc, reqs) is False

    def test_meets_requirements_model_pass(self, rotator):
        """প্রয়োজন মডেল প্রোভাইডারের মডেল লিস্টে থাকলে true।"""
        provider = Provider(
            name="groq",
            base_url="https://api.groq.com",
            models=["llama3-70b-8192"],
            rate_limit_rpm=60,
            rate_limit_tpm=1000000,
        )
        acc = Account(id="a1", provider="groq", email="a@b.com")
        reqs = {"required_model": "llama3-70b-8192"}
        assert rotator._meets_requirements(provider, acc, reqs) is True

    def test_meets_requirements_model_fail(self, rotator):
        """প্রয়োজন মডেল না থাকলে false।"""
        provider = Provider(
            name="groq",
            base_url="https://api.groq.com",
            models=["llama3-70b-8192"],
            rate_limit_rpm=60,
            rate_limit_tpm=1000000,
        )
        acc = Account(id="a1", provider="groq", email="a@b.com")
        reqs = {"required_model": "gpt-4"}
        assert rotator._meets_requirements(provider, acc, reqs) is False

    def test_meets_requirements_speed_priority(self, rotator):
        """স্পিড প্রায়োরিটি > ০.৮ হলে RPM < ৩০ হলে false."""
        provider = Provider(
            name="slow",
            base_url="https://api.slow.com",
            models=["slow-model"],
            rate_limit_rpm=10,
            rate_limit_tpm=100000,
        )
        acc = Account(id="a1", provider="slow", email="a@b.com")
        reqs = {"speed_priority": 0.9}
        assert rotator._meets_requirements(provider, acc, reqs) is False

    def test_get_system_status_empty(self, rotator):
        """প্রোভাইডার না থাকলে সিস্টেম স্ট্যাটাস শূন্যদ perpetual."""
        status = rotator.get_system_status()
        assert status["total_providers"] == 0
        assert status["total_accounts"] == 0
        assert status["active_accounts"] == 0
        assert status["system_health"] == 0.0

    def test_get_system_status_aggregates_data(self, rotator, sample_provider):
        """সিস্টেম স্ট্যাটাস সঠিকভাবে অ্যাগ্রিগেট করে।"""
        rotator.providers["groq"] = sample_provider
        status = rotator.get_system_status()
        assert status["total_providers"] == 1
        assert status["total_accounts"] == 2
        assert "active_accounts" in status
        assert "system_health" in status
        assert "providers" in status
        assert "groq" in status["providers"]

    @pytest.mark.anyio
    async def test_call_api_returns_provider_specific_response(self, rotator):
        """_call_api প্রোভাইডার অনুযায়ী আলাদা উত্তর দেয়।"""
        provider = Provider(
            name="deepseek",
            base_url="https://api.deepseek.com",
            models=["deepseek-coder"],
            rate_limit_rpm=100,
            rate_limit_tpm=5000000,
        )
        acc = Account(id="a1", provider="deepseek", email="a@b.com")
        # বাংলা মন্তব্য: রিয়েল নেটওয়ার্ক কল এড়াতে LLMGateway.acompletion মক করা হলো।
        with patch("core.llm.llm_gateway.LLMGateway.acompletion", new_callable=AsyncMock) as mock_acompletion:
            mock_acompletion.return_value = {
                "success": True,
                "text": "DeepSeek analysis: test response",
            }
            result = await rotator._call_api(provider, acc, "test prompt")
            assert "DeepSeek analysis" in result
            mock_acompletion.assert_called_once()

    @pytest.mark.anyio
    async def test_get_best_provider_for_task_with_preferences(self, rotator):
        """টাস্ক প্রেফারেন্স অনুযায়ী সঠিক প্রোভাইডার বেছে নেয়।"""
        rotator.providers["groq"] = Provider(
            name="groq",
            base_url="https://api.groq.com",
            models=["llama3-70b-8192"],
            rate_limit_rpm=60,
            rate_limit_tpm=1000000,
            accounts=[
                Account(
                    id="a1",
                    provider="groq",
                    email="a@b.com",
                    status=ProviderStatus.ACTIVE,
                )
            ],
        )
        rotator.task_preferences = {"coding": ["groq"]}
        result = rotator.get_best_provider_for_task(TaskType.CODING, {})
        assert result is not None
        provider, _account = result
        assert provider.name == "groq"

    def test_get_best_provider_for_task_no_match(self, rotator):
        """কোনো প্রোভাইডার মিল না থাকলে None ফেরত দেয়।"""
        result = rotator.get_best_provider_for_task(TaskType.CODING, {})
        assert result is None

    def test_add_account_creates_provider_if_missing(self, rotator):
        """প্রোভাইডার না থাকলে add_account এটি তৈরি করে।"""
        rotator.add_account("groq", "new@example.com", "gsk_test")
        assert "groq" in rotator.providers

    def test_load_providers_from_config_deserializes_status(self, rotator, tmp_path):
        """কনফিগ থেকে লোড করার সময় স্ট্যাটাস স্ট্রিং থেকে এনামে রূপান্তর হয়।"""
        config_data = {
            "providers": [
                {
                    "name": "groq",
                    "base_url": "https://api.groq.com",
                    "models": ["llama3-70b-8192"],
                    "rate_limit_rpm": 60,
                    "rate_limit_tpm": 1000000,
                    "status": "active",
                    "accounts": [
                        {
                            "id": "acc-1",
                            "provider": "groq",
                            "email": "a@b.com",
                            "status": "active",
                        }
                    ],
                }
            ],
            "task_preferences": {},
        }
        config_file = str(tmp_path / "rotation_config.json")
        with open(config_file, "w") as f:
            json.dump(config_data, f)
        rotator2 = MultiAccountRotator(config_file=config_file)
        assert "groq" in rotator2.providers
        assert rotator2.providers["groq"].status == ProviderStatus.ACTIVE
        accounts = rotator2.providers["groq"].accounts
        assert len(accounts) == 1
        assert accounts[0]["status"] == ProviderStatus.ACTIVE

    def test_save_config_writes_json_file(self, rotator, tmp_path):
        """save_config ডাটা JSON ফাইলে সেভ করে।"""
        rotator.providers["groq"] = Provider(
            name="groq",
            base_url="https://api.groq.com",
            models=["llama3-70b-8192"],
            rate_limit_rpm=60,
            rate_limit_tpm=1000000,
            accounts=[
                Account(
                    id="acc-1",
                    provider="groq",
                    email="a@b.com",
                    status=ProviderStatus.ACTIVE,
                )
            ],
        )
        rotator.save_config()
        with open(rotator.config_file) as f:
            data = json.load(f)
        assert len(data["providers"]) == 1
        assert data["providers"][0]["name"] == "groq"
        assert data["providers"][0]["status"] == "active"

    def test_load_config_handles_missing_file(self, tmp_path):
        """কনফিগ ফাইল মিসিং থাকলে _create_default_config কল হয়।"""
        config_file = str(tmp_path / "missing.json")
        assert not os.path.exists(config_file)
        with patch.object(MultiAccountRotator, "_create_default_config") as mock_default:
            MultiAccountRotator(config_file=config_file)
            mock_default.assert_called_once()
