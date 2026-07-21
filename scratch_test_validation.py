import os
import sys

# Add backend directory to path
sys.path.append(os.path.join(os.getcwd(), "backend"))

# Load config to test validation
try:
    # Set env to production to trigger validation
    os.environ["ENV"] = "production"
    os.environ["OPENROUTER_API_KEY"] = "sk-or-v1-dummy"
    os.environ["GEMINI_API_KEY"] = "AIzaSyDummy"
    os.environ["CI_WEBHOOK_SECRET"] = "dummy"
    os.environ["SUPREMEAI_ENCRYPTION_KEY"] = (
        "9llmzMU2XSRhbAS-R__JMW1XLZzc0ll7obD_RqaVwno="
    )
    os.environ["ENCRYPTION_KEY"] = "X-mE_EtEtiznG1yU-Z0cQjhdh_ZjO1QT4gv1gSIx4ao="
    os.environ["SUPREMEAI_JWT_SECRET"] = (
        "2a656d9a200f2ccb22d524c889e2f348474dbef71ed122fd808c4791b319446e"
    )
    os.environ["SUPREMEAI_ADMIN_PASSWORD_HASH"] = (
        "$2b$12$LJ3m8yV6qN4xZ7wE5rT8yU2iO9pA3sD6fG8hJ0kL2mN4oP6qR8sT0u"
    )

    # Set CORS_ORIGINS to a valid value
    os.environ["CORS_ORIGINS"] = "https://example.com"

    # Leave USER_CORS_ORIGINS and ADMIN_CORS_ORIGINS unset
    if "USER_CORS_ORIGINS" in os.environ:
        del os.environ["USER_CORS_ORIGINS"]
    if "ADMIN_CORS_ORIGINS" in os.environ:
        del os.environ["ADMIN_CORS_ORIGINS"]

    from core.config import settings

    print("Settings loaded successfully!")
    print("cors_origins:", settings.cors_origins)
    print("user_cors_origins:", settings.user_cors_origins)
    print("admin_cors_origins:", settings.admin_cors_origins)
except Exception as e:
    print("Validation failed:")
    import traceback

    traceback.print_exc()
