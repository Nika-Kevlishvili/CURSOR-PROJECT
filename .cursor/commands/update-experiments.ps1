# update-experiments.ps1
# Command: Updates experiments branch with local changes (commit and push)
# Usage: .cursor/commands/update-experiments.ps1 [-Message "commit message"]

param(
    [string]$Message = "Update experiments branch"
)

$ErrorActionPreference = "Stop"

# Get the script directory and navigate to workspace root
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)

if (-not (Test-Path $workspaceRoot)) {
    Write-Error "Workspace root not found at: $workspaceRoot"
    exit 1
}

Push-Location $workspaceRoot

try {
    Write-Host "=== Updating experiments branch ===" -ForegroundColor Cyan
    Write-Host ""
    
    # Step 1: Check current branch
    $currentBranch = git branch --show-current
    Write-Host "Current branch: $currentBranch" -ForegroundColor Yellow
    Write-Host ""
    
    if ($currentBranch -ne "experiments") {
        Write-Host "Warning: Not on 'experiments' branch. Switching to experiments..." -ForegroundColor Yellow
        git checkout experiments
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to switch to experiments branch"
            exit 1
        }
    }
    
    # Step 2: Check for uncommitted changes
    $hasChanges = git status --porcelain
    $hasUncommittedChanges = $null -ne $hasChanges -and $hasChanges.Count -gt 0
    
    if (-not $hasUncommittedChanges) {
        Write-Host "No uncommitted changes found." -ForegroundColor Green
        Write-Host ""
    } else {
        Write-Host "Found uncommitted changes:" -ForegroundColor Yellow
        git status --short | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
        Write-Host ""
        
        # Step 3: Add all changes
        Write-Host "Staging all changes..." -ForegroundColor Cyan
        git add -A
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to stage changes"
            exit 1
        }
        Write-Host "Changes staged successfully." -ForegroundColor Green
        Write-Host ""
        
        # Step 4: Commit changes
        Write-Host "Committing changes..." -ForegroundColor Cyan
        git commit -m $Message
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to commit changes"
            exit 1
        }
        Write-Host "Changes committed successfully." -ForegroundColor Green
        Write-Host ""
    }
    
    # Step 5: Check if branch is ahead of remote
    git fetch origin experiments 2>&1 | Out-Null
    $localCommit = git rev-parse experiments
    $remoteCommit = git rev-parse origin/experiments 2>&1
    
    if ($LASTEXITCODE -eq 0 -and $localCommit -ne $remoteCommit) {
        # Step 6: Push to remote
        Write-Host "Pushing changes to origin/experiments..." -ForegroundColor Cyan
        git push origin experiments
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to push to origin/experiments"
            exit 1
        }
        Write-Host "Changes pushed successfully." -ForegroundColor Green
        Write-Host ""
    } else {
        Write-Host "Branch is already up to date with origin/experiments." -ForegroundColor Green
        Write-Host ""
    }
    
    # Step 7: Show final status
    Write-Host "=== Update completed ===" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Current status:" -ForegroundColor Yellow
    git status --short
    
    Write-Host ""
    Write-Host "Experiments branch has been updated and synchronized." -ForegroundColor Green
    
} catch {
    Write-Host ""
    Write-Host "Error occurred: $_" -ForegroundColor Red
    exit 1
} finally {
    Pop-Location
}
