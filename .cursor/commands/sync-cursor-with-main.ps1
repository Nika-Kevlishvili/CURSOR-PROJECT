# sync-cursor-with-main.ps1
# Command: Updates cursor branch from main or staging, auto-resolves conflicts (keeping cursor versions), and preserves local changes
# Usage: .cursor/commands/sync-cursor-with-main.ps1 [-SourceBranch main|staging]

param(
    [string]$SourceBranch = "main",  # Source branch to sync from (main or staging)
    [switch]$KeepCursorChanges = $true  # By default, keep cursor branch changes
)

$ErrorActionPreference = "Stop"

# Get the script directory and navigate to EnergoTS
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$energotsPath = Join-Path $workspaceRoot "Cursor-Project\EnergoTS"

if (-not (Test-Path $energotsPath)) {
    Write-Error "EnergoTS directory not found at: $energotsPath"
    exit 1
}

Push-Location $energotsPath

try {
    # Validate source branch
    if ($SourceBranch -notin @("main", "staging")) {
        Write-Error "Invalid source branch: $SourceBranch. Must be 'main' or 'staging'"
        exit 1
    }
    
    Write-Host "=== Syncing cursor branch with $SourceBranch ===" -ForegroundColor Cyan
    Write-Host ""
    
    # Step 1: Check current branch
    $currentBranch = git branch --show-current
    Write-Host "Current branch: $currentBranch" -ForegroundColor Yellow
    Write-Host "Source branch: $SourceBranch" -ForegroundColor Yellow
    Write-Host ""
    
    if ($currentBranch -ne "cursor") {
        Write-Host "Warning: Not on 'cursor' branch. Switching to cursor..." -ForegroundColor Yellow
        git checkout cursor
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to switch to cursor branch"
            exit 1
        }
    }
    
    # Step 2: Check for uncommitted changes
    $hasChanges = git status --porcelain
    $hasUncommittedChanges = $hasChanges -ne $null -and $hasChanges.Count -gt 0
    
    if ($hasUncommittedChanges) {
        Write-Host ""
        Write-Host "Found uncommitted changes. Stashing to preserve them..." -ForegroundColor Yellow
        Write-Host "Modified files:" -ForegroundColor Gray
        git status --short | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
        
        # Stash uncommitted changes (including untracked files)
        git stash push -u -m "Stashed before syncing cursor with $SourceBranch - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to stash changes"
            exit 1
        }
        $stashed = $true
        Write-Host "Changes stashed successfully." -ForegroundColor Green
    } else {
        $stashed = $false
        Write-Host "No uncommitted changes found." -ForegroundColor Green
    }
    
    # Step 3: Fetch latest from source branch
    Write-Host ""
    Write-Host "Fetching latest changes from $SourceBranch branch..." -ForegroundColor Cyan
    git fetch origin $SourceBranch
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to fetch from origin/$SourceBranch"
        if ($stashed) {
            Write-Host "Restoring stashed changes..." -ForegroundColor Yellow
            git stash pop
        }
        exit 1
    }
    Write-Host "Fetch completed successfully." -ForegroundColor Green
    
    # Step 4: Check if cursor is behind source branch
    $cursorCommit = git rev-parse cursor
    $sourceCommit = git rev-parse "origin/$SourceBranch"
    
    if ($cursorCommit -eq $sourceCommit) {
        Write-Host ""
        Write-Host "Cursor branch is already up to date with $SourceBranch." -ForegroundColor Green
    } else {
        # Step 5: Merge source branch into cursor
        Write-Host ""
        Write-Host "Merging origin/$SourceBranch into cursor branch..." -ForegroundColor Cyan
        
        # Try merge
        git merge --no-edit "origin/$SourceBranch" 2>&1 | Tee-Object -Variable mergeOutput
        
        if ($LASTEXITCODE -ne 0) {
            # Check if there are merge conflicts
            $conflictFiles = git diff --name-only --diff-filter=U
            
            if ($conflictFiles) {
                Write-Host ""
                Write-Host "Merge conflicts detected. Auto-resolving by keeping cursor branch changes..." -ForegroundColor Yellow
                Write-Host ""
                
                # Get all conflicted files
                $allConflicts = git status --short | Where-Object { $_ -match "^UU|^AA|^DU|^UD" }
                
                # Resolve conflicts automatically
                foreach ($conflict in $allConflicts) {
                    $status = $conflict.Substring(0, 2).Trim()
                    $file = $conflict.Substring(3).Trim()
                    
                    if ($status -eq "UU" -or $status -eq "AA") {
                        # Both modified - keep cursor version (ours)
                        Write-Host "  Keeping cursor version: $file" -ForegroundColor Gray
                        git checkout --ours $file
                        git add $file
                    }
                    elseif ($status -eq "DU") {
                        # Deleted by us (cursor) - keep deleted
                        Write-Host "  Keeping deleted: $file" -ForegroundColor Gray
                        git rm $file 2>&1 | Out-Null
                    }
                    elseif ($status -eq "UD") {
                        # Deleted by them (main) - keep file (cursor version)
                        Write-Host "  Keeping file: $file" -ForegroundColor Gray
                        git add $file
                    }
                }
                
                # Check if all conflicts are resolved
                $remainingConflicts = git diff --name-only --diff-filter=U
                
                if ($remainingConflicts) {
                    Write-Host ""
                    Write-Host "Warning: Some conflicts could not be auto-resolved:" -ForegroundColor Yellow
                    $remainingConflicts | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow }
                    Write-Host ""
                    Write-Host "Please resolve them manually, then run:" -ForegroundColor Yellow
                    Write-Host "  git add <resolved-files>" -ForegroundColor Gray
                    Write-Host "  git commit" -ForegroundColor Gray
                    
                    if ($stashed) {
                        Write-Host ""
                        Write-Host "Your local changes are still stashed. After resolving conflicts, run:" -ForegroundColor Yellow
                        Write-Host "  git stash pop" -ForegroundColor Gray
                    }
                    
                    exit 1
                }
                
                # Complete the merge
                Write-Host ""
                Write-Host "All conflicts resolved. Completing merge..." -ForegroundColor Cyan
                git commit -m "Merge $SourceBranch into cursor - keeping cursor branch changes" --no-edit
                
                if ($LASTEXITCODE -ne 0) {
                    Write-Error "Failed to complete merge commit"
                    if ($stashed) {
                        Write-Host "Restoring stashed changes..." -ForegroundColor Yellow
                        git stash pop
                    }
                    exit 1
                }
                
                Write-Host "Merge completed successfully." -ForegroundColor Green
            } else {
                Write-Error "Merge failed for unknown reason"
                if ($stashed) {
                    Write-Host "Restoring stashed changes..." -ForegroundColor Yellow
                    git stash pop
                }
                exit 1
            }
        } else {
            Write-Host "Merge completed successfully (no conflicts)." -ForegroundColor Green
        }
    }
    
    # Step 6: Restore stashed changes
    if ($stashed) {
        Write-Host ""
        Write-Host "Restoring your local changes..." -ForegroundColor Cyan
        git stash pop
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host ""
            Write-Host "Warning: Some conflicts occurred while restoring stashed changes." -ForegroundColor Yellow
            Write-Host "Please resolve them manually." -ForegroundColor Yellow
            Write-Host ""
            Write-Host "To see conflicts:" -ForegroundColor Gray
            Write-Host "  git status" -ForegroundColor Gray
        } else {
            Write-Host "Local changes restored successfully." -ForegroundColor Green
        }
    }
    
    # Step 7: Show final status
    Write-Host ""
    Write-Host "=== Sync completed ===" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Current status:" -ForegroundColor Yellow
    git status --short
    
    Write-Host ""
    Write-Host "Cursor branch has been synced with $SourceBranch." -ForegroundColor Green
    Write-Host "All conflicts resolved (kept cursor branch changes)." -ForegroundColor Green
    Write-Host "Your local changes have been preserved." -ForegroundColor Green
    
} catch {
    Write-Host ""
    Write-Host "Error occurred: $_" -ForegroundColor Red
    if ($stashed) {
        Write-Host "Attempting to restore stashed changes..." -ForegroundColor Yellow
        git stash pop 2>&1 | Out-Null
    }
    exit 1
} finally {
    Pop-Location
}
