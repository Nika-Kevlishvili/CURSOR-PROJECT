# Monitors pdt-2529-bulk-run.log every 10 minutes and appends progress lines.
param(
    [string]$LogPath = "d:\Cursor\cursor-project\Cursor-Project\EnergoTS\pdt-2529-bulk-run.log",
    [string]$ProgressPath = "d:\Cursor\cursor-project\Cursor-Project\EnergoTS\pdt-2529-bulk-progress.txt",
    [int]$Total = 8000,
    [int]$IntervalMinutes = 10
)

function Get-ProgressFromLog {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        return @{ completed = 0; failed = 0; running = $false }
    }
    $text = Get-Content -Path $Path -Raw -ErrorAction SilentlyContinue
    if (-not $text) {
        return @{ completed = 0; failed = 0; running = $true }
    }
    $passed = ([regex]::Matches($text, '\bpassed\b')).Count
    $failed = ([regex]::Matches($text, '\bfailed\b')).Count
    # Playwright line reporter: [N/8000] is authoritative when present
    $m = [regex]::Matches($text, '\[(\d+)/(\d+)\]')
    $completed = 0
    if ($m.Count -gt 0) {
        $completed = [int]$m[$m.Count - 1].Groups[1].Value
    } else {
        $completed = $passed
    }
    $done = $text -match '(\d+)\s+passed' -and $text -match 'exit|elapsed|passed\s*\('
    return @{ completed = $completed; failed = $failed; running = -not $done }
}

"=== PDT-2529 bulk monitor started $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ===" | Out-File $ProgressPath -Encoding utf8

while ($true) {
    $p = Get-ProgressFromLog -Path $LogPath
    $line = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') | completed: $($p.completed)/$Total | failed markers: $($p.failed) | running: $($p.running)"
    Add-Content -Path $ProgressPath -Value $line -Encoding utf8
    if (-not $p.running -and $p.completed -ge ($Total - 1)) { break }
    Start-Sleep -Seconds ($IntervalMinutes * 60)
}

$final = Get-ProgressFromLog -Path $LogPath
"=== FINISHED $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') | $($final.completed)/$Total ===" | Add-Content $ProgressPath -Encoding utf8
