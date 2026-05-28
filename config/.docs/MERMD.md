# MERMD - Configuration

## Overview
Configuration files and scripts for SupremeAI deployment.

## How It Works

### Architecture Flow
```
Config Files → Application → Runtime Configuration
```

## Key Files

| File | Purpose |
|------|---------|
| `build.gradle.kts` | Gradle build configuration |
| `settings.gradle.kts` | Gradle settings |
| `application-local.properties` | Local environment config |
| `application-cloud.properties` | Cloud deployment config |
| `firebase.json` | Firebase deployment config |
| `Dockerfile` | Container configuration |
| `local.properties` | Local build properties |

## Configuration Structure
- `config/` directory - Additional config files
- Environment-specific properties
- Secret management via `security/` package