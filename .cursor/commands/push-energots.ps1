# push-energots.ps1
# Push EnergoTS: updates GitHub's EnergoTS cursor branch with local version (commit + push).
# Usage: .cursor/commands/push-energots.ps1 [-Message "commit message"]

param(
    [string]$Message = "Update EnergoTS cursor branch"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$energotsPath = Join-Path $workspaceRoot "Cursor-Project\EnergoTS"

if (-not (Test-Path $energotsPath)) {
    Write-Error "EnergoTS directory not found at: $energotsPath"
    exit 1
}

$gitDir = Join-Path $energotsPath ".git"
if (-not (Test-Path $gitDir)) {
    Write-Error "EnergoTS path is not a git repository: $energotsPath"
    exit 1
}

Push-Location $energotsPath
try {
    Write-Host "=== Push EnergoTS (update GitHub cursor branch with local) ===" -ForegroundColor Cyan
    Write-Host ""

    $currentBranch = git branch --show-current 2>$null
    Write-Host "Current branch: $currentBranch" -ForegroundColor Yellow

    if ($currentBranch -ne "cursor") {
        Write-Host "Switching to cursor branch..." -ForegroundColor Yellow
        git checkout cursor
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to switch to cursor branch"
            exit 1
        }
    }

    $hasChanges = git status --porcelain 2>$null
    $hasUncommitted = $hasChanges -and $hasChanges.Count -gt 0

    if ($hasUncommitted) {
        Write-Host "Uncommitted changes:" -ForegroundColor Yellow
        git status --short | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
        Write-Host ""
        Write-Host "Staging all changes..." -ForegroundColor Cyan
        git add -A
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to stage changes"
            exit 1
        }
        Write-Host "Committing..." -ForegroundColor Cyan
        git commit -m $Message
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to commit"
            exit 1
        }
        Write-Host "Committed successfully." -ForegroundColor Green
    } else {
        Write-Host "No uncommitted changes." -ForegroundColor Green
    }

    git fetch origin cursor 2>&1 | Out-Null
    $localRev = git rev-parse cursor 2>$null
    $remoteRev = git rev-parse origin/cursor 2>$null
    if ($LASTEXITCODE -ne 0) { $remoteRev = $null }

    if (-not $remoteRev -or $localRev -ne $remoteRev) {
        Write-Host ""
        Write-Host "Pushing to origin/cursor..." -ForegroundColor Cyan
        git push origin cursor
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Push failed. Check credentials and remote."
            exit 1
        }
        Write-Host "Pushed successfully. GitHub EnergoTS cursor branch updated." -ForegroundColor Green
    } else {
        Write-Host "Local cursor is already in sync with origin/cursor (nothing to push)." -ForegroundColor Green
    }

    Write-Host ""
    Write-Host "=== Done ===" -ForegroundColor Cyan
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
    exit 1
} finally {
    Pop-Location
}
