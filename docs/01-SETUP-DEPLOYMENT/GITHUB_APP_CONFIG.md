# GitHub App Configuration for SupremeAI Bot

## ✅ Credentials Extracted

```
App ID: 3300194
Client ID: Iv23liZY31q8QhovvzQt
Private Key SHA256: tfB6a0APUctSLv35UmlinXrO+P35MahzbACmMSMANFg=
App Name: SupremeAI Bot
```

## 🔧 Setup Steps

### Step 1: Download Private Key from GitHub

1. Go to: https://github.com/settings/apps/supremeai-bot
2. Scroll to "Private keys" section
3. Click the "Generate a private key" OR use existing one
4. Download the `.pem` file
5. **Keep it safe** — this is sensitive!

### Step 2: Encode Private Key (Base64)

If your private key file is saved as `supremeai-bot.pem`:

**PowerShell:**

```powershell
$key = Get-Content -Raw "path/to/supremeai-bot.pem"
$encoded = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($key))
Write-Output $encoded
```

**Linux/Mac:**

```bash
cat supremeai-bot.pem | base64 -w 0
```

Copy the entire base64 output.

### Step 3: Set Environment Variables in Cloud Run

Go to: **Google Cloud Console → Cloud Run → supremeai**

Click **"Edit & Deploy New Revision"**

Add these environment variables:

```
GITHUB_APP_ID=3300194

GITHUB_APP_CLIENT_ID=Iv23liZY31q8QhovvzQt

GITHUB_APP_PRIVATE_KEY=<paste-base64-encoded-key-here>

GITHUB_APP_WEBHOOK_SECRET=<your-webhook-secret-if-configured>
```

### Step 4: Redeploy

Click "Deploy" to restart with new credentials.

---

## 🧪 Test After Configuration

Your backend should now:

1. ✅ Push generated code to dolilbook repo
2. ✅ Create commits with SupremeAI Bot as author
3. ✅ Pull improvement data from existing repos
4. ✅ Run "Improve Existing App" feature

---

## 📝 Note

- The private key should NEVER be committed to Git
- It's stored as an environment variable in Cloud Run (encrypted)
- You can rotate it anytime from GitHub App settings
