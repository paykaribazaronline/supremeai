class TestCloudStorageManager:
    def test_init(self):
        from core.cloud_storage import CloudStorageManager
        from core.config import settings
        from unittest.mock import patch

        with patch('core.cloud_storage.settings') as mock_settings:
            mock_settings.supabase_url = "https://example.supabase.co"
            mock_settings.supabase_key = "secret-key"
            storage = CloudStorageManager()
            assert storage.supabase_url == "https://example.supabase.co"
            assert storage.supabase_key == "secret-key"
            assert storage.bucket_name == "supremeai-assets"

    def test_init_missing_credentials(self):
        from core.cloud_storage import CloudStorageManager
        from core.config import settings
        from unittest.mock import patch

        with patch('core.cloud_storage.settings') as mock_settings:
            mock_settings.supabase_url = None
            mock_settings.supabase_key = None
            storage = CloudStorageManager()
            assert storage.supabase_url is None
            assert storage.supabase_key is None

    @pytest.mark.asyncio
    async def test_upload_file_async_missing_credentials(self):
        from core.cloud_storage import CloudStorageManager
        from fastapi import HTTPException, status
        from core.config import settings
        from unittest.mock import patch

        with patch('core.cloud_storage.settings') as mock_settings:
            mock_settings.supabase_url = None
            mock_settings.supabase_key = None
            storage = CloudStorageManager()
            with pytest.raises(HTTPException) as exc_info:
                await storage.upload_file_async("test/path", b"data")
            assert exc_info.value.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
            assert "Cloud storage infrastructure is unconfigured" in str(exc_info.value.detail)

    @pytest.mark.asyncio
    async def test_upload_file_async_success(self):
        from core.cloud_storage import CloudStorageManager
        from core.config import settings
        from unittest.mock import patch, AsyncMock
        import httpx

        with patch('core.cloud_storage.settings') as mock_settings:
            mock_settings.supabase_url = "https://example.supabase.co"
            mock_settings.supabase_key = "secret-key"
            storage = CloudStorageManager()
            # Mock httpx.AsyncClient
            mock_response = AsyncMock()
            mock_response.status_code = 200
            mock_response.text = "OK"
            mock_client = AsyncMock()
            mock_client.__aenter__.return_value.post.return_value = mock_response
            with patch('core.cloud_storage.httpx.AsyncClient', return_value=mock_client):
                url = await storage.upload_file_async("test/file.txt", b"hello world", "text/plain")
                expected_url = "https://example.supabase.co/storage/v1/object/public/supremeai-assets/test/file.txt"
                assert url == expected_url
                # Check that the request was made with the correct parameters
                mock_client.__aenter__.return_value.post.assert_called_once()
                args, kwargs = mock_client.__aenter__.return_value.post.call_args
                assert args[0] == "https://example.supabase.co/storage/v1/object/supremeai-assets/test/file.txt"
                assert kwargs["content"] == b"hello world"
                assert kwargs["headers"]["Authorization"] == "Bearer secret-key"
                assert kwargs["headers"]["API-Key"] == "secret-key"
                assert kwargs["headers"]["Content-Type"] == "text/plain"

    @pytest.mark.asyncio
    async def test_upload_file_async_http_error_400(self):
        from core.cloud_storage import CloudStorageManager
        from fastapi import HTTPException, status
        from core.config import settings
        from unittest.mock import patch, AsyncMock
        import httpx

        with patch('core.cloud_storage.settings') as mock_settings:
            mock_settings.supabase_url = "https://example.supabase.co"
            mock_settings.supabase_key = "secret-key"
            storage = CloudStorageManager()
            # Mock httpx.AsyncClient to return a 400 response
            mock_response = AsyncMock()
            mock_response.status_code = 400
            mock_response.text = "Bad Request"
            mock_client = AsyncMock()
            mock_client.__aenter__.return_value.post.return_value = mock_response
            with patch('core.cloud_storage.httpx.AsyncClient', return_value=mock_client):
                with pytest.raises(HTTPException) as exc_info:
                    await storage.upload_file_async("test/file.txt", b"data")
                assert exc_info.value.status_code == status.HTTP_400_BAD_REQUEST
                assert "Cloud storage engine rejected the asset package" in str(exc_info.value.detail)

    @pytest.mark.asyncio
    async def test_upload_file_async_http_error_503(self):
        from core.cloud_storage import CloudStorageManager
        from fastapi import HTTPException, status
        from core.config import settings
        from unittest.mock import patch, AsyncMock
        import httpx

        with patch('core.cloud_storage.settings') as mock_settings:
            mock_settings.supabase_url = "https://example.supabase.co"
            mock_settings.supabase_key = "secret-key"
            storage = CloudStorageManager()
            # Mock httpx.AsyncClient to raise an HTTPError
            mock_client = AsyncMock()
            mock_client.__aenter__.return_value.post.side_effect = httpx.HTTPError("Network error")
            with patch('core.cloud_storage.httpx.AsyncClient', return_value=mock_client):
                with pytest.raises(HTTPException) as exc_info:
                    await storage.upload_file_async("test/file.txt", b"data")
                assert exc_info.value.status_code == status.HTTP_503_SERVICE_UNAVAILABLE
                assert "Storage cluster network timeout" in str(exc_info.value.detail)