"""
Cache interface and local filesystem cache implementation
"""
import hashlib
import json
from pathlib import Path
from typing import Optional, Dict, Any
import tempfile
import shutil


class CacheInterface:
    """Abstract cache interface"""
    
    def get(self, key: str) -> Optional[Dict[str, Any]]:
        raise NotImplementedError
        
    def set(self, key: str, data: Dict[str, Any]) -> None:
        raise NotImplementedError
        
    def delete(self, key: str) -> None:
        raise NotImplementedError
        
    def clear(self) -> None:
        raise NotImplementedError


class LocalCache(CacheInterface):
    """Local filesystem cache (for development/stub)"""
    
    def __init__(self, cache_dir: Optional[str] = None):
        if cache_dir:
            self.cache_dir = Path(cache_dir)
        else:
            self.cache_dir = Path(tempfile.gettempdir()) / "gitingest_cache"
        self.cache_dir.mkdir(parents=True, exist_ok=True)
    
    def get(self, key: str) -> Optional[Dict[str, Any]]:
        """Get cached data by key"""
        cache_file = self._get_cache_path(key)
        
        if not cache_file.exists():
            return None
        
        try:
            content = cache_file.read_text(encoding='utf-8')
            return json.loads(content)
        except Exception:
            return None
    
    def set(self, key: str, data: Dict[str, Any]) -> None:
        """Store data in cache"""
        cache_file = self._get_cache_path(key)
        try:
            content = json.dumps(data)
            cache_file.write_text(content, encoding='utf-8')
        except Exception:
            pass  # Fail silently for cache
    
    def delete(self, key: str) -> None:
        """Delete cached data"""
        cache_file = self._get_cache_path(key)
        if cache_file.exists():
            cache_file.unlink()
    
    def clear(self) -> None:
        """Clear all cached data"""
        if self.cache_dir.exists():
            shutil.rmtree(self.cache_dir)
            self.cache_dir.mkdir(parents=True, exist_ok=True)
    
    def _get_cache_path(self, key: str) -> Path:
        """Get path for cache key"""
        # Use hash of key for filename
        key_hash = hashlib.sha256(key.encode()).hexdigest()[:32]
        return self.cache_dir / f"{key_hash}.json"


class S3Cache(CacheInterface):
    """S3/MinIO cache implementation"""
    
    def __init__(self, settings):
        self.enabled = settings.S3_ENABLED
        self.bucket = settings.S3_BUCKET_NAME
        
        if not self.enabled:
            self.client = None
            return
        
        try:
            import boto3
            from botocore.exceptions import ClientError, NoCredentialsError
            
            self.client = boto3.client(
                "s3",
                endpoint_url=settings.S3_ENDPOINT,
                aws_access_key_id=settings.S3_ACCESS_KEY,
                aws_secret_access_key=settings.S3_SECRET_KEY,
                use_ssl=settings.S3_USE_SSL,
                verify=False if not settings.S3_USE_SSL else None,
            )
            
            # Test connection and create bucket if needed
            self._ensure_bucket()
            
        except ImportError:
            print("Warning: boto3 not installed. S3 cache disabled.")
            self.client = None
            self.enabled = False
        except Exception as e:
            print(f"Warning: S3 cache initialization failed: {e}")
            self.client = None
            self.enabled = False
    
    def _ensure_bucket(self) -> None:
        """Ensure S3 bucket exists"""
        if not self.client:
            return
        
        try:
            self.client.head_bucket(Bucket=self.bucket)
        except Exception:
            try:
                self.client.create_bucket(Bucket=self.bucket)
            except Exception:
                pass
    
    def get(self, key: str) -> Optional[Dict[str, Any]]:
        """Get cached data from S3"""
        if not self.enabled or not self.client:
            return None
        
        try:
            import json
            
            # Get metadata (JSON)
            response = self.client.get_object(
                Bucket=self.bucket,
                Key=f"{key}.json",
            )
            metadata = json.loads(response['Body'].read())
            
            # Get content (text)
            response = self.client.get_object(
                Bucket=self.bucket,
                Key=f"{key}.txt",
            )
            content = response['Body'].read().decode('utf-8')
            
            return {**metadata, "content": content}
            
        except Exception:
            return None
    
    def set(self, key: str, data: Dict[str, Any]) -> None:
        """Store data in S3"""
        if not self.enabled or not self.client:
            return
        
        try:
            import json
            
            # Separate content from metadata
            content = data.pop("content", "")
            
            # Store metadata (JSON)
            self.client.put_object(
                Bucket=self.bucket,
                Key=f"{key}.json",
                Body=json.dumps(data),
                ContentType="application/json",
            )
            
            # Store content (text)
            self.client.put_object(
                Bucket=self.bucket,
                Key=f"{key}.txt",
                Body=content.encode('utf-8'),
                ContentType="text/plain",
            )
            
            # Restore content to data
            data["content"] = content
            
        except Exception:
            pass  # Fail silently for cache
    
    def delete(self, key: str) -> None:
        """Delete cached data from S3"""
        if not self.enabled or not self.client:
            return
        
        try:
            self.client.delete_object(Bucket=self.bucket, Key=f"{key}.json")
            self.client.delete_object(Bucket=self.bucket, Key=f"{key}.txt")
        except Exception:
            pass
    
    def clear(self) -> None:
        """Clear all cached data from S3 bucket"""
        if not self.enabled or not self.client:
            return
        
        try:
            objects = self.client.list_objects_v2(Bucket=self.bucket)
            if 'Contents' in objects:
                for obj in objects['Contents']:
                    self.client.delete_object(
                        Bucket=self.bucket,
                        Key=obj['Key'],
                    )
        except Exception:
            pass


def get_cache(settings=None) -> CacheInterface:
    """
    Get appropriate cache implementation based on settings.
    
    Args:
        settings: Optional settings object
        
    Returns:
        CacheInterface implementation
    """
    if settings is None:
        from ..config import get_settings
        settings = get_settings()
    
    if settings.S3_ENABLED:
        return S3Cache(settings)
    else:
        return LocalCache()
