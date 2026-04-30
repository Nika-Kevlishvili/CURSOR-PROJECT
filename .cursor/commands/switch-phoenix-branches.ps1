param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('dev', 'dev2', 'test', 'preprod', 'prod', 'experiments')]
    [string]$Environment,
    [switch]$DryRun,
    [switch]$ConfirmProd,
    [string]$LogPath
)

$ErrorActionPreference = 'Continue'

$branchMap = @{
    dev = 'dev'
    dev2 = 'dev2'
    test = 'test'
    preprod = 'preprod'
    prod = 'prod'
    experiments = 'experiments'
}
$targetBranch = $branchMap[$Environment]

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$phoenixRoot = Join-Path (Join-Path $workspaceRoot 'Cursor-Project') 'Phoenix'
$lockPath = Join-Path $workspaceRoot '.cursor\.phoenix_switch.lock'
$logDir = Join-Path $workspaceRoot '.cursor\logs'
if ([string]::IsNullOrWhiteSpace($LogPath)) {
    $ts = Get-Date -Format 'yyyyMMdd-HHmmss'
    $LogPath = Join-Path $logDir "switch-phoenix-branches-$Environment-$ts.log"
}
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
}

function Write-Log {
    param([string]$Message, [string]$Level = 'INFO')
    $line = "[{0}] [{1}] {2}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff'), $Level, $Message
    Add-Content -Path $LogPath -Value $line
}

if ($Environment -eq 'prod' -and -not $ConfirmProd -and -not $DryRun) {
    Write-Host "ERROR: prod alignment requires -ConfirmProd." -ForegroundColor Red
    Write-Log "Prod alignment rejected without -ConfirmProd." 'ERROR'
    exit 1
}
if (-not (Test-Path $phoenixRoot)) {
    Write-Host "ERROR: Phoenix root not found: $phoenixRoot" -ForegroundColor Red
    Write-Log "Phoenix root not found: $phoenixRoot" 'ERROR'
    exit 1
}

function Invoke-Git {
    param([string]$GitArgs)
    $output = cmd /c "git $GitArgs 2>&1"
    @{ ExitCode = $LASTEXITCODE; Output = ($output | Out-String) }
}

function Get-FetchFailureCategory {
    param([string]$StdErr)
    if ([string]::IsNullOrWhiteSpace($StdErr)) { return 'fetch-failed' }
    $s = $StdErr.ToLowerInvariant()
    if ($s -match 'no such remote' -or $s -match "does not appear to be a git repository" -or $s -match "unknown remote") { return 'no-origin' }
    if ($s -match 'could not resolve host' -or $s -match 'operation timed out' -or $s -match 'failed to connect' -or $s -match 'network is unreachable' -or $s -match 'tls handshake') { return 'network-failure' }
    if ($s -match 'authentication failed' -or $s -match 'could not read username' -or $s -match 'permission denied \(publickey\)') { return 'auth-failure' }
    return 'fetch-failed'
}

function Stop-InProgressGitOp {
    param([string]$RepoGitDir)
    if (Test-Path (Join-Path $RepoGitDir 'MERGE_HEAD')) { Invoke-Git 'merge --abort' | Out-Null }
    if (Test-Path (Join-Path $RepoGitDir 'CHERRY_PICK_HEAD')) { Invoke-Git 'cherry-pick --abort' | Out-Null }
    if (Test-Path (Join-Path $RepoGitDir 'REVERT_HEAD')) { Invoke-Git 'revert --abort' | Out-Null }
    if ((Test-Path (Join-Path $RepoGitDir 'rebase-apply')) -or (Test-Path (Join-Path $RepoGitDir 'rebase-merge'))) { Invoke-Git 'rebase --abort' | Out-Null }
}

function New-PhoenixSwitchLock {
    param([string]$Path)
    if (Test-Path $Path) {
        try {
            $existing = Get-Content -Raw -Path $Path -ErrorAction Stop
            $existingPid = ($existing -split "`n")[0].Trim()
            if ($existingPid -match '^\d+$') {
                $proc = Get-Process -Id ([int]$existingPid) -ErrorAction SilentlyContinue
                if ($null -ne $proc) {
                    Write-Host "ERROR: Lock is active (pid=$existingPid)." -ForegroundColor Red
                    Write-Log "Lock is active (pid=$existingPid)." 'ERROR'
                    return $false
                }
            }
        } catch {}
        Remove-Item -Path $Path -Force -ErrorAction SilentlyContinue
    }

    try {
        Set-Content -Path $Path -Value "$PID`n$(Get-Date -Format o)`n$Environment" -ErrorAction Stop
        return $true
    } catch {
        Write-Host "ERROR: cannot create lock: $($_.Exception.Message)" -ForegroundColor Red
        Write-Log "Cannot create lock: $($_.Exception.Message)" 'ERROR'
        return $false
    }
}

$lockAcquired = $false
if (-not $DryRun) {
    $lockAcquired = New-PhoenixSwitchLock -Path $lockPath
    if (-not $lockAcquired) { exit 1 }
}

Write-Log "Switch started. environment=$Environment, branch=origin/$targetBranch, dryRun=$DryRun, root=$phoenixRoot"

$repos = Get-ChildItem -Path $phoenixRoot -Directory -ErrorAction SilentlyContinue | Where-Object { Test-Path (Join-Path $_.FullName '.git') }
if (-not $repos -or $repos.Count -eq 0) {
    if ($lockAcquired) { Remove-Item -Path $lockPath -Force -ErrorAction SilentlyContinue }
    Write-Host "WARN: No Phoenix git repositories found." -ForegroundColor Yellow
    Write-Log "No Phoenix git repositories found under $phoenixRoot" 'WARN'
    exit 1
}

$results = @()

foreach ($repo in $repos) {
    $name = $repo.Name
    Write-Log "Repo [$name] start."
    Push-Location $repo.FullName
    try {
        $repoGitDir = Join-Path $repo.FullName '.git'
        $status = 'pending'
        $detail = ''

        if (-not $DryRun) {
            Stop-InProgressGitOp -RepoGitDir $repoGitDir
        }

        if ($DryRun) {
            Write-Host "[$name] DRY RUN: fetch/checkout/reset for origin/$targetBranch"
            Write-Log "Repo [$name] dry-run path."
        } else {
            $fetch = $null
            $attempts = 0

            for ($i = 1; $i -le 3; $i++) {
                $fetch = Invoke-Git 'fetch origin --prune'
                $attempts = $i
                if ($fetch.ExitCode -eq 0) { break }
                if ($i -lt 3) { Start-Sleep -Milliseconds (600 * $i) }
            }

            if ($fetch.ExitCode -eq 0) {
                for ($j = 1; $j -le 3; $j++) {
                    $fetch = Invoke-Git "fetch origin --prune +refs/heads/${targetBranch}:refs/remotes/origin/$targetBranch"
                    $attempts = $attempts + 1
                    if ($fetch.ExitCode -eq 0) { break }
                    if ($fetch.Output -match "couldn't find remote ref refs/heads/$targetBranch") { break }
                    if ($j -lt 3) { Start-Sleep -Milliseconds (600 * $j) }
                }
            }

            if ($fetch.ExitCode -ne 0 -and -not ($fetch.Output -match "couldn't find remote ref refs/heads/$targetBranch")) {
                $preview = (($fetch.Output -split "`n") | Select-Object -First 2) -join ' / '
                $results += [PSCustomObject]@{
                    Repo = $name
                    Status = (Get-FetchFailureCategory -StdErr $fetch.Output)
                    Detail = "git fetch exit=$($fetch.ExitCode), attempts=$attempts; $preview"
                }
                Write-Log "Repo [$name] fetch failed. attempts=$attempts, exit=$($fetch.ExitCode)" 'ERROR'
                Write-Log "Repo [$name] fetch stderr(full): $($fetch.Output.Trim())" 'ERROR'
                continue
            }
        }

        $hasRemote = $true
        if (-not $DryRun) {
            $check = Invoke-Git "show-ref --verify --quiet refs/remotes/origin/$targetBranch"
            $hasRemote = ($check.ExitCode -eq 0)
        }
        if (-not $hasRemote) {
            $results += [PSCustomObject]@{
                Repo = $name
                Status = 'missing-remote'
                Detail = "origin/$targetBranch not found"
            }
            Write-Log "Repo [$name] missing remote branch origin/$targetBranch." 'WARN'
            continue
        }

        if (-not $DryRun) {
            $headSha = (Invoke-Git 'rev-parse HEAD').Output.Trim()
            $remoteSha = (Invoke-Git "rev-parse refs/remotes/origin/$targetBranch").Output.Trim()
            $headRef = (Invoke-Git 'symbolic-ref --quiet HEAD').Output.Trim()
            $dirty = (Invoke-Git 'status --porcelain').Output.Trim()
            if ($headSha -and $remoteSha -and $headSha -eq $remoteSha -and $headRef -eq "refs/heads/$targetBranch" -and [string]::IsNullOrEmpty($dirty)) {
                $results += [PSCustomObject]@{
                    Repo = $name
                    Status = 'already-aligned'
                    Detail = "HEAD=$($headSha.Substring(0,[Math]::Min(7,$headSha.Length)))"
                }
                Write-Log "Repo [$name] already aligned."
                continue
            }
        }

        if ($DryRun) {
            $status = 'dry-run-ok'
            $detail = 'no changes applied'
        } else {
            $dirtyState = (Invoke-Git 'status --porcelain').Output
            if ($dirtyState -and $dirtyState.Trim()) {
                Invoke-Git 'reset --hard HEAD' | Out-Null
                Invoke-Git 'clean -fd' | Out-Null
            }

            $co = Invoke-Git "checkout -B $targetBranch origin/$targetBranch"
            if ($co.ExitCode -ne 0) {
                $preview = (($co.Output -split "`n") | Select-Object -First 2) -join ' / '
                $results += [PSCustomObject]@{
                    Repo = $name
                    Status = 'checkout-failed'
                    Detail = "git checkout exit=$($co.ExitCode); $preview"
                }
                Write-Log "Repo [$name] checkout failed. exit=$($co.ExitCode)" 'ERROR'
                Write-Log "Repo [$name] checkout stderr(full): $($co.Output.Trim())" 'ERROR'
                continue
            }

            $reset = Invoke-Git "reset --hard origin/$targetBranch"
            if ($reset.ExitCode -ne 0) {
                $preview = (($reset.Output -split "`n") | Select-Object -First 2) -join ' / '
                $results += [PSCustomObject]@{
                    Repo = $name
                    Status = 'reset-failed'
                    Detail = "git reset exit=$($reset.ExitCode); $preview"
                }
                Write-Log "Repo [$name] reset failed. exit=$($reset.ExitCode)" 'ERROR'
                Write-Log "Repo [$name] reset stderr(full): $($reset.Output.Trim())" 'ERROR'
                continue
            }

            $head = (Invoke-Git 'rev-parse --short HEAD').Output.Trim()
            $status = 'ok'
            $detail = "HEAD=$head"
        }

        $results += [PSCustomObject]@{
            Repo = $name
            Status = $status
            Detail = $detail
        }
        Write-Log "Repo [$name] done. status=$status detail=$detail"
    } catch {
        $results += [PSCustomObject]@{
            Repo = $name
            Status = 'error'
            Detail = $_.Exception.Message
        }
        Write-Log "Repo [$name] exception: $($_.Exception.Message)" 'ERROR'
    } finally {
        Pop-Location
    }
}

Write-Host ""
Write-Host "=== Summary ===" -ForegroundColor Cyan
$results | Format-Table -AutoSize
Write-Host "Log file: $LogPath" -ForegroundColor DarkCyan

if ($lockAcquired) {
    Remove-Item -Path $lockPath -Force -ErrorAction SilentlyContinue
}

$success = @('ok', 'already-aligned', 'dry-run-ok')
$okCount = ($results | Where-Object { $_.Status -in $success }).Count
$badCount = $results.Count - $okCount

if ($badCount -eq 0) {
    Write-Log "Switch finished with exit code 0. all repos success."
    exit 0
}
if ($okCount -eq 0) {
    Write-Log "Switch finished with exit code 3. all repos failed." 'ERROR'
    exit 3
}
Write-Log "Switch finished with exit code 2. partial failures."
exit 2
