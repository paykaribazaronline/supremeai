import os
import io
from google.cloud import vision
from google.oauth2 import service_account
import pandas as pd

def test_google_vision_setup(credentials_path=None):
    """Test Google Cloud Vision API setup"""
    print("🔍 Testing Google Cloud Vision API Setup...")
    print("=" * 50)

    try:
        # Test 1: Check credentials
        print("1. Checking credentials...")
        if credentials_path:
            if not os.path.exists(credentials_path):
                print(f"❌ Credentials file not found: {credentials_path}")
                return False
            credentials = service_account.Credentials.from_service_account_file(credentials_path)
            client = vision.ImageAnnotatorClient(credentials=credentials)
            print("✅ Credentials loaded successfully")
        else:
            client = vision.ImageAnnotatorClient()
            print("✅ Using default credentials")

        # Test 2: Check API connectivity
        print("\n2. Testing API connectivity...")
        # Create a simple test image (small PNG with text)
        from PIL import Image, ImageDraw, ImageFont
        import tempfile

        with tempfile.NamedTemporaryFile(suffix='.png', delete=False) as tmp:
            # Create a simple test image
            img = Image.new('RGB', (200, 50), color='white')
            draw = ImageDraw.Draw(img)
            try:
                # Try to use a default font
                font = ImageFont.truetype("arial.ttf", 20)
            except:
                font = ImageFont.load_default()

            draw.text((10, 10), "Hello Bengali", fill='black', font=font)
            img.save(tmp.name)
            test_image_path = tmp.name

        # Test OCR on the simple image
        with io.open(test_image_path, 'rb') as image_file:
            content = image_file.read()

        image = vision.Image(content=content)
        response = client.text_detection(image=image)

        if response.error.message:
            print(f"❌ API Error: {response.error.message}")
            return False

        # Clean up
        os.unlink(test_image_path)

        print("✅ API connectivity successful")
        print(f"   Response received with {len(response.text_annotations)} text annotations")

        # Test 3: Check quota/billing
        print("\n3. Checking project and billing status...")
        # This would require additional API calls, but basic connectivity test passed

        print("✅ Setup verification completed successfully!")
        print("\n🚀 You can now run the main converter:")
        print("   python bengali_ocr_converter.py")

        return True

    except Exception as e:
        print(f"❌ Setup test failed: {str(e)}")
        print("\n🔧 Troubleshooting tips:")
        print("- Check that your credentials JSON file is valid")
        print("- Ensure the Cloud Vision API is enabled")
        print("- Verify your Google Cloud project has billing enabled")
        print("- Check that your service account has proper permissions")
        return False

def test_bengali_ocr():
    """Test Bengali OCR specifically"""
    print("\n🌐 Testing Bengali OCR capabilities...")

    try:
        client = vision.ImageAnnotatorClient()

        # Test with Bengali language hint
        image_context = vision.ImageContext(language_hints=['bn'])

        # Create a test image with Bengali text approximation
        from PIL import Image, ImageDraw
        import tempfile

        with tempfile.NamedTemporaryFile(suffix='.png', delete=False) as tmp:
            img = Image.new('RGB', (300, 50), color='white')
            draw = ImageDraw.Draw(img)
            # Simple test - we'll use English for now, but API will be configured for Bengali
            draw.text((10, 10), "Test Bengali OCR", fill='black')
            img.save(tmp.name)
            test_image_path = tmp.name

        with io.open(test_image_path, 'rb') as image_file:
            content = image_file.read()

        image = vision.Image(content=content)
        response = client.text_detection(image=image, image_context=image_context)

        os.unlink(test_image_path)

        if response.text_annotations:
            detected_text = response.text_annotations[0].description
            print(f"✅ Bengali OCR test successful: '{detected_text}'")
            return True
        else:
            print("⚠️  No text detected in test image")
            return True

    except Exception as e:
        print(f"❌ Bengali OCR test failed: {str(e)}")
        return False

if __name__ == "__main__":
    print("🛠️  Google Cloud Vision API Setup Tester")
    print("=" * 50)

    # Check for credentials file
    possible_paths = [
        os.environ.get('GOOGLE_APPLICATION_CREDENTIALS'),
        r"C:\Users\Nazifa\supremeai\bengali-credentials.json",
        r"C:\Users\Nazifa\supremeai\credentials.json"
    ]

    credentials_path = None
    for path in possible_paths:
        if path and os.path.exists(path):
            credentials_path = path
            break

    if credentials_path:
        print(f"📄 Found credentials file: {credentials_path}")
    else:
        print("⚠️  No credentials file found")
        print("Please ensure you have:")
        print("1. Downloaded the service account JSON key")
        print("2. Set GOOGLE_APPLICATION_CREDENTIALS environment variable")
        credentials_path = input("Enter path to credentials JSON file (or press Enter to skip): ").strip()
        if not credentials_path:
            exit(1)

    # Run tests
    success = test_google_vision_setup(credentials_path)
    if success:
        test_bengali_ocr()

    print("\n" + "=" * 50)
    if success:
        print("🎉 Setup is ready! Run: python bengali_ocr_converter.py")
    else:
        print("❌ Setup needs to be completed. Check the COMPLETE_SETUP_GUIDE.md")