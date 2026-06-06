# MERMD - Repository Layer

## Overview

The Repository layer provides data persistence using Firestore and reactive programming.

## How It Works

### Architecture Flow

```
Service → Repository Interface → Firestore → Database
```

## Key Repositories

### Main Repositories (`repository/`)

| Repository                       | Purpose             |
| -------------------------------- | ------------------- |
| `UserSimulatorProfileRepository` | User simulator data |
| `UserApiKeyRepository`           | API key persistence |
| `ProviderRepository`             | AI provider config  |
| `ProjectRepository`              | Project management  |

### Subpackage Repositories

#### `repository/analysis/`

- Analysis result storage

#### `repository/browser/`

- Browser automation data

## Repository Pattern

```java
@Repository
public interface UserSimulatorProfileRepository
    extends ReactiveFirestoreRepository<UserSimulatorProfile, String> {

    Flux<UserSimulatorProfile> findByUserId(String userId);
}
```

## Features

- Reactive streams (`Flux`, `Mono`)
- Firestore integration
- Automatic query generation
- Audit fields support
