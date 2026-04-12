# GitHub App Setup Guide for SupremeAI Healing System

## Overview

SupremeAI uses a **GitHub App** (`supremeai-bot`) for authentication instead of personal access tokens. This provides:

- **10,000 requests/hour** rate limit (vs 5,000 for personal tokens)
- **Better security** - credentials can be restricted per-repo
- **Better audit trail** - all actions logged to app
- **No personal credentials** stored in environment

---

## Step 1: Create GitHub App

### Option A: Create New App (Fresh Setup)

1. **Go to GitHub Settings:**
   - Personal: https://github.com/settings/developers
   - Organization: https://github.com/organizations/{org}/settings/apps

2. **Click "New GitHub App"**

3. **Fill in Application Details:**
   - **Name:** `supremeai-bot` (or your choice)
   - **Homepage URL:** `https://your-domain.com`
   - **Webhook URL:** `https://your-domain.com/webhook/github`
   - **Webhook secret:** Generate with: `openssl rand -hex 32`

4. **Set Permissions:**
   - **Repository permissions:**
     - `Contents`: Read & Write (for commits)
     - `Workflows`: Read & Write (for workflow dispatch)
     - `Pull requests`: Read (for PR info)
     - `Issues`: Read & Write (for escalation issues)
     - `Checks`: Read (for CI status)
   - **User permissions:**
     - `Email`: Read (for notifications)
   - **Webhook events:**
     - Check: `Push`, `Pull request`, `Workflow run`, `Issues`

5. **Generate Private Key:**
   - Scroll to "Private keys"
   - Click "Generate a private key"
   - Save the `.pem` file immediately (can't re-download)

6. **Note the Details:**
   - App ID (visible at top)
   - Installation ID (visible after installation)
   - Webhook secret (you created this)

### Option B: Use Existing App

If the app already exists at https://github.com/apps/supremeai-bot:

1. Go to https://github.com/apps/supremeai-bot
2. Click "Install"
3. Select repository: `paykaribazaronline/supremeai`
4. Get the Installation ID from the URL after installation

---

## Step 2: Install App on Repository

1. **Navigate to the repository:**
   - https://github.com/paykaribazaronline/supremeai/settings/installations

2. **Find the app in "Installed GitHub Apps"**

3. **Click the app to configure:**
   - Select repository access: `Only select repositories`
   - Choose: `paykaribazaronline/supremeai`
   - Click "Save"

4. **Note the Installation ID:**
   - The URL will show: `.../installations/{installation_id}`
   - Save this ID

---

## Step 3: Get Private Key

If you haven't already:

1. Go to app settings: https://github.com/settings/apps/supremeai-bot
2. Scroll to "Private keys"
3. Click "Generate a private key" (only one active at a time)
4. A `.pem` file will download automatically
5. Open it and copy the entire content (including the `-----BEGIN` and `-----END` lines)

---

## Step 4: Configure Environment Variables

### Create `.env` file from template

```bash
cp .env.example .env
```

### Fill in the values

```bash
# GitHub App Authentication
GITHUB_APP_ID=123456
GITHUB_APP_PRIVATE_KEY=-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA...\n-----END RSA PRIVATE KEY-----
GITHUB_APP_INSTALLATION_ID=12345678

# Webhook Security
GITHUB_WEBHOOK_SECRET=your-webhook-secret-here

# Test Repository
TEST_REPO_OWNER=paykaribazaronline
TEST_REPO_NAME=supremeai

# ... other settings (see .env.example)
```

### Important Notes

❗ **Do NOT commit `.env` to git** - it's already in `.gitignore`

✅ **For GitHub Actions** - use GitHub Secrets instead:

```bash
# Go to: Settings → Secrets and variables → Actions

# Add these secrets:
- GITHUB_APP_ID=123456
- GITHUB_APP_PRIVATE_KEY=-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA...\n-----END RSA PRIVATE KEY-----
- GITHUB_APP_INSTALLATION_ID=12345678
- GITHUB_WEBHOOK_SECRET=your-secret
```

---

## Step 5: Configure Webhook

### Set up incoming webhook on GitHub

1. **Go to repository settings:**
   - https://github.com/paykaribazaronline/supremeai/settings/hooks

2. **Click "Add webhook"**

3. **Fill in webhook details:**
   - **Payload URL:** `https://your-domain.com/webhook/github`
   - **Content type:** `application/json`
   - **Secret:** Same as `GITHUB_WEBHOOK_SECRET`
   - **Events:** Select:
     - ✓ Workflow runs
     - ✓ Push
     - ✓ Pull requests
     - ✓ Issues
   - **Active:** ✓ Check

4. **Click "Add webhook"**

5. **Test the webhook:**
   - Push a commit to the repo
   - Check the webhook delivery logs on GitHub
   - Should see `200 OK` responses

---

## Step 6: Verify Configuration

### Run verification script

```bash
./scripts/verify-github-app.sh
```

Or manually:

```bash
# Check environment variables are set
echo $GITHUB_APP_ID
echo $GITHUB_APP_INSTALLATION_ID
echo $GITHUB_WEBHOOK_SECRET

# Expected output:
# 123456
# 12345678
# your-webhook-secret-here
```

### Check application logs

```bash
mvn spring-boot:run

# Should see:
# ✓ GitHub App configuration verified (appId=123456, installationId=12345678)
# ✓ Webhook listener initialized
```

---

## Step 7: Test the System

### Manually trigger a workflow failure

```bash
# Go to: https://github.com/paykaribazaronline/supremeai/actions

# Select a workflow → Click "Run workflow" → Choose branch
# Add a parameter to force failure or edit workflow to fail intentionally
```

### Monitor healing system

```bash
# In terminal:
curl http://localhost:8080/api/healing/status

# Should see:
{
  "watchdog": { "enabled": true, "status": "HEALTHY" },
  "rateLimiter": { "tokensAvailable": 4500 },
  "recentStats": { "total": 1, "failed": 0 }
}
```

### Check webhook deliveries

```bash
# Go to: https://github.com/paykaribazaronline/supremeai/settings/hooks/[webhook-id]
# Click "Recent Deliveries"
# Should see POST requests with 200 status
```

---

## Troubleshooting

### Problem: "GITHUB_APP_ID not configured"

**Solution:**

1. Check `.env` file exists and has values filled in
2. If running with Docker:

   ```bash
   docker run -e GITHUB_APP_ID=123456 ... supremeai:latest
   ```

3. If running with GitHub Actions:
   - Check Secrets are set in repo settings
   - Use: `${{ secrets.GITHUB_APP_ID }}`

### Problem: "Failed to get GitHub App token"

**Cause:** Private key format is wrong

**Solution:**

1. Private key must be in `.pem` format (with `-----BEGIN` and `-----END`)
2. Multi-line keys in env vars need `\n` for newlines:

   ```bash
   # Convert from multi-line:
   -----BEGIN RSA PRIVATE KEY-----
   MIIEpAIBAAKCAQEA...
   ...
   -----END RSA PRIVATE KEY-----
   
   # To single line for .env:
   -----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA...\n-----END RSA PRIVATE KEY-----
   ```

### Problem: "Webhook secret verification failed"

**Cause:** Webhook secret doesn't match

**Solution:**

1. Get webhook secret from GitHub:
   - Settings → Webhooks → Your webhook → Secret
2. Set in `.env`:

   ```bash
   GITHUB_WEBHOOK_SECRET=your-actual-secret
   ```

3. Restart application

### Problem: "Rate limit exceeded"

**Expected:** Should not happen with GitHub App (10,000/hour limit)

**If it does:**

1. Check rate limiter is working:

   ```bash
   curl http://localhost:8080/api/healing/rate-limit
   ```

2. Disable healing temporarily:

   ```bash
   curl -X POST http://localhost:8080/api/healing/disable
   ```

3. Wait for rate limit reset (typically 1 hour)

---

## Rotating Private Key

GitHub Apps can have multiple private keys:

1. **Go to app settings:** https://github.com/settings/apps/supremeai-bot
2. **Generate new private key** (now you have 2)
3. **Update `.env` with new key**
4. **Restart application**
5. **Delete old private key** from GitHub

---

## Security Best Practices

✅ **DO:**

- Store private key in `.env` locally (git-ignored)
- Store as GitHub Secret for CI/CD
- Rotate key regularly (quarterly)
- Use restrictive permissions (only what's needed)
- Monitor app activity in GitHub audit logs

❌ **DON'T:**

- Commit `.env` or private key to git
- Share private key via Slack/email
- Use same app for multiple projects (unless intentional)
- Keep old private keys after rotation

---

## Monitoring & Limits

### GitHub App Rate Limits

| Limit | Value |
|-------|-------|
| Requests/hour | 10,000 |
| Workflow dispatch/min | 60 |
| Comment rate | 1 per creation attempt |

### Check Current Usage

```bash
curl http://localhost:8080/api/healing/rate-limit
```

Response:

```json
{
  "rateLimitStatus": {
    "limit": 10000,
    "remaining": 9500,
    "resetEpochSeconds": 1712973600
  },
  "isAtRisk": false
}
```

---

## Reference: App vs Personal Token

| Feature | GitHub App | Personal Token |
|---------|-----------|---|
| Rate limit | 10,000/hour | 5,000/hour |
| Installable per-repo | ✓ Yes | ✗ No |
| Permission scoping | ✓ Detailed | ✗ Broad |
| Multi-key | ✓ Yes (rotate) | ✗ Single key |
| Audit trail | ✓ Full | ✗ Limited |
| Personal credentials | ✗ No | ✓ Yes (risky) |

---

## Support

**Stuck?** Check:

1. App is installed on repo: https://github.com/paykaribazaronline/supremeai/settings/installations
2. Webhook is configured: https://github.com/paykaribazaronline/supremeai/settings/hooks
3. `.env` file is filled (locally) or Secrets are set (CI/CD)
4. Private key is in `.pem` format with `\n` for newlines
5. Application logs show: `✓ GitHub App configuration verified`
