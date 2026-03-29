# Auto Sync Git Script

This PowerShell script automatically commits local changes, fetches the latest changes from the remote repository, merges them, and then pushes back to the origin.

## How it works:
1. Auto-commit local changes.
2. Fetch from origin.
3. Merge `origin/main` into `main`, keeping merge commits.
4. Push changes to `origin`.

### Clear Conflict Instructions:
If there are any merge conflicts during execution, resolve them manually and run the script again to ensure your changes are correctly pushed.

```powershell
# git-autosync.ps1

# Variables
$repoPath = "C:\path\to\your\repo"

# Navigate to the repository
Set-Location $repoPath

# Auto-commit local changes
git add .
git commit -m "Auto-commit changes"

# Fetch from the origin
git fetch origin

# Merge origin/main into main, keeping merge commits
git merge origin/main --no-ff

# Push to origin
git push origin main
```
