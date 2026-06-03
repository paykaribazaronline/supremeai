# SupremeAI Batch Error Scanner
# Purpose: Find ALL errors in codebase at once (markdown, java, gradle, etc)
# Output: Consolidated error report for SupremeAI to fix

param(
    [string]$OutputFile = "BATCH_ERROR_REPORT.md",
    [switch]$FixAll = $false
)

Write-Host "=== SupremeAI Batch Error Scanner ===" -ForegroundColor Cyan
Write-Host "Scanning entire codebase..." -ForegroundColor Yellow

$reportContent = @"
# SupremeAI Batch Error Scan Report
Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')

---

"@

# ==========================================
# 1. MARKDOWN LINT ERRORS
# ==========================================
Write-Host "`n[1/5] Scanning Markdown files..." -ForegroundColor Green

$mdErrors = @()
try {
    $mdResult = markdownlint '**/*.md' 2>&1 | Out-String
    $mdCount = ($mdResult | Measure-Object -Line).Lines
    $mdErrors = $mdResult
    $reportContent += @"
## 1. MARKDOWN ERRORS (MDLint)
**Total Issues:** $mdCount

\`\`\`
$mdResult
\`\`\`

"@
    Write-Host "   Found $mdCount markdown issues"
} catch {
    Write-Host "   ⚠️  Markdown lint not installed. Install: npm install -g markdownlint-cli" -ForegroundColor Yellow
}

# ==========================================
# 2. JAVA COMPILATION ERRORS
# ==========================================
Write-Host "`n[2/5] Scanning Java/Gradle..." -ForegroundColor Green

try {
    $javaResult = ./gradlew clean build -x test 2>&1 | Out-String
    $javaErrors = $javaResult | Select-String -Pattern "error:|ERROR|BUILD FAILED" | Out-String
    $javaCount = ($javaErrors | Measure-Object -Line).Lines
    
    $reportContent += @"
## 2. JAVA COMPILATION ERRORS
**Total Issues:** $javaCount

\`\`\`
$javaErrors
\`\`\`

"@
    Write-Host "   Found $javaCount Java compilation issues"
} catch {
    Write-Host "   ⚠️  Gradle build check failed" -ForegroundColor Yellow
}

# ==========================================
# 3. GITHUB WORKFLOWS LINT
# ==========================================
Write-Host "`n[3/5] Scanning GitHub Workflows..." -ForegroundColor Green

$workflowErrors = @()
Get-ChildItem .github/workflows/*.yml 2>/dev/null | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    
    # Check for common errors
    if ($content -match "continue-on-error: true") {
        $workflowErrors += "⚠️  $($_.Name): Found continue-on-error: true (bypass risk)"
    }
    if ($content -match "TODO|FIXME|XXX") {
        $workflowErrors += "📝 $($_.Name): Contains TODO/FIXME comments"
    }
    if ($content -match "secrets\." -and $content -notmatch "\$\{\{") {
        $workflowErrors += "🔒 $($_.Name): Possible hardcoded secret reference"
    }
}

$reportContent += @"
## 3. GITHUB WORKFLOWS ISSUES
**Total Issues:** $($workflowErrors.Count)

$($workflowErrors -join "`n")

"@
Write-Host "   Found $($workflowErrors.Count) workflow issues"

# ==========================================
# 4. CONFIGURATION FILE VALIDATION
# ==========================================
Write-Host "`n[4/5] Scanning Config files..." -ForegroundColor Green

$configErrors = @()

# Check .markdownlint.json
if (Test-Path ".markdownlint.json") {
    try {
        $mdLintConfig = Get-Content .markdownlint.json | ConvertFrom-Json
        $disabledRules = ($mdLintConfig | Get-Member -MemberType NoteProperty | Where-Object {$mdLintConfig.($_.Name) -eq $false}).Count
        $configErrors += "ℹ️  .markdownlint.json: $disabledRules rules disabled"
    } catch {
        $configErrors += "❌ .markdownlint.json: Invalid JSON syntax"
    }
}

# Check build.gradle.kts
if (Test-Path "build.gradle.kts") {
    $content = Get-Content build.gradle.kts | Out-String
    if ($content -match "TODO|FIXME") {
        $configErrors += "📝 build.gradle.kts: Contains TODO/FIXME"
    }
}

$reportContent += @"
## 4. CONFIGURATION ISSUES
**Total Issues:** $($configErrors.Count)

$($configErrors -join "`n")

"@
Write-Host "   Found $($configErrors.Count) config issues"

# ==========================================
# 5. FILE STRUCTURE ISSUES
# ==========================================
Write-Host "`n[5/5] Scanning File structure..." -ForegroundColor Green

$structureErrors = @()

# Check for orphaned files
$orphanedFiles = Get-ChildItem -Recurse -File | Where-Object {
    $_.Extension -in @('.log', '.tmp', '.bak', '.swp') -and $_.FullName -notmatch '\\build\\'
}

if ($orphanedFiles.Count -gt 0) {
    $structureErrors += "🗑️  Found $($orphanedFiles.Count) temporary files that should be cleaned up"
}

# Check for large files
$largeFiles = Get-ChildItem -Recurse -File | Where-Object {$_.Length -gt 100MB}
if ($largeFiles.Count -gt 0) {
    $structureErrors += "📦 Found $($largeFiles.Count) files >100MB (may cause git issues)"
}

$reportContent += @"
## 5. FILE STRUCTURE ISSUES
**Total Issues:** $($structureErrors.Count)

$($structureErrors -join "`n")

---

## SUMMARY
| Category | Count |
|----------|-------|
| Markdown | $mdCount |
| Java | $javaCount |
| Workflows | $($workflowErrors.Count) |
| Config | $($configErrors.Count) |
| Structure | $($structureErrors.Count) |
| **TOTAL** | **$($mdCount + $javaCount + $workflowErrors.Count + $configErrors.Count + $structureErrors.Count)** |

## NEXT STEPS FOR SupremeAI
1. Review all errors in this report
2. Group by category (Markdown, Java, Workflows)
3. Auto-fix where possible (markdownlint --fix)
4. Create commits by category
5. Push to git

---
Generated by batch-error-scan.ps1
"@

# Save report
$reportContent | Out-File -FilePath $OutputFile -Encoding UTF8
Write-Host "`n✅ Report saved to: $OutputFile" -ForegroundColor Cyan

# Auto-fix markdown if requested
if ($FixAll) {
    Write-Host "`n🔧 Auto-fixing markdown errors..." -ForegroundColor Yellow
    markdownlint --fix '**/*.md'
    Write-Host "✅ Markdown auto-fixed" -ForegroundColor Green
}

Write-Host "`n📊 Full error report ready for SupremeAI processing." -ForegroundColor Cyan
