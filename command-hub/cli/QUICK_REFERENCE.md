# SupremeAI CLI Quick Reference

## Installation

```bash
cd command-hub/cli
./install.sh   # Linux/Mac
# or
install.bat    # Windows
```

## Essential Commands

### Authentication

```bash
supremeai login                  # Login with Firebase token
supremeai list                   # List all commands
```

### System Learning (NEW!)

```bash
supremeai system learning improve   # Trigger AI learning improvement
supremeai system learning status    # View learning statistics
```

### Admin Control

```bash
supremeai admin mode AUTO          # Set mode: AUTO, WAIT, FORCE_STOP
supremeai admin audit              # Show audit log
supremeai admin approve <ID>       # Approve pending action
```

### Provider Management

```bash
supremeai providers list           # List all AI providers
supremeai providers enable <name>  # Enable provider (openai, claude, etc.)
supremeai providers disable <name> # Disable provider
```

### Monitoring

```bash
supremeai metrics cache            # Cache statistics
supremeai metrics providers        # Provider performance
supremeai metrics cost             # Cost analysis
```

### Testing

```bash
supremeai consensus test solo -p query="..."      # Test solo strategy
supremeai consensus test compare -p query="..."   # Compare strategies
```

### Development

```bash
supremeai dev build               # Build project
supremeai dev test                # Run tests
supremeai dev lint                # Lint code
```

## API Endpoints

| CLI Command | API Endpoint | Method |
|-------------|--------------|--------|
| `system learning improve` | `/api/system-learning/improve` | POST |
| `system learning status` | `/api/system-learning/stats` | GET |
| `providers list` | `/api/providers` | GET |
| `admin mode` | `/api/admin/set-mode` | POST |
| `metrics cache` | `/api/v1/optimization/cache/stats` | GET |

## Configuration

```bash
# Environment variables
export SUPREMEAI_API_URL="https://supremeai-lhlwyikwlq-uc.a.run.app/api"

# Token file location
~/.supremeai_token   (Unix)
%USERPROFILE%\.supremeai_token   (Windows)
```

## Examples

### Daily Routine

```bash
# Morning check
supremeai system learning improve
supremeai metrics cache

# Evening review  
supremeai system learning status
supremeai admin audit
```

### Troubleshooting

```bash
# Fix authentication
rm ~/.supremeai_token
supremeai login

# Check system health
curl $SUPREMEAI_API_URL/health

# View all commands
supremeai list
```

## Error Codes

| Code | Meaning | Solution |
|------|---------|----------|
| `401` | Unauthorized | Run `supremeai login` |
| `404` | Not Found | Check API URL |
| `500` | Server Error | Check backend logs |
| `503` | Provider Down | Switch to backup provider |

## Pro Tips

1. **Combine commands:**

```bash
supremeai system learning improve && supremeai metrics cost
```

2. **Auto-login on shell start:** Add to `~/.bashrc` or `~/.zshrc`:

```bash
if [ -f ~/.supremeai_token ]; then
    export SUPREMEAI_API_TOKEN=$(cat ~/.supremeai_token)
fi
```

3. **JSON output for scripts:**

```bash
supremeai exec system/status --param format=json
```

4. **Cron job for daily improvement:**

```bash
0 2 * * * /usr/local/bin/supremeai system learning improve >> /var/log/supremeai/cron.log 2>&1
```

## See Also

- Full Documentation: [PROJECT_DOCUMENTATION.md](../PROJECT_DOCUMENTATION.md)
- API Reference: `docs/13-REPORTS/API_ENDPOINT_INVENTORY.md`
- Backend Code: `src/main/java/com/supremeai/`
- CLI Source: `command-hub/cli/supcmd.py`

---

**Quick Command:** `supremeai system learning improve` 🧠
