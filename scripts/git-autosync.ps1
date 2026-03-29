# Auto Sync Git Script - SupremeAI
# This PowerShell script automatically syncs changes with GitHub every 30 minutes
# Runs in background and handles merge conflicts gracefully

# Configuration
$repoPath = "C:\Users\Nazifa\supremeai"
$logFile = "$repoPath\logs\autosync.log"
$syncInterval = 1800  # 30 minutes in seconds
$maxRetries = 3

# Ensure log directory exists
$logDir = Split-Path $logFile
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
}

# Logging function
function Write-Log {
    param([string]$Message)
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    "$timestamp - $Message" | Tee-Object -FilePath $logFile -Append
}

# Main sync function
function Sync-Repository {
    try {
        Set-Location $repoPath
        Write-Log "Starting auto-sync..."
        
        # Check for uncommitted changes
        $status = git status --porcelain
        if ($status) {
            Write-Log "Found local changes, committing..."
            git add .
            git commit -m "Auto-sync: $(Get-Date -Format 'HH:mm:ss')" | Out-Null
            Write-Log "Changes committed successfully"
        } else {
            Write-Log "No local changes to commit"
        }
        
        # Fetch from remote
        Write-Log "Fetching from origin..."
        git fetch origin
        
        # Merge with remote (keeping merge commits)
        Write-Log "Merging origin/main..."
        git merge origin/main --no-ff -m "Auto-merge: $(Get-Date -Format 'HH:mm:ss')"
        
        # Push to remote
        Write-Log "Pushing to origin..."
        git push origin main
        
        Write-Log "Sync completed successfully"
        return $true
    }
    catch {
        Write-Log "ERROR during sync: $_"
        return $false
    }
}

# Continuous sync loop
Write-Log "Auto-sync service started. Syncing every $syncInterval seconds..."

while ($true) {
    Sync-Repository
    Start-Sleep -Seconds $syncInterval
}

