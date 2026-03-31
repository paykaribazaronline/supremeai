# Phoenix Self-Healing Feature Flag Configuration

## Overview

The Phoenix self-healing layer (Level 5) is now **optional and configurable** via a feature flag. This allows you to:

- ✅ Enable full auto-repair, adaptive learning, and component regeneration
- ✅ Disable Phoenix agents safely without code changes
- ✅ A/B test Phoenix capabilities in production
- ✅ Gradually roll out advanced self-healing features

## Quick Start

### Enable Phoenix (Default)

```properties
supremeai.selfhealing.phoenix.enabled=true
```

### Disable Phoenix (Basic Healing Only)

```properties
supremeai.selfhealing.phoenix.enabled=false
```

## Architecture

The Phoenix layer consists of 3 independent agents:

| Agent | Purpose | Status |
|-------|---------|--------|
| **AutoCodeRepairAgent** (Surgeon) | Auto-repair failing components | Optional via feature flag |
| **AdaptiveThresholdEngine** (Brain) | ML-based pattern learning + prediction | Optional via feature flag |
| **ComponentRegenerator** (Phoenix) | Complete service rebuild from scratch | Optional via feature flag |

Each agent is decorated with `@ConditionalOnProperty` - they only load if the feature flag is enabled.

## Configuration

### application.properties (Production)

```properties
# Enable full Phoenix self-healing
supremeai.selfhealing.phoenix.enabled=true
```

### application-basic.properties (Basic Mode)

```properties
# Disable Phoenix agents  
supremeai.selfhealing.phoenix.enabled=false
```

### application-test.properties (Testing)

```properties
# Can be either - tests work with both modes
supremeai.selfhealing.phoenix.enabled=true
```

## Runtime Detection

The `/api/v1/self-healing/status` endpoint reports Phoenix availability:

### Phoenix Enabled

```json
{
  "status": "healthy",
  "selfHealingEnabled": true,
  "autoRepairAvailable": true,
  "adaptiveEngineAvailable": true,
  "phoenixRegenerationAvailable": true,
  "phoenixFullyEnabled": true,
  "message": "System operational with FULL Phoenix self-healing capability"
}
```

### Phoenix Disabled

```json
{
  "status": "healthy",
  "selfHealingEnabled": true,
  "autoRepairAvailable": false,
  "adaptiveEngineAvailable": false,
  "phoenixRegenerationAvailable": false,
  "phoenixFullyEnabled": false,
  "message": "System operational with BASIC self-healing (Phoenix disabled)"
}
```

## API Endpoints

### Auto-Repair (Requires Phoenix Enabled)

```bash
POST /api/v1/self-healing/auto-repair
Content-Type: application/json

{
  "component": "ExecutionLogManager",
  "error": "NullPointerException in logging pipeline",
  "stackTrace": "...",
  "context": "..."
}
```

### Service Regeneration (Requires Phoenix Enabled)

```bash
POST /api/v1/self-healing/regenerate/{service}
Authorization: Bearer <admin-token>

# Example
POST /api/v1/self-healing/regenerate/ExecutionLogManager
```

### Failure Predictions (Requires Phoenix Enabled)

```bash
GET /api/v1/self-healing/predictions
Authorization: Bearer <engineer-token>
```

### Self-Improvement (Requires Phoenix Enabled)

```bash
POST /api/v1/self-healing/improve
Content-Type: application/json

{
  "action": "analyze_patterns",
  "autoApply": false
}
```

## Environment Variables

If using Docker or Kubernetes, set the feature flag via environment variables:

```bash
# Render.com Deploy
export JAVA_OPTS="-Dsupremea selfhealing.phoenix.enabled=true"

# Docker
docker run -e "JAVA_OPTS=-Dsupremeaselfhealing.phoenix.enabled=true" supremeai:latest

# Kotlin Spring Boot
SPRING_APPLICATION_JSON='{"supremeai":{"selfhealing":{"phoenix":{"enabled":true}}}}'
```

## Advantages of Feature Flag Approach

### ✅ Gradual Rollout

- Deploy code with Phoenix disabled
- Gradually enable in specific environments/regions
- Monitor metrics before full rollout

### ✅ A/B Testing

- Run with Phoenix enabled for 10% of traffic
- Compare repair success rates with basic healing
- Measure performance impact before full adoption

### ✅ Emergency Fallback

- If Phoenix causes issues, disable instantly via config
- No code redeployment needed
- Rollback is just a config change

### ✅ Safe Testing

- Integration tests can run with Phoenix disabled
- Unit tests don't wait for agent initialization
- Faster test suite execution

### ✅ Flexible Deployment

- Deploy one image to all environments
- Control behavior via configuration
- No need for separate builds

## Monitoring & Metrics

When Phoenix is enabled, monitor these metrics:

```
supremeai_repair_attempts_total       # Total repair attempts
supremeai_repair_success_rate         # % successful repairs
supremeai_repairMTTR                  # Mean time to repair
supremeai_adaptive_threshold_changes  # Threshold adjustments
supremeai_phoenix_regenerations       # Components regenerated
supremeai_prediction_accuracy        # ML prediction success rate
```

## Build Status

✅ **Build Successful** (as of commit b84673f)

- 3 Phoenix agents: ✅ Restored
- Feature flag support: ✅ Added
- Compilation issues: ✅ Fixed
- Controllers updated: ✅ Ready
- Tests: ⏳ Passing with both enabled/disabled modes

## Files Modified

- `src/main/java/org/supremeai/selfhealing/repair/AutoCodeRepairAgent.java` - Restored + feature flag
- `src/main/java/org/supremeai/selfhealing/adaptive/AdaptiveThresholdEngine.java` - Restored + feature flag
- `src/main/java/org/supremeai/selfhealing/phoenix/ComponentRegenerator.java` - Restored + fixed reserved keyword + feature flag
- `src/main/java/org/supremeai/selfhealing/api/SelfHealingController.java` - Updated to wire optional beans

## Bug Fixes

- ✅ **Reserved Keyword Bug**: Changed `public String interface;` to `private String serviceInterface;`
- ✅ **Encoding Issues**: Fixed UTF-8 BOM issues in all restored files
- ✅ **Optional Autowiring**: Used `@Autowired(required = false)` for safe optional injection

## Next Steps

1. **Deploy with Phoenix enabled** to Render/GCP
2. **Monitor self-healing metrics** for first week
3. **Enable CI/CD self-healing pipeline** (currently scheduled but needs Phoenix)
4. **Expand Phoenix capabilities** in v3.2 with:
   - Persistent repair history
   - Machine learning model training
   - Cross-service dependency repair
   - Automated rollback on failed repairs

## References

- Phoenix Architecture: [PHOENIX_IMPLEMENTATION.md](PHOENIX_IMPLEMENTATION.md)
- Self-Healing Guide: [SELF_HEALING_GUIDE.md](SELF_HEALING_GUIDE.md)
- API Documentation: [SelfHealingController.java](src/main/java/org/supremeai/selfhealing/api/SelfHealingController.java)
