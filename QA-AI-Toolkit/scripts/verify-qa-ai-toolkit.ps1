# verify-qa-ai-toolkit.ps1
param([Parameter(Mandatory = $true)][string]$TargetPath)

$ErrorActionPreference = 'Continue'
$fail = $false
$partial = $false

function Check([string]$Name, [bool]$Ok, [string]$Detail, [switch]$Hard) {
    if ($Ok) { Write-Host "  OK   $Name - $Detail" -ForegroundColor Green }
    else {
        Write-Host "  FAIL $Name - $Detail" -ForegroundColor Red
        if ($Hard) { $script:fail = $true } else { $script:partial = $true }
    }
}

$TargetPath = [System.IO.Path]::GetFullPath($TargetPath)
Write-Host ""
Write-Host "=== Verify QA AI Toolkit @ $TargetPath ===" -ForegroundColor Cyan

$npmOk = $false
try { $null = npm -v 2>$null; if ($LASTEXITCODE -eq 0) { $npmOk = $true } } catch {}
Check 'npm' $npmOk $(if ($npmOk) { npm -v } else { 'missing' })

$cursorDir = Join-Path $TargetPath '.cursor'
Check '.cursor' (Test-Path $cursorDir) $(if (Test-Path $cursorDir) { 'present' } else { 'missing' }) -Hard

$cfgPath = Join-Path $cursorDir 'project-config.json'
$cfgOk = Test-Path $cfgPath
Check 'project-config.json' $cfgOk $(if ($cfgOk) { 'present' } else { 'missing' }) -Hard

if ($cfgOk) {
    try {
        $cfg = Get-Content $cfgPath -Raw | ConvertFrom-Json
        Check 'projectName' (-not [string]::IsNullOrWhiteSpace($cfg.projectName)) $cfg.projectName
        Check 'codeRoot' (-not [string]::IsNullOrWhiteSpace($cfg.codeRoot)) $cfg.codeRoot
        $envCount = @($cfg.environments).Count
        Check 'environments' ($envCount -ge 1) "$envCount configured"
        $cr = Join-Path $TargetPath $cfg.codeRoot
        Check 'codeRoot folder' (Test-Path $cr) $cr
    } catch {
        Check 'project-config parse' $false "$_" -Hard
    }
}

$envPath = Join-Path $TargetPath '.env'
Check '.env' (Test-Path $envPath) $(if (Test-Path $envPath) { 'present' } else { 'missing' })

$rules = Join-Path $cursorDir 'rules\main\core_rules.mdc'
Check 'core_rules.mdc' (Test-Path $rules) $(if (Test-Path $rules) { 'present' } else { 'missing' })

$hooks = Join-Path $cursorDir 'hooks.json'
Check 'hooks.json' (Test-Path $hooks) $(if (Test-Path $hooks) { 'present' } else { 'missing' })

$userMcp = Join-Path $env:USERPROFILE '.cursor\mcp.json'
$mcpOk = $false
$mcpDetail = 'missing'
if (Test-Path $userMcp) {
    try {
        $mcp = Get-Content $userMcp -Raw | ConvertFrom-Json
        $names = @($mcp.mcpServers.PSObject.Properties.Name)
        $need = @('Confluence', 'Jira')
        $missing = @($need | Where-Object { $names -notcontains $_ })
        if ($missing.Count -eq 0) { $mcpOk = $true; $mcpDetail = "servers: $($names -join ', ')" }
        else { $mcpDetail = "missing: $($missing -join ', ')" }
    } catch { $mcpDetail = "invalid JSON: $_" }
}
Check 'user mcp.json' $mcpOk $mcpDetail

Write-Host ""
if ($fail) { exit 1 }
if ($partial) { exit 2 }
exit 0
