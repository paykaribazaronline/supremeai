param(
    [Parameter(Mandatory = $true)]
    [string]$BaseUrl,

    [Parameter(Mandatory = $true)]
    [string]$SetupToken,

    [string]$Repo = "supremeai/supremeai"
)

$gh = Get-Command gh -ErrorAction SilentlyContinue
if (-not $gh) {
    Write-Error "GitHub CLI (gh) is not installed. Install it first, then run: gh auth login"
    exit 1
}

gh auth status | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Error "GitHub CLI is not authenticated. Run: gh auth login"
    exit 1
}

gh secret set SUPREMEAI_BASE_URL --body $BaseUrl --repo $Repo
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to set SUPREMEAI_BASE_URL"
    exit 1
}

gh secret set SUPREMEAI_SETUP_TOKEN --body $SetupToken --repo $Repo
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to set SUPREMEAI_SETUP_TOKEN"
    exit 1
}

Write-Host "Knowledge reseed secrets configured for $Repo" -ForegroundColor Green