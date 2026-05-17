# Provider Activation & Status Management Guide

## Problem Solved

**Issue:** All AI providers (real/active and fake/test) were being treated as `INACTIVE`, preventing system from using any AI models including GCloud.

**Root Cause:** 
- When providers were added, they were ALWAYS set to `"inactive"` status by default
- GCloud provider validation was failing due to simple `generate("hi")` test
- No mechanism existed to manually activate providers

## Solution Implemented

### 1. **Automatic Activation on Valid Key**
When a provider is added with a valid API key, it's now automatically set to `ACTIVE` status.

```java
// Before: Always set to "inactive"
provider.setStatus("inactive");

// Now: Set to "active" if validation passes
provider.setStatus("active");
```

### 2. **Enhanced GCloud Provider Validation**
GCloud/Gemini providers now use basic format validation instead of test API calls:

```java
// For GCloud/Gemini: Check key length (> 10 chars)
if (type.equalsIgnoreCase("GOOGLE") || type.equalsIgnoreCase("gemini")) {
    if (apiKey.length() > 10) {
        return Mono.just(true);  // Valid
    }
}

// For other providers: Full validation with 10-second timeout
```

### 3. **Manual Provider Activation & Deactivation Endpoints**

#### **Activate a Provider (set status to ACTIVE)**
```bash
POST /api/admin/providers/{id}/activate

# Example:
curl -X POST "http://localhost:8080/api/admin/providers/gemini-prod/activate" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Response:
{
  "success": true,
  "data": {
    "message": "Provider activated successfully",
    "status": "ACTIVE",
    "provider": {
      "id": "gemini-prod",
      "name": "Gemini Production",
      "status": "active",
      "type": "GOOGLE",
      "deploymentSource": "GCLOUD"
    }
  }
}
```

#### **Deactivate a Provider (set status to INACTIVE)**
```bash
POST /api/admin/providers/{id}/deactivate?reason=REASON_TEXT

# Example - Mark fake key as inactive:
curl -X POST "http://localhost:8080/api/admin/providers/test-key-1/deactivate?reason=Fake%20test%20key" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Response:
{
  "success": true,
  "data": {
    "message": "Provider deactivated successfully",
    "status": "INACTIVE",
    "reason": "Fake test key",
    "provider": {
      "id": "test-key-1",
      "status": "inactive",
      "lastErrorMessage": "Fake test key"
    }
  }
}
```

## Workflow Examples

### Scenario 1: Add Real GCloud API Key

```bash
# 1. Add provider with real Gemini API key
POST /api/admin/providers/add
{
  "name": "Gemini Production",
  "type": "GOOGLE",
  "apiKey": "AIzaSyDxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "deploymentSource": "GCLOUD",
  "baseUrl": "https://generativelanguage.googleapis.com/v1beta"
}

# ✅ AUTOMATIC: Status set to "active" (validation passed)
```

### Scenario 2: Add Fake Test Key, Then Deactivate

```bash
# 1. Add fake test key (for testing/demo)
POST /api/admin/providers/add
{
  "name": "Test Key",
  "type": "GOOGLE",
  "apiKey": "fake_test_key_12345",  # > 10 chars, so format "valid"
  "deploymentSource": "API"
}

# ✅ AUTOMATIC: Status set to "active" (format check passed)

# 2. Later, manually deactivate fake key
POST /api/admin/providers/test-key-1/deactivate?reason=Testing%20only%20-%20fake%20key

# ✅ Status now "inactive" (won't be used for actual requests)
```

### Scenario 3: Provider Status Overview

```bash
# Get health stats and provider status
GET /api/admin/providers/health-stats

# Response shows real vs fake:
{
  "success": true,
  "data": {
    "total": 5,
    "active": 3,        # Real/valid providers
    "inactive": 2,      # Fake/test providers or disabled
    "error": 0,
    "rotating": 0,
    "dead": 0,
    "healthScore": 60   # (3 active / 5 total)
  }
}
```

## Provider Status States

| Status | Meaning | Use Case |
|--------|---------|----------|
| `ACTIVE` | Provider is ready and valid | Real API key, actively being used |
| `INACTIVE` | Provider is disabled/test | Fake test keys, admin-disabled |
| `ERROR` | Provider failed recently | Quota exceeded, temporary failure |
| `ROTATING` | Replacing dead key | Key rotation in progress |
| `DEAD` | Provider permanently failed | Invalid key, account deleted |

## How System Uses Provider Status

### ✅ Only `ACTIVE` providers are used for:
- AI model queries
- Multi-consensus voting
- Fallback routing
- Task assignment

### ❌ `INACTIVE` providers are:
- Skipped in provider selection
- Not used for AI requests
- Useful for testing without affecting production

## Configuration for GCloud/Vertex AI

```yaml
# application.yml or application.properties
ai:
  providers:
    google:
      type: GOOGLE
      deploymentSource: GCLOUD
      apiKey: ${GOOGLE_API_KEY}
      baseUrl: https://generativelanguage.googleapis.com/v1beta
```

## Monitoring Provider Health

```bash
# Get all providers and their current status
GET /api/admin/providers/configured

# Test a key before adding
POST /api/admin/providers/test-key
{
  "name": "GOOGLE",
  "apiKey": "YOUR_KEY_HERE"
}

# Run health check on all providers
GET /api/admin/providers/health-stats
```

## FAQ

**Q: Why is my GCloud provider showing as INACTIVE?**  
A: It wasn't automatically activated. Use `POST /api/admin/providers/{id}/activate` to manually activate it.

**Q: How do I tell if an API key is real or fake?**  
A: Check provider status:
- Real keys → Status: `ACTIVE` (passed validation)
- Fake keys → Status: `INACTIVE` (admin deactivated) or check in logs

**Q: Can I have multiple GCloud providers?**  
A: Yes! Add multiple providers with different API keys, all set to `ACTIVE` or `INACTIVE` based on whether they're real.

**Q: Will fake keys break the system?**  
A: No - if they're marked as `INACTIVE`, they won't be used. If marked as `ACTIVE`, they may cause errors in AI requests, but system has fallbacks.

## Next Steps

1. **Activate all real GCloud providers:**
   ```bash
   curl -X POST "http://localhost:8080/api/admin/providers/gemini-1/activate"
   curl -X POST "http://localhost:8080/api/admin/providers/vertex-ai-1/activate"
   ```

2. **Deactivate test/fake keys:**
   ```bash
   curl -X POST "http://localhost:8080/api/admin/providers/test-1/deactivate?reason=Test%20key"
   ```

3. **Verify health:**
   ```bash
   curl "http://localhost:8080/api/admin/providers/health-stats"
   ```

4. **Test AI requests** - system should now use active providers
