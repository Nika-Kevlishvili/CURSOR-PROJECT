# update-main-from-experiments.ps1
# Command: Updates main branch with experiments branch changes (merge and push)
# Usage: .cursor/commands/update-main-from-experiments.ps1

param(
    [switch]$KeepExperimentsChanges  # Keep experiments branch changes (default behavior)
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
    Write-Host "=== Updating main branch from experiments ===" -ForegroundColor Cyan
    Write-Host ""
    
    # Step 1: Check current branch and stash changes if needed
    $currentBranch = git branch --show-current
    Write-Host "Current branch: $currentBranch" -ForegroundColor Yellow
    Write-Host ""
    
    # Check for uncommitted changes
    $hasChanges = git status --porcelain
    $hasUncommittedChanges = $null -ne $hasChanges -and $hasChanges.Count -gt 0
    
    if ($hasUncommittedChanges) {
        Write-Host "Found uncommitted changes. Stashing to preserve them..." -ForegroundColor Yellow
        git stash push -u -m "Stashed before updating main from experiments - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to stash changes"
            exit 1
        }
        $stashed = $true
        Write-Host "Changes stashed successfully." -ForegroundColor Green
        Write-Host ""
    } else {
        $stashed = $false
    }
    
    # Step 2: Fetch latest from both branches
    Write-Host "Fetching latest changes from origin..." -ForegroundColor Cyan
    git fetch origin experiments
    git fetch origin main
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to fetch from origin"
        if ($stashed) {
            git stash pop
        }
        exit 1
    }
    Write-Host "Fetch completed successfully." -ForegroundColor Green
    Write-Host ""
    
    # Step 3: Switch to main branch
    Write-Host "Switching to main branch..." -ForegroundColor Cyan
    git checkout main
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to switch to main branch"
        if ($stashed) {
            git stash pop
        }
        exit 1
    }
    Write-Host "Switched to main branch." -ForegroundColor Green
    Write-Host ""
    
    # Step 4: Check if experiments has changes that main doesn't have
    $mainCommit = git rev-parse main
    $experimentsCommit = git rev-parse origin/experiments
    
    if ($mainCommit -eq $experimentsCommit) {
        Write-Host "Main branch is already up to date with experiments." -ForegroundColor Green
    } else {
        # Step 5: Merge experiments into main
        Write-Host "Merging origin/experiments into main branch..." -ForegroundColor Cyan
        
        # Try merge
        git merge --no-edit origin/experiments 2>&1 | Tee-Object -Variable mergeOutput
        
        if ($LASTEXITCODE -ne 0) {
            # Check if there are merge conflicts
            $conflictFiles = git diff --name-only --diff-filter=U
            
            if ($conflictFiles) {
                Write-Host ""
                Write-Host "Merge conflicts detected. Auto-resolving by keeping experiments branch changes..." -ForegroundColor Yellow
                Write-Host ""
                
                # Get all conflicted files
                $allConflicts = git status --short | Where-Object { $_ -match "^UU|^AA|^DU|^UD" }
                
                # Resolve conflicts automatically (keep experiments version)
                foreach ($conflict in $allConflicts) {
                    $status = $conflict.Substring(0, 2).Trim()
                    $file = $conflict.Substring(3).Trim()
                    
                    if ($status -eq "UU" -or $status -eq "AA") {
                        # Both modified - keep experiments version (theirs)
                        Write-Host "  Keeping experiments version: $file" -ForegroundColor Gray
                        git checkout --theirs $file
                        git add $file
                    }
                    elseif ($status -eq "DU") {
                        # Deleted by us (main) - keep deleted
                        Write-Host "  Keeping deleted: $file" -ForegroundColor Gray
                        git rm $file 2>&1 | Out-Null
                    }
                    elseif ($status -eq "UD") {
                        # Deleted by them (experiments) - keep file (experiments version)
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
                git commit -m "Merge experiments into main - keeping experiments branch changes" --no-edit
                
                if ($LASTEXITCODE -ne 0) {
                    Write-Error "Failed to complete merge commit"
                    if ($stashed) {
                        git stash pop
                    }
                    exit 1
                }
                
                Write-Host "Merge completed successfully." -ForegroundColor Green
            } else {
                Write-Error "Merge failed for unknown reason"
                if ($stashed) {
                    git stash pop
                }
                exit 1
            }
        } else {
            Write-Host "Merge completed successfully (no conflicts)." -ForegroundColor Green
        }
    }
    
    # Step 6: Push main to remote
    Write-Host ""
    Write-Host "Pushing main branch to origin..." -ForegroundColor Cyan
    git push origin main
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to push to origin/main"
        if ($stashed) {
            git stash pop
        }
        exit 1
    }
    Write-Host "Main branch pushed successfully." -ForegroundColor Green
    Write-Host ""
    
    # Step 7: Restore stashed changes
    if ($stashed) {
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
        Write-Host ""
    }
    
    # Step 8: Show final status
    Write-Host "=== Update completed ===" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Current status:" -ForegroundColor Yellow
    git status --short
    
    Write-Host ""
    Write-Host "Main branch has been updated with experiments branch changes." -ForegroundColor Green
    Write-Host "All conflicts resolved (kept experiments branch changes)." -ForegroundColor Green
    if ($stashed) {
        Write-Host "Your local changes have been preserved." -ForegroundColor Green
    }
    
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
