# MERMD - Model Layer

## Overview
The Model layer contains domain entities and data structures.

## How It Works

### Architecture Flow
```
Database → Repository → Model → Service → Controller → API
```

## Key Models

### Core Models (`model/`)

| Model | Purpose |
|-------|---------|
| `UserSimulatorProfile` | User's simulator configuration |
| `SimulatorDeploymentRecord` | Deployment history |
| `UserApiKey` | API key entity |
| `UserTier` | Subscription tier |

### Subpackage Models

#### `model/analysis/`
- Analysis results and metrics

#### `model/browser/`
- Browser automation models

## UserSimulatorProfile Structure
```java
public class UserSimulatorProfile {
    String userId;              // Firebase UID
    int installQuota;         // Max installations
    List<InstalledApp> installedApps;
    DeviceProfile currentDevice;
    
    public class InstalledApp {
        String appId;
        String appName;
        String version;
        String deployedUrl;
        String status;
        LocalDateTime installedAt;
        int launchCount;
    }
    
    public class DeviceProfile {
        DeviceType type;
        String osVersion;
        String screenResolution;
        int densityDpi;
    }
}
```

## DTOs vs Models
- **Models**: Internal domain representation
- **DTOs**: API input/output structures (in `dto/` package)