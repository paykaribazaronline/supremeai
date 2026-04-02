# Environment Configuration System Guide

## Overview

SupremeAI now has a centralized environment configuration system that reads from `.env` files. This eliminates the need to search through code and documentation for tokens, API keys, database credentials, and other configuration values.

## Quick Start

### 1. Create Your Local Environment File

```bash
# Copy the template
cp .env.example .env

# Edit with your actual values
nano .env     # or use your preferred editor (vim, code, VS Code, etc.)
```

### 2. Add Your Credentials

Open `.env` and fill in the actual values for your environment:

```env
# Firebase (required if using Firebase features)
FIREBASE_PROJECT_ID=supremeai-a
FIREBASE_API_KEY=AIzaSy...
FIREBASE_DATABASE_URL=https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/
FIREBASE_SERVICE_ACCOUNT_PATH=./secrets/firebase-service-account.json

# AI API Keys (for Multi-AI Consensus System)
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...
GOOGLE_GEMINI_API_KEY=...
# ... add only the provider keys you actually plan to use

# GitHub Integration
GITHUB_TOKEN=ghp_...
GITHUB_USERNAME=your-username
GITHUB_REPO=supremeai

# Database
DATABASE_URL=jdbc:mysql://localhost:3306/supremeai
DATABASE_USERNAME=root
DATABASE_PASSWORD=your_password

# Security
JWT_SECRET_KEY=your-very-long-secret-key-at-least-32-chars
ADMIN_API_KEY=admin-key-here

# Google Cloud (for Cloud Run, BigQuery)
GOOGLE_APPLICATION_CREDENTIALS=./secrets/google-cloud-key.json
GOOGLE_CLOUD_PROJECT_ID=supremeai-123
GOOGLE_CLOUD_REGION=us-central1
```

### 3. Verify Configuration

```bash
# Start the app - it will load your .env file
./gradlew run

# You should see:
# ✅ Environment configuration loaded (profile: development)
# ✅ Loaded environment file: .env
```

## File Priority System

The system loads files in this order (later files override earlier ones):

1. **System environment variables** (highest priority)
2. **Java system properties** (e.g., -Dkey=value)
3. **`.env`** - Main configuration file
4. **`.env.local`** - Local overrides (not committed to git)
5. **`.env.{profile}`** - Profile-specific (development, staging, production)
6. **`.env.{profile}.local`** - Profile-specific local overrides (not committed)

### Example with Profiles

```bash
# Development mode
export SPRING_PROFILES_ACTIVE=development
./gradlew run
# Loads: .env → .env.local → .env.development → .env.development.local

# Staging mode
export SPRING_PROFILES_ACTIVE=staging
./gradlew run
# Loads: .env → .env.local → .env.staging → .env.staging.local

# Production mode
export SPRING_PROFILES_ACTIVE=production
./gradlew run
# Loads: .env → .env.local → .env.production → .env.production.local
```

## Using Configuration in Java Code

The `EnvConfig` class provides convenient access to environment variables:

### Basic Usage

```java
import org.example.config.EnvConfig;

// Get a configuration value with default
String firebaseKey = EnvConfig.get("FIREBASE_API_KEY", "default_key");

// Get required configuration (null if missing)
String githubToken = EnvConfig.get("GITHUB_TOKEN");

// Check if exists
if (EnvConfig.has("FIREBASE_API_KEY")) {
    // Use Firebase
}
```

### Type-Safe Access

```java
// Integer values
int jwtExpirationHours = EnvConfig.getInt("JWT_EXPIRATION_HOURS", 24);

// Long values
long maxUploadSize = EnvConfig.getLong("MAX_UPLOAD_BYTES", 10485760);

// Boolean values
boolean isProduction = EnvConfig.getBoolean("IS_PRODUCTION", false);
```

### Convenience Methods

```java
// Firebase configuration
String projectId = EnvConfig.Firebase.getProjectId();
String dbUrl = EnvConfig.Firebase.getDatabaseUrl();

// Database configuration
String dbUrl = EnvConfig.Database.getUrl();
String dbUser = EnvConfig.Database.getUsername();

// Security configuration
String jwtSecret = EnvConfig.Security.getJwtSecret();
long jwtExpiration = EnvConfig.Security.getJwtExpirationMs();

// AI API Keys
String openAiKey = EnvConfig.AI.getOpenAiKey();
String geminiKey = EnvConfig.AI.getGeminiKey();
```

## Used in Services

### Before (Hard-coded or scattered)

```java
@Service
public class FirebaseService {
    @Value("${firebase.project-id:supremeai-a}")
    private String projectId;
    
    @Value("${firebase.api-key}")  // Might be missing!
    private String apiKey;
}
```

### After (Centralized and safe)

```java
@Service
public class FirebaseService {
    private final String projectId = EnvConfig.Firebase.getProjectId();
    private final String apiKey = EnvConfig.get("FIREBASE_API_KEY");
    
    public void init() {
        if (projectId == null || apiKey == null) {
            logger.warn("Firebase credentials not configured, using fallback mode");
            return;
        }
        // Initialize Firebase...
    }
}
```

## Security Best Practices

### ✅ DO

- Store `.env` in `.gitignore` (already configured) ✓
- Use `.env.local` for personal development overrides
- Use `.env.{profile}` for environment-specific configs
- Store actual credential files in `./secrets/` folder (also gitignored)
- Use strong, unique JWT_SECRET_KEY (minimum 32 characters)
- Never commit secrets to git

### ❌ DON'T

- Add `.env` to git (it's gitignored, good!)
- Store actual API keys in code comments
- Use the same secret for all environments
- Share your `.env` or `.env.local` files via Slack/email/chat
- Commit example credentials (use placeholders in `.env.example`)

## Configuration Sections

### Firebase

```env
FIREBASE_PROJECT_ID=supremeai-a
FIREBASE_API_KEY=AIzaSy...
FIREBASE_DATABASE_URL=https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/
FIREBASE_STORAGE_BUCKET=supremeai-a.appspot.com
FIREBASE_SERVICE_ACCOUNT_PATH=./secrets/firebase-service-account.json
```

### Google Cloud Platform

```env
GOOGLE_CLOUD_PROJECT_ID=supremeai-123456
GOOGLE_CLOUD_REGION=us-central1
GOOGLE_APPLICATION_CREDENTIALS=./secrets/google-cloud-key.json
GOOGLE_CLOUD_BIGQUERY_DATASET=supremeai_analytics
```

### AI API Providers (Multi-AI Consensus)

```env
# Configure only the providers your admin wants to enable
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...
GOOGLE_GEMINI_API_KEY=...
META_API_KEY=...
MISTRAL_API_KEY=...
COHERE_API_KEY=...
HUGGINGFACE_API_KEY=...
XAI_GROK_API_KEY=...
DEEPSEEK_API_KEY_CONSENSUS=...
PERPLEXITY_API_KEY=...
```

### GitHub Integration

```env
GITHUB_TOKEN=ghp_...
GITHUB_USERNAME=your-username
GITHUB_EMAIL=your-email@example.com
GITHUB_REPO=supremeai
GITHUB_ORG=your-org
```

### Database

```env
DATABASE_URL=jdbc:mysql://localhost:3306/supremeai
DATABASE_USERNAME=root
DATABASE_PASSWORD=your_password
DATABASE_POOL_SIZE=10
```

### Authentication & Security

```env
JWT_SECRET_KEY=very-long-secret-key-at-least-32-chars
JWT_EXPIRATION_MS=86400000
ADMIN_API_KEY=admin-key-here
SUPREMEAI_SETUP_TOKEN=setup-token-for-initialization
```

### Cloud Platforms

**AWS:**

```env
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=...
AWS_REGION=us-east-1
AWS_S3_BUCKET=supremeai-bucket
```

**Azure:**

```env
AZURE_SUBSCRIPTION_ID=...
AZURE_TENANT_ID=...
AZURE_CLIENT_ID=...
AZURE_CLIENT_SECRET=...
```

### Docker & Kubernetes

```env
DOCKER_REGISTRY=docker.io
DOCKER_USERNAME=your-username
DOCKER_PASSWORD=...
KUBERNETES_CONFIG_PATH=./.kube/config
KUBERNETES_NAMESPACE=supremeai
```

### Monitoring & Logging

```env
LOG_LEVEL=INFO
LOGS_PATH=./logs/
DEBUG_MODE=false

# Datadog
DATADOG_API_KEY=...
DATADOG_SITE=datadoghq.com

# New Relic
NEW_RELIC_API_KEY=...
```

### File Storage Paths

```env
OUTPUT_DIR=./output/
BUILD_DIR=./build/
TEMP_DIR=./temp/
CACHE_DIR=./cache/
```

## Troubleshooting

### Issue: "Environment variable not found: ..."

**Solution:** Check that:

1. Variable is defined in `.env` file
2. Variable name matches exactly (case-sensitive)
3. `.env` file is not listed in `.gitignore` (it should be!)

```bash
# Verify .env file exists
ls -la .env

# Check a specific variable
grep FIREBASE_API_KEY .env
```

### Issue: Firebase not initializing

**Solution:** Verify credentials are set:

```bash
# Check if key is loaded
echo $FIREBASE_API_KEY

# If empty, add to .env and try again
echo "FIREBASE_API_KEY=your-key" >> .env
```

### Issue: Different configurations between dev/prod environments

**Solution:** Use profile-specific files:

```bash
# Development (.env.development)
FIREBASE_PROJECT_ID=supremeai-dev

# Production (.env.production)
FIREBASE_PROJECT_ID=supremeai-prod

# Run with specific profile
SPRING_PROFILES_ACTIVE=production ./gradlew run
```

## Migration Guide

### From Hard-coded Values

**Before:**

```java
String firebaseKey = "AIzaSy123456..."; // ❌ In code!
```

**After:**

```java
String firebaseKey = EnvConfig.get("FIREBASE_API_KEY"); // ✅ From .env
```

### From Spring @Value Annotations

**Before:**

```java
@Value("${firebase.api-key}")
private String apiKey;
```

**After:**

```java
private String apiKey = EnvConfig.get("FIREBASE_API_KEY");
```

### From Environment Variables (Shell)

**Before:**

```bash
export FIREBASE_API_KEY=AIzaSy...
./gradlew run
```

**After:**

```bash
# Add to .env (persistent)
echo "FIREBASE_API_KEY=AIzaSy..." >> .env

# Start app
./gradlew run
```

## Reference: All Available Variables

See **`.env.example`** for the complete list of 100+ configuration variables organized by category.

```bash
# View all available configuration variables
cat .env.example | grep -v "^#" | grep -v "^$"
```

## Summary

| Task | Command | Notes |
|------|---------|-------|
| Create local config | `cp .env.example .env` | One-time setup |
| Edit config | `nano .env` | Use your editor |
| Use in Java | `EnvConfig.get("KEY")` | Type-safe methods available |
| Set profile | `SPRING_PROFILES_ACTIVE=prod ./gradlew run` | For environment-specific configs |
| Check configuration | `./gradlew run` (look for ✅ messages) | App logs which files loaded |
| Verify secret not committed | `git status` (verify `.env` not listed) | Should be gitignored ✓ |

## Questions?

- **Missing a configuration section?** Check `.env.example` for all available variables
- **Need a new configuration variable?** Add to `.env.example` and document in this file
- **Security concerns?** Ensure `.env` and `./secrets/` are in `.gitignore` (they are!)
