# Environment Variables Reference

**Version:** 1.0  
**Last Updated:** April 5, 2026  
**Purpose:** Complete reference for all SupremeAI environment variables

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Required Variables](#required-variables)
3. [Firebase Configuration](#firebase-configuration)
4. [AI Provider Configuration](#ai-provider-configuration)
5. [Security & Authentication](#security--authentication)
6. [Database Configuration](#database-configuration)
7. [Notification Channels](#notification-channels)
8. [Monitoring & Observability](#monitoring--observability)
9. [Deployment Configuration](#deployment-configuration)
10. [Feature Flags](#feature-flags)
11. [Development Settings](#development-settings)
12. [Security Best Practices](#security-best-practices)

---

## Quick Start

### Minimum Required Variables

For local development, you need at least these variables:

```properties
# Firebase
FIREBASE_SERVICE_ACCOUNT=your_base64_service_account
SUPREMEAI_FIREBASE_WEB_API_KEY=your_web_api_key

# AI Provider (at least one)
GEMINI_API_KEY=your_gemini_key

# Security
JWT_SECRET=your_jwt_secret_min_32_chars
BOOTSTRAP_TOKEN=your_bootstrap_token

# Admin
SUPREMEAI_ADMIN_EMAIL=admin@example.com
SUPREMEAI_ADMIN_PASSWORD=your_secure_password
```

---

## Required Variables

These variables are **essential** for the application to start:

| Variable | Description | Example |
|----------|-------------|---------|
| `FIREBASE_SERVICE_ACCOUNT` | Base64-encoded Firebase service account JSON | `eyJ0eXBlIjo...` |
| `JWT_SECRET` | Secret key for JWT token signing (min 32 chars) | `your-super-secret-key-here-32chars` |
| `BOOTSTRAP_TOKEN` | Token for initial admin creation | `bootstrap-token-12345` |
| `SUPREMEAI_ADMIN_EMAIL` | Default admin email address | `admin@supremeai.com` |
| `SUPREMEAI_ADMIN_PASSWORD` | Default admin password | `SecurePass123!` |

---

## Firebase Configuration

### Core Firebase Settings

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `FIREBASE_PROJECT_ID` | Yes | - | Your Firebase project ID |
| `FIREBASE_DATABASE_URL` | Yes | - | Realtime Database URL |
| `FIREBASE_STORAGE_BUCKET` | Yes | - | Cloud Storage bucket |
| `FIREBASE_SERVICE_ACCOUNT` | Yes | - | Base64 service account JSON |
| `SUPREMEAI_FIREBASE_WEB_API_KEY` | Yes | - | Web API key for auth |

### Optional Firebase Settings

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `FIREBASE_CREDENTIALS_FILE` | No | - | Path to service account JSON file |
| `FIREBASE_API_KEY` | No | - | Alternative API key reference |

### Example Firebase Configuration

```properties
FIREBASE_PROJECT_ID=supremeai-prod
FIREBASE_DATABASE_URL=https://supremeai-prod.firebaseio.com
FIREBASE_STORAGE_BUCKET=supremeai-prod.appspot.com
FIREBASE_SERVICE_ACCOUNT=eyJ0eXBlIjoic2VydmljZV9hY2NvdW50In0...
SUPREMEAI_FIREBASE_WEB_API_KEY=AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8
```

---

## AI Provider Configuration

### Primary Providers

Configure at least one AI provider. Multiple providers enable failover.

#### Google Gemini (Recommended)

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `GEMINI_API_KEY` | Yes* | - | Gemini API key |
| `GEMINI_MODEL` | No | `gemini-2.0-flash` | Model to use |

#### OpenAI

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `OPENAI_API_KEY` | Yes* | - | OpenAI API key |
| `OPENAI_MODEL` | No | `gpt-4` | Model to use |

#### DeepSeek

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DEEPSEEK_API_KEY` | Yes* | - | DeepSeek API key |
| `DEEPSEEK_MODEL` | No | `deepseek-chat` | Model to use |

### Additional Providers

| Variable | Description |
|----------|-------------|
| `ANTHROPIC_API_KEY` | Anthropic Claude API key |
| `MISTRAL_API_KEY` | Mistral AI API key |
| `COHERE_API_KEY` | Cohere API key |
| `HUGGINGFACE_API_KEY` | Hugging Face API key |
| `XAI_GROK_API_KEY` | xAI Grok API key |
| `PERPLEXITY_API_KEY` | Perplexity API key |
| `META_API_KEY` | Meta/Llama API key |

### Provider Priority

The system uses providers in this priority order:

1. Gemini (primary)
2. OpenAI (secondary)
3. DeepSeek (tertiary)
4. Groq (quaternary)

---

## Security & Authentication

### JWT Configuration

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | Yes | - | JWT signing secret (min 32 chars) |
| `JWT_SECRET_KEY` | No | - | Alternative JWT secret |
| `JWT_EXPIRATION_MS` | No | `86400000` | Token expiration (24 hours) |

### Admin Configuration

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SUPREMEAI_ADMIN_EMAIL` | Yes | - | Admin email address |
| `SUPREMEAI_ADMIN_PASSWORD` | Yes | - | Admin password |
| `SUPREMEAI_ADMIN_USERNAME` | No | `supremeai` | Admin username |
| `SUPREMEAI_ADMIN_DEFAULT_PASSWORD` | No | - | Default password for new admins |

### API Keys

| Variable | Required | Description |
|----------|----------|-------------|
| `ADMIN_API_KEY` | No | API key for protected endpoints |
| `SUPREMEAI_SETUP_TOKEN` | No | Token for system setup |

### Security Features

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SECRET_MANAGER_BACKEND` | No | `env` | Secret manager: env/gcp/aws/azure/vault |
| `KEY_ROTATION_ENABLED` | No | `true` | Enable automatic key rotation |
| `KING_MODE_ENABLED` | No | `true` | Enable admin override mode |

---

## Database Configuration

### PostgreSQL (Recommended for Production)

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DATABASE_URL` | Yes* | - | JDBC connection URL |
| `DATABASE_TYPE` | No | `postgresql` | Database type |
| `DATABASE_USERNAME` | Yes* | - | Database username |
| `DATABASE_PASSWORD` | Yes* | - | Database password |
| `DATABASE_POOL_SIZE` | No | `20` | Connection pool size |
| `DATABASE_MAX_CONNECTIONS` | No | `100` | Max connections |

### MySQL (Alternative)

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DATABASE_URL` | Yes* | `jdbc:mysql://localhost:3306/supremeai` | JDBC URL |
| `DATABASE_USERNAME` | Yes* | `root` | Database username |
| `DATABASE_PASSWORD` | Yes* | - | Database password |

### Example Database URLs

```properties
# PostgreSQL
DATABASE_URL=jdbc:postgresql://localhost:5432/supremeai

# MySQL
DATABASE_URL=jdbc:mysql://localhost:3306/supremeai

# With credentials in URL
DATABASE_URL=jdbc:postgresql://user:pass@host:5432/supremeai
```

---

## Notification Channels

### Slack

| Variable | Required | Description |
|----------|----------|-------------|
| `SLACK_WEBHOOK_URL` | No | Slack incoming webhook URL |
| `SLACK_CHANNEL` | No | Default channel (e.g., `#alerts`) |

### Discord

| Variable | Required | Description |
|----------|----------|-------------|
| `DISCORD_WEBHOOK_URL` | No | Discord webhook URL |
| `DISCORD_CHANNEL` | No | Default channel name |

### Email (SendGrid)

| Variable | Required | Description |
|----------|----------|-------------|
| `MAIL_API_KEY` | No | SendGrid API key |
| `MAIL_FROM_ADDRESS` | No | From email address |
| `MAIL_FROM_NAME` | No | From name |
| `SENDGRID_API_KEY` | No | Alternative SendGrid key |
| `SENDGRID_FROM_EMAIL` | No | Alternative from email |

### SMS (Twilio)

| Variable | Required | Description |
|----------|----------|-------------|
| `TWILIO_ACCOUNT_SID` | No | Twilio account SID |
| `TWILIO_AUTH_TOKEN` | No | Twilio auth token |
| `TWILIO_PHONE_NUMBER` | No | Twilio phone number |
| `TWILIO_MESSAGE_SERVICE_SID` | No | Message service SID |

---

## Monitoring & Observability

### Datadog

| Variable | Required | Description |
|----------|----------|-------------|
| `DATADOG_API_KEY` | No | Datadog API key |
| `DATADOG_SITE` | No | Datadog site (e.g., `datadoghq.com`) |

### New Relic

| Variable | Required | Description |
|----------|----------|-------------|
| `NEW_RELIC_LICENSE_KEY` | No | New Relic license key |

### Prometheus

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `PROMETHEUS_ENABLED` | No | `true` | Enable Prometheus metrics |
| `PROMETHEUS_PORT` | No | `9090` | Prometheus port |

### Logging

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `LOG_LEVEL` | No | `INFO` | Log level (DEBUG/INFO/WARN/ERROR) |
| `METRICS_EXPORT_ENABLED` | No | `true` | Enable metrics export |
| `METRICS_EXPORT_INTERVAL_SECONDS` | No | `60` | Export interval |

---

## Deployment Configuration

### Google Cloud Platform

| Variable | Required | Description |
|----------|----------|-------------|
| `GCP_PROJECT_ID` | No | GCP project ID |
| `GCP_REGION` | No | Default region (e.g., `us-central1`) |
| `GCP_SERVICE_ACCOUNT_EMAIL` | No | Service account email |
| `CLOUD_BUILD_TRIGGER_ID` | No | Cloud Build trigger ID |
| `GCP_SECRET_LOCATION` | No | Secret location (default: `global`) |

### AWS

| Variable | Required | Description |
|----------|----------|-------------|
| `AWS_ACCESS_KEY_ID` | No | AWS access key |
| `AWS_SECRET_ACCESS_KEY` | No | AWS secret key |
| `AWS_REGION` | No | AWS region |
| `AWS_S3_BUCKET` | No | S3 bucket name |

### Azure

| Variable | Required | Description |
|----------|----------|-------------|
| `AZURE_SUBSCRIPTION_ID` | No | Azure subscription ID |
| `AZURE_TENANT_ID` | No | Azure tenant ID |
| `AZURE_CLIENT_ID` | No | Azure client ID |
| `AZURE_CLIENT_SECRET` | No | Azure client secret |
| `AZURE_KEY_VAULT_URL` | No | Key Vault URL |

### Render

| Variable | Required | Description |
|----------|----------|-------------|
| `RENDER_API_KEY` | No | Render API key |
| `RENDER_SERVICE_ID` | No | Render service ID |

### Docker

| Variable | Required | Description |
|----------|----------|-------------|
| `DOCKER_REGISTRY` | No | Registry URL (default: `docker.io`) |
| `DOCKER_USERNAME` | No | Docker username |
| `DOCKER_PASSWORD` | No | Docker password |
| `DOCKER_IMAGE_NAME` | No | Image name (default: `supremeai`) |

### GitHub

| Variable | Required | Description |
|----------|----------|-------------|
| `GITHUB_TOKEN` | No | GitHub personal access token |
| `GITHUB_USERNAME` | No | GitHub username |
| `GITHUB_REPO` | No | Repository name |
| `GITHUB_ORG` | No | Organization name |

---

## Feature Flags

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `FEATURE_WEBSOCKET_ENABLED` | No | `true` | Enable WebSocket real-time updates |
| `FEATURE_ANALYTICS_PERSISTENCE_ENABLED` | No | `true` | Enable analytics persistence |
| `FEATURE_ML_INTELLIGENCE_ENABLED` | No | `true` | Enable ML features |
| `FEATURE_NOTIFICATIONS_ENABLED` | No | `true` | Enable notifications |

---

## Development Settings

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | No | `development` | Active profile |
| `APP_ENV` | No | `development` | Environment name |
| `DEBUG_MODE` | No | `false` | Enable debug logging |
| `MOCK_EXTERNAL_SERVICES` | No | `false` | Mock external APIs |
| `SERVER_PORT` | No | `8080` | Server port |
| `PORT` | No | `8080` | Alternative port variable |

### File Paths

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `OUTPUT_FILES_PATH` | No | `./output/` | Generated files location |
| `BUILD_FILES_PATH` | No | `./build/` | Build output location |
| `LOGS_PATH` | No | `./logs/` | Log files location |
| `TEMP_FILES_PATH` | No | `./tmp/` | Temporary files |

### Secret File Paths

| Variable | Required | Description |
|----------|----------|-------------|
| `FIREBASE_SERVICE_ACCOUNT_PATH` | No | Path to Firebase service account JSON |
| `GOOGLE_CLOUD_KEY_PATH` | No | Path to Google Cloud key JSON |
| `AWS_CREDENTIALS_PATH` | No | Path to AWS credentials |
| `KUBERNETES_CONFIG_PATH` | No | Path to Kubernetes config |

---

## Security Best Practices

### ✅ DO

1. **Use strong secrets**

   ```properties
   # Good
   JWT_SECRET=MyS3cur3R@nd0mStr1ngW1thSp3c1@lCh@rs!2024
   
   # Bad
   JWT_SECRET=secret123
   ```

2. **Rotate keys regularly**
   - API keys: Every 90 days
   - Service accounts: Every 180 days
   - Passwords: Every 90 days

3. **Use environment-specific files**

   ```
   .env.local      # Local development
   .env.staging    # Staging environment
   .env.production # Production (use secrets manager instead)
   ```

4. **Store sensitive files securely**

   ```bash
   mkdir secrets/
   chmod 700 secrets/
   cp service-account.json secrets/
   # secrets/ is in .gitignore
   ```

### ❌ DON'T

1. **Never commit .env files**

   ```bash
   # Verify .gitignore includes:
   .env
   .env.*
   secrets/
   *.key
   *.pem
   ```

2. **Never hardcode credentials**

   ```java
   // Bad
   String apiKey = "sk-1234567890abcdef";
   
   // Good
   String apiKey = System.getenv("OPENAI_API_KEY");
   ```

3. **Never share tokens in chat/email**
   - Use secret managers
   - Use environment variables
   - Use secure vaults

4. **Never use production credentials in development**
   - Create separate Firebase projects
   - Use different API keys
   - Isolate environments

### Environment Isolation

| Environment | Credential Source | File |
|-------------|-------------------|------|
| Local | `.env.local` file | `.env.local` |
| CI/CD | GitHub Secrets | Repository settings |
| Staging | Environment variables | Platform config |
| Production | Secret manager | GCP/AWS/Azure Vault |

---

## Related Documentation

- [Quick Start Guide](../00-START-HERE/QUICK_START_5MIN.md)
- [Deployment Guide](PRODUCTION_DEPLOYMENT_GUIDE.md)
- [Security Guide](../05-AUTHENTICATION-SECURITY/SECURITY_GUIDE.md)
- [Troubleshooting](../00-START-HERE/QUICKSTART_TROUBLESHOOTING.md)

---

**Last Updated:** April 5, 2026  
**Maintained by:** SupremeAI DevOps Team  
**Status:** ✅ Complete
