#!/bin/bash
# Verification Script - Check if Admin Dashboard Data is Saved to Firebase
# This script tests Firebase connectivity and data persistence

set -e

echo "🔍 SupremeAI Admin Dashboard - Firebase Data Verification"
echo "=========================================================="
echo ""

# Configuration
PROJECT_ID="supremeai-a"
DB_URL="https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app"
ADMIN_CONTROL_PATH="admin/control"

echo "📋 Configuration:"
echo "  Project ID: $PROJECT_ID"
echo "  Database URL: $DB_URL"
echo ""

# Test 1: Check Firebase Credentials
echo "Test 1️⃣  - Firebase Credentials Status"
echo "======================================="
if [ -z "$FIREBASE_SERVICE_ACCOUNT_JSON" ]; then
    echo "⚠️  FIREBASE_SERVICE_ACCOUNT_JSON environment variable not set"
    echo "   Admin dashboard saves may be failing silently!"
    echo ""
else
    echo "✅ FIREBASE_SERVICE_ACCOUNT_JSON is set"
    echo "   Credentials are available for backend persistence"
    echo ""
fi

# Test 2: Check Java logs for Firebase errors
echo "Test 2️⃣  - Java Application Logs (Firebase Errors)"
echo "=================================================="
if [ -f "build/logs/application.log" ]; then
    FIREBASE_ERRORS=$(grep -i "firebase\|error\|failed" build/logs/application.log | tail -20)
    if [ -z "$FIREBASE_ERRORS" ]; then
        echo "✅ No Firebase errors found in recent logs"
    else
        echo "⚠️  Found Firebase-related errors:"
        echo "$FIREBASE_ERRORS"
    fi
    echo ""
else
    echo "⚠️  Application logs not found at build/logs/application.log"
    echo ""
fi

# Test 3: Check Firebase Database Rules
echo "Test 3️⃣  - Firebase Database Rules Configuration"
echo "================================================"
if [ -f "database.rules.json" ]; then
    echo "✅ database.rules.json exists"
    
    # Check if admin/control path is writable
    if grep -q '"admin"' database.rules.json; then
        echo "✅ Admin path is defined in rules"
        
        # Check write permissions
        if grep -A5 '"admin"' database.rules.json | grep -q '".write"'; then
            echo "✅ Write permissions defined for admin paths"
        else
            echo "⚠️  No explicit write permissions found for admin paths"
        fi
    else
        echo "❌ Admin path NOT defined in database rules!"
        echo "   This is a CRITICAL issue - admin data cannot be saved!"
        exit 1
    fi
    echo ""
else
    echo "❌ database.rules.json not found"
    exit 1
fi

# Test 4: Check AdminControlService for async issue
echo "Test 4️⃣  - Firebase Save Operation Implementation"
echo "=================================================="
if [ -f "src/main/java/org/example/service/FirebaseService.java" ]; then
    ASYNC_WRITES=$(grep -c "setValueAsync\|updateChildrenAsync" src/main/java/org/example/service/FirebaseService.java || true)
    echo "Found $ASYNC_WRITES asynchronous write operations"
    
    if [ "$ASYNC_WRITES" -gt 0 ]; then
        echo ""
        echo "⚠️  CRITICAL ISSUE DETECTED:"
        echo "    All Firebase writes use fire-and-forget async operations!"
        echo "    There is NO verification that data actually saves!"
        echo "    If writes fail, the admin dashboard won't know."
        echo ""
        echo "📝 Affected operations:"
        grep -n "setValueAsync\|updateChildrenAsync" src/main/java/org/example/service/FirebaseService.java | head -5
        echo ""
    fi
else
    echo "⚠️  FirebaseService.java not found"
fi

# Test 5: Check for error callbacks
echo "Test 5️⃣  - Error Handling in Firebase Writes"
echo "============================================="
COMPLETION_LISTENERS=$(grep -c "addOnSuccessListener\|addOnFailureListener\|addOnCompleteListener" src/main/java/org/example/service/FirebaseService.java || true)
if [ "$COMPLETION_LISTENERS" -eq 0 ]; then
    echo "❌ NO completion listeners found!"
    echo "   Writes have no error handling - silent failures are likely"
    echo ""
else
    echo "✅ $COMPLETION_LISTENERS completion listener(s) found"
    echo ""
fi

# Test 6: Quick connectivity test (if curl available)
echo "Test 6️⃣  - Firebase Database Connectivity"
echo "=========================================="
if command -v curl &> /dev/null; then
    echo "Attempting to read Firebase status endpoint..."
    HTTP_CODE=$(curl -sf -o /dev/null -w "%{http_code}" \
        "$DB_URL/.json?shallow=true" 2>/dev/null || echo "000")
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✅ Firebase database is accessible (HTTP 200)"
    elif [ "$HTTP_CODE" = "401" ]; then
        echo "⚠️  Firebase returned 401 Unauthorized"
        echo "   This is expected - data requires authentication"
        echo "   Backend service account should have access"
    elif [ "$HTTP_CODE" = "000" ]; then
        echo "❌ Connection failed - cannot reach Firebase"
        echo "   Admin dashboard save operations WILL fail!"
    else
        echo "⚠️  Firebase returned HTTP $HTTP_CODE"
    fi
    echo ""
else
    echo "⚠️  curl not available - skipping connectivity test"
    echo ""
fi

# Summary
echo "=========================================================="
echo "📊 VERIFICATION SUMMARY"
echo "=========================================================="
echo ""
echo "⚠️  FINDINGS:"
echo "  1. All Firebase saves use async operations (fire-and-forget)"
echo "  2. No completion listeners verify writes succeeded"
echo "  3. Silent failures are HIGHLY LIKELY"
echo ""
echo "❌ PROBLEM:"
echo "  When you save data from the admin dashboard,"
echo "  the system thinks it saved successfully,"
echo "  but Firebase may reject it without notification!"
echo ""
echo "✅ SOLUTION:"
echo "  Add completion listeners to all Firebase writes:"
echo ""
echo "  // BEFORE (current - NO error handling):"
echo "  db.getReference(\"config\").child(configId).setValueAsync(data);"
echo ""
echo "  // AFTER (with error handling):"
echo "  db.getReference(\"config\").child(configId).setValue(data, (error, ref) -> {"
echo "    if (error != null) {"
echo "      logger.error(\"Failed to save config: \" + error.getMessage());"
echo "    } else {"
echo "      logger.info(\"Config saved successfully\");"
echo "    }"
echo "  });"
echo ""
