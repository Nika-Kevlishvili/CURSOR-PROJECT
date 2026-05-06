<#
.SYNOPSIS
  Static integrity checks for workspace Cursor rules/skills (no Cursor IDE required).

.DESCRIPTION
  - Ensures referenced paths under .cursor/ exist (from workflow_rules.mdc).
  - Warns if alwaysApply:false rules lack globs.
  - Fails if plaintext DB-style secrets appear in .cursor/**/*.md and .cursor/**/*.mdc.
  - Usage: powershell -ExecutionPolicy Bypass -File "Cursor-Project/scripts/validate-cursor-rules.ps1"
#>
$ErrorActionPreference = 'Stop'
# scripts live under Cursor-Project/scripts → workspace root is parent of Cursor-Project
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$CursorRoot = Join-Path $RepoRoot '.cursor'
if (-not (Test-Path $CursorRoot)) {
    Write-Error ".cursor not found at $CursorRoot"
}

$failures = @()
$warnings = @()

function Test-PathFromRepo([string]$RelativePath) {
    $clean = $RelativePath.Trim().TrimStart('/', '\')
    $full = Join-Path $RepoRoot $clean
    Test-Path -LiteralPath $full
}

# --- References from workflow_rules.mdc (Rule 32–41 pointers)
$workflowPath = Join-Path $CursorRoot 'rules\workflows\workflow_rules.mdc'
if (-not (Test-Path $workflowPath)) {
    $failures += "Missing $workflowPath"
} else {
    $content = Get-Content -LiteralPath $workflowPath -Raw
    # Longest suffix first: otherwise `.mdc` matches `.md` + stray `c`
    $pattern = '\.cursor/[a-zA-Z0-9_./-]+\.(?:mdc|md)'
    $refs = [regex]::Matches($content, $pattern) | ForEach-Object { $_.Value } | Sort-Object -Unique
    foreach ($ref in $refs) {
        $rel = $ref -replace '/', '\'
        if (-not (Test-PathFromRepo $rel)) {
            $failures += "Broken reference in workflow_rules.mdc: $ref"
        }
    }
}

# --- Scoped rules: alwaysApply false should have globs
Get-ChildItem -Path (Join-Path $CursorRoot 'rules') -Recurse -Filter '*.mdc' | ForEach-Object {
    $raw = Get-Content -LiteralPath $_.FullName -Raw
    if ($raw -match 'alwaysApply:\s*false') {
        if ($raw -notmatch 'globs:\s*\r?\n\s*-') {
            $warnings += "alwaysApply:false without globs list: $($_.FullName.Replace($RepoRoot, '').TrimStart('\'))"
        }
    }
}

# --- No plaintext passwords in .cursor markdown (best-effort)
# Markdown-style leaked secrets (backticks after Password label)
$secretPatterns = @(
    @{ Name = 'PasswordBacktick'; Regex = '(?i)\*\*Password:\*\*\s*`[^`\r\n]+`' }
    @{ Name = 'PasswordLine'; Regex = '(?i)^\s*-\s*\*?\*?Password\*?\*?:\s*.+$' }
)
Get-ChildItem -Path $CursorRoot -Recurse -Include '*.md', '*.mdc' | ForEach-Object {
    $t = Get-Content -LiteralPath $_.FullName -Raw
    foreach ($sp in $secretPatterns) {
        if ([regex]::IsMatch($t, $sp.Regex)) {
            $rel = $_.FullName.Replace($RepoRoot, '').TrimStart('\')
            $failures += "Possible secret pattern '$($sp.Name)' in: $rel"
        }
    }
}

Write-Host "Repo root: $RepoRoot"
Write-Host ""

if ($warnings.Count -gt 0) {
    Write-Host "WARNINGS:" -ForegroundColor Yellow
    $warnings | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow }
    Write-Host ""
}

if ($failures.Count -gt 0) {
    Write-Host "FAILURES:" -ForegroundColor Red
    $failures | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
    exit 1
}

Write-Host "validate-cursor-rules: OK (no failures)." -ForegroundColor Green
exit 0
