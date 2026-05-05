# SupremeAI CLI Tool

Command-line interface for SupremeAI system management and learning improvement.

## Installation

### Quick Install

```bash
cd command-hub/cli
./install.sh      # Linux/Mac
# or
install.bat       # Windows
```

### Manual Setup

```bash
# Make executable
chmod +x supcmd.py

# Optional: Create system-wide command
sudo ln -s $(pwd)/supcmd.py /usr/local/bin/supremeai

# Windows: Add to PATH or use directly
python supcmd.py --help
```

## Available Commands

### Authentication

```bash
supremeai login
```

Prompts for Firebase ID token and saves it for future use.

```bash
supremeai list
```

Lists all available commands with descriptions.

### Command Execution

```bash
supremeai exec <command_name> [-p key=value ...]
```

Executes a command with optional parameters.

Example:

```bash
supremeai exec generate-app -p prompt="Create a todo app" -p platform=android
```

### System Learning Commands

#### `system learning improve`

Triggers a comprehensive learning improvement cycle.

**What it does:**

- Analyzes all collected error patterns from IDE plugins
- Identifies low-success learning categories
- Optimizes knowledge base by consolidating duplicates
- Generates actionable recommendations
- Cleans up obsolete entries

**Example output:**

```
✅ System learning improvement cycle completed!

📊 Analysis Results:
   • Total learnings analyzed: 1,247
   • Improvements identified: 23
   • Optimizations applied: 8
   • Recommendations generated: 5

💡 Recommendations:
   1. Increase training data for GRADLE_BUILD_FAILURE (success rate: 62.3%)
   2. Provider openai is heavily used (456 times). Consider dedicated optimization.
   3. Consider increasing cache size if knowledge base exceeds 1GB

🔧 Optimizations:
   • Consolidated 15 learnings for topic: AndroidX_Conflict
   • Identified 47 obsolete learnings for cleanup
```

**API Endpoint:** `POST /api/system-learning/improve`

#### `system learning status`

Displays current learning system statistics.

**Example:**

```
📈 System Learning Status:

Total learnings: 3,421
Success rate: 87.2%
Average quality: 0.84

📚 Learning Types:
   • GRADLE_BUILD_FAILURE: 1,247
   • CODE_GENERATION: 892
   • APP_GENERATION: 634
   • ANDROIDX_CONFLICT: 421
   • NLP_Interaction: 227
```

**API Endpoint:** `GET /api/system-learning/stats`

### Provider Management

```bash
supremeai providers list
```

Shows all AI providers with status and metrics.

```bash
supremeai providers enable <name>
```

Enables a specific provider (e.g., `openai`, `claude`, `groq`).

```bash
supremeai providers disable <name>
```

Disables a provider.

**API Endpoints:**

- `GET /api/providers`
- `POST /api/providers/{id}/enable`
- `POST /api/providers/{id}/disable`

### Admin Commands

```bash
supremeai admin mode AUTO
```

Sets admin control mode. Options:

- `AUTO` - Instant execution, no approval needed
- `WAIT` - Requires admin approval for sensitive operations
- `FORCE_STOP` - Emergency halt, stops all agents

```bash
supremeai admin audit
```

Shows audit log of recent admin actions.

```bash
supremeai admin approve <action_id>
```

Approves a pending admin action.

**API Endpoints:**

- `POST /api/admin/set-mode`
- `GET /api/admin/audit`
- `POST /api/admin/approve/{id}`

### Consensus Testing

```bash
supremeai consensus test solo -p query="Create login screen"
```

Tests solo provider strategy.

```bash
supremeai consensus test compare -p query="Build calculator app"
```

Compares different voting strategies.

**API Endpoints:**

- `POST /api/v1/consensus/test/solo`
- `POST /api/v1/consensus/compare-strategies`

### Metrics & Monitoring

```bash
supremeai metrics cache
```

Shows cache statistics (hit rate, size, evictions).

```bash
supremeai metrics providers
```

Displays provider performance metrics.

```bash
supremeai metrics cost
```

Shows cost analysis and Firebase billing info.

**API Endpoints:**

- `GET /api/v1/optimization/cache/stats`
- `GET /api/providers/metrics`
- `GET /api/v1/optimization/cost-impact`

### Development Commands

```bash
supremeai dev build
```

Builds the project (runs Gradle).

```bash
supremeai dev test
```

Runs all tests.

```bash
supremeai dev lint
```

Runs code linting.

## Configuration

### Environment Variables

The CLI reads configuration from:

| Variable | Description | Default |
|----------|-------------|---------|
| `SUPREMEAI_API_URL` | Base API URL | `https://supremeai-lhlwyikwlq-uc.a.run.app/api` |
| `SUPREMEAI_TOKEN_PATH` | Token storage location | `~/.supremeai_token` |

### Token Storage

Authentication tokens are stored in `~/.supremeai_token` (Unix) or `%USERPROFILE%\.supremeai_token` (Windows). Tokens are Firebase ID tokens obtained from the web dashboard.

To manually set a token:

```bash
# Get token from web dashboard (F12 → Application → Local Storage)
supremeai exec auth/login -p token="your-firebase-id-token"
```

## Examples

### Daily Maintenance

```bash
# Morning routine
supremeai system learning improve    # Update AI knowledge
supremeai metrics cache              # Check cache health
supremeai providers list             # Verify providers

# Evening routine
supremeai system learning status     # Review learning progress
supremeai admin audit                # Check approvals
```

### Troubleshooting

```bash
# Clear token if authentication fails
rm ~/.supremeai_token
supremeai login

# View all commands
supremeai list

# Test API connectivity
supremeai exec health/check

# Check system status
supremeai exec system/status
```

## Error Handling

| Error | Solution |
|-------|----------|
| `401 Unauthorized` | Run `supremeai login` to refresh token |
| `404 Not Found` | Check API URL with `echo $SUPREMEAI_API_URL` |
| `Connection Error` | Verify network or set correct API URL |
| `Permission Denied` | Ensure ADMIN role for protected commands |

## Advanced Usage

### Custom API Endpoint

```bash
export SUPREMEAI_API_URL="http://localhost:8080/api"
supremeai list
```

For local development:

```bash
# Run backend locally
cd supremeai
./gradlew bootRun

# Point CLI to local
export SUPREMEAI_API_URL="http://localhost:8080/api"
supremeai system learning improve
```

### Batch Operations

Create a shell script for automated maintenance:

```bash
#!/bin/bash
# Auto-learning script - runs daily via cron

supremeai system learning improve > /var/log/supremeai/learning-$(date +%Y%m%d).log 2>&1
supremeai metrics cost > /var/log/supremeai/cost-$(date +%Y%m%d).log

echo "Daily learning cycle completed: $(date)" >> /var/log/supremeai/cron.log
```

### Use with curl (direct API)

If you prefer direct API calls:

```bash
TOKEN=$(cat ~/.supremeai_token)

# Trigger learning improvement
curl -X POST "https://supremeai-lhlwyikwlq-uc.a.run.app/api/system-learning/improve" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"

# Get learning stats
curl "https://supremeai-lhlwyikwlq-uc.a.run.app/api/system-learning/stats" \
  -H "Authorization: Bearer $TOKEN"
```

## Integration with IDE Plugins

The IntelliJ and VS Code plugins automatically send error data to the learning system. Use the CLI to:

1. **Review collected errors:** `supremeai system learning status`
2. **Trigger improvement:** `supremeai system learning improve`
3. **Monitor optimization:** Check logs for applied changes

## Troubleshooting CLI Issues

### Permission Denied (Linux/Mac)

```bash
sudo chmod +x /usr/local/bin/supremeai
```

### Command Not Found (Windows)

```powershell
# Add to PATH permanently
[Environment]::SetEnvironmentVariable("PATH", $env:PATH + ";" + "C:\path\to\supremeai\command-hub\cli", "User")
```

### Authentication Errors

1. Verify token exists: `cat ~/.supremeai_token`
2. Get fresh token from web dashboard
3. Re-login: `supremeai login`

### API Connection Issues

Test connectivity:

```bash
curl https://supremeai-lhlwyikwlq-uc.a.run.app/api/health
```

If offline, switch to local:

```bash
export SUPREMEAI_API_URL="http://localhost:8080/api"
```

## Support

- **Documentation:** [PROJECT_DOCUMENTATION.md](../PROJECT_DOCUMENTATION.md)
- **GitHub Issues:** Report bugs and feature requests
- **API Reference:** See `/docs/13-REPORTS/API_ENDPOINT_INVENTORY.md`

---

**Version:** 3.2 | **Last Updated:** April 28, 2026 | **Status:** Production ✅
