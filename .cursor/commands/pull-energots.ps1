# pull-energots.ps1
# Pull EnergoTS: fetches/updates the latest cursor branch from remote. Works even if folder is empty or missing (clone when RepoUrl provided).
# Usage: .cursor/commands/pull-energots.ps1 [-RepoUrl "https://..."]

param(
    [string]$RepoUrl = $env:ENERGOTS_REPO_URL   # Optional: clone URL when EnergoTS folder is missing or not a git repo
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$cursorProjectPath = Join-Path $workspaceRoot "Cursor-Project"
$energotsPath = Join-Path $cursorProjectPath "EnergoTS"

function Ensure-EnergoTSRepo {
    if (-not (Test-Path $energotsPath)) {
        if (-not $RepoUrl) {
            Write-Error "EnergoTS folder not found at: $energotsPath. Provide -RepoUrl 'https://...' or set `$env:ENERGOTS_REPO_URL to clone."
            exit 1
        }
        Write-Host "EnergoTS folder not found. Cloning from $RepoUrl ..." -ForegroundColor Cyan
        if (-not (Test-Path $cursorProjectPath)) {
            New-Item -ItemType Directory -Path $cursorProjectPath -Force | Out-Null
        }
        git clone --branch cursor $RepoUrl $energotsPath
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Clone failed. Check RepoUrl and network."
            exit 1
        }
        Write-Host "Cloned and on cursor branch." -ForegroundColor Green
        return $true
    }
    $gitDir = Join-Path $energotsPath ".git"
    if (-not (Test-Path $gitDir)) {
        if (-not $RepoUrl) {
            Write-Error "EnergoTS path exists but is not a git repo. Provide -RepoUrl to clone into a new location or fix the repo."
            exit 1
        }
        Write-Host "EnergoTS path exists but has no .git. Cloning into it (may require empty or removable content)..." -ForegroundColor Yellow
        # Clone into a temp name then move contents (avoid cloning into non-empty dir)
        $parent = Split-Path -Parent $energotsPath
        $tempClone = Join-Path $parent "EnergoTS_clone_temp"
        if (Test-Path $tempClone) { Remove-Item $tempClone -Recurse -Force }
        git clone --branch cursor $RepoUrl $tempClone
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Clone failed."
            exit 1
        }
        Get-ChildItem $tempClone -Force | Move-Item -Destination $energotsPath -Force
        Remove-Item $tempClone -Recurse -Force
        Write-Host "Cloned and on cursor branch." -ForegroundColor Green
        return $true
    }
    return $false
}

$cloned = Ensure-EnergoTSRepo
if ($cloned) {
    Write-Host "=== Pull EnergoTS (already done via clone) ===" -ForegroundColor Cyan
    exit 0
}

Push-Location $energotsPath
try {
    Write-Host "=== Pull EnergoTS ===" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "EnergoTS path: $energotsPath" -ForegroundColor Gray

    $currentBranch = git branch --show-current 2>$null
    Write-Host "Current branch: $currentBranch" -ForegroundColor Yellow

    if ($currentBranch -ne "cursor") {
        Write-Host "Switching to cursor branch..." -ForegroundColor Yellow
        git checkout cursor
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to checkout cursor."
            exit 1
        }
    }

    $hasChanges = git status --porcelain 2>$null
    $stashed = $false
    if ($hasChanges -and $hasChanges.Count -gt 0) {
        Write-Host "Stashing local changes..." -ForegroundColor Yellow
        git stash push -u -m "Stashed before pull-energots - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to stash."
            exit 1
        }
        $stashed = $true
        Write-Host "Stashed." -ForegroundColor Green
    }

    Write-Host ""
    Write-Host "Fetching origin (cursor)..." -ForegroundColor Cyan
    git fetch origin cursor
    if ($LASTEXITCODE -ne 0) {
        git fetch origin
    }
    $remoteCursorExists = git show-ref --verify --quiet refs/remotes/origin/cursor 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Remote branch origin/cursor not found. Fetch completed; no update." -ForegroundColor Yellow
    } else {
        $localRev = git rev-parse cursor 2>$null
        $remoteRev = git rev-parse origin/cursor 2>$null
        if ($localRev -ne $remoteRev) {
            Write-Host "Updating local cursor to origin/cursor..." -ForegroundColor Cyan
            # git merge writes hints to stderr; avoid PS treating that as terminating when EAP is Stop
            $prevEap = $ErrorActionPreference
            try {
                # PS 5.1: avoid *>&1 (PS7+); stderr from git must not trigger terminating errors when EAP is Stop
                $ErrorActionPreference = "Continue"
                git merge --ff-only origin/cursor 2>$null
            } finally {
                $ErrorActionPreference = $prevEap
            }
            if ($LASTEXITCODE -ne 0) {
                git reset --hard origin/cursor
                if ($LASTEXITCODE -ne 0) {
                    Write-Error "git reset --hard origin/cursor failed."
                    exit 1
                }
            }
            Write-Host "Local cursor is now up to date with origin/cursor." -ForegroundColor Green
        } else {
            Write-Host "Local cursor is already up to date with origin/cursor." -ForegroundColor Green
        }
    }

    if ($stashed) {
        Write-Host "Restoring local changes..." -ForegroundColor Cyan
        git stash pop 2>$null
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Warning: stash pop had conflicts. Resolve manually." -ForegroundColor Yellow
        } else {
            Write-Host "Local changes restored." -ForegroundColor Green
        }
    }

    Write-Host ""
    Write-Host "=== Done ===" -ForegroundColor Cyan
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
    if ($stashed) {
        git stash pop 2>&1 | Out-Null
    }
    exit 1
} finally {
    Pop-Location
}
