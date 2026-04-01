# Configuration Quick Reference

## 30-Second Setup

```bash
# Copy template to your local config
cp .env.example .env

# Open and edit with your credentials
code .env    # VS Code
# or
nano .env    # Terminal editor

# Start app
./gradlew run

# Look for this message:
# ✅ Environment configuration loaded (profile: development)
# ✅ Loaded environment file: .env
```

## Essential Variables to Set

These are the most important ones to configure:

```env
# Firebase (AI Learning Feature)
FIREBASE_PROJECT_ID=supremeai-a
FIREBASE_API_KEY=AIzaSy...

# AI APIs (Multi-AI Consensus - pick at least 1-2)
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...
GOOGLE_GEMINI_API_KEY=...

# GitHub (Git Automation)
GITHUB_TOKEN=ghp_...

# Database (if using MySQL instead of H2)
DATABASE_URL=jdbc:mysql://localhost:3306/supremeai
DATABASE_USERNAME=root
DATABASE_PASSWORD=your_password

# Security (IMPORTANT: Use strong key)
JWT_SECRET_KEY=very-long-secret-key-at-least-32-characters-here
```

## Using in Java Code

### Get Configuration
```java
import org.example.config.EnvConfig;

// String with default
String apiKey = EnvConfig.get("OPENAI_API_KEY", "default");

// String required (null if missing)
String firebaseKey = EnvConfig.get("FIREBASE_API_KEY");

// Check if exists
if (EnvConfig.has("FIREBASE_API_KEY")) {
    // Use Firebase feature
}
```

### Type-Safe Access
```java
int port = EnvConfig.getInt("SERVER_PORT", 8080);
long timeout = EnvConfig.getLong("API_TIMEOUT_MS", 30000);
boolean debug = EnvConfig.getBoolean("DEBUG_MODE", false);
```

### Convenience Methods
```java
// Pre-configured accessor classes
String projectId = EnvConfig.Firebase.getProjectId();
String jwtSecret = EnvConfig.Security.getJwtSecret();
String openAiKey = EnvConfig.AI.getOpenAiKey();
```

## File Structure

```
supremeai/
├── .env.example          ← Template (commit to git)
├── .env                  ← Your local config (DO NOT COMMIT)
├── .env.local            ← Local overrides (DO NOT COMMIT)
├── .env.development      ← Development profile
├── .env.production       ← Production profile
├── .gitignore            ← Already has .env* entries
├── secrets/              ← Store JSON key files here
│   ├── firebase-service-account.json
│   ├── google-cloud-key.json
│   └── aws-credentials.json
└── src/main/java/org/example/config/
    └── EnvConfig.java    ← Configuration loader class
```

## Profile-Based Configuration

```bash
# Development (default)
./gradlew run
# Loads: .env, .env.local, .env.development, .env.development.local

# Staging
SPRING_PROFILES_ACTIVE=staging ./gradlew run

# Production
SPRING_PROFILES_ACTIVE=production ./gradlew run
```

## Environment-Specific Example

```env
# .env (all environments)
FIREBASE_PROJECT_ID=supremeai-a
DEBUG_MODE=false

# .env.development (local override)
DEBUG_MODE=true
DATABASE_URL=jdbc:h2:mem:testdb

# .env.production (only in production)
DATABASE_URL=jdbc:mysql://prod-db:3306/supremeai
FIREBASE_PROJECT_ID=supremeai-prod

# .env.local (YOUR personal overrides - never commit)
OPENAI_API_KEY=sk-your-personal-key
```

## Common Tasks

### Add New Configuration Variable

1. Define in `.env.example`:
   ```env
   # Feature X Configuration
   FEATURE_X_ENABLED=true
   FEATURE_X_API_KEY=your-key-here
   ```

2. Define in `.env` with your value:
   ```env
   FEATURE_X_ENABLED=true
   FEATURE_X_API_KEY=actual-key
   ```

3. Use in Java:
   ```java
   boolean featureEnabled = EnvConfig.getBoolean("FEATURE_X_ENABLED", false);
   String apiKey = EnvConfig.get("FEATURE_X_API_KEY");
   ```

### Add Environment-Specific Override

```env
# .env.staging/production - different Firebase project
FIREBASE_PROJECT_ID=supremeai-staging
```

### Debug Configuration Loading

```bash
# Run with debug logging
LOG_LEVEL=DEBUG ./gradlew run

# Check your .env file was read
grep -c "=" .env

# Verify variable is set (in terminal)
echo %OPENAI_API_KEY%    # Windows
echo $OPENAI_API_KEY     # Mac/Linux
```

## Security Checklist

- ✅ `.env` is in `.gitignore` (never commits secrets)
- ✅ `.env.local` is in `.gitignore` (local overrides stay local)
- ✅ `secrets/` folder is in `.gitignore` (JSON keys stay safe)
- ✅ Use `.env.production` for production configs (different from dev)
- ✅ JWT_SECRET_KEY is strong (32+ characters, randomized)
- ✅ API keys are never logged (checked by EnvConfig)
- ✅ `.env.example` has only placeholder values (safe to commit)

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "Environment variable not found" | Add to `.env`, check spelling (case-sensitive) |
| Firebase not working | Verify FIREBASE_API_KEY is in `.env` |
| Different config per environment | Use `.env.production` with different values |
| Can't find a configuration key | Search `.env.example` for the variable name |
| Variable always null | Make sure you edited `.env` (not `.env.example`) |
| App won't start - "missing bean" | Not a configuration issue - check logs for details |

## Examples

### Setup Development Environment

```bash
# Copy template
cp .env.example .env

# Add minimal config for testing
cat >> .env << 'EOF'
FIREBASE_API_KEY=test-key
GITHUB_TOKEN=ghp_test
OPENAI_API_KEY=sk-test
JWT_SECRET_KEY=my-super-secret-key-that-is-very-long-and-random-1234567890
EOF

# Start
./gradlew run
```

### Setup Production Environment

```bash
# Copy template
cp .env.example .env.production

# Edit with production credentials
nano .env.production

# Define differences from development
cat >> .env.production << 'EOF'
SPRING_PROFILES_ACTIVE=production
DEBUG_MODE=false
DATABASE_URL=jdbc:mysql://prod-db:3306/supremeai
FIREBASE_PROJECT_ID=supremeai-prod
EOF

# Run production
SPRING_PROFILES_ACTIVE=production ./gradlew run
```

## See Also

- Full guide: `ENVIRONMENT_CONFIGURATION.md`
- Configuration class: `src/main/java/org/example/config/EnvConfig.java`
- Template: `.env.example` (all 100+ variables)
