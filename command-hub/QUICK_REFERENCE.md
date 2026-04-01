# CommandHub - Quick Reference Card

## Available Commands

### Monitoring Category (SYNC - Immediate Response)

| Command | Type | Permission | Purpose | Usage |
|---------|------|-----------|---------|-------|
| `health-check` | SYNC | view.health | Overall system health | `supcmd exec health-check` |
| `quota-status` | SYNC | view.quotas | Check quota usage | `supcmd exec quota-status` |
| `metrics` | SYNC | view.metrics | System performance metrics | `supcmd exec metrics` |

### Data Refresh Category (ASYNC - Queued)

| Command | Type | Permission | Purpose | Parameters | Usage |
|---------|------|-----------|---------|-----------|-------|
| `refresh-github` | ASYNC | execute.refresh | Fetch GitHub data | owner, repo | `supcmd exec refresh-github -p owner supremeai -p repo core` |
| `refresh-vercel` | ASYNC | execute.refresh | Fetch Vercel status | projectId | `supcmd exec refresh-vercel -p projectId my-project` |
| `refresh-firebase` | ASYNC | execute.refresh | Fetch Firebase metrics | (none) | `supcmd exec refresh-firebase` |
| `refresh-all` | ASYNC | execute.refresh | Refresh all sources | (none) | `supcmd exec refresh-all` |

---

## REST API Endpoints

### Execute Command

```
POST /api/commands/execute
Content-Type: application/json

{
  "name": "health-check",
  "parameters": {}
}

Response (200 Success / 202 Accepted):
{
  "commandName": "health-check",
  "success": true,
  "message": "...",
  "data": { ... }
}
```

### List Commands
```
GET /api/commands/list
GET /api/commands/list?category=MONITORING
GET /api/commands/list?type=SYNC

Response (200):
{
  "success": true,
  "message": "Commands retrieved successfully",
  "commands": [ { name, description, category, type, permissions }, ... ]
}
```

### Get Command Info

```
GET /api/commands/{name}

Example: GET /api/commands/health-check

Response (200):
{
  "success": true,
  "message": "Command details retrieved",
  "command": { name, description, category, type, permissions },
  "parameters": [ { name, type, required, default }, ... ]
}
```

### Check Health

```
GET /api/commands/health

Response (200):
Commands service is healthy
```

---

## CLI Tool Usage

### Installation

```bash
# Copy to system PATH
cp command-hub/cli/supcmd.py /usr/local/bin/supcmd
chmod +x /usr/local/bin/supcmd

# Or run directly with Python
python3 command-hub/cli/supcmd.py <command> [options]
```

### Subcommands

#### Execute a Command

```bash
supcmd exec <command-name> [-p key value] [--url url] [--token token]

Examples:
  supcmd exec health-check
  supcmd exec refresh-github -p owner supremeai -p repo core
  supcmd exec metrics --url http://localhost:8080
  supcmd exec quota-status --token YOUR_API_TOKEN
```

#### List Commands

```bash
supcmd list [--category CATEGORY] [--type TYPE] [--url url]

Examples:
  supcmd list                           # List all
  supcmd list --category MONITORING     # By category
  supcmd list --type SYNC               # By type (SYNC/ASYNC)
  supcmd list --url http://prod-api:8080
```

#### Get Command Details

```bash
supcmd info <command-name> [--url url]

Examples:
  supcmd info health-check
  supcmd info refresh-github
  supcmd info quota-status --url http://prod-api:8080
```

#### Authenticate

```bash
supcmd login <api-token> [--url url]

Examples:
  supcmd login eyJhbGciOiJIUzI1NiIs...
  supcmd login YOUR_TOKEN --url http://prod-api:8080

# Token saved to ~/.supcmd/config.json
```

#### Check Server Health

```bash
supcmd health [--url url]

Examples:
  supcmd health
  supcmd health --url http://prod-api:8080
```

### Global Options

```
--url URL          API server URL (default: http://localhost:8080)
--token TOKEN      API authentication token (overrides saved token)
--help, -h         Show help message

Examples:
  supcmd --url http://prod:8080 list
  supcmd --token YOUR_TOKEN exec health-check
```

---

## Curl Examples

### Health Check
```bash
curl http://localhost:8080/api/commands/health
```

### List All Commands
```bash
curl http://localhost:8080/api/commands/list
```

### List Monitoring Commands
```bash
curl "http://localhost:8080/api/commands/list?category=MONITORING"
```

### Get Health Check Details
```bash
curl http://localhost:8080/api/commands/health-check
```

### Execute Health Check
```bash
curl -X POST http://localhost:8080/api/commands/execute \
  -H "Content-Type: application/json" \
  -d '{"name":"health-check","parameters":{}}'
```

### Execute with Parameters
```bash
curl -X POST http://localhost:8080/api/commands/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name":"refresh-github",
    "parameters":{
      "owner":"supremeai",
      "repo":"core"
    }
  }'
```

### With Authentication
```bash
curl -X POST http://localhost:8080/api/commands/execute \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"name":"health-check","parameters":{}}'
```

---

## Response Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| 200 | Success - Sync command completed | health-check succeeded |
| 202 | Accepted - Async command queued | refresh-github job created |
| 400 | Bad Request - Invalid parameters | Missing required parameter |
| 403 | Forbidden - Permission denied | User lacks required permission |
| 404 | Not Found - Command doesn't exist | Command name misspelled |
| 500 | Server Error - Execution failed | Unexpected error in command |

---

## Command Response Examples

### Success Response (Sync Command)
```json
{
  "commandName": "health-check",
  "success": true,
  "message": "Health check passed",
  "data": {
    "status": "HEALTHY",
    "timestamp": 1699564800000,
    "dataCollector": {
      "status": "OK",
      "successRate": 0.99
    },
    "budget": "OK",
    "quotas": "OK"
  }
}
```

### Pending Response (Async Command)
```json
{
  "commandName": "refresh-github",
  "success": true,
  "message": "Command queued",
  "data": {
    "jobId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

### Error Response
```json
{
  "commandName": "health-check",
  "success": false,
  "message": "Insufficient permissions to execute commands",
  "data": null
}
```

---

## Permission Model

### Required for Each Command

| Command | Permission | Role Requirements |
|---------|-----------|------------------|
| health-check | view.health | VIEWER, USER, ADMIN |
| quota-status | view.quotas | VIEWER, USER, ADMIN |
| metrics | view.metrics | USER, ADMIN |
| refresh-* | execute.refresh | USER, ADMIN |

### Role Hierarchy

- **ADMIN** - Full access to all commands
- **USER** - Access to most commands except dangerous ones
- **VIEWER** - Read-only access (health, quota, metrics)

---

## Troubleshooting

### API Connection Fails
```bash
# Check server is running
supcmd health

# Try explicit URL
supcmd --url http://localhost:8080 list

# Check firewall/network
ping localhost
```

### Command Not Found
```bash
# List all available commands
supcmd list

# Check command name spelling
supcmd info health-check  # verify exists
```

### Permission Denied
```bash
# Authenticate with valid token
supcmd login YOUR_API_TOKEN

# Check user role
supcmd info <command>  # shows required permissions
```

### Parameter Error
```bash
# Get command details
supcmd info refresh-github

# Review parameter requirements
# Pass parameters with -p key value
supcmd exec refresh-github -p owner supremeai
```

### Server Error (500)
```
Check server logs for details:
  tail -f logs/supremeai.log

Review command parameters validity
Verify all required services are running
```

---

## Config Files

### CLI Configuration
```
~/.supcmd/config.json
{
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

---

## Performance Guidelines

- **Sync commands:** Respond within 100ms (health-check, metrics)
- **List commands:** Respond within 500ms  
- **Async commands:** Queued immediately (202), processed by workers
- **Max concurrent:** 100+ simultaneous requests

---

## Security Tips

1. **Store token securely:** `supcmd login` saves to ~/.supcmd/config.json
2. **Use HTTPS in production:** Change localhost to https://api.example.com
3. **Rotate tokens regularly:** Update auth token periodically
4. **Audit logs:** Check all command executions in history
5. **Limit permissions:** Use appropriate roles for different users

---

## Integration Checklist

- [ ] Copy command-hub/core/* to src/main/java/org/example/command/core/
- [ ] Copy command-hub/rest/* to src/main/java/org/example/controller/
- [ ] Add CommandHubConfiguration bean to Spring configuration
- [ ] Build and compile: `./gradlew build`
- [ ] Start application: `./gradlew bootRun`
- [ ] Test endpoint: `curl http://localhost:8080/api/commands/health`
- [ ] Test CLI: `supcmd health`
- [ ] Review commands: `supcmd list`
- [ ] Execute sample command: `supcmd exec health-check`

---

## Additional Resources

- **Full Implementation Guide:** command-hub/IMPLEMENTATION.md
- **Integration Steps:** command-hub/INTEGRATION_GUIDE.md
- **Completion Summary:** command-hub/PHASE2_COMPLETE.md
- **Original Blueprint:** command-hub/README.md

---

**Last Updated:** This Session
**Version:** 1.0 Release Ready
