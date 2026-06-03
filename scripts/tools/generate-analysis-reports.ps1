$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$javaRoot = Join-Path $repoRoot "src/main/java"
$reportDir = Join-Path $repoRoot "docs/13-REPORTS"

New-Item -ItemType Directory -Force -Path $reportDir | Out-Null

$javaFiles = Get-ChildItem -Path $javaRoot -Filter *.java -Recurse | Sort-Object FullName

$javaInventoryPath = Join-Path $reportDir "JAVA_FILE_INVENTORY.md"
$apiInventoryPath = Join-Path $reportDir "API_ENDPOINT_INVENTORY.md"

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

$javaMd = @()
$javaMd += "# Java File Inventory"
$javaMd += ""
$javaMd += "Generated: $timestamp"
$javaMd += ""
$javaMd += "Total Java files: $($javaFiles.Count)"
$javaMd += ""
$javaMd += "| # | Package | Class/File | Path |"
$javaMd += "|---|---------|------------|------|"

$index = 1
foreach ($file in $javaFiles) {
    $rel = $file.FullName.Substring($repoRoot.Length + 1).Replace("\\", "/")
    $pkg = "(none)"
    $firstLines = Get-Content -Path $file.FullName -TotalCount 20
    $pkgLine = $firstLines | Where-Object { $_ -match '^\s*package\s+.+;' } | Select-Object -First 1
    if ($pkgLine) {
        $pkg = ($pkgLine -replace '^\s*package\s+', '' -replace ';\s*$', '').Trim()
    }
    $className = [System.IO.Path]::GetFileNameWithoutExtension($file.Name)
    $javaMd += "| $index | $pkg | $className | $rel |"
    $index++
}

Set-Content -Path $javaInventoryPath -Value $javaMd -Encoding UTF8

$controllers = Get-ChildItem -Path $javaRoot -Filter *Controller.java -Recurse | Sort-Object FullName

$apiMd = @()
$apiMd += "# API Endpoint Inventory"
$apiMd += ""
$apiMd += "Generated: $timestamp"
$apiMd += ""
$apiMd += "| Controller | HTTP Method | Endpoint | Source File |"
$apiMd += "|------------|-------------|----------|-------------|"

$mappingRegex = '@(Get|Post|Put|Delete|Patch)Mapping\s*(\((?<args>.*)\))?'
$requestRegex = '@RequestMapping\s*\((?<args>.*)\)'

foreach ($controller in $controllers) {
    $rel = $controller.FullName.Substring($repoRoot.Length + 1).Replace("\\", "/")
    $controllerName = [System.IO.Path]::GetFileNameWithoutExtension($controller.Name)
    $lines = Get-Content -Path $controller.FullName

    $basePath = ""
    foreach ($line in $lines) {
        if ($line -match $requestRegex) {
            $args = $Matches['args']
            if ($args -match '"([^"]+)"') {
                $basePath = $Matches[1]
                break
            }
        }
    }

    foreach ($line in $lines) {
        if ($line -match $mappingRegex) {
            $method = $Matches[1].ToUpper()
            $args = $Matches['args']
            $path = ""
            if ($args -match '"([^"]+)"') {
                $path = $Matches[1]
            }

            if ([string]::IsNullOrWhiteSpace($path)) {
                $full = $basePath
            } elseif ($path.StartsWith("/")) {
                $full = "$basePath$path"
            } else {
                $full = "$basePath/$path"
            }

            if ([string]::IsNullOrWhiteSpace($full)) {
                $full = "/"
            }
            $full = $full -replace '//+', '/'
            $apiMd += "| $controllerName | $method | $full | $rel |"
        }
    }
}

Set-Content -Path $apiInventoryPath -Value $apiMd -Encoding UTF8

Write-Output "Generated: $javaInventoryPath"
Write-Output "Generated: $apiInventoryPath"
