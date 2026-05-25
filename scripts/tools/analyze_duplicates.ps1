# Comprehensive Duplicate File Analysis Script
$duplicates = @{}
$allFiles = @()

# Scan all files
Write-Host "Scanning workspace for duplicate files (excluding build/lib dirs)..." -ForegroundColor Cyan
$excludeDirs = @('node_modules', 'build', 'bin', '.git', '.gradle', 'logs', 'projects')
Get-ChildItem -Path "." -Recurse -File -ErrorAction SilentlyContinue | Where-Object { 
    $path = $_.FullName
    $exclude = $false
    foreach ($dir in $excludeDirs) {
        if ($path -like "*\$dir\*") { $exclude = $true; break }
    }
    -not $exclude
} | ForEach-Object {
    $allFiles += $_
    $name = $_.Name
    if (-not $duplicates.ContainsKey($name)) {
        $duplicates[$name] = @()
    }
    $duplicates[$name] += $_
}

# Create report
$report = @()
$report += "DUPLICATE FILES COMPREHENSIVE ANALYSIS"
$report += "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$report += "Location: c:\Users\Nazifa\supremeai"
$report += "=" * 150
$report += ""

$duplicateList = @()

# Build duplicate list
foreach ($name in $duplicates.Keys | Sort-Object) {
    $files = $duplicates[$name]
    if ($files.Count -gt 1) {
        $duplicateList += [PSCustomObject]@{
            Filename = $name
            Count = $files.Count
            Files = $files | Sort-Object FullName
        }
    }
}

# Sort by count descending
$duplicateList = $duplicateList | Sort-Object Count -Descending

# Output details
$duplicateCount = 0
foreach ($dup in $duplicateList) {
    $duplicateCount++
    $name = $dup.Filename
    $count = $dup.Count
    $files = $dup.Files
    
    $report += ""
    $report += "[$($duplicateCount)] [$($count)x] $name"
    $report += "-" * 150
    
    $uniqueSizes = @{}
    foreach ($file in $files) {
        $size = $file.Length
        $path = $file.FullName -replace [regex]::Escape("C:\Users\Nazifa\supremeai\"), ""
        $report += "  Size: $($size.ToString().PadLeft(12)) bytes | Path: $path"
        $uniqueSizes[$size] += 1
    }
    
    # Determine if truly identical
    if ($uniqueSizes.Count -eq 1) {
        $report += "  [OK] Status: IDENTICAL FILES (same size)"
        $report += "  [PLAN] Action: SAFE TO CONSOLIDATE - All copies are identical"
    } else {
        $report += "  [WARN] Status: DIFFERENT SIZES (likely different content)"
        $report += "  [PLAN] Action: REVIEW REQUIRED - Verify before consolidation"
    }
}

# Add summary section
$report += ""
$report += ""
$report += "=" * 150
$report += "SUMMARY STATISTICS"
$report += "=" * 150
$report += "Total unique filenames with duplicates: $($duplicateList.Count)"
$report += "Total duplicate instances (extra files): $($duplicateList | ForEach-Object { $_.Count - 1 } | Measure-Object -Sum | Select-Object -ExpandProperty Sum)"
$report += "Total files scanned: $($allFiles.Count)"
$report += ""

# Categorize by file type
$report += "DUPLICATES BY FILE TYPE:"
$report += "-" * 150
$byType = @{}
foreach ($dup in $duplicateList) {
    $ext = [System.IO.Path]::GetExtension($dup.Filename)
    if ([string]::IsNullOrEmpty($ext)) { $ext = "(no extension)" }
    if (-not $byType.ContainsKey($ext)) { $byType[$ext] = 0 }
    $byType[$ext] += 1
}

foreach ($type in $byType.Keys | Sort-Object) {
    $report += "  $type : $($byType[$type]) files"
}

# Output to file
$report | Out-File -FilePath "DUPLICATE_ANALYSIS_REPORT.txt" -Encoding UTF8

# Display
$report | ForEach-Object { Write-Host $_ }

Write-Host ""
Write-Host "[OK] Report saved to: DUPLICATE_ANALYSIS_REPORT.txt" -ForegroundColor Green
