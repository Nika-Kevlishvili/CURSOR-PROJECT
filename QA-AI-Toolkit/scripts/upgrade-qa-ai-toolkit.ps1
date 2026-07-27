# upgrade-qa-ai-toolkit.ps1
# Refresh .cursor rules/skills/agents/hooks from toolkit template.
# Does NOT manage repos. Preserves project-config.json and .env.

param(
    [Parameter(Mandatory = $true)][string]$TargetPath
)

$ErrorActionPreference = 'Stop'
$toolkitRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$templateCursor = Join-Path $toolkitRoot '.cursor-template'
$versionFile = Join-Path $toolkitRoot 'VERSION'
$toolkitVersion = if (Test-Path $versionFile) { (Get-Content $versionFile -Raw).Trim() } else { '0.1.0' }

$TargetPath = [System.IO.Path]::GetFullPath($TargetPath)
$destCursor = Join-Path $TargetPath '.cursor'
$cfgPath = Join-Path $destCursor 'project-config.json'

if (-not (Test-Path $destCursor)) {
    Write-Error 'No .cursor in target — run install-qa-ai-toolkit.ps1 first'
    exit 1
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$bak = Join-Path $TargetPath ".cursor.bak-upgrade-$stamp"
Copy-Item -Path $destCursor -Destination $bak -Recurse -Force
Write-Host "Backup: $bak" -ForegroundColor Green

$cfgBackup = $null
if (Test-Path $cfgPath) {
    $cfgBackup = Get-Content -LiteralPath $cfgPath -Raw -Encoding UTF8
}

foreach ($sub in @('rules', 'skills', 'agents', 'hooks')) {
    $src = Join-Path $templateCursor $sub
    $dst = Join-Path $destCursor $sub
    if (Test-Path $src) {
        if (Test-Path $dst) { Remove-Item $dst -Recurse -Force }
        Copy-Item -Path $src -Destination $dst -Recurse -Force
        Write-Host "Refreshed .cursor/$sub" -ForegroundColor Green
    }
}
Copy-Item -Path (Join-Path $templateCursor 'hooks.json') -Destination (Join-Path $destCursor 'hooks.json') -Force

# restore project-config and bump version
if ($cfgBackup) {
    $cfg = $cfgBackup | ConvertFrom-Json
    $cfg | Add-Member -NotePropertyName toolkitVersion -NotePropertyValue $toolkitVersion -Force
    $cfg | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $cfgPath -Encoding UTF8
    Write-Host "Restored project-config.json (toolkitVersion=$toolkitVersion)" -ForegroundColor Green
}

# refresh docs + config scripts/templates (safe)
$configDir = Join-Path $toolkitRoot 'config'
$destConfig = Join-Path $TargetPath 'config'
Copy-Item -Path (Join-Path $configDir 'scripts') -Destination (Join-Path $destConfig 'scripts') -Recurse -Force
Copy-Item -Path (Join-Path $configDir 'templates') -Destination (Join-Path $destConfig 'templates') -Recurse -Force
Copy-Item -Path (Join-Path $toolkitRoot 'docs\*') -Destination (Join-Path $TargetPath 'docs') -Recurse -Force -ErrorAction SilentlyContinue

Write-Host 'Upgrade complete. Repos under codeRoot were not modified.' -ForegroundColor Cyan
Write-Host 'To clone additional repos, run install with -FromStep 6' -ForegroundColor Gray
exit 0
