# switch-phoenix-branches.ps1
# Command: Aligns every Phoenix repo (Cursor-Project/Phoenix/<repo>) to a remote branch
#          (origin/<branch>), discards uncommitted local changes, and force-resets the
#          local branch to the latest origin/<branch>.
#
# Reliability features (Rule PHOENIX-SWITCH.0):
# - Lock file prevents concurrent alignment runs (.cursor/.phoenix_switch.lock).
# - Mid-operation cleanup: aborts in-progress merge / rebase / cherry-pick / revert before reset.
# - Idempotent fast-path: repos already at origin/<branch> with a clean tree are reported as
#   `already-aligned` and skipped (no destructive ops, no extra resets).
# - Categorized fetch errors: `no-origin`, `network-failure`, `auth-failure`, `fetch-failed`.
# - Structured exit codes: 0 ok, 1 setup error / lock conflict, 2 partial failure, 3 all failed.
# - Optional `-ConfirmProd` is REQUIRED when `-Environment prod` (unless `-DryRun`).
#
# Allowed environment names (lowercase canonical): dev, dev2, test, preprod, prod, experiments
#
# Per-repo execution order:
#   1. Pre-clean any in-progress git operation (merge / rebase / cherry-pick / revert)
#   2. git fetch origin --prune
#   3. Verify origin/<branch> exists; otherwise mark `missing-remote` and skip
#   4. Idempotent check: if HEAD == origin/<branch> AND tree is clean -> `already-aligned`
#   5. Discard ALL uncommitted changes (`git reset --hard HEAD` + `git clean -fd`) if dirty
#   6. `git checkout -B <branch> origin/<branch>` (creates / re-points local tracking branch)
#   7. `git reset --hard origin/<branch>` (force-align to the latest remote tip)
#
# This script is intentionally destructive on local working trees in Phoenix. Per workspace
# policy: "discard existing local changes and stay at repo level". Do NOT use this script if
# you have uncommitted Phoenix work you want to preserve.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment dev
#   powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment test
#   powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment experiments
#   powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment prod -ConfirmProd
#   powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment dev -DryRun
#
# Reference rule: .cursor/rules/integrations/phoenix_branch_switching.mdc (Rule PHOENIX-SWITCH.0)

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('dev', 'dev2', 'test', 'preprod', 'prod', 'experiments')]
    [string]$Environment,

    [switch]$DryRun,

    [switch]$ConfirmProd
)

$ErrorActionPreference = 'Continue'

# Map lowercase environment names to canonical remote branch names.
# Per user instruction: lowercase canonical names — origin/dev, origin/dev2, origin/test,
# origin/preprod, origin/prod, origin/experiments.
$BranchMap = @{
    'dev'         = 'dev'
    'dev2'        = 'dev2'
    'test'        = 'test'
    'preprod'     = 'preprod'
    'prod'        = 'prod'
    'experiments' = 'experiments'
}

$TargetBranch = $BranchMap[$Environment]

# Resolve workspace and Phoenix root.
$ScriptDir         = Split-Path -Parent $MyInvocation.MyCommand.Path
$WorkspaceRoot     = Split-Path -Parent (Split-Path -Parent $ScriptDir)
$CursorProjectRoot = Join-Path $WorkspaceRoot 'Cursor-Project'
$PhoenixRoot       = Join-Path $CursorProjectRoot 'Phoenix'
$LockPath          = Join-Path $WorkspaceRoot '.cursor\.phoenix_switch.lock'

# ---------- Prod safety gate ----------
if ($Environment -eq 'prod' -and -not $ConfirmProd -and -not $DryRun) {
    Write-Host "ERROR: Aligning to origin/prod is destructive on local Phoenix repos." -ForegroundColor Red
    Write-Host "       Re-run with '-ConfirmProd' to proceed (or '-DryRun' to preview)." -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $PhoenixRoot)) {
    Write-Host "ERROR: Phoenix root not found: $PhoenixRoot" -ForegroundColor Red
    exit 1
}

# ---------- Concurrency lock ----------
$lockAcquired = $false
function Get-PhoenixSwitchLock {
    param([string]$Path)
    if (Test-Path $Path) {
        try {
            $existing = Get-Content -Path $Path -ErrorAction Stop -Raw
            $existingPid = ($existing -split "`n")[0].Trim()
            if ($existingPid -match '^\d+$') {
                $proc = Get-Process -Id ([int]$existingPid) -ErrorAction SilentlyContinue
                if ($null -ne $proc) {
                    Write-Host "ERROR: Another Phoenix branch switch is already running (pid=$existingPid)." -ForegroundColor Red
                    Write-Host "       Wait for it to finish or remove '$Path' if it is stale." -ForegroundColor Red
                    return $false
                }
            }
        } catch { }
        # stale lock — remove it
        Remove-Item -Path $Path -Force -ErrorAction SilentlyContinue
    }
    try {
        $lockBody = "$PID`n$(Get-Date -Format o)`n$Environment"
        Set-Content -Path $Path -Value $lockBody -ErrorAction Stop
        return $true
    } catch {
        Write-Host "ERROR: Could not create lock file '$Path' — $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

if (-not $DryRun) {
    $lockAcquired = Get-PhoenixSwitchLock -Path $LockPath
    if (-not $lockAcquired) { exit 1 }
}

Write-Host "=== Phoenix branch switch ===" -ForegroundColor Cyan
Write-Host "Environment      : $Environment"
Write-Host "Target branch    : origin/$TargetBranch"
Write-Host "Phoenix root     : $PhoenixRoot"
Write-Host "Mode             : $(if ($DryRun) { 'DRY RUN (no changes)' } else { 'EXECUTE (destructive on dirty repos)' })"
if ($Environment -eq 'prod' -and -not $DryRun) {
    Write-Host "Prod confirmation: ACK (-ConfirmProd was passed)" -ForegroundColor Yellow
}
Write-Host ""

# ---------- Helper functions ----------
function Invoke-Git {
    param([string]$GitArgs)
    $output = cmd /c "git $GitArgs 2>&1"
    return @{ ExitCode = $LASTEXITCODE; Output = ($output | Out-String) }
}

function Get-FetchFailureCategory {
    param([string]$StdErr)
    if ([string]::IsNullOrWhiteSpace($StdErr)) { return 'fetch-failed' }
    $s = $StdErr.ToLower()
    if ($s -match "does not appear to be a git repository" -or
        $s -match "no such remote" -or
        $s -match "'origin' does not appear" -or
        $s -match "unknown remote" ) { return 'no-origin' }
    if ($s -match "could not resolve host" -or
        $s -match "operation timed out" -or
        $s -match "failed to connect" -or
        $s -match "no route to host" -or
        $s -match "network is unreachable" -or
        $s -match "ssl certificate problem" -or
        $s -match "tls handshake" ) { return 'network-failure' }
    if ($s -match "authentication failed" -or
        $s -match "could not read username" -or
        $s -match "permission denied \(publickey\)" -or
        $s -match "fatal: authentication" -or
        $s -match "remote: http basic" -or
        $s -match "support for password authentication was removed" ) { return 'auth-failure' }
    return 'fetch-failed'
}

function Stop-InProgressGitOp {
    param([string]$RepoGitDir)
    $aborted = @()
    if (Test-Path (Join-Path $RepoGitDir 'MERGE_HEAD'))         { Invoke-Git 'merge --abort'        | Out-Null; $aborted += 'merge' }
    if (Test-Path (Join-Path $RepoGitDir 'CHERRY_PICK_HEAD'))   { Invoke-Git 'cherry-pick --abort'  | Out-Null; $aborted += 'cherry-pick' }
    if (Test-Path (Join-Path $RepoGitDir 'REVERT_HEAD'))        { Invoke-Git 'revert --abort'       | Out-Null; $aborted += 'revert' }
    if ((Test-Path (Join-Path $RepoGitDir 'rebase-apply')) -or
        (Test-Path (Join-Path $RepoGitDir 'rebase-merge'))) { Invoke-Git 'rebase --abort'       | Out-Null; $aborted += 'rebase' }
    return ,$aborted
}

# ---------- Iterate repos ----------
$Repos = Get-ChildItem -Path $PhoenixRoot -Directory -ErrorAction SilentlyContinue |
         Where-Object { Test-Path (Join-Path $_.FullName '.git') }

if (-not $Repos -or $Repos.Count -eq 0) {
    Write-Host "WARN: No git repositories found under $PhoenixRoot" -ForegroundColor Yellow
    if ($lockAcquired) { Remove-Item -Path $LockPath -Force -ErrorAction SilentlyContinue }
    exit 1
}

$Results = @()

foreach ($Repo in $Repos) {
    $Name = $Repo.Name
    Push-Location $Repo.FullName
    try {
        $Status = 'pending'
        $Detail = ''
        $RepoGitDir = Join-Path $Repo.FullName '.git'

        # Stale index lock detection (warn-only; let actual git command surface a clean failure).
        $indexLock = Join-Path $RepoGitDir 'index.lock'
        if ((Test-Path $indexLock) -and -not $DryRun) {
            Write-Host "[$Name] WARN: stale-looking '.git/index.lock' present — proceeding, may fail." -ForegroundColor Yellow
        }

        # 1) Pre-clean in-progress merge/rebase/cherry-pick/revert.
        if (-not $DryRun) {
            $aborted = Stop-InProgressGitOp -RepoGitDir $RepoGitDir
            if ($aborted.Count -gt 0) {
                Write-Host "[$Name] aborted in-progress: $($aborted -join ',')" -ForegroundColor DarkYellow
            }
        }

        # 2) Fetch + prune (categorize failures).
        if ($DryRun) {
            Write-Host "[$Name] DRY RUN: would run 'git fetch origin --prune'" -ForegroundColor DarkGray
        } else {
            $fetch = Invoke-Git 'fetch origin --prune'
            if ($fetch.ExitCode -ne 0) {
                $cat = Get-FetchFailureCategory -StdErr $fetch.Output
                $Results += [PSCustomObject]@{
                    Repo   = $Name
                    Status = $cat
                    Detail = "git fetch exit=$($fetch.ExitCode); $((($fetch.Output -split "`n") | Select-Object -First 2) -join ' / ')"
                }
                continue
            }
        }

        # 3) Verify origin/<TargetBranch> exists.
        $hasRemote = $false
        if ($DryRun) {
            $hasRemote = $true
        } else {
            $check = Invoke-Git "show-ref --verify --quiet refs/remotes/origin/$TargetBranch"
            $hasRemote = ($check.ExitCode -eq 0)
        }
        if (-not $hasRemote) {
            $Results += [PSCustomObject]@{
                Repo   = $Name
                Status = 'missing-remote'
                Detail = "origin/$TargetBranch not found in this repo"
            }
            continue
        }

        # 4) Idempotent fast-path: already at origin/<branch> and clean.
        if (-not $DryRun) {
            $headSha   = (Invoke-Git 'rev-parse HEAD').Output.Trim()
            $remoteSha = (Invoke-Git "rev-parse refs/remotes/origin/$TargetBranch").Output.Trim()
            $headRef   = (Invoke-Git 'symbolic-ref --quiet HEAD').Output.Trim()
            $dirty     = (Invoke-Git 'status --porcelain').Output.Trim()
            if ($headSha -and $remoteSha -and $headSha -eq $remoteSha -and
                $headRef -eq "refs/heads/$TargetBranch" -and [string]::IsNullOrEmpty($dirty)) {
                $Results += [PSCustomObject]@{
                    Repo   = $Name
                    Status = 'already-aligned'
                    Detail = "HEAD=$($headSha.Substring(0,[Math]::Min(7,$headSha.Length)))"
                }
                continue
            }
        }

        # 5) Discard uncommitted local changes (per user policy).
        if ($DryRun) {
            Write-Host "[$Name] DRY RUN: would 'git reset --hard HEAD' and 'git clean -fd' if dirty" -ForegroundColor DarkGray
        } else {
            $dirty = (Invoke-Git 'status --porcelain').Output
            if ($dirty -and $dirty.Trim()) {
                Invoke-Git 'reset --hard HEAD' | Out-Null
                Invoke-Git 'clean -fd'         | Out-Null
            }
        }

        # 6) Checkout target branch (creating local tracking branch if missing).
        if ($DryRun) {
            Write-Host "[$Name] DRY RUN: would 'git checkout -B $TargetBranch origin/$TargetBranch'" -ForegroundColor DarkGray
        } else {
            $co = Invoke-Git "checkout -B $TargetBranch origin/$TargetBranch"
            if ($co.ExitCode -ne 0) {
                $Results += [PSCustomObject]@{
                    Repo   = $Name
                    Status = 'checkout-failed'
                    Detail = "git checkout exit=$($co.ExitCode); $((($co.Output -split "`n") | Select-Object -First 2) -join ' / ')"
                }
                continue
            }
        }

        # 7) Force-reset local branch to remote tip.
        if ($DryRun) {
            Write-Host "[$Name] DRY RUN: would 'git reset --hard origin/$TargetBranch'" -ForegroundColor DarkGray
            $Status = 'dry-run-ok'
        } else {
            $reset = Invoke-Git "reset --hard origin/$TargetBranch"
            if ($reset.ExitCode -ne 0) {
                $Results += [PSCustomObject]@{
                    Repo   = $Name
                    Status = 'reset-failed'
                    Detail = "git reset --hard exit=$($reset.ExitCode); $((($reset.Output -split "`n") | Select-Object -First 2) -join ' / ')"
                }
                continue
            }
            $head = (Invoke-Git 'rev-parse --short HEAD').Output.Trim()
            $Status = 'ok'
            $Detail = "HEAD=$head"
        }

        $Results += [PSCustomObject]@{
            Repo   = $Name
            Status = $Status
            Detail = $Detail
        }
    } catch {
        $Results += [PSCustomObject]@{
            Repo   = $Name
            Status = 'error'
            Detail = $_.Exception.Message
        }
    } finally {
        Pop-Location
    }
}

Write-Host ""
Write-Host "=== Summary ===" -ForegroundColor Cyan
$Results | Format-Table -AutoSize

# Release lock.
if ($lockAcquired) {
    Remove-Item -Path $LockPath -Force -ErrorAction SilentlyContinue
}

# Exit codes:
#   0 — every repo reached 'ok' / 'already-aligned' (or 'dry-run-ok' in dry mode).
#   2 — at least one repo reached 'ok' / 'already-aligned' but others failed.
#   3 — every repo failed (catastrophic).
$success = @('ok', 'already-aligned', 'dry-run-ok')
$okCount  = ($Results | Where-Object { $_.Status -in $success }).Count
$badCount = $Results.Count - $okCount

if ($badCount -eq 0) {
    exit 0
} elseif ($okCount -eq 0) {
    Write-Host ""
    Write-Host "All repos failed alignment. See table above." -ForegroundColor Red
    exit 3
} else {
    Write-Host ""
    Write-Host "$badCount repo(s) did not switch cleanly. See table above." -ForegroundColor Yellow
    exit 2
}
