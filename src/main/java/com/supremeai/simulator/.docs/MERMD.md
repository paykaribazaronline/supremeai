# MERMD - Simulator Feature

## Overview

The Simulator feature enables users to install, run, and test Android applications in virtual devices directly from the SupremeAI platform.

## How It Works

### Architecture Flow

```
User Request → REST API → SimulatorService → Deployment Service → Android Emulator → Live Preview
```

### Key Components

| Component                    | File                                      | Purpose                                 |
| ---------------------------- | ----------------------------------------- | --------------------------------------- |
| `SimulatorController`        | `SimulatorController.java`                | REST endpoints for simulator operations |
| `SimulatorService`           | `service/SimulatorService.java`           | Business logic orchestration            |
| `SimulatorDeploymentService` | `service/SimulatorDeploymentService.java` | App deployment management               |
| `DeviceEmulationService`     | `service/DeviceEmulationService.java`     | Device profile handling                 |
| `UserSimulatorProfile`       | `model/UserSimulatorProfile.java`         | User's simulator configuration          |

### API Endpoints

#### Profile Management

- `GET /api/simulator/profile` - Get user's simulator profile
- `POST /api/simulator/profile` - Update profile (quota, device settings)

#### Installation

- `POST /api/simulator/install` - Install an app to the simulator
- `DELETE /api/simulator/install/{appId}` - Uninstall an app
- `GET /api/simulator/installed` - List installed apps

#### Sessions

- `POST /api/simulator/session/start` - Start a simulator session
- `POST /api/simulator/session/stop` - Stop current session
- `GET /api/simulator/session/status` - Get session status

#### Admin

- `GET /api/simulator/admin/usage` - Get all deployments (ADMIN)
- `POST /api/simulator/admin/set-quota/{userId}` - Override quota (ADMIN)

### Data Models

#### UserSimulatorProfile

```java
class UserSimulatorProfile {
    String userId;
    int installQuota;           // Max apps user can install
    List<InstalledApp> installedApps;
    DeviceProfile currentDevice;
}
```

#### InstalledApp

```java
class InstalledApp {
    String appId;
    String appName;
    String version;
    String deployedUrl;         // Live preview URL
    String status;
    LocalDateTime installedAt;
    int launchCount;
}
```

### Device Profiles

Available device types:

- PIXEL_6, PIXEL_5, SAMSUNG_S21, ONEPLUS_9, etc.

Each profile includes:

- OS version
- Screen resolution
- DPI density

### Quota System

- Default quota: 5 installations per user
- Admin can override (1-20 range)
- Active installs count toward quota

### Session Management

- WebSocket-based session control
- Heartbeat tracking
- State management (IDLE, RUNNING, STOPPED)

### Integration Points

- `SimulatorWebSocketHandler` - Real-time communication
- `FirebaseEmulatorController` - Firebase integration
- `WebSocketConfig` - WebSocket configuration
