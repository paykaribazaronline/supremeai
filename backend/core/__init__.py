# FILE_PATH: core/__init__.py
"""
Core package initialization.
Defines configuration flags and features for various system aspects,
including CI/CD pipeline readiness.
"""

# Feature flags for CI/CD production readiness checks.
# This list is used to ensure that specific steps or checks are included
# in the CI/CD workflow, verifying adherence to production readiness standards.
# The 'detect-changes' flag specifically indicates the necessity for a CI step
# that intelligently detects changes to optimize pipeline execution.
PRODUCTION_READINESS_FEATURES = [
    "detect-changes",
    # Add other production readiness features here as needed.
]

# Example of other package-level configurations or variables:
# PACKAGE_VERSION = "0.1.0"
# DEFAULT_API_TIMEOUT_SECONDS = 30
