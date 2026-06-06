@echo off
echo Uploading secrets to /system_secrets/primary_vault...
call npx firebase-tools database:set /system_secrets/primary_vault secrets_to_seed.json --project supremeai-a --force

if %errorlevel%==0 (
    echo [SUCCESS] Secrets seeded successfully!
    echo For security, deleting the local secrets_to_seed.json file...
    del secrets_to_seed.json
    echo Done!
) else (
    echo [ERROR] Failed to seed secrets. Please make sure you are logged in to Firebase (npx firebase-tools login).
)

pause
