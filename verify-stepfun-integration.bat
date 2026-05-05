@echo off
REM StepFun Integration Verification Script for Windows
REM Run this after implementing StepFun provider to verify everything works

echo ==========================================
echo StepFun Integration Verification
echo ==========================================
echo.

REM Check if backend is running
echo 1. Checking if backend is running...
curl -s http://localhost:8080/actuator/health > NUL 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] Backend is running on port 8080
) else (
    echo [ERROR] Backend not running. Start it with: gradlew.bat bootRun
    exit /b 1
)

REM Check if StepFun provider is registered
echo.
echo 2. Checking if StepFun provider is registered...
curl -s http://localhost:8080/api/providers/list | findstr /i "stepfun" > NUL 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] StepFun found in provider list
) else (
    echo [ERROR] StepFun NOT in provider list. Check AIProviderFactory.java
    exit /b 1
)

REM Check environment variable
echo.
echo 3. Checking STEPFUN_API_KEY environment variable...
if defined STEPFUN_API_KEY (
    echo [OK] STEPFUN_API_KEY is set
    echo    Key starts with: %STEPFUN_API_KEY:~0,10%...
) else (
    echo [WARN] STEPFUN_API_KEY not set
    echo    Set it in .env file or:
    echo    set STEPFUN_API_KEY=sf-your-actual-key
)

REM Test generation (if key is set and not placeholder)
echo.
echo 4. Testing StepFun generation (if key valid)...
if defined STEPFUN_API_KEY (
    curl -s -X POST http://localhost:8080/api/ai/generate ^
      -H "Content-Type: application/json" ^
      -d "{\"provider\":\"stepfun\",\"prompt\":\"Say 'Hello'\"}" ^
      | findstr /i "Hello" > NUL 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo [OK] Generation successful!
    ) else (
        echo [INFO] Generation test skipped or failed (check if key is valid)
    )
) else (
    echo [WARN] Skipping generation test (no API key)
)

REM Check logs
echo.
echo 5. Checking logs for StepFun errors...
if exist logs\supremeai.log (
    findstr /i "stepfun.*error STEPFUN.*ERROR" logs\supremeai.log > NUL 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo [OK] No StepFun errors in logs
    ) else (
        echo [ERROR] Found StepFun errors in logs
        type logs\supremeai.log | findstr /i "stepfun" | tail -3
    )
) else (
    echo [WARN] Log file not found at logs\supremeai.log
)

REM Frontend check
echo.
echo 6. Checking frontend integration...
if exist dashboard\src\components\APIKeysManager.tsx (
    findstr /c:"step-3.5-flash" dashboard\src\components\APIKeysManager.tsx > NUL 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo [OK] StepFun models added to frontend
    ) else (
        echo [ERROR] StepFun models NOT in APIKeysManager.tsx
    )

    findstr /c:"stepfun: 'https://api.stepfun.com/v1'" dashboard\src\components\APIKeysManager.tsx > NUL 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo [OK] StepFun endpoint configured
    ) else (
        echo [ERROR] StepFun endpoint NOT configured
    )
) else (
    echo [ERROR] APIKeysManager.tsx not found
)

echo.
echo ==========================================
echo Verification Complete
echo ==========================================
echo.
echo Next steps:
echo 1. Fix any errors above
echo 2. Add your real StepFun API key to .env
echo 3. Restart backend: gradlew.bat bootRun
echo 4. Open dashboard: http://localhost:5173/admin/apikeys
echo 5. Add StepFun provider with your key
echo.
pause
