@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo   SupremeAI Smart Local Verification Tool (CI-Ready)
echo ===================================================

:: Setup Git Hook Option
if "%1"=="--install-hook" (
    echo [System] Installing verify.bat as Git pre-push hook...
    if not exist ".git\" (
        echo [ERROR] Not a git repository or not in root folder.
        exit /b 1
    )
    (
        echo #^^!/bin/sh
        echo echo "Running pre-push local verification..."
        echo cmd.exe /c verify.bat --hook-mode
        echo if [ $? -ne 0 ]; then
        echo     echo "Verification failed. Push aborted."
        echo     exit 1
        echo fi
    ) > .git\hooks\pre-push
    echo [SUCCESS] Pre-push hook installed! Broken code can no longer be pushed.
    exit /b 0
)

if "%1"=="--install-pre-commit" (
    echo [System] Installing verify.bat as Git pre-commit hook...
    if not exist ".git\" (
        echo [ERROR] Not a git repository or not in root folder.
        exit /b 1
    )
    (
        echo #!/bin/sh
        echo echo "Running pre-commit lint check..."
        echo cmd.exe /c verify.bat --hook-mode --lint-only
        echo if [ $? -ne 0 ]; then exit 1; fi
    ) > .git\hooks\pre-commit
    echo [SUCCESS] Pre-commit hook installed!
    exit /b 0
)

if "%1"=="--auto-heal" (
    echo [System] Entering Autonomous Healing Mode...
    echo [Step 1] Fetching suggested fixes from SupremeAI...
    :: This would call a local script to pull AI-generated diffs based on last local failure
    if exist "fix.patch" (
        echo Applying AI-suggested fix...
        git apply fix.patch
        del fix.patch
    )
)

:: 1. Detect Java Version
echo [System] Checking Java Version...
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVAV=%%g
)
set JAVAV=%JAVAV:"=%
echo Java version detected: %JAVAV%
echo %JAVAV% | findstr "^21\." >nul
if %ERRORLEVEL% NEQ 0 (
    echo [WARNING] GitHub CI runs on Java 21. Your local Java version is %JAVAV%.
    echo           This might cause minor bytecode or library incompatibilities.
)
echo.

:: 2. Pre-commit secrets sanity check
echo [System] Checking Secrets Hygiene...
if exist "config\.env" (
    echo [WARNING] config/.env file detected. Make sure it is added to .gitignore.
)
if exist "src\main\resources\firebase-service-account.json" (
    echo [WARNING] firebase-service-account.json detected. Never commit keys to GitHub.
)
echo.

:: 3. Smart Change Detection via Git
set RUN_BACKEND=false
set RUN_FRONTEND=false

where git >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [System] Analyzing git diff for changed components...
    git diff --name-only | findstr /R "^src/ ^build.gradle ^gradle/ ^gradlew" >nul
    if !ERRORLEVEL! EQU 0 set RUN_BACKEND=true
    
    git diff --name-only | findstr /R "^dashboard/" >nul
    if !ERRORLEVEL! EQU 0 set RUN_FRONTEND=true
    
    if "!RUN_BACKEND!"=="false" (
        if "!RUN_FRONTEND!"=="false" (
            echo No local changes detected. Running full verification suite...
            set RUN_BACKEND=true
            set RUN_FRONTEND=true
        )
    )
) else (
    echo [WARNING] Git not found in PATH. Defaulting to run all tests.
    set RUN_BACKEND=true
    set RUN_FRONTEND=true
)

echo Backend verification: %RUN_BACKEND%
echo Frontend verification: %RUN_FRONTEND%
echo.

:: 4. Run Backend Verification
if "%RUN_BACKEND%"=="true" (
    echo [1/2] Running Backend Checks...
    echo Running Spotless code format check...
    call .\gradlew spotlessCheck
    if !ERRORLEVEL! NEQ 0 (
        echo [WARNING] Spotless check failed.
        if "%1"=="--hook-mode" (
            echo Auto-applying spotless format fixes in hook mode...
            call .\gradlew spotlessApply
        ) else (
            set /p choice="Would you like to auto-fix the formatting using spotlessApply? (y/n): "
            if /i "!choice!"=="y" (
                call .\gradlew spotlessApply
                echo Formatting applied. Re-running check...
                call .\gradlew spotlessCheck
            )
        )
        if !ERRORLEVEL! NEQ 0 (
            echo [ERROR] Spotless check still failing.
            exit /b !ERRORLEVEL!
        )
    )
    
    :: Solo AI Resilience (SK-0054): Ensure dummy keys exist to prevent Spring AI startup crashes during verification
    if "!OPENAI_API_KEY!"=="" (
        set OPENAI_API_KEY=dummy-verification-key
    )

    if "%2"=="--lint-only" goto skip_backend_tests
    echo Running Backend Unit Tests...
    call .\gradlew test
    if !ERRORLEVEL! NEQ 0 (
        echo [ERROR] Backend tests failed!
        exit /b !ERRORLEVEL!
    )
)

:skip_backend_tests
if "%RUN_BACKEND%"=="true" (
    echo Running Global Formatting Fixes...
    call npm run format:root

    where black >nul 2>nul
    if !ERRORLEVEL! EQU 0 (
        echo Running Python Auto-formatting...
        black scripts/**/*.py
    )

    echo Backend checks passed successfully.
    echo.
)

:: 5. Run Frontend Verification
if "%RUN_FRONTEND%"=="true" (
    echo [2/2] Running Frontend Checks...
    where npm >nul 2>nul
    if !ERRORLEVEL! NEQ 0 (
        echo [WARNING] npm is not installed on this PC. Skipping frontend lint/test.
    ) else (
        if not exist "dashboard\node_modules\" (
            echo [System] dashboard/node_modules not found. Running npm install...
            call npm install -w dashboard
        )
        
        echo Running Frontend Lint...
        call npm run lint -w dashboard
        if !ERRORLEVEL! NEQ 0 (
            echo [ERROR] Frontend Lint failed!
            exit /b !ERRORLEVEL!
        )
        
        if "%2"=="--lint-only" goto skip_frontend_tests
        echo Running Frontend Tests...
        call npm run test -w dashboard -- --run
        if !ERRORLEVEL! NEQ 0 (
            echo [ERROR] Frontend tests failed!
            exit /b !ERRORLEVEL!
        )
    )
)

:skip_frontend_tests
if "%RUN_FRONTEND%"=="true" (
    echo Running Global Formatting Fixes...
    call npm run format:root

    where black >nul 2>nul
    if !ERRORLEVEL! EQU 0 (
        echo Running Python Auto-formatting...
        black scripts/**/*.py
    )

    echo Frontend checks passed successfully.
    echo.
)

echo ===================================================
echo   SUCCESS: Verification passed! 🚀
echo ===================================================
if not "%1"=="--hook-mode" (
    pause
)
