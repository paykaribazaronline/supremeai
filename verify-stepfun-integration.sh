#!/bin/bash
# StepFun Integration Verification Script
# Run this after implementing StepFun provider to verify everything works

echo "=========================================="
echo "StepFun Integration Verification"
echo "=========================================="
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if backend is running
echo "1. Checking if backend is running..."
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Backend is running on port 8080${NC}"
else
    echo -e "${RED}✗ Backend not running. Start it with: ./gradlew bootRun${NC}"
    exit 1
fi

# Check if StepFun provider is registered
echo ""
echo "2. Checking if StepFun provider is registered..."
PROVIDERS=$(curl -s http://localhost:8080/api/providers/list)
if echo "$PROVIDERS" | grep -q "stepfun"; then
    echo -e "${GREEN}✓ StepFun found in provider list${NC}"
else
    echo -e "${RED}✗ StepFun NOT in provider list. Check AIProviderFactory.java${NC}"
    exit 1
fi

# Check environment variable
echo ""
echo "3. Checking STEPFUN_API_KEY environment variable..."
if [ -f .env ]; then
    source .env 2>/dev/null || true
fi

if [ -n "$STEPFUN_API_KEY" ] && [ "$STEPFUN_API_KEY" != "sf-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" ]; then
    echo -e "${GREEN}✓ STEPFUN_API_KEY is set${NC}"
    echo "   Key starts with: ${STEPFUN_API_KEY:0:10}..."
else
    echo -e "${YELLOW}⚠ STEPFUN_API_KEY not set or using placeholder${NC}"
    echo "   Set it in .env file:"
    echo "   export STEPFUN_API_KEY=sf-your-actual-key"
fi

# Test direct generation (if key is set)
if [ -n "$STEPFUN_API_KEY" ] && [[ ! "$STEPFUN_API_KEY" =~ "xxxxxxxx" ]]; then
    echo ""
    echo "4. Testing StepFun generation (simple prompt)..."
    RESPONSE=$(curl -s -X POST http://localhost:8080/api/ai/generate \
      -H "Content-Type: application/json" \
      -d "{\"provider\":\"stepfun\",\"prompt\":\"Say 'Hello from StepFun'\"}" 2>/dev/null)

    if echo "$RESPONSE" | grep -q "Hello"; then
        echo -e "${GREEN}✓ Generation successful!${NC}"
        echo "   Response: ${RESPONSE:0:100}..."
    else
        echo -e "${RED}✗ Generation failed${NC}"
        echo "   Response: $RESPONSE"
    fi
else
    echo ""
    echo -e "${YELLOW}4. Skipping generation test (no valid API key)${NC}"
fi

# Check logs for errors
echo ""
echo "5. Checking logs for StepFun errors..."
if [ -f logs/supremeai.log ]; then
    ERROR_COUNT=$(grep -i "stepfun.*error\|STEPFUN.*ERROR" logs/supremeai.log 2>/dev/null | wc -l)
    if [ "$ERROR_COUNT" -eq 0 ]; then
        echo -e "${GREEN}✓ No StepFun errors in logs${NC}"
    else
        echo -e "${RED}✗ Found $ERROR_COUNT StepFun errors in logs${NC}"
        echo "   Recent errors:"
        grep -i "stepfun.*error\|STEPFUN.*ERROR" logs/supremeai.log | tail -3
    fi
else
    echo -e "${YELLOW}⚠ Log file not found at logs/supremeai.log${NC}"
fi

# Verify frontend integration
echo ""
echo "6. Checking frontend integration..."
if [ -f dashboard/src/components/APIKeysManager.tsx ]; then
    if grep -q "step-3.5-flash" dashboard/src/components/APIKeysManager.tsx; then
        echo -e "${GREEN}✓ StepFun models added to frontend${NC}"
    else
        echo -e "${RED}✗ StepFun models NOT in APIKeysManager.tsx${NC}"
    fi

    if grep -q "stepfun: 'https://api.stepfun.com/v1'" dashboard/src/components/APIKeysManager.tsx; then
        echo -e "${GREEN}✓ StepFun endpoint configured in frontend${NC}"
    else
        echo -e "${RED}✗ StepFun endpoint NOT in PROVIDER_ENDPOINTS${NC}"
    fi
else
    echo -e "${RED}✗ APIKeysManager.tsx not found${NC}"
fi

# Summary
echo ""
echo "=========================================="
echo "Verification Complete"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. If any errors above, fix them"
echo "2. Add your real StepFun API key to .env"
echo "3. Restart backend: ./gradlew bootRun"
echo "4. Open dashboard: http://localhost:5173/admin/apikeys"
echo "5. Add StepFun provider with your key"
echo ""
