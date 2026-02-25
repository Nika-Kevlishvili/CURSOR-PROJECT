# sync-main-project.ps1
# Sync main project: full pull and sync of Cursor-Project with remote (EnergoTS + Phoenix/*). EnergoTS stays on cursor.
# Usage: .cursor/commands/sync-main-project.ps1

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$cursorProjectPath = Join-Path $workspaceRoot "Cursor-Project"

if (-not (Test-Path $cursorProjectPath)) {
    Write-Error "Cursor-Project not found at: $cursorProjectPath"
    exit 1
}

Write-Host "=== Sync main project ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Workspace: $workspaceRoot" -ForegroundColor Gray
Write-Host "Cursor-Project: $cursorProjectPath" -ForegroundColor Gray
Write-Host ""

# Collect all git repos: EnergoTS, Phoenix/*, and any other direct child with .git
$repos = @()
$energotsPath = Join-Path $cursorProjectPath "EnergoTS"
$phoenixPath = Join-Path $cursorProjectPath "Phoenix"

if (Test-Path $energotsPath) {
    $gitDir = Join-Path $energotsPath ".git"
    if (Test-Path $gitDir) {
        $repos += @{ Path = $energotsPath; Name = "EnergoTS"; IsEnergoTS = $true }
    }
}

if (Test-Path $phoenixPath) {
    Get-ChildItem -Path $phoenixPath -Directory | ForEach-Object {
        $gitDir = Join-Path $_.FullName ".git"
        if (Test-Path $gitDir) {
            $repos += @{ Path = $_.FullName; Name = "Phoenix\$($_.Name)"; IsEnergoTS = $false }
        }
    }
}

# Any other direct child of Cursor-Project with .git (e.g. other project folders)
Get-ChildItem -Path $cursorProjectPath -Directory | ForEach-Object {
    if ($_.Name -eq "Phoenix" -or $_.Name -eq "EnergoTS") { return }
    $gitDir = Join-Path $_.FullName ".git"
    if (Test-Path $gitDir) {
        $repos += @{ Path = $_.FullName; Name = $_.Name; IsEnergoTS = $false }
    }
}

if ($repos.Count -eq 0) {
    Write-Host "No Git repositories found under Cursor-Project." -ForegroundColor Yellow
    exit 0
}

Write-Host "Repos to sync: $($repos.Count)" -ForegroundColor Yellow
$repos | ForEach-Object { Write-Host "  - $($_.Name)" -ForegroundColor Gray }
Write-Host ""

foreach ($repo in $repos) {
    $repoPath = $repo.Path
    $repoName = $repo.Name
    $isEnergoTS = $repo.IsEnergoTS

    Write-Host "--- $repoName ---" -ForegroundColor Cyan
    Push-Location $repoPath
    try {
        $currentBranch = git branch --show-current 2>$null
        if (-not $currentBranch) {
            Write-Host "  Not a git repo or detached HEAD; skip." -ForegroundColor Yellow
            continue
        }

        if ($isEnergoTS -and $currentBranch -ne "cursor") {
            Write-Host "  Switching to cursor (EnergoTS lock)..." -ForegroundColor Yellow
            git checkout cursor 2>$null
            if ($LASTEXITCODE -ne 0) {
                Write-Host "  Failed to checkout cursor; skip." -ForegroundColor Red
                continue
            }
            $currentBranch = "cursor"
        }

        $hasChanges = git status --porcelain 2>$null
        $stashed = $false
        if ($hasChanges -and $hasChanges.Count -gt 0) {
            git stash push -u -m "sync-main-project $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" 2>$null
            if ($LASTEXITCODE -eq 0) { $stashed = $true }
        }

        git fetch origin 2>&1 | Out-Null
        $remoteRef = "origin/$currentBranch"
        $remoteExists = git show-ref --verify --quiet "refs/remotes/$remoteRef" 2>$null
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  Branch: $currentBranch | No remote ref $remoteRef" -ForegroundColor Yellow
            if ($stashed) { git stash pop 2>&1 | Out-Null }
            continue
        }

        $behind = [int](git rev-list --count "HEAD..$remoteRef" 2>$null)
        $ahead = [int](git rev-list --count "$remoteRef..HEAD" 2>$null)
        if ($behind -eq 0 -and $ahead -eq 0) {
            Write-Host "  Branch: $currentBranch | Up to date" -ForegroundColor Green
            if ($stashed) { git stash pop 2>&1 | Out-Null }
            continue
        }
        if ($behind -gt 0 -and $ahead -gt 0) {
            Write-Host "  Branch: $currentBranch | Diverged (fetch OK; merge skipped — resolve manually)" -ForegroundColor Yellow
            if ($stashed) { git stash pop 2>&1 | Out-Null }
            continue
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
            git stash pop 2>&1 | Out-Null
        }
    } catch {
        Write-Host "  Error: $_" -ForegroundColor Red
    } finally {
        Pop-Location
    }
    Write-Host ""
}

Write-Host "=== Done ===" -ForegroundColor Cyan
