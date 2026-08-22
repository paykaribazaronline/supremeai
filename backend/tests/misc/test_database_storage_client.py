import os

import pytest

from database.storage_client import StorageClient


def test_storage_client_defaults():
    client = StorageClient()
    assert client.provider == "supabase"
    assert client.bucket_name == os.getenv("STORAGE_BUCKET", "supremeai-assets")


def test_storage_client_s3_init(monkeypatch):
    monkeypatch.setenv("STORAGE_BUCKET", "my-bucket")
    client = StorageClient(provider="s3")
    assert client.provider == "s3"
    assert client.bucket_name == "my-bucket"


def test_storage_client_supabase_init_with_env(monkeypatch):
    monkeypatch.setenv("SUPABASE_URL", "http://example.com")
    monkeypatch.setenv("SUPABASE_KEY", "key")
    client = StorageClient(provider="supabase")
    assert client.provider == "supabase"
    assert client.supabase_client is not None


def test_upload_file_raises_when_missing(tmp_path):
    client = StorageClient(provider="supabase")
    with pytest.raises(FileNotFoundError):
        client.upload_file(str(tmp_path / "nope.txt"), "remote.txt")


def test_get_public_url_supabase(monkeypatch):
    monkeypatch.setenv("SUPABASE_URL", "http://example.com")
    monkeypatch.setenv("SUPABASE_KEY", "key")
    client = StorageClient(provider="supabase")

    class FakeStorage:
        def from_(self, bucket):
            class FakeFrom:
                def get_public_url(self, path):
                    return f"https://cdn.supabase.co/{bucket}/{path}"

            return FakeFrom()

    client.supabase_client = type("SC", (), {"storage": FakeStorage()})()
    url = client.get_public_url("file.png")
    assert "file.png" in url


def test_get_public_url_s3(monkeypatch):
    monkeypatch.setenv("AWS_REGION", "us-west-2")
    client = StorageClient(provider="s3")
    client.s3_client = True
    url = client.get_public_url("file.png")
    assert url == "https://supremeai-assets.s3.us-west-2.amazonaws.com/file.png"


def test_get_public_url_fallback():
    client = StorageClient(provider="unknown")
    client.supabase_client = None
    client.s3_client = None
    url = client.get_public_url("file.png")
    assert url == "https://cdn.supremeai.example/file.png"
