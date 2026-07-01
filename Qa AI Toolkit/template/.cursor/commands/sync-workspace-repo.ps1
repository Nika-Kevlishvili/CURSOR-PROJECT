# sync-workspace-repo.ps1
# Sync ONLY the workspace root repository (the repo that contains Cursor-Project).
# Does NOT touch sub-repos (EnergoTS, Phoenix). Stash, fetch, merge current branch, unstash.
# Usage: .cursor/commands/sync-workspace-repo.ps1

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$cursorProjectPath = Join-Path $workspaceRoot "Cursor-Project"

if (-not (Test-Path $cursorProjectPath)) {
    Write-Error "Cursor-Project not found at: $cursorProjectPath"
    exit 1
}

$gitDir = Join-Path $workspaceRoot ".git"
if (-not (Test-Path $gitDir)) {
    Write-Host "Workspace root is not a Git repository. No sync performed." -ForegroundColor Yellow
    exit 0
}

Write-Host "=== Sync workspace repo (Cursor-Project repository only) ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Repository: $workspaceRoot" -ForegroundColor Gray
Write-Host ""

Push-Location $workspaceRoot
try {
    $currentBranch = git branch --show-current 2>$null
    if (-not $currentBranch) {
        Write-Host "Not a git repo or detached HEAD; skip." -ForegroundColor Yellow
        exit 0
    }

    $hasChanges = git status --porcelain 2>$null
    $stashed = $false
    if ($hasChanges -and $hasChanges.Count -gt 0) {
        Write-Host "Stashing uncommitted changes..." -ForegroundColor Gray
        git stash push -u -m "sync-workspace-repo $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" 2>$null
        if ($LASTEXITCODE -eq 0) { $stashed = $true }
    }

    Write-Host "Fetching origin..." -ForegroundColor Gray
    git fetch origin 2>&1 | Out-Null
    $remoteRef = "origin/$currentBranch"
    $remoteExists = git show-ref --verify --quiet "refs/remotes/$remoteRef" 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  Branch: $currentBranch | No remote ref $remoteRef" -ForegroundColor Yellow
        if ($stashed) { git stash pop 2>&1 | Out-Null }
        exit 0
    }

    $behind = [int](git rev-list --count "HEAD..$remoteRef" 2>$null)
    $ahead = [int](git rev-list --count "$remoteRef..HEAD" 2>$null)
    if ($behind -eq 0 -and $ahead -eq 0) {
        Write-Host "  Branch: $currentBranch | Up to date" -ForegroundColor Green
        if ($stashed) { git stash pop 2>&1 | Out-Null }
        exit 0
    }
    if ($behind -gt 0 -and $ahead -gt 0) {
        Write-Host "  Branch: $currentBranch | Diverged (fetch OK; merge skipped - resolve manually)" -ForegroundColor Yellow
        if ($stashed) { git stash pop 2>&1 | Out-Null }
        exit 0
    }
    if ($behind -gt 0) {
        git merge --no-edit $remoteRef 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  Branch: $currentBranch | Merged $remoteRef" -ForegroundColor Green
        } else {
            Write-Host "  Branch: $currentBranch | Merge failed (resolve conflicts)" -ForegroundColor Red
        }
    }
    if ($stashed) {
        Write-Host "Restoring stashed changes..." -ForegroundColor Gray
        git stash pop 2>&1 | Out-Null
    }
} catch {
    Write-Host "  Error: $_" -ForegroundColor Red
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "=== Done ===" -ForegroundColor Cyan
