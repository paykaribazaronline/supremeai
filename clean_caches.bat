@echo off
setlocal enabledelayedexpansion

echo ==========================================
echo    SUPREME AI - DEEP CACHE CLEANER
echo ==========================================
echo.

:: 1. STOP GRADLE DAEMONS
echo [1/4] Stopping Gradle Daemons...
jps 2>nul | findstr "GradleDaemon" > temp_jps.txt
for /f "tokens=1" %%a in (temp_jps.txt) do (
    taskkill /F /PID %%a >nul 2>&1
)
del temp_jps.txt
echo Done.
echo.

:: 2. PROJECT-SPECIFIC CLEANUP (Recursive from current dir)
echo [2/4] Cleaning Project-Specific Caches...
echo This will remove: build, node_modules, .gradle, .dart_tool, .venv, etc.

set "TARGET_DIRS=build .gradle .dart_tool node_modules .venv logs execution_logs out __pycache__ .pytest_cache"

for %%d in (%TARGET_DIRS%) do (
    echo   - Searching and removing all "%%d" directories...
    for /d /r . %%p in (%%d) do (
        if exist "%%p" (
            echo     Deleting: %%p
            rd /s /q "%%p" >nul 2>&1
        )
    )
)

echo   - Removing compiled python files...
del /s /q /f *.pyc *.pyo >nul 2>&1
echo Done.
echo.

:: 3. GLOBAL USER CACHES
echo [3/4] Cleaning Global User Caches...

echo   - Cleaning Global Gradle Caches (~30GB+)...
if exist "%USERPROFILE%\.gradle\caches" (
    echo     Deleting: %USERPROFILE%\.gradle\caches
    rd /s /q "%USERPROFILE%\.gradle\caches" >nul 2>&1
)
if exist "%USERPROFILE%\.gradle\wrapper" (
    echo     Deleting: %USERPROFILE%\.gradle\wrapper
    rd /s /q "%USERPROFILE%\.gradle\wrapper" >nul 2>&1
)
if exist "%USERPROFILE%\.gradle\daemon" (
    echo     Deleting: %USERPROFILE%\.gradle\daemon
    rd /s /q "%USERPROFILE%\.gradle\daemon" >nul 2>&1
)

echo   - Cleaning Shorebird Cache...
if exist "%USERPROFILE%\.shorebird" (
    echo     Deleting: %USERPROFILE%\.shorebird
    rd /s /q "%USERPROFILE%\.shorebird" >nul 2>&1
)

echo   - Cleaning Android Cache...
if exist "%USERPROFILE%\.android\cache" (
    echo     Deleting: %USERPROFILE%\.android\cache
    rd /s /q "%USERPROFILE%\.android\cache" >nul 2>&1
)

echo   - Cleaning System Temp Files...
del /s /f /q "%TEMP%\*.*" >nul 2>&1
for /d %%p in ("%TEMP%\*") do rd /s /q "%%p" >nul 2>&1

echo Done.
echo.

:: 4. RUN BUILT-IN TOOLS
echo [4/4] Running Clean Commands...
if exist "gradlew.bat" (
    echo   - Running gradlew clean...
    call gradlew clean >nul 2>&1
)

if exist "flutter_admin_app" (
    pushd flutter_admin_app
    if exist "pubspec.yaml" (
        echo   - Running flutter clean...
        call flutter clean >nul 2>&1
    )
    popd
)

echo.
echo ==========================================
echo    CLEANUP COMPLETE!
echo ==========================================
echo Re-run this script whenever storage is low.
echo Note: Dependencies will be re-downloaded on next build.
pause
