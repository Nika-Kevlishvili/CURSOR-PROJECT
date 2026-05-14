<#
.SYNOPSIS
    Pre-commit hook: blocks commits that include .env files or credential patterns.
#>

$ErrorActionPreference = "Stop"

$stagedFiles = git diff --cached --name-only 2>$null

$blocked = @()

foreach ($file in $stagedFiles) {
    if (-not $file) { continue }

    if ($file -match '\.env$' -and $file -notmatch '\.env\.example$') {
        $blocked += "[BLOCKED] $file — .env files must not be committed (contains credentials)"
    }

    if ($file -match 'credentials\.json$|secrets\.json$|\.pem$|\.key$') {
        $blocked += "[BLOCKED] $file — potential credential/key file"
    }
}

if ($blocked.Count -gt 0) {
    Write-Host "`n=== COMMIT BLOCKED ===" -ForegroundColor Red
    $blocked | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    Write-Host "`nRemove these files from staging: git reset HEAD <file>" -ForegroundColor Yellow
    exit 1
}

Write-Host "[OK] No credential files in commit" -ForegroundColor Green
exit 0
