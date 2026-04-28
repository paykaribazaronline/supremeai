@echo off
REM SupremeAI CLI Installation Script for Windows
echo 🚀 Installing SupremeAI CLI...

REM Check Python installation
python --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Python is not installed. Please install Python 3.8+ first.
    pause
    exit /b 1
)

echo ✅ Python installation found

REM Get script directory
set SCRIPT_DIR=%~dp0

REM Add to PATH if needed (optional step)
echo.
echo 📝 Installation complete!
echo.
echo 🔗 To use the CLI, you can:
echo   1. Run directly: python "%SCRIPT_DIR%supcmd.py" [command]
echo   2. Or add to PATH: setx PATH "%PATH%;%SCRIPT_DIR%"
echo.
echo 📋 Quick Examples:
echo   python supcmd.py login
echo   python supcmd.py list
echo   python supcmd.py system learning improve
echo   python supcmd.py admin mode AUTO
echo.
echo 🧠 First time? Run: python supcmd.py login
echo.
pause
