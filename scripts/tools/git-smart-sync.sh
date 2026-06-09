#!/usr/bin/env bash
# ==============================================================================
# SupremeAI - Smart Git Autosync & Failover Manager (Linux Version)
# ==============================================================================
# Implementing the 3 Smart Tips:
# 1. Workload Separation: Dev pushes go to GitHub (origin), System pushes go to GitLab (system-auto/gitlab)
# 2. Smart Failover (Active-Passive): If one remote hits quota limit (429/403/Forbidden), automatically rotate
# 3. Dynamic Local vs Cloud Routing: If running on Local PC, run tests locally before pushing to save Cloud minutes.
# ==============================================================================

# Configurations
REPO_PATH="${REPO_PATH:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
LOG_FILE="$REPO_PATH/logs/git-smart-sync.log"
SYNC_INTERVAL=1800 # 30 minutes in seconds
MAX_RETRIES=3

# Remotes (Customize as needed)
PRIMARY_SYSTEM_REMOTE="system-gitlab"
FALLBACK_SYSTEM_REMOTE="system-auto"
DEVELOPER_REMOTE="origin"

# Ensure log directory exists
mkdir -p "$(dirname "$LOG_FILE")"

# Logging utility
log() {
    local timestamp
    timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    echo "$timestamp - $1" | tee -a "$LOG_FILE"
}

# Check if running on Local PC or Cloud
is_local_pc() {
    # Check hostname or a local PC environment marker
    # You can customize this condition (e.g., if hostname contains 'nazifa' or specific local file exists)
    if [[ "$HOSTNAME" == *"nazifa"* ]] || [[ -f "$REPO_PATH/.local_pc_marker" ]] || [[ "$USER" == "nazifarabbu" ]]; then
        return 0 # True
    else
        return 1 # False
    fi
}

# Run local test suite (Tip 3 - Save Cloud CI Minutes)
run_local_tests() {
    log "Running local verification tests to save Cloud CI runtime..."
    cd "$REPO_PATH" || return 1
    
    if ./gradlew test --no-daemon; then
        log "✅ Local tests passed successfully!"
        return 0
    else
        log "❌ Local tests failed. Aborting push to prevent broken builds in cloud."
        return 1
    fi
}

# Smart Push with Fallback (Tip 2 - Active-Passive Quota Rotation)
smart_push() {
    local branch="main"
    log "Attempting smart push for branch: $branch"
    
    # Try GitLab (Primary System Remote)
    log "Trying primary remote: $PRIMARY_SYSTEM_REMOTE..."
    if git push "$PRIMARY_SYSTEM_REMOTE" "$branch" 2>&1 | tee /tmp/git_push_out; then
        log "✅ Push to $PRIMARY_SYSTEM_REMOTE completed successfully!"
        rm -f /tmp/git_push_out
        return 0
    fi
    
    local push_error
    push_error=$(cat /tmp/git_push_out)
    rm -f /tmp/git_push_out
    
    # Detect Quota or Rate Limit Errors (429, 403, limit, quota, abuse)
    if [[ "$push_error" == *"429"* ]] || [[ "$push_error" == *"403"* ]] || [[ "$push_error" == *"limit"* ]] || [[ "$push_error" == *"quota"* ]] || [[ "$push_error" == *"abuse"* ]]; then
        log "⚠️ Quota limit or rate limit hit on $PRIMARY_SYSTEM_REMOTE!"
        log "Rotating target and initiating smart failover to $FALLBACK_SYSTEM_REMOTE..."
        
        if git push "$FALLBACK_SYSTEM_REMOTE" "$branch"; then
            log "✅ Failover push to $FALLBACK_SYSTEM_REMOTE succeeded!"
            return 0
        else
            log "❌ Both primary and fallback remotes failed. Escalation required."
            return 1
        fi
    else
        log "❌ Push failed due to non-quota error: $push_error"
        return 1
    fi
}

# Main Repository Sync Cycle
sync_repository() {
    log "--------------------------------------------------"
    log "Starting SupremeAI smart sync cycle..."
    cd "$REPO_PATH" || { log "Error: Cannot cd to $REPO_PATH"; return 1; }
    
    # 1. Check for uncommitted self-healing / automated changes
    local status
    status=$(git status --porcelain)
    if [ -n "$status" ]; then
        log "Found system/local updates, preparing auto-commit..."
        git add .
        git commit -m "Auto-sync [System Healed]: $(date +'%H:%M:%S')" > /dev/null
        log "System changes committed locally."
    else
        log "No new local changes detected."
    fi
    
    # 2. Fetch and Merge to keep workspace up-to-date
    log "Fetching latest updates..."
    git fetch "$PRIMARY_SYSTEM_REMOTE" 2>/dev/null || git fetch "$FALLBACK_SYSTEM_REMOTE" 2>/dev/null
    
    # Merge remote changes gracefully
    log "Merging remote changes..."
    git merge FETCH_HEAD --no-ff -m "Auto-merge: $(date +'%H:%M:%S')" 2>/dev/null
    
    # 3. Dynamic Local vs Cloud Test Routing (Tip 3)
    if is_local_pc; then
        log "System identified as LOCAL DEVICE (nazifarabbu)."
        if ! run_local_tests; then
            log "Skipping push until test errors are fixed."
            return 1
        fi
    else
        log "System identified as CLOUD INSTANCE. Skipping heavy local tests to save resources."
    fi
    
    # 4. Smart Push (Tip 1 & Tip 2)
    smart_push
}

# Continuous daemon execution loop
log "SupremeAI Git Smart Sync Daemon Started."
log "System Mode: $(is_local_pc && echo "Local PC (Active Verification)" || echo "Cloud Instance (Fast Tracking)")"
log "Sync interval: $SYNC_INTERVAL seconds."

while true; do
    sync_repository
    sleep "$SYNC_INTERVAL"
done
