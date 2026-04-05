#!/usr/bin/env powershell
# SupremeAI Documentation Auto-Update Script
# This script teaches the system how to complete and maintain documentation

param(
    [switch]$ScanOnly,
    [switch]$GenerateApiDocs,
    [switch]$UpdateIndexes,
    [switch]$ValidateLinks,
    [string]$OutputPath = "docs/13-REPORTS/DOCUMENTATION_STATUS_REPORT.md"
)

Write-Host "`n═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "   SUPREMEAI DOCUMENTATION AUTO-UPDATE SYSTEM              " -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════`n" -ForegroundColor Cyan

$script:ReportData = @{
    Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    TotalDocs = 0
    MissingDocs = @()
    BrokenLinks = @()
    ApiEndpoints = @()
    UndocumentedApis = @()
    Suggestions = @()
}

# ============================================================================
# FUNCTION: Scan for Documentation Gaps
# ============================================================================
function Scan-DocumentationGaps {
    Write-Host "STEP 1: Scanning for documentation gaps..." -ForegroundColor Yellow
    
    $gaps = @()
    
    # Check 1: API Controllers without documentation
    Write-Host "  Checking API documentation..." -ForegroundColor Gray
    $controllers = Get-ChildItem -Path "src/main/java" -Filter "*Controller.java" -Recurse -ErrorAction SilentlyContinue
    $apiDocFile = "docs/13-REPORTS/API_ENDPOINT_INVENTORY.md"
    
    if (Test-Path $apiDocFile) {
        $apiDocContent = Get-Content $apiDocFile -Raw
        foreach ($controller in $controllers) {
            $controllerName = $controller.BaseName
            if ($apiDocContent -notmatch $controllerName) {
                $gaps += "API Controller not documented: $controllerName"
                $script:ReportData.UndocumentedApis += $controllerName
            }
        }
    }
    
    # Check 2: Environment variables without documentation
    Write-Host "  Checking environment variable documentation..." -ForegroundColor Gray
    $envDocFile = "docs/01-SETUP-DEPLOYMENT/ENVIRONMENT_VARIABLES_REFERENCE.md"
    if (Test-Path ".env.example") {
        $envVars = Get-Content ".env.example" | Where-Object { $_ -match '^[A-Z_]+=' } | ForEach-Object { ($_ -split '=')[0] }
        if (Test-Path $envDocFile) {
            $envDocContent = Get-Content $envDocFile -Raw
            foreach ($var in $envVars | Select-Object -First 20) {
                if ($envDocContent -notmatch [regex]::Escape($var)) {
                    $gaps += "Environment variable not documented: $var"
                }
            }
        }
    }
    
    # Check 3: Missing core documentation files
    Write-Host "  Checking core documentation files..." -ForegroundColor Gray
    $requiredDocs = @(
        "docs/README.md",
        "docs/02-ARCHITECTURE/ARCHITECTURE_AND_IMPLEMENTATION.md",
        "docs/00-START-HERE/QUICK_START_5MIN.md",
        "docs/12-GUIDES/CONTRIBUTING.md",
        "docs/12-GUIDES/GLOSSARY.md",
        "docs/12-GUIDES/DEVELOPER_ONBOARDING.md"
    )
    
    foreach ($doc in $requiredDocs) {
        if (-not (Test-Path $doc)) {
            $gaps += "Missing required documentation: $doc"
            $script:ReportData.MissingDocs += $doc
        }
    }
    
    # Check 4: Count total documentation
    $allDocs = Get-ChildItem -Path "docs" -Filter "*.md" -Recurse -ErrorAction SilentlyContinue
    $script:ReportData.TotalDocs = $allDocs.Count
    
    Write-Host "  Found $($gaps.Count) documentation gaps" -ForegroundColor $(if ($gaps.Count -eq 0) { "Green" } else { "Yellow" })
    
    return $gaps
}

# ============================================================================
# FUNCTION: Generate API Documentation
# ============================================================================
function Generate-ApiDocumentation {
    Write-Host "`nSTEP 2: Generating API documentation..." -ForegroundColor Yellow
    
    $endpoints = @()
    $controllers = Get-ChildItem -Path "src/main/java" -Filter "*Controller.java" -Recurse -ErrorAction SilentlyContinue
    
    foreach ($controller in $controllers) {
        $content = Get-Content $controller.FullName -Raw
        $controllerName = $controller.BaseName -replace "Controller", ""
        
        # Extract endpoints using regex
        $methodPattern = '@(GetMapping|PostMapping|PutMapping|DeleteMapping|RequestMapping)\s*\(\s*"([^"]+)"'
        $matches = [regex]::Matches($content, $methodPattern)
        
        foreach ($match in $matches) {
            $httpMethod = $match.Groups[1].Value -replace "Mapping", "" -replace "Request", ""
            $path = $match.Groups[2].Value
            
            $endpoints += [PSCustomObject]@{
                Controller = $controllerName
                Method = $httpMethod.ToUpper()
                Path = $path
                SourceFile = $controller.FullName.Replace($PWD.Path + "\", "")
            }
        }
    }
    
    $script:ReportData.ApiEndpoints = $endpoints
    
    # Generate markdown
    $markdown = @"
# API Endpoint Inventory

Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

| Controller | HTTP Method | Endpoint | Source File |
|------------|-------------|----------|-------------|
"@
    
    foreach ($ep in $endpoints | Sort-Object Controller, Path) {
        $markdown += "`n| $($ep.Controller)Controller | $($ep.Method) | $($ep.Path) | $($ep.SourceFile) |"
    }
    
    $markdown += "`n`n---`n`n**Total Endpoints:** $($endpoints.Count)`n"
    $markdown += "**Last Updated:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")`n"
    
    # Save to file
    $outputFile = "docs/13-REPORTS/API_ENDPOINT_INVENTORY.md"
    $markdown | Out-File -FilePath $outputFile -Encoding UTF8
    
    Write-Host "  Generated API documentation: $outputFile" -ForegroundColor Green
    Write-Host "  Total endpoints documented: $($endpoints.Count)" -ForegroundColor Green
}

# ============================================================================
# FUNCTION: Update Documentation Indexes
# ============================================================================
function Update-DocumentationIndexes {
    Write-Host "`nSTEP 3: Updating documentation indexes..." -ForegroundColor Yellow
    
    # Update main docs README
    $docsReadme = "docs/README.md"
    if (Test-Path $docsReadme) {
        $content = Get-Content $docsReadme -Raw
        
        # Update document count
        $allDocs = Get-ChildItem -Path "docs" -Filter "*.md" -Recurse -ErrorAction SilentlyContinue
        $newCount = $allDocs.Count
        $content = $content -replace "Total Documents.*", "Total Documents**: $newCount+"
        
        # Update timestamp
        $content = $content -replace "Last Updated.*", "**Last Updated**: $(Get-Date -Format "MMMM yyyy")"
        
        $content | Out-File -FilePath $docsReadme -Encoding UTF8
        Write-Host "  Updated docs/README.md" -ForegroundColor Green
    }
    
    # Update category indexes
    $categories = Get-ChildItem -Path "docs" -Directory -ErrorAction SilentlyContinue
    foreach ($cat in $categories) {
        $catName = $cat.Name
        $catFiles = Get-ChildItem -Path $cat.FullName -Filter "*.md" -ErrorAction SilentlyContinue
        Write-Host "  Category $catName`: $($catFiles.Count) documents" -ForegroundColor Gray
    }
}

# ============================================================================
# FUNCTION: Validate Documentation Links
# ============================================================================
function Validate-DocumentationLinks {
    Write-Host "`nSTEP 4: Validating documentation links..." -ForegroundColor Yellow
    
    $brokenLinks = @()
    $docs = Get-ChildItem -Path "docs" -Filter "*.md" -Recurse -ErrorAction SilentlyContinue
    
    foreach ($doc in $docs) {
        $content = Get-Content $doc.FullName -Raw
        
        # Find markdown links
        $linkPattern = '\[([^\]]+)\]\(([^)]+)\)'
        $matches = [regex]::Matches($content, $linkPattern)
        
        foreach ($match in $matches) {
            $linkText = $match.Groups[1].Value
            $linkUrl = $match.Groups[2].Value
            
            # Skip external links
            if ($linkUrl -match '^https?://') { continue }
            
            # Check internal links
            if ($linkUrl -match '^\.\.?/') {
                $basePath = Split-Path $doc.FullName -Parent
                $targetPath = Join-Path $basePath $linkUrl
                $targetPath = [System.IO.Path]::GetFullPath($targetPath)
                
                if (-not (Test-Path $targetPath)) {
                    $brokenLinks += [PSCustomObject]@{
                        Document = $doc.FullName.Replace($PWD.Path + "\", "")
                        LinkText = $linkText
                        BrokenUrl = $linkUrl
                    }
                }
            }
        }
    }
    
    $script:ReportData.BrokenLinks = $brokenLinks
    
    if ($brokenLinks.Count -gt 0) {
        Write-Host "  Found $($brokenLinks.Count) broken links:" -ForegroundColor Red
        foreach ($link in $brokenLinks | Select-Object -First 10) {
            Write-Host "    - $($link.Document): [$($link.LinkText)]($($link.BrokenUrl))" -ForegroundColor Red
        }
    } else {
        Write-Host "  All links validated successfully" -ForegroundColor Green
    }
}

# ============================================================================
# FUNCTION: Generate Suggestions
# ============================================================================
function Generate-Suggestions {
    Write-Host "`nSTEP 5: Generating improvement suggestions..." -ForegroundColor Yellow
    
    $suggestions = @()
    
    # Suggestion 1: Add missing glossary terms
    $glossary = Get-Content "docs/12-GUIDES/GLOSSARY.md" -ErrorAction SilentlyContinue
    if ($glossary) {
        $techTerms = @("WebSocket", "Circuit Breaker", "Failover", "Consensus", "Orchestration")
        foreach ($term in $techTerms) {
            if ($glossary -notmatch $term) {
                $suggestions += "Add '$term' to glossary"
            }
        }
    }
    
    # Suggestion 2: Check for outdated screenshots
    $quickStart = Get-Content "docs/00-START-HERE/QUICK_START_5MIN.md" -ErrorAction SilentlyContinue
    if ($quickStart -and ($quickStart -match "202[0-5]")) {
        $suggestions += "QUICK_START_5MIN.md may have outdated screenshots (check dates)"
    }
    
    # Suggestion 3: Check README freshness
    $readme = Get-Content "README.md" -ErrorAction SilentlyContinue
    if ($readme -match "Last Updated.*202[0-5]") {
        $suggestions += "README.md last update date is old"
    }
    
    $script:ReportData.Suggestions = $suggestions
    
    Write-Host "  Generated $($suggestions.Count) suggestions" -ForegroundColor $(if ($suggestions.Count -eq 0) { "Green" } else { "Yellow" })
    
    return $suggestions
}

# ============================================================================
# FUNCTION: Generate Report
# ============================================================================
function Generate-Report {
    param($Gaps, $Suggestions)
    
    Write-Host "`nSTEP 6: Generating documentation report..." -ForegroundColor Yellow
    
    $report = @"
# Documentation Status Report

Generated: $($script:ReportData.Timestamp)

## Summary

| Metric | Value |
|--------|-------|
| Total Documents | $($script:ReportData.TotalDocs) |
| Missing Core Docs | $($script:ReportData.MissingDocs.Count) |
| Undocumented APIs | $($script:ReportData.UndocumentedApis.Count) |
| Broken Links | $($script:ReportData.BrokenLinks.Count) |
| API Endpoints | $($script:ReportData.ApiEndpoints.Count) |
| Suggestions | $($script:ReportData.Suggestions.Count) |

## Missing Core Documentation

"@
    
    if ($script:ReportData.MissingDocs.Count -eq 0) {
        $report += "`n✅ All core documentation files present`n"
    } else {
        foreach ($doc in $script:ReportData.MissingDocs) {
            $report += "`n- [ ] $doc"`
        }
    }
    
    $report += "`n`n## Undocumented API Controllers`n`n"
    if ($script:ReportData.UndocumentedApis.Count -eq 0) {
        $report += "✅ All API controllers documented`n"
    } else {
        foreach ($api in $script:ReportData.UndocumentedApis) {
            $report += "- [ ] $api`n"
        }
    }
    
    $report += "`n`n## Broken Links`n`n"
    if ($script:ReportData.BrokenLinks.Count -eq 0) {
        $report += "✅ No broken links found`n"
    } else {
        foreach ($link in $script:ReportData.BrokenLinks) {
            $report += "- **$($link.Document)**: [$($link.LinkText)]($($link.BrokenUrl))`n"
        }
    }
    
    $report += "`n`n## Improvement Suggestions`n`n"
    if ($script:ReportData.Suggestions.Count -eq 0) {
        $report += "✅ No suggestions at this time`n"
    } else {
        foreach ($suggestion in $script:ReportData.Suggestions) {
            $report += "- $suggestion`n"
        }
    }
    
    $report += @"

## Next Steps

1. Address missing documentation gaps
2. Fix broken links
3. Review and implement suggestions
4. Run this script again to verify

## Related Documentation

- [Documentation Completion Guide](../12-GUIDES/DOCUMENTATION_COMPLETION_GUIDE.md)
- [Documentation Standards](../DOCUMENTATION_STANDARDS.md)

---

**Report Generated:** $($script:ReportData.Timestamp)
**Status:** $(if ($script:ReportData.MissingDocs.Count -eq 0 -and $script:ReportData.BrokenLinks.Count -eq 0) { "✅ Healthy" } else { "⚠️ Needs Attention" })
"@
    
    $report | Out-File -FilePath $OutputPath -Encoding UTF8
    Write-Host "  Report saved to: $OutputPath" -ForegroundColor Green
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

# Step 1: Scan for gaps
$gaps = Scan-DocumentationGaps

# Step 2: Generate API docs if requested
if ($GenerateApiDocs -or -not $ScanOnly) {
    Generate-ApiDocumentation
}

# Step 3: Update indexes if requested
if ($UpdateIndexes -or -not $ScanOnly) {
    Update-DocumentationIndexes
}

# Step 4: Validate links if requested
if ($ValidateLinks -or -not $ScanOnly) {
    Validate-DocumentationLinks
}

# Step 5: Generate suggestions
$suggestions = Generate-Suggestions

# Step 6: Generate report if not scan-only
if (-not $ScanOnly) {
    Generate-Report -Gaps $gaps -Suggestions $suggestions
}

# Summary
Write-Host "`n═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "   DOCUMENTATION UPDATE COMPLETE                           " -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "`nSummary:" -ForegroundColor White
Write-Host "  Total Documents: $($script:ReportData.TotalDocs)" -ForegroundColor Gray
Write-Host "  Missing Core Docs: $($script:ReportData.MissingDocs.Count)" -ForegroundColor $(if ($script:ReportData.MissingDocs.Count -eq 0) { "Green" } else { "Red" })
Write-Host "  Undocumented APIs: $($script:ReportData.UndocumentedApis.Count)" -ForegroundColor $(if ($script:ReportData.UndocumentedApis.Count -eq 0) { "Green" } else { "Yellow" })
Write-Host "  Broken Links: $($script:ReportData.BrokenLinks.Count)" -ForegroundColor $(if ($script:ReportData.BrokenLinks.Count -eq 0) { "Green" } else { "Red" })
Write-Host "  Suggestions: $($script:ReportData.Suggestions.Count)" -ForegroundColor $(if ($script:ReportData.Suggestions.Count -eq 0) { "Green" } else { "Yellow" })

if (-not $ScanOnly) {
    Write-Host "`nReport saved to: $OutputPath" -ForegroundColor Cyan
}

Write-Host "`n═══════════════════════════════════════════════════════════`n" -ForegroundColor Cyan
