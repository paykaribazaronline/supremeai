# Google Cloud Vision API Setup Instructions
# Follow these steps to set up Bengali OCR for your images:

## Step 1: Create Google Cloud Project
1. Go to https://console.cloud.google.com/
2. Click "Create Project" or select existing project
3. Note down your Project ID

## Step 2: Enable Cloud Vision API
1. Go to "APIs & Services" > "Library"
2. Search for "Cloud Vision API"
3. Click on it and enable the API

## Step 3: Create Service Account
1. Go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "Service Account"
3. Fill in service account details:
   - Name: bengali-ocr-service
   - Description: Service account for Bengali OCR
4. Click "Create and Continue"
5. Skip role assignment for now
6. Click "Done"

## Step 4: Create Service Account Key
1. In the Credentials page, find your service account
2. Click on it, then go to "Keys" tab
3. Click "Add Key" > "Create new key"
4. Select "JSON" format
5. Download the JSON file - this is your credentials file

## Step 5: Set Environment Variable
1. Place the downloaded JSON file in a safe location
2. Set environment variable:
   setx GOOGLE_APPLICATION_CREDENTIALS "C:\path\to\your\credentials.json"

## Step 6: Run the Converter
1. Open command prompt in your project folder
2. Run: python bengali_ocr_converter.py

The script will:
- Extract Bengali text accurately using Google Cloud Vision
- Parse tabular data into proper Excel format
- Create one Excel file per image with structured data