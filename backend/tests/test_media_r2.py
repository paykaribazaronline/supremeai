from unittest.mock import patch, MagicMock
from storage.r2_storage_client import R2StorageClient

# বাংলা মন্তব্য: ক্লাউডফ্লেয়ার R2 এর প্রে-সাইনড ইউআরএল জেনারেট করার লজিক টেস্ট করা হচ্ছে।

def test_r2_client_mock_fallback():
    # বাংলা মন্তব্য: যদি ক্রেডেনশিয়াল না থাকে, ক্লায়েন্টটি যেন সঠিক মক ইউআরএল জেনারেট করতে পারে তা যাচাই করা হচ্ছে।
    with patch.dict("os.environ", {}, clear=True):
        client = R2StorageClient()
        assert client.dry_run is True
        
        upload_url = client.generate_presigned_upload_url("test_file.txt", "text/plain")
        assert "mock-r2-upload.local" in upload_url
        assert "test_file.txt" in upload_url
        
        download_url = client.generate_presigned_download_url("test_file.txt")
        assert "mock-r2-download.local" in download_url


def test_r2_client_generate_presigned_url():
    # বাংলা মন্তব্য: যদি ক্রেডেনশিয়াল থাকে, তাহলে boto3 সাকসেসফুলি প্রে-সাইনড ইউআরএল তৈরি করছে কিনা তা যাচাইয়ের টেস্ট।
    env_vars = {
        "R2_ACCOUNT_ID": "mock_account_id",
        "R2_ACCESS_KEY": "mock_access_key",
        "R2_SECRET_KEY": "mock_secret_key",
        "R2_BUCKET_NAME": "mock-bucket"
    }
    
    with patch.dict("os.environ", env_vars):
        with patch("boto3.client") as mock_boto:
            mock_s3 = MagicMock()
            mock_s3.generate_presigned_url.return_value = "https://r2-real-url.com/mock-bucket/test_file.txt"
            mock_boto.return_value = mock_s3
            
            client = R2StorageClient()
            assert client.dry_run is False
            
            url = client.generate_presigned_upload_url("test_file.txt", "text/plain")
            assert url == "https://r2-real-url.com/mock-bucket/test_file.txt"
            mock_s3.generate_presigned_url.assert_called_once()


def test_media_route_generate_upload_url():
    # বাংলা মন্তব্য: FastAPI টেস্ট ক্লায়েন্ট ব্যবহার করে সরাসরি এপিআই এন্ডপয়েন্ট টেস্ট করা হচ্ছে।
    from fastapi.testclient import TestClient
    from core.app import app
    
    with TestClient(app) as test_client:
        payload = {
            "file_name": "skills_bundle.zip",
            "file_type": "application/zip",
            "folder": "test_folder"
        }
        # যেহেতু এটি রিকোয়ার্ড ডিপেন্ডেন্সি ছাড়া মক ইউজার ব্যবহার করে, তাই অথরাইজেশন চেক অটোমেটিক পাস হবে।
        response = test_client.post("/api/v1/media/generate-upload-url", json=payload)
        assert response.status_code == 200
        data = response.json()
        assert "upload_url" in data
        assert "file_path" in data
        assert "skills_bundle.zip" in data["file_path"]
