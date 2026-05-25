#!/usr/bin/env powershell
param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Endpoint = "/actuator/health",
    [ValidateSet("GET", "POST")]
    [string]$Method = "GET",
    [int]$TotalRequests = 200,
    [int]$Concurrency = 20,
    [int]$TimeoutSec = 10
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-Percentile {
    param([double[]]$Values, [double]$Percent)
    if ($Values.Count -eq 0) { return 0.0 }
    $sorted = $Values | Sort-Object
    $index = [Math]::Ceiling(($Percent / 100.0) * $sorted.Count) - 1
    if ($index -lt 0) { $index = 0 }
    if ($index -ge $sorted.Count) { $index = $sorted.Count - 1 }
    return [double]$sorted[$index]
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "SUPREMEAI QUICK PERFORMANCE CHECK" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Target      : $BaseUrl$Endpoint"
Write-Host "Method      : $Method"
Write-Host "Requests    : $TotalRequests"
Write-Host "Concurrency : $Concurrency"
Write-Host ""

$uri = "$BaseUrl$Endpoint"
$warmup = [Math]::Min(10, [Math]::Max(3, [int]($TotalRequests * 0.05)))
Write-Host "Warmup requests: $warmup" -ForegroundColor Yellow

for ($i = 0; $i -lt $warmup; $i++) {
    try {
        if ($Method -eq "GET") {
            Invoke-WebRequest -Uri $uri -Method Get -TimeoutSec $TimeoutSec | Out-Null
        } else {
            $payload = @{ question = "health check"; userId = "perf-warmup" } | ConvertTo-Json
            Invoke-WebRequest -Uri $uri -Method Post -ContentType "application/json" -Body $payload -TimeoutSec $TimeoutSec | Out-Null
        }
    } catch {
        # Warmup should not fail the run.
    }
}

$results = [System.Collections.ArrayList]::new()
$start = Get-Date

$jobs = @()
for ($w = 0; $w -lt $Concurrency; $w++) {
    $workerRequests = [Math]::Floor($TotalRequests / $Concurrency)
    if ($w -lt ($TotalRequests % $Concurrency)) { $workerRequests++ }
    if ($workerRequests -le 0) { continue }
    $jobs += Start-ThreadJob -ArgumentList $uri, $Method, $TimeoutSec, $workerRequests, $w -ScriptBlock {
        param($jobUri, $jobMethod, $jobTimeout, $workerTotal, $workerId)
        $local = @()
        for ($n = 1; $n -le $workerTotal; $n++) {
            $globalN = ($workerId * 1000000) + $n
            $sw = [System.Diagnostics.Stopwatch]::StartNew()
            try {
                if ($jobMethod -eq "GET") {
                    $resp = Invoke-WebRequest -Uri $jobUri -Method Get -TimeoutSec $jobTimeout
                } else {
                    $payload = @{ question = "perf test"; userId = "perf-user-$globalN" } | ConvertTo-Json
                    $resp = Invoke-WebRequest -Uri $jobUri -Method Post -ContentType "application/json" -Body $payload -TimeoutSec $jobTimeout
                }
                $sw.Stop()
                $local += [PSCustomObject]@{
                    ok = $true
                    status = [int]$resp.StatusCode
                    latencyMs = [double]$sw.Elapsed.TotalMilliseconds
                }
            } catch {
                $sw.Stop()
                $statusCode = 0
                if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
                    $statusCode = [int]$_.Exception.Response.StatusCode
                }
                $local += [PSCustomObject]@{
                    ok = $false
                    status = $statusCode
                    latencyMs = [double]$sw.Elapsed.TotalMilliseconds
                }
            }
        }
        return $local
    }
}

Wait-Job -Job $jobs | Out-Null
foreach ($job in $jobs) {
    $chunk = Receive-Job -Job $job
    if ($chunk) {
        foreach ($item in $chunk) { [void]$results.Add($item) }
    }
    Remove-Job -Job $job | Out-Null
}

$elapsed = ((Get-Date) - $start).TotalSeconds
$total = $results.Count
$ok = ($results | Where-Object { $_.ok }).Count
$fail = $total - $ok
$errorRate = if ($total -gt 0) { ($fail * 100.0 / $total) } else { 100.0 }
$latencies = @($results | ForEach-Object { [double]$_.latencyMs })
$avg = if ($latencies.Count -gt 0) { ($latencies | Measure-Object -Average).Average } else { 0.0 }
$p50 = Get-Percentile -Values $latencies -Percent 50
$p95 = Get-Percentile -Values $latencies -Percent 95
$p99 = Get-Percentile -Values $latencies -Percent 99
$rps = if ($elapsed -gt 0) { $total / $elapsed } else { 0.0 }

Write-Host ""
Write-Host "============= RESULT =============" -ForegroundColor Cyan
Write-Host ("Total requests : {0}" -f $total)
Write-Host ("Success        : {0}" -f $ok)
Write-Host ("Failures       : {0}" -f $fail)
Write-Host ("Error rate     : {0:N2}%" -f $errorRate)
Write-Host ("Elapsed        : {0:N2}s" -f $elapsed)
Write-Host ("Throughput     : {0:N2} req/s" -f $rps)
Write-Host ("Avg latency    : {0:N2} ms" -f $avg)
Write-Host ("P50 latency    : {0:N2} ms" -f $p50)
Write-Host ("P95 latency    : {0:N2} ms" -f $p95)
Write-Host ("P99 latency    : {0:N2} ms" -f $p99)

if ($errorRate -gt 1.0 -or $p95 -gt 1000) {
    Write-Host ""
    Write-Host "Status: NEEDS OPTIMIZATION (errorRate>1% or p95>1000ms)" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "Status: HEALTHY PERFORMANCE" -ForegroundColor Green
exit 0
