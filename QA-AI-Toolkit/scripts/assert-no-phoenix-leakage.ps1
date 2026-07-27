# assert-no-phoenix-leakage.ps1
# Fail if toolkit templates contain product-specific leakage strings.
param(
    [string]$ToolkitRoot = ''
)

$ErrorActionPreference = 'Stop'
if (-not $ToolkitRoot) {
    $ToolkitRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
}

$banned = @(
    'PhoenixExpert',
    'EnergoTS',
    'Energo-Pro',
    'energo-pro',
    'playwright',
    'Playwright',
    'git.domain.internal',
    'switch-phoenix',
    'PHOENIX-SWITCH',
    'protect-phoenix',
    'protect-energots'
)

# Scan shipped templates/config only (not this assert script itself)
$roots = @(
    (Join-Path $ToolkitRoot '.cursor-template'),
    (Join-Path $ToolkitRoot 'config')
)

$hits = @()
foreach ($root in $roots) {
    if (-not (Test-Path $root)) { continue }
    Get-ChildItem -Path $root -Recurse -File | ForEach-Object {
        $path = $_.FullName
        if ($path -match 'MIGRATION') { return }
        $text = Get-Content -LiteralPath $path -Raw -ErrorAction SilentlyContinue
        if (-not $text) { return }
        foreach ($b in $banned) {
            if ($text.Contains($b)) {
                $hits += "$path :: contains '$b'"
            }
        }
    }
}

# Special: "Phoenix" as product name in template content (not folder QA toolkit comments)
$phoenixRoots = @((Join-Path $ToolkitRoot '.cursor-template'))
foreach ($root in $phoenixRoots) {
    Get-ChildItem -Path $root -Recurse -File -ErrorAction SilentlyContinue | ForEach-Object {
        $text = Get-Content -LiteralPath $_.FullName -Raw -ErrorAction SilentlyContinue
        if ($text -match '\bPhoenix\b') {
            $hits += "$($_.FullName) :: contains word Phoenix"
        }
    }
}

if ($hits.Count -gt 0) {
    Write-Host 'LEAKAGE DETECTED:' -ForegroundColor Red
    $hits | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
    exit 1
}

Write-Host 'No banned product leakage found in toolkit templates.' -ForegroundColor Green
exit 0
