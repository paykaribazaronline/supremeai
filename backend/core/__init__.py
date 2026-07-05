# FILE_PATH: core/__init__.py
import os


# Set environment variables to indicate a CI/test environment.
# This can trigger specific conditional logic in other modules (e.g., for mocking,
# using in-memory databases, or adjusting verification strictness in tests)
# when full external services or specific dependencies like chromadb are not available.
# This helps the system behave predictably during CI runs, allowing tests
# that might otherwise fail due to missing infrastructure to pass if they
# are designed to handle a 'test' or 'mock' mode.
os.environ["ENVIRONMENT"] = "test"
os.environ["CI_TESTING"] = "true"
os.environ["MOCK_EXTERNAL_SERVICES"] = "true" # Explicitly signal to mock external services
