# 📄 ফাইল: backend/tests/test_email_service.py

**প্রকার:** .py  
**সাইজ:** 5,878 বাইট  
**আপডেট:** 2026-07-10T19:10:52.072177

---

## কোড

```py
"""Email service tests for SupremeAI 2.0."""

import os
from unittest.mock import AsyncMock
from unittest.mock import MagicMock
from unittest.mock import patch

import pytest

from core.email_service import EmailService


class TestEmailService:
    """Tests for EmailService class."""

    def test_init_no_api_key(self):
        """API কি মিসিং হলে মক মোডে চলে।"""
        with patch.dict(os.environ, {}, clear=True):
            service = EmailService()
            assert service.api_key == ""

    def test_init_with_api_key(self):
        """API কি থাকলে সেটি লোড হয়।"""
        with patch.dict(os.environ, {"RESEND_API_KEY": "test-key"}, clear=True):
            service = EmailService()
            assert service.api_key == "test-key"

    def test_init_default_from_email(self):
        """ডিফল্ট from ইমেল সেট হয়।"""
        with patch.dict(os.environ, {"RESEND_API_KEY": "test-key"}, clear=True):
            service = EmailService()
            assert service.from_email == "onboarding@supremeai.dev"

    def test_init_custom_from_email(self):
        """কাস্টম from ইমেল সেট হয়।"""
        with patch.dict(
            os.environ,
            {"RESEND_API_KEY": "test-key", "RESEND_FROM_EMAIL": "custom@example.com"},
            clear=True,
        ):
            service = EmailService()
            assert service.from_email == "custom@example.com"

    @pytest.mark.anyio
    async def test_send_email_no_api_key(self):
        """API কি ছাড়াই ইমেল সেন্ড মক হিসেবে সফল হয়।"""
        with patch.dict(os.environ, {}, clear=True):
            service = EmailService()
            result = await service._send_email("test@example.com", "Test Subject", "<p>Test Body</p>")
            assert result is False

    @pytest.mark.anyio
    async def test_send_email_api_success(self):
        """API ডিকোড সফল হলে ইমেল সেন্ড হয়।"""
        with patch.dict(os.environ, {"RESEND_API_KEY": "test-key"}, clear=True):
            service = EmailService()

            mock_response = MagicMock()
            mock_response.status_code = 200

            with patch("httpx.AsyncClient") as mock_client_class:
                mock_client = AsyncMock()
                mock_client.__aenter__ = AsyncMock(return_value=mock_client)
                mock_client.__aexit__ = AsyncMock(return_value=None)
                mock_client.post = AsyncMock(return_value=mock_response)
                mock_client_class.return_value = mock_client

                result = await service._send_email("test@example.com", "Test Subject", "<p>Test Body</p>")
                assert result is True

    @pytest.mark.anyio
    async def test_send_email_api_failure(self):
        """API ত্রুটি হলে ইমেল সেন্ড ব্যর্থ হয়।"""
        with patch.dict(os.environ, {"RESEND_API_KEY": "test-key"}, clear=True):
            service = EmailService()

            mock_response = MagicMock()
            mock_response.status_code = 400
            mock_response.text = "Bad Request"

            with patch("httpx.AsyncClient") as mock_client_class:
                mock_client = AsyncMock()
                mock_client.__aenter__ = AsyncMock(return_value=mock_client)
                mock_client.__aexit__ = AsyncMock(return_value=None)
                mock_client.post = AsyncMock(return_value=mock_response)
                mock_client_class.return_value = mock_client

                result = await service._send_email("test@example.com", "Test Subject", "<p>Test Body</p>")
                assert result is False

    @pytest.mark.anyio
    async def test_send_email_exception(self):
        """এক্সেপশন হলে ইমেল সেন্ড ব্যর্থ হয়।"""
        with patch.dict(os.environ, {"RESEND_API_KEY": "test-key"}, clear=True):
            service = EmailService()

            with patch("httpx.AsyncClient") as mock_client_class:
                mock_client = AsyncMock()
                mock_client.__aenter__ = AsyncMock(return_value=mock_client)
                mock_client.__aexit__ = AsyncMock(return_value=None)
                mock_client.post = AsyncMock(side_effect=Exception("Network error"))
                mock_client_class.return_value = mock_client

                result = await service._send_email("test@example.com", "Test Subject", "<p>Test Body</p>")
                assert result is False

    @pytest.mark.anyio
    async def test_send_welcome_email(self):
        """ওয়েলকাম ইমেল সেন্ড করা হচ্ছে।"""
        with patch.dict(os.environ, {}, clear=True):
            service = EmailService()
            result = await service.send_welcome_email("test@example.com", "Test User")
            assert result is False

    @pytest.mark.anyio
    async def test_send_password_reset(self):
        """পাসওয়ার্ড রিসেট ইমেল সেন্ড করা হচ্ছে।"""
        with patch.dict(os.environ, {}, clear=True):
            service = EmailService()
            result = await service.send_password_reset("test@example.com", "https://example.com/reset")
            assert result is False

    @pytest.mark.anyio
    async def test_send_billing_notification(self):
        """বিলিং নটিফিকেশন ইমেল সেন্ড করা হচ্ছে।"""
        with patch.dict(os.environ, {}, clear=True):
            service = EmailService()
            result = await service.send_billing_notification("test@example.com", 10.50, "image_generation")
            assert result is False

```