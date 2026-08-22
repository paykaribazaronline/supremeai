"""Cloud storage manager tests for SupremeAI 2.0."""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from services.storage.cloud_storage import CloudStorageManager


class TestCloudStorageManager:
    """Tests for CloudStorageManager class."""

    def test_init_no_credentials(self):
        """ক্রেডেনশিয়াল ছাড়াই ইনিশialization করা হচ্ছে।"""
        with patch("services.storage.cloud_storage.settings") as mock_settings:
            mock_settings.supabase_url = None
            mock_settings.supabase_key = None
            manager = CloudStorageManager()
            assert manager.supabase_url is None
            assert manager.supabase_key is None

    def test_init_with_credentials(self):
        """ক্রেডেনশিয়াল সহ ইনিশialization করা হচ্ছে।"""
        with patch("services.storage.cloud_storage.settings") as mock_settings:
            mock_settings.supabase_url = "https://test.supabase.co"
            mock_settings.supabase_key = "test-key"
            manager = CloudStorageManager()
            assert manager.supabase_url == "https://test.supabase.co"
            assert manager.supabase_key == "test-key"
            assert manager.bucket_name == "supremeai-assets"

    @pytest.mark.anyio
    async def test_upload_file_no_credentials(self):
        """ক্রেডেনশিয়াল ছাড়াই আপলোড রিজেক্স করা হচ্ছে।"""
        from fastapi import HTTPException

        with patch("services.storage.cloud_storage.settings") as mock_settings:
            mock_settings.supabase_url = None
            mock_settings.supabase_key = None
            manager = CloudStorageManager()

            with pytest.raises(HTTPException) as exc_info:
                await manager.upload_file_async("test/path", b"test content")

            assert exc_info.value.status_code == 500

    @pytest.mark.anyio
    async def test_upload_file_success(self):
        """সফল ফাইল আপলোড করা হচ্ছে।"""
        with patch("services.storage.cloud_storage.settings") as mock_settings:
            mock_settings.supabase_url = "https://test.supabase.co"
            mock_settings.supabase_key = "test-key"
            manager = CloudStorageManager()

            mock_response = MagicMock()
            mock_response.status_code = 200

            with patch("httpx.AsyncClient") as mock_client_class:
                mock_client = AsyncMock()
                mock_client.__aenter__ = AsyncMock(return_value=mock_client)
                mock_client.__aexit__ = AsyncMock(return_value=None)
                mock_client.post = AsyncMock(return_value=mock_response)
                mock_client_class.return_value = mock_client

                result = await manager.upload_file_async("test/file.json", b'{"data": "test"}')
                assert "test.file.json" in result or "supabase" in result

    @pytest.mark.anyio
    async def test_upload_file_server_error(self):
        """সার্ভার ত্রুটি হলে আপলোড ব্যর্থ হয়।"""
        from fastapi import HTTPException

        with patch("services.storage.cloud_storage.settings") as mock_settings:
            mock_settings.supabase_url = "https://test.supabase.co"
            mock_settings.supabase_key = "test-key"
            manager = CloudStorageManager()

            mock_response = MagicMock()
            mock_response.status_code = 400
            mock_response.text = "Bad Request"

            with patch("httpx.AsyncClient") as mock_client_class:
                mock_client = AsyncMock()
                mock_client.__aenter__ = AsyncMock(return_value=mock_client)
                mock_client.__aexit__ = AsyncMock(return_value=None)
                mock_client.post = AsyncMock(return_value=mock_response)
                mock_client_class.return_value = mock_client

                with pytest.raises(HTTPException) as exc_info:
                    await manager.upload_file_async("test/file.json", b'{"data": "test"}')

                assert exc_info.value.status_code == 400

    @pytest.mark.anyio
    async def test_upload_file_network_error(self):
        """নেটওয়ার্ক ত্রুটি হলে আপলোড ব্যর্থ হয়।"""
        import httpx
        from fastapi import HTTPException

        with patch("services.storage.cloud_storage.settings") as mock_settings:
            mock_settings.supabase_url = "https://test.supabase.co"
            mock_settings.supabase_key = "test-key"
            manager = CloudStorageManager()

            with patch("httpx.AsyncClient") as mock_client_class:
                mock_client = AsyncMock()
                mock_client.__aenter__ = AsyncMock(return_value=mock_client)
                mock_client.__aexit__ = AsyncMock(return_value=None)
                mock_client.post = AsyncMock(side_effect=httpx.HTTPError("Network error"))
                mock_client_class.return_value = mock_client

                with pytest.raises(HTTPException) as exc_info:
                    await manager.upload_file_async("test/file.json", b'{"data": "test"}')

                assert exc_info.value.status_code == 503
