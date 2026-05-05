@echo off
echo 🚀 Deploying Bengali OCR Feature for SupremeAI
echo ================================================

echo 📦 Installing dependencies...
cd functions
call npm install
if %errorlevel% neq 0 (
    echo ❌ Failed to install dependencies
    exit /b 1
)
cd ..

echo 🔥 Deploying Firebase Functions...
call firebase deploy --only functions
if %errorlevel% neq 0 (
    echo ❌ Failed to deploy functions
    exit /b 1
)

echo ✅ Deployment completed successfully!
echo.
echo 🌐 Access the feature at:
echo    https://your-project.firebaseapp.com/bengali-ocr.html
echo.
echo 📋 Next steps:
echo 1. Enable Google Cloud Vision API in your project
echo 2. Set up service account credentials
echo 3. Test the OCR functionality
echo.
echo 📖 See BENGALI_OCR_README.md for detailed instructions

pause