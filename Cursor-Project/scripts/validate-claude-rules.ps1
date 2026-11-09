<#
.SYNOPSIS
  Static integrity checks for the workspace Claude Code configuration under .claude/ (no Claude Code session required).

.DESCRIPTION
  - Ensures internal references in .claude/rules/**/*.md to .claude/... and Cursor-Project/... paths exist.
  - Fails if a rule's `paths:` frontmatter has no list, or a rule still carries Cursor frontmatter (alwaysApply / globs).
  - Fails if .claude/settings.json is not valid JSON or a guard registered in hooks/claude-hook.ps1 is missing.
  - Fails if an agent or skill lacks name/description frontmatter.
  - Fails if plaintext DB-style secrets appear in .claude/**/*.md.
  - Usage: pwsh -NoProfile -File "Cursor-Project/scripts/validate-claude-rules.ps1"
#>
$ErrorActionPreference = 'Stop'
# scripts live under Cursor-Project/scripts → workspace root is parent of Cursor-Project
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$RepoRoot = $RepoRoot.Path
$ClaudeRoot = Join-Path $RepoRoot '.claude'
if (-not (Test-Path $ClaudeRoot)) {
    Write-Error ".claude not found at $ClaudeRoot"
}

function Get-Rel([string]$FullPath) { [System.IO.Path]::GetRelativePath($RepoRoot, $FullPath) -replace '\\', '/' }
function Get-Frontmatter([string]$Text) {
    $m = [regex]::Match($Text, '(?s)\A\uFEFF?---\r?\n(.*?)\r?\n---')
    if ($m.Success) { $m.Groups[1].Value } else { $null }
}

$failures = @()
$warnings = @()

function Test-PathFromRepo([string]$RelativePath) {
    $clean = $RelativePath.Trim().TrimStart('/', '\')
    $full = Join-Path $RepoRoot $clean
    Test-Path -LiteralPath $full
}

# --- Internal path references in all workflow rules (.md under .claude/rules)
$refPattern = '(?:\.claude|Cursor-Project)/[a-zA-Z0-9_./-]+\.(?:md|ps1|mjs)'
$rulesRoot = Join-Path $ClaudeRoot 'rules'
if (-not (Test-Path -LiteralPath $rulesRoot)) {
    $failures += "Missing rules directory: $rulesRoot"
} else {
    Get-ChildItem -Path $rulesRoot -Recurse -Filter '*.md' -File | ForEach-Object {
        $mdcPath = $_.FullName
        $content = Get-Content -LiteralPath $mdcPath -Raw
        $refs = [regex]::Matches($content, $refPattern) | ForEach-Object { $_.Value } | Sort-Object -Unique
        foreach ($ref in $refs) {
            $rel = $ref -replace '/', '\'
            if (Test-PathFromRepo $rel) { continue }
            $relMdc = Get-Rel $mdcPath
            if ($ref -like '.claude/*') {
                $failures += "Broken reference in ${relMdc}: $ref"
            } else {
                # Skip paths that appear only inside backtick-wrapped inline code examples
                $escaped = [regex]::Escape($ref)
                $isExample = [regex]::IsMatch($content, '`[^`]*' + $escaped + '[^`]*`')
                if ($isExample) {
                    $warnings += "Example-only path (backtick-wrapped, skipped): $ref (from $relMdc)"
                } else {
                    $failures += "Missing referenced Cursor-Project path: $ref (from $relMdc)"
                }
            }
        }
    }
}

# --- Rule frontmatter: `paths:` must carry a list; no Cursor-only keys left behind
Get-ChildItem -Path $rulesRoot -Recurse -Filter '*.md' -File | ForEach-Object {
    $fm = Get-Frontmatter (Get-Content -LiteralPath $_.FullName -Raw)
    if ($null -eq $fm) { return }
    $rel = Get-Rel $_.FullName
    if ($fm -match '(?m)^paths:' -and $fm -notmatch '(?m)^paths:\s*\r?\n\s*-\s*\S') {
        $failures += "paths: without a glob list (rule would never load): $rel"
    }
    if ($fm -match '(?m)^(alwaysApply|globs):') {
        $failures += "Cursor frontmatter (alwaysApply / globs) in a Claude Code rule: $rel"
    }
}

# --- No README.md inside rules/ or agents/ (Claude Code would load it as a rule / agent)
foreach ($dir in @('rules', 'agents')) {
    Get-ChildItem -Path (Join-Path $ClaudeRoot $dir) -Recurse -Filter 'README.md' -File -ErrorAction SilentlyContinue | ForEach-Object {
        $failures += "README.md inside .claude/$dir is loaded by Claude Code; keep it under .claude/docs/: $(Get-Rel $_.FullName)"
    }
}

# --- Agents and skills need name + description frontmatter
$defs = @(Get-ChildItem -Path (Join-Path $ClaudeRoot 'agents') -Filter '*.md' -File)
Get-ChildItem -Path (Join-Path $ClaudeRoot 'skills') -Directory | ForEach-Object {
    $skillFile = Join-Path $_.FullName 'SKILL.md'
    if (Test-Path -LiteralPath $skillFile) { $defs += Get-Item -LiteralPath $skillFile }
    else { $failures += "Skill folder without SKILL.md: $(Get-Rel $_.FullName)" }
}
foreach ($def in $defs) {
    $fm = Get-Frontmatter (Get-Content -LiteralPath $def.FullName -Raw)
    if ($null -eq $fm -or $fm -notmatch '(?m)^name:\s*\S' -or $fm -notmatch '(?m)^description:\s*\S') {
        $failures += "Missing name/description frontmatter: $(Get-Rel $def.FullName)"
    }
}

# --- Hooks: settings.json parses, calls the dispatcher, and every registered guard exists
$settingsPath = Join-Path $ClaudeRoot 'settings.json'
$dispatcherPath = Join-Path $ClaudeRoot 'hooks/claude-hook.ps1'
if (-not (Test-Path -LiteralPath $settingsPath)) {
    $failures += 'Missing .claude/settings.json'
} else {
    $settingsRaw = Get-Content -LiteralPath $settingsPath -Raw
    try { $null = $settingsRaw | ConvertFrom-Json } catch { $failures += ".claude/settings.json is not valid JSON: $_" }
    foreach ($group in @('UserPromptSubmit', 'PreEdit', 'PostEdit', 'PreBash', 'PreMcp', 'Stop')) {
        if ($settingsRaw -notmatch ('claude-hook\.ps1\\"\s+' + $group + '"')) {
            $failures += ".claude/settings.json does not run claude-hook.ps1 for group: $group"
        }
    }
}
if (-not (Test-Path -LiteralPath $dispatcherPath)) {
    $failures += 'Missing .claude/hooks/claude-hook.ps1'
} else {
    $dispatcher = Get-Content -LiteralPath $dispatcherPath -Raw
    [regex]::Matches($dispatcher, "(?m)^\s*@\{\s*Script\s*=\s*'([^']+)'") | ForEach-Object {
        $guard = $_.Groups[1].Value
        if (-not (Test-Path -LiteralPath (Join-Path $ClaudeRoot "hooks/$guard"))) {
            $failures += "claude-hook.ps1 registers a missing guard: .claude/hooks/$guard"
        }
    }
}

# --- No plaintext passwords in .claude markdown (best-effort)
# Markdown-style leaked secrets (backticks after Password label)
$secretPatterns = @(
    @{ Name = 'PasswordBacktick'; Regex = '(?i)\*\*Password:\*\*\s*`[^`\r\n]+`' }
    @{ Name = 'PasswordLine'; Regex = '(?i)^\s*-\s*\*?\*?Password\*?\*?:\s*.+$' }
)
Get-ChildItem -Path $ClaudeRoot -Recurse -Include '*.md' -File | ForEach-Object {
    $t = Get-Content -LiteralPath $_.FullName -Raw
    foreach ($sp in $secretPatterns) {
        if ([regex]::IsMatch($t, $sp.Regex)) {
            $rel = Get-Rel $_.FullName
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

Write-Host "validate-claude-rules: OK (no failures)." -ForegroundColor Green
exit 0
