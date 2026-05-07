#!/bin/bash
# CodeGeeX4 Integration Verification Script
# This script verifies that CodeGeeX4 is properly integrated into SupremeAI

echo "=========================================="
echo "CodeGeeX4 Integration Verification"
echo "=========================================="
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PASS=0
FAIL=0

# Test 1: Check CodeGeeX4Provider.java exists
echo -n "Test 1: CodeGeeX4Provider.java exists... "
if [ -f "src/main/java/com/supremeai/provider/CodeGeeX4Provider.java" ]; then
    echo -e "${GREEN}✓ PASS${NC}"
    ((PASS++))
else
    echo -e "${RED}✗ FAIL${NC}"
    ((FAIL++))
fi

# Test 2: Check CodeGeeX4Provider.java has correct class name
echo -n "Test 2: CodeGeeX4Provider class defined... "
if grep -q "public class CodeGeeX4Provider" src/main/java/com/supremeai/provider/CodeGeeX4Provider.java; then
    echo -e "${GREEN}✓ PASS${NC}"
    ((PASS++))
else
    echo -e "${RED}✗ FAIL${NC}"
    ((FAIL++))
fi

# Test 3: Check CodeGeeX4Provider extends AbstractHttpProvider
echo -n "Test 3: Extends AbstractHttpProvider... "
if grep -q "extends AbstractHttpProvider" src/main/java/com/supremeai/provider/CodeGeeX4Provider.java; then
    echo -e "${GREEN}✓ PASS${NC}"
    ((PASS++))
else
    echo -e "${RED}✗ FAIL${NC}"
    ((FAIL++))
fi

# Test 4: Check getName() returns "codegeex4"
echo -n "Test 4: getName() returns 'codegeex4'... "
if grep -q 'return "codegeex4"' src/main/java/com/supremeai/provider/CodeGeeX4Provider.java; then
    echo -e "${GREEN}✓ PASS${NC}"
    ((PASS++))
else
    echo -e "${RED}✗ FAIL${NC}"
    ((FAIL++))
fi

# Test 5: Check AIProviderFactory has codegeex4 case
echo -n "Test 5: AIProviderFactory has codegeex4 case... "
if grep -q 'case "codegeex4":' src/main/java/com/supremeai/provider/AIProviderFactory.java; then
    echo -e "${GREEN}✓ PASS${NC}"
    ((PASS++))
else
    echo -e "${RED}✗ FAIL${NC}"
    ((FAIL++))
fi

# Test 6: Check codegeex4 in supported providers
echo -n "Test 6: codegeex4 in supported providers... "
if grep -q '"codegeex4"' src/main/java/com/supremeai/provider/AIProviderFactory.java; then
    echo -e "${GREEN}✓ PASS${NC}"
    ((PASS++))
else
    echo -e "${RED}✗ FAIL${NC}"
    ((FAIL++))
fi

# Test 7: Check ProviderConfig has codegeex4ApiKey
echo -n "Test 7: ProviderConfig has codegeex4ApiKey... "
if grep -q "codegeex4ApiKey" src/main/java/com/supremeai/config/ProviderConfig.java; then
    echo -e "${GREEN}✓ PASS${NC}"
    ((PASS++))
else
    echo -e "${RED}✗ FAIL${NC}"
    ((FAIL++))
fi

# Test 8: Check application.properties has CodeGeeX4 config
echo -n "Test 8: application.properties has CodeGeeX4 config... "
if grep -q "supremeai.provider.codegeex4.api-key" src/main/resources/application.properties; then
    echo -e "${GREEN}✓ PASS${NC}"
    ((PASS++))
else
    echo -e "${RED}✗ FAIL${NC}"
    ((FAIL++))
fi

# Test 9: Check .env has CODEGEEX4_API_KEY
echo -n "Test 9: .env has CODEGEEX4_API_KEY... "
if grep -q "CODEGEEX4_API_KEY" .env; then
    echo -e "${GREEN}✓ PASS${NC}"
    ((PASS++))
else
    echo -e "${RED}✗ FAIL${NC}"
    ((FAIL++))
fi

# Test 10: Check tests exist
echo -n "Test 10: CodeGeeX4ProviderTest exists... "
if [ -f "src/test/java/com/supremeai/provider/CodeGeeX4ProviderTest.java" ]; then
    echo -e "${GREEN}✓ PASS${NC}"
    ((PASS++))
else
    echo -e "${RED}✗ FAIL${NC}"
    ((FAIL++))
fi

echo ""
echo "=========================================="
echo "Results: $PASS passed, $FAIL failed"
echo "=========================================="
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}✅ All tests passed! CodeGeeX4 is properly integrated.${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Get API key from https://bigmodel.cn"
    echo "2. Add to .env: CODEGEEX4_API_KEY=your-key"
    echo "3. Build: ./gradlew clean build -x test"
    echo "4. Run: ./gradlew bootRun"
    exit 0
else
    echo -e "${RED}❌ Some tests failed. Please check the implementation.${NC}"
    exit 1
fi
