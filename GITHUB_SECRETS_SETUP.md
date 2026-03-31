# GitHub Secrets Configuration Guide

## Overview

The Flutter CI/CD pipeline requires Firebase authentication to deploy automatically to Firebase Hosting. This guide explains how to set up the necessary GitHub Secrets.

---

## Required Secrets

### 1. `FIREBASE_TOKEN` ⭐ (REQUIRED)

This is the authentication token that allows GitHub Actions to deploy to Firebase Hosting.

#### How to Generate Firebase Token

**Step 1: Open Terminal/PowerShell**

```bash
# Windows PowerShell
powershell

# macOS/Linux
Terminal
```

**Step 2: Install Firebase CLI (if not already installed)**

```bash
npm install -g firebase-tools
```

**Step 3: Generate the CI Token**

```bash
firebase login:ci
```

This will:
- Open a browser window asking you to log in to Google
- Log in with your Google account that has access to the Firebase project
- Return a long token string

**Step 4: Copy the Token**

The output will look like:
```
✓ Success! Use this token to login on a CI server:

<VERY_LONG_TOKEN_STRING_HERE>

Example command:
  firebase deploy --token "<VERY_LONG_TOKEN_STRING_HERE>"
```

**Copy the entire token string** (not including the angle brackets).

---

## Adding Secrets to GitHub

### via GitHub Web UI (Recommended)

**Step 1: Go to Your Repository Settings**

1. Navigate to: https://github.com/your-username/supremeai
2. Click **Settings** (top right)
3. In the left sidebar, expand **Secrets and variables**
4. Click **Actions**

**Step 2: Create New Secret**

1. Click **New repository secret**
2. Fill in the form:
   - **Name:** `FIREBASE_TOKEN`
   - **Secret:** Paste the token you generated
3. Click **Add secret**

**Result:**
```
✅ FIREBASE_TOKEN
   Added by you just now
```

### via GitHub CLI

If you have `gh` CLI installed:

```bash
# Set the environment variable
$env:FIREBASE_TOKEN = "your-token-here"

# Or on macOS/Linux:
export FIREBASE_TOKEN="your-token-here"

# Create the secret
gh secret set FIREBASE_TOKEN --body "your-token-here"
```

---

## Verifying Secrets are Set

### Check in GitHub UI
1. Go to Settings → Secrets and variables → Actions
2. You should see `FIREBASE_TOKEN` listed
3. Click it to verify (won't show the actual value, just confirms it exists)

### Check via GitHub CLI
```bash
gh secret list
# Output should show:
# FIREBASE_TOKEN  Updated 5 minutes ago
```

---

## Optional Secrets (For Advanced Features)

These are optional but recommended for enhanced functionality:

### `GITHUB_TOKEN` (Automatic)
```
Already Provided by GitHub Actions
- Automatically created for every workflow run
- Used for commenting on PRs
- No configuration needed
```

### `SENTRY_DSN` (Optional - Future Use)
```
For crash reporting (when Sentry integration is added)
- Create account at https://sentry.io
- Get your DSN from project settings
- Add to GitHub Secrets
```

---

## Troubleshooting

### ❌ Token is Expired
**Symptoms:** `Error: Authentication failed`

**Solution:**
1. Generate a new token: `firebase login:ci`
2. Update the secret in GitHub
3. Re-run the workflow

### ❌ Permission Denied
**Symptoms:** `Error: User doesn't have permission`

**Solution:**
1. Make sure you're logged in with the correct Google account
2. Verify the Google account has "Editor" or "Firebase Admin" role in the Firebase project
3. In Firebase Console → Project Settings → Permissions → Check your account

### ❌ Token Not Working in CI
**Symptoms:** Deployment fails in GitHub Actions but works locally

**Solution:**
1. Re-generate token: `firebase login:ci`
2. Make sure to use the EXACT token (no extra spaces)
3. Update GitHub secret with the new token
4. Clear any local Firebase cache: `rm ~/.config/firebase/tokens.json`

### ❌ Deployment Still Fails
**Debug Steps:**

1. Check the GitHub Actions logs:
   ```
   https://github.com/your-username/supremeai/actions
   ```

2. Look for the error in the deploy step

3. Common issues:
   - Missing `flutter build web` output
   - `.firebaserc` not configured correctly
   - `firebase.json` has syntax errors

4. Run locally to verify:
   ```bash
   firebase deploy --only hosting:flutter-admin --token $FIREBASE_TOKEN --debug
   ```

---

## Security Best Practices

### ✅ DO:
- ✅ Keep your token secret
- ✅ Regenerate tokens periodically
- ✅ Use separate tokens for different CI/CD services
- ✅ Limit token scope to necessary permissions
- ✅ Monitor GitHub secret usage in logs

### ❌ DON'T:
- ❌ Commit tokens to version control
- ❌ Share tokens in issues, discussions, or emails
- ❌ Use the same token across multiple services
- ❌ Log the token in output
- ❌ Keep expired tokens in GitHub

---

## Setting Up Multiple Environments

If you need separate deployments for staging and production:

### Create Multiple Secrets
```
FIREBASE_TOKEN_STAGING    (for develop branch)
FIREBASE_TOKEN_PRODUCTION (for main branch)
```

### Update Workflow
```yaml
# Deploy to production (main branch)
- if: github.ref == 'refs/heads/main'
  run: firebase deploy --token ${{ secrets.FIREBASE_TOKEN_PRODUCTION }}

# Deploy to staging (develop branch)
- if: github.ref == 'refs/heads/develop'
  run: firebase deploy --token ${{ secrets.FIREBASE_TOKEN_STAGING }}
```

---

## Additional Resources

- [Firebase CLI Documentation](https://firebase.google.com/docs/cli)
- [GitHub Secrets Documentation](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [GitHub Actions Security Best Practices](https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions)
- [Firebase Hosting Authentication](https://firebase.google.com/docs/hosting/github-integration)

---

## Quick Reference

| Task | Command |
|------|---------|
| Generate token | `firebase login:ci` |
| List secrets | `gh secret list` |
| Set secret | `gh secret set SECRET_NAME` |
| Remove secret | `gh secret delete SECRET_NAME` |
| Deploy locally | `firebase deploy --token $FIREBASE_TOKEN` |

---

## Next Steps

Once secrets are configured:

1. ✅ Push code to main branch
2. ✅ GitHub Actions triggers automatically
3. ✅ Monitor at: https://github.com/your-username/supremeai/actions
4. ✅ View live deployment: https://supremeai-565236080752.web.app/admin/

---

**Last Updated:** March 31, 2026  
**Version:** 1.0.0
