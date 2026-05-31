#!/bin/bash
# AI Validation Script - validates model integration and basic health checks
echo "Running AI validation checks..."
python3 scripts/test/validate_all.py "$@"