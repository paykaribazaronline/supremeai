# MERMD - DTO Layer

## Overview
The DTO layer contains data transfer objects for API requests and responses.

## How It Works

### Architecture Flow
```
API Request → Controller → DTO → Service → Model
```

## Key DTOs

### Request DTOs
| DTO | Purpose |
|-----|---------|
| `UserCreateDTO` | User creation request |
| `ApiKeyCreateDTO` | API key creation |
| `ChatRequest` | Chat message request |
| `TranslationRequest` | Translation request |
| `ProjectCreateRequest` | Project creation |

### Response DTOs
| DTO | Purpose |
|-----|---------|
| `UserDTO` | User response |
| `UserApiKeyDTO` | API key response |
| `ProjectDTO` | Project response |
| `TranslationResponse` | Translation result |

### Valid DTOs (`dto/valid/`)
| DTO | Purpose |
|-----|---------|
| `UserCreateDTO` | Validated user creation |
| `ApiKeyCreateDTO` | Validated API key creation |

## Structure
```java
public class UserSimulatorProfile {
    String userId;
    int installQuota;
    List<InstalledApp> installedApps;
}
```

## Usage
- Used in Controller method signatures
- Validated with `@Valid` annotation
- Converted to Models by services