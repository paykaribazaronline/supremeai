param(
    [ValidateSet('check', 'fix')]
    [string]$Mode = 'check'
)

$ErrorActionPreference = 'Stop'
$root = "C:\Users\Nazifa\supremeai"
$docsDir = Join-Path $root 'docs'

$allowedRootMd = @('README.md', 'CODE_OF_CONDUCT.md')

function Get-TargetDir([string]$filename) {
    $existing = Get-ChildItem $docsDir -Recurse -File -Filter $filename -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($existing) {
        return $existing.DirectoryName
    }

    switch -Regex ($filename) {
        '^(ADMIN_|.*ADMIN.*)' { return (Join-Path $docsDir '04-ADMIN') }
        '^(AUTH_|.*SECURITY.*|.*AUTHENTICATION.*)' { return (Join-Path $docsDir '05-AUTHENTICATION-SECURITY') }
        '^(.*ARCHITECTURE.*|.*ROADMAP.*|.*STRUCTURE.*)' { return (Join-Path $docsDir '02-ARCHITECTURE') }
        '^(PHASE.*|.*PHASE.*)' { return (Join-Path $docsDir '03-PHASES') }
        '^(.*DEPLOY.*|.*SETUP.*|.*CONFIG.*|.*ENVIRONMENT.*)' { return (Join-Path $docsDir '01-SETUP-DEPLOYMENT') }
        '^(.*TROUBLE.*|.*FIX.*|.*ROOT_CAUSE.*|.*MISTAKE.*)' { return (Join-Path $docsDir '09-TROUBLESHOOTING') }
        '^(.*IMPLEMENTATION.*|.*STATUS.*|.*READINESS.*)' { return (Join-Path $docsDir '10-IMPLEMENTATION') }
        '^(.*GUIDE.*|.*TUTORIAL.*|HOW_TO_.*)' { return (Join-Path $docsDir '12-GUIDES') }
        '^(.*REPORT.*|.*SUMMARY.*|.*VERIFICATION.*|.*ANALYSIS.*|.*COMPARISON.*)' { return (Join-Path $docsDir '13-REPORTS') }
        default { return (Join-Path $docsDir '13-REPORTS') }
    }
}

if (-not (Test-Path $docsDir)) {
    throw "docs directory not found at $docsDir"
}

$rootMd = Get-ChildItem $root -File -Filter '*.md' | Sort-Object Name
$offenders = $rootMd | Where-Object { $allowedRootMd -notcontains $_.Name }

if ($Mode -eq 'check') {
    if ($offenders.Count -gt 0) {
        Write-Host 'Found markdown files in root that must be under docs/:'
        $offenders | ForEach-Object { Write-Host "  - $($_.Name)" }
        exit 1
    }
    Write-Host 'OK: root markdown layout is clean.'
    exit 0
}

$moved = 0
foreach ($file in $offenders) {
    $targetDir = Get-TargetDir $file.Name
    if (-not (Test-Path $targetDir)) {
        New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
    }

    $targetPath = Join-Path $targetDir $file.Name
    if (Test-Path $targetPath) {
        $timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
        $base = [System.IO.Path]::GetFileNameWithoutExtension($file.Name)
        $targetPath = Join-Path $targetDir ($base + '_from-root_' + $timestamp + '.md')
    }

    Move-Item -Path $file.FullName -Destination $targetPath
    Write-Host "Moved: $($file.Name) -> $targetPath"
    $moved++
}

Write-Host "Done. Moved $moved markdown files from root into docs/."
