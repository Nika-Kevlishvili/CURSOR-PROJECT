# Ramp worker count to find stable throughput for PDT-2529 contract prep (Dev API).
param(
    [int[]]$Workers = @(4, 8, 12, 16),
    [int]$RepeatEach = 24,
    [string]$OutFile = "pdt-2529-worker-benchmark.txt"
)

$root = Split-Path $PSScriptRoot -Parent
Set-Location $root
$env:PDT2529_BULK_FAST = "1"
$env:PDT2529_BULK_REUSE_CATALOG = "1"

$lines = @(
    "=== PDT-2529 worker benchmark $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ===",
    "cwd=$root",
    "repeat-each=$RepeatEach per worker level",
    ""
)

foreach ($w in $Workers) {
    $logFile = Join-Path $root "bench-workers-$w.log"
    if (Test-Path $logFile) { Remove-Item $logFile -Force }
    Write-Host "`n--- workers=$w repeat-each=$RepeatEach (log: $logFile) ---"

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $cmd = "npx playwright test tests/cursor/PDT-2529-rfd-data-model-happy-path.spec.ts --project=main --repeat-each=$RepeatEach --workers=$w --retries=0 --reporter=line > `"$logFile`" 2>&1"
    $p = Start-Process -FilePath "cmd.exe" -WorkingDirectory $root -Wait -PassThru -NoNewWindow -ArgumentList @("/c", $cmd)
    $sw.Stop()
    $sec = [math]::Round($sw.Elapsed.TotalSeconds, 1)

    $log = ""
    if (Test-Path $logFile) { $log = Get-Content $logFile -Raw -ErrorAction SilentlyContinue }

    $passed = 0
    $failed = 0
    if ($log -match '(\d+)\s+passed') { $passed = [int]$Matches[1] }
    if ($log -match '(\d+)\s+failed') { $failed = [int]$Matches[1] }

    $perSec = if ($sec -gt 0 -and $passed -gt 0) { [math]::Round($passed / $sec, 3) } else { 0 }
    $exit = $p.ExitCode
    $line = "workers=$w | exit=$exit | passed=$passed failed=$failed | ${sec}s | throughput=${perSec} contracts/s"
    Write-Host $line
    $lines += $line

    if ($failed -gt 0 -or $exit -ne 0) {
        $errLines = ($log -split "`n" | Where-Object { $_ -match 'failed|Error|timeout|429|502|503|ECONN' } | Select-Object -First 6)
        if ($errLines) { $lines += ("  errors: " + ($errLines -join " | ")) }
    }
    Start-Sleep -Seconds 8
}

$lines += ""
$lines += "=== done $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ==="
$lines | Out-File -FilePath (Join-Path $root $OutFile) -Encoding utf8
Write-Host "`nWrote $OutFile"
