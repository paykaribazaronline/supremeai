import os

import boto3
from botocore.config import Config
from botocore.exceptions import ClientError
from loguru import logger


class R2StorageClient:
    def __init__(self):
        # বাংলা মন্তব্য: ক্লাউডফ্লেয়ার R2 এর ক্রেডেনশিয়াল এনভায়রনমেন্ট থেকে পড়া হচ্ছে।
        self.account_id = os.getenv("R2_ACCOUNT_ID")
        self.access_key = os.getenv("R2_ACCESS_KEY")
        self.secret_key = os.getenv("R2_SECRET_KEY")
        self.bucket_name = os.getenv("R2_BUCKET_NAME", "supremeai-assets")
        
        # বাংলা মন্তব্য: যদি ক্রেডেনশিয়াল মিসিং থাকে, তবে মক মোডে ফলব্যাক করা হবে।
        self.dry_run = not (self.account_id and self.access_key and self.secret_key)
        
        if self.dry_run:
            logger.warning("Cloudflare R2 credentials missing. R2StorageClient will run in dry-run/mock mode.")
            self.s3_client = None
        else:
            # Cloudflare R2 Endpoint
            endpoint_url = f"https://{self.account_id}.r2.cloudflarestorage.com"
            self.s3_client = boto3.client(
                "s3",
                endpoint_url=endpoint_url,
                aws_access_key_id=self.access_key,
                aws_secret_access_key=self.secret_key,
                region_name="auto", # R2 uses 'auto'
                config=Config(signature_version="s3v4")
            )

    def generate_presigned_upload_url(self, object_name: str, file_type: str, expiration=3600):
        # বাংলা মন্তব্য: মক মোড অ্যাক্টিভ থাকলে লোকাল মক আপলোড URL জেনারেট করা হচ্ছে।
        if self.dry_run:
            logger.info(f"Dry-run: Generating mock presigned upload URL for {object_name}")
            return f"https://mock-r2-upload.local/{self.bucket_name}/{object_name}?expires={expiration}&type={file_type}"
            
        try:
            response = self.s3_client.generate_presigned_url(
                'put_object',
                Params={
                    'Bucket': self.bucket_name,
                    'Key': object_name,
                    'ContentType': file_type
                },
                ExpiresIn=expiration
            )
            return response
        except ClientError as e:
            logger.error(f"Error generating presigned URL: {e}")
            return None

    def generate_presigned_download_url(self, object_name: str, expiration=3600):
        # বাংলা মন্তব্য: মক মোড অ্যাক্টিভ থাকলে লোকাল মক ডাউনলোড URL জেনারেট করা হচ্ছে।
        if self.dry_run:
            logger.info(f"Dry-run: Generating mock presigned download URL for {object_name}")
            return f"https://mock-r2-download.local/{self.bucket_name}/{object_name}?expires={expiration}"
            
        try:
            response = self.s3_client.generate_presigned_url(
                'get_object',
                Params={
                    'Bucket': self.bucket_name,
                    'Key': object_name
                },
                ExpiresIn=expiration
            )
            return response
        except ClientError as e:
            logger.error(f"Error generating download URL: {e}")
            return None
