
import os
import base64
from typing import Dict, List, Optional, Tuple
from datetime import datetime
import uuid
import mimetypes

class ImageProcessor:
    def __init__(self, upload_dir: str = None):
        """
        ইমেজ প্রসেসর ইনিশিয়ালাইজ করে।

        Args:
            upload_dir: ইমেজ আপলোড ডিরেক্টরি (ডিফল্ট: বর্তমান ডিরেক্টরির uploads)
        """
        if upload_dir is None:
            # বর্তমান ডিরেক্টরির পাথ পেতে
            current_dir = os.path.dirname(os.path.abspath(__file__))
            upload_dir = os.path.join(current_dir, "uploads")

        self.upload_dir = upload_dir
        self._ensure_upload_dir()

        # সাপোর্টেড ইমেজ ফরম্যাট
        self.supported_formats = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml']

        # সর্বোচ্চ ইমেজ সাইজ (বাইটে)
        self.max_image_size = 10 * 1024 * 1024  # 10MB

        # ইমেজ কোয়ালিটি সেটিংস
        self.quality_settings = {
            'thumbnail': {'width': 150, 'height': 150, 'quality': 70},
            'medium': {'width': 600, 'height': 600, 'quality': 80},
            'large': {'width': 1200, 'height': 1200, 'quality': 90}
        }

    def _ensure_upload_dir(self):
        """আপলোড ডিরেক্টরি নিশ্চিত করে।"""
        if not os.path.exists(self.upload_dir):
            os.makedirs(self.upload_dir)

        # সাব-ডিরেক্টরি তৈরি করা
        for subdir in ['thumbnails', 'medium', 'large', 'original']:
            subdir_path = os.path.join(self.upload_dir, subdir)
            if not os.path.exists(subdir_path):
                os.makedirs(subdir_path)

    def process_base64_image(self, base64_data: str, user_id: str) -> Dict:
        """
        Base64 এনকোডেড ইমেজ প্রসেস করে এবং সেভ করে।

        Data URL ফরম্যাট: data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wBD...

        Args:
            base64_data: Base64 এনকোডেড ইমেজ ডাটা
            user_id: ইউজার আইডি

        Returns:
            Dict: প্রসেস করা ইমেজ সম্পর্কিত তথ্য
        """
        # ডাটা URL ফরম্যাট থেকে মাইম টাইপ এবং বাইনারি ডাটা এক্সট্র্যাক্ট করা
        if base64_data.startswith('data:'):
            # ডাটা URL ফরম্যাট
            header, data = base64_data.split(',', 1)
            mime_type = header.split(':')[1].split(';')[0]
        else:
            # সরাসরি Base64 ডাটা
            mime_type = 'image/jpeg'  # ডিফল্ট মাইম টাইপ
            data = base64_data

        # মাইম টাইপ চেক করা
        if mime_type not in self.supported_formats:
            return {
                'success': False,
                'error': f'অসমর্থিত ইমেজ ফরম্যাট: {mime_type}'
            }

        # Base64 ডাটা ডিকোড করা
        try:
            image_data = base64.b64decode(data)
        except Exception as e:
            return {
                'success': False,
                'error': f'ইমেজ ডিকোড করতে ব্যর্থ: {str(e)}'
            }

        # ইমেজ সাইজ চেক করা
        if len(image_data) > self.max_image_size:
            return {
                'success': False,
                'error': f'ইমেজ সাইজ খুব বড়। সর্বোচ্চ সাইজ: {self.max_image_size / (1024 * 1024)}MB'
            }

        # ইউনিক ফাইল নাম তৈরি করা
        file_ext = mimetypes.guess_extension(mime_type)
        if not file_ext:
            file_ext = '.jpg'  # ডিফল্ট এক্সটেনশন

        timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
        unique_id = str(uuid.uuid4())[:8]
        filename = f"{user_id}_{timestamp}_{unique_id}{file_ext}"

        # ইমেজ সেভ করা
        try:
            # অরিজিনাল ইমেজ সেভ করা
            original_path = os.path.join(self.upload_dir, 'original', filename)
            with open(original_path, 'wb') as f:
                f.write(image_data)

            # ইমেজ প্রসেস করা (PIL ব্যবহার করে)
            try:
                from PIL import Image
                import io

                # ইমেজ লোড করা
                img = Image.open(io.BytesIO(image_data))

                # থাম্বনেইল তৈরি করা
                thumbnail_path = os.path.join(self.upload_dir, 'thumbnails', filename)
                self._create_thumbnail(img, thumbnail_path)

                # মিডিয়াম সাইজ ইমেজ তৈরি করা
                medium_path = os.path.join(self.upload_dir, 'medium', filename)
                self._create_resized_image(img, medium_path, self.quality_settings['medium'])

                # লার্জ সাইজ ইমেজ তৈরি করা
                large_path = os.path.join(self.upload_dir, 'large', filename)
                self._create_resized_image(img, large_path, self.quality_settings['large'])

                # ইমেজ ডাইমেনশন পাওয়া
                width, height = img.size

                return {
                    'success': True,
                    'filename': filename,
                    'mime_type': mime_type,
                    'size': len(image_data),
                    'width': width,
                    'height': height,
                    'urls': {
                        'original': f'/uploads/original/{filename}',
                        'large': f'/uploads/large/{filename}',
                        'medium': f'/uploads/medium/{filename}',
                        'thumbnail': f'/uploads/thumbnails/{filename}'
                    },
                    'uploaded_at': datetime.now().isoformat()
                }
            except ImportError:
                # PIL ইনস্টল না থাকলে শুধুমাত্র অরিজিনাল ইমেজ সেভ করা
                return {
                    'success': True,
                    'filename': filename,
                    'mime_type': mime_type,
                    'size': len(image_data),
                    'urls': {
                        'original': f'/uploads/original/{filename}'
                    },
                    'uploaded_at': datetime.now().isoformat(),
                    'note': 'PIL ইনস্টল না থাকায় শুধুমাত্র অরিজিনাল ইমেজ সেভ করা হয়েছে'
                }
        except Exception as e:
            return {
                'success': False,
                'error': f'ইমেজ সেভ করতে ব্যর্থ: {str(e)}'
            }

    def _create_thumbnail(self, img, path: str):
        """ইমেজ থেকে থাম্বনেইল তৈরি করে।"""
        settings = self.quality_settings['thumbnail']
        img.thumbnail((settings['width'], settings['height']))
        img.save(path, quality=settings['quality'])

    def _create_resized_image(self, img, path: str, settings: Dict):
        """ইমেজ রিসাইজ করে।"""
        width, height = img.size

        # অ্যাসপেক্ট রেশিও বজায় রেখে রিসাইজ করা
        if width > height:
            new_width = settings['width']
            new_height = int(height * (settings['width'] / width))
        else:
            new_height = settings['height']
            new_width = int(width * (settings['height'] / height))

        resized_img = img.resize((new_width, new_height), Image.LANCZOS)
        resized_img.save(path, quality=settings['quality'])

    def delete_image(self, filename: str) -> bool:
        """
        ইমেজ এবং এর সকল সাইজ ডিলিট করে।

        Args:
            filename: ইমেজ ফাইলের নাম

        Returns:
            bool: ডিলিট সফল হলে True, অন্যথায় False
        """
        success = True

        # সকল সাইজের ইমেজ ডিলিট করা
        for subdir in ['thumbnails', 'medium', 'large', 'original']:
            file_path = os.path.join(self.upload_dir, subdir, filename)
            if os.path.exists(file_path):
                try:
                    os.remove(file_path)
                except Exception as e:
                    print(f"ইমেজ ডিলিট করতে ব্যর্থ: {file_path}, এরর: {str(e)}")
                    success = False

        return success

    def get_image_url(self, filename: str, size: str = 'medium') -> Optional[str]:
        """
        ইমেজের URL রিটার্ন করে।

        Args:
            filename: ইমেজ ফাইলের নাম
            size: ইমেজ সাইজ (thumbnail, medium, large, original)

        Returns:
            Optional[str]: ইমেজ URL যদি পাওয়া যায়, অন্যথায় None
        """
        if size not in ['thumbnail', 'medium', 'large', 'original']:
            size = 'medium'  # ডিফল্ট সাইজ

        file_path = os.path.join(self.upload_dir, size, filename)

        if os.path.exists(file_path):
            return f'/uploads/{size}/{filename}'

        return None
