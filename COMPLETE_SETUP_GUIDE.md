# Complete Google Cloud Vision API Setup Guide

## Step 1: Access Google Cloud Console
1. Open your web browser
2. Go to: https://console.cloud.google.com/
3. Sign in with your Google account (or create one if needed)

## Step 2: Create a New Project
1. Click on the project dropdown at the top
2. Click "NEW PROJECT"
3. Enter project name: `bengali-ocr-project`
4. Choose organization (if applicable) or leave as "No organization"
5. Click "CREATE"

## Step 3: Enable Cloud Vision API
1. In the left sidebar, go to "APIs & Services" → "Library"
2. In the search box, type "Cloud Vision API"
3. Click on "Cloud Vision API" from the results
4. Click "ENABLE"

## Step 4: Create Service Account
1. Go to "APIs & Services" → "Credentials"
2. Click "+ CREATE CREDENTIALS" → "Service account"
3. Fill in the details:
   - Service account name: `bengali-ocr-service`
   - Service account ID: `bengali-ocr-service` (auto-filled)
   - Description: `Service account for Bengali OCR conversion`
4. Click "CREATE AND CONTINUE"
5. Skip the role assignment (optional)
6. Click "DONE"

## Step 5: Create Service Account Key
1. In the Credentials page, find your new service account
2. Click on the service account name
3. Go to the "Keys" tab
4. Click "ADD KEY" → "Create new key"
5. Select "JSON" as the key type
6. Click "CREATE"
7. The JSON file will automatically download
8. **IMPORTANT**: Save this file securely and don't share it

## Step 6: Set Up Environment Variables
1. Place the downloaded JSON file in your project folder:
   - Location: `C:\Users\Nazifa\supremeai\bengali-credentials.json`
2. Open Command Prompt as Administrator
3. Run this command:
   ```
   setx GOOGLE_APPLICATION_CREDENTIALS "C:\Users\Nazifa\supremeai\bengali-credentials.json"
   ```

## Step 7: Test the Setup
1. Restart your command prompt (to pick up environment variables)
2. Run: `python bengali_ocr_converter.py`

## Troubleshooting:

### If you get "Permission denied" errors:
- Make sure you're running Command Prompt as Administrator
- Check that the JSON file path is correct

### If you get "API has not been used" errors:
- Wait a few minutes for the API to fully enable
- Check that you're in the correct project

### If you get authentication errors:
- Verify the JSON file is valid (open it in a text editor)
- Check that the service account has proper permissions

## Cost Information:
- Cloud Vision API has a generous free tier
- First 1000 images per month are free
- Additional usage is very low cost (~$1.50 per 1000 images)

## Need Help?
If you encounter any issues during setup, share the exact error message and I'll help troubleshoot.