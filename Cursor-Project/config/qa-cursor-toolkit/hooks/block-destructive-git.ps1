<#
.SYNOPSIS
    Pre-push hook: blocks force-push and other destructive git operations.
#>

$ErrorActionPreference = "Stop"

$pushArgs = $args -join " "

if ($pushArgs -match '--force|-f\b|--force-with-lease') {
    Write-Host "`n=== PUSH BLOCKED ===" -ForegroundColor Red
    Write-Host "Force-push is not allowed by project safety rules." -ForegroundColor Red
    Write-Host "If you truly need this, remove the hook temporarily and push manually." -ForegroundColor Yellow
    exit 1
}

$currentBranch = git rev-parse --abbrev-ref HEAD 2>$null

if ($currentBranch -match '^(main|master|prod|production)$') {
    Write-Host "`n=== PUSH BLOCKED ===" -ForegroundColor Red
    Write-Host "Direct push to '$currentBranch' is not allowed." -ForegroundColor Red
    Write-Host "Use a feature branch and create a merge/pull request." -ForegroundColor Yellow
    exit 1
}

exit 0
