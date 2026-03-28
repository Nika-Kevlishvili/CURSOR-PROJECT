# update-swagger-specs.ps1
# Downloads OpenAPI JSON (/v3/api-docs) for every environment listed in
# Cursor-Project/config/swagger/environments.json into config/swagger/<id>/swagger-spec.json
#
# Usage (from anywhere):
#   .cursor/commands/update-swagger-specs.ps1
#
# Requires: curl.exe (Windows 10+), network access to internal hosts.

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$cursorProjectPath = Join-Path $workspaceRoot "Cursor-Project"
$swaggerDir = Join-Path $cursorProjectPath "config\swagger"
$manifestPath = Join-Path $swaggerDir "environments.json"

if (-not (Test-Path $manifestPath)) {
    Write-Error "Missing manifest: $manifestPath"
    exit 1
}

$curl = Get-Command curl.exe -ErrorAction SilentlyContinue
if (-not $curl) {
    Write-Error "curl.exe not found. Install curl or use Windows 10+."
    exit 1
}

$jsonText = Get-Content -Path $manifestPath -Raw -Encoding UTF8
$cfg = $jsonText | ConvertFrom-Json

Write-Host "=== Update Swagger / OpenAPI specs ===" -ForegroundColor Cyan
Write-Host "Manifest: $manifestPath" -ForegroundColor Gray
Write-Host ""

$failed = $false
foreach ($e in $cfg.environments) {
    $id = $e.id
    $url = $e.openapi_json
    if ([string]::IsNullOrWhiteSpace($url)) {
        Write-Host "Skip $id (no openapi_json)" -ForegroundColor Yellow
        continue
    }
    $outDir = Join-Path $swaggerDir $id
    if (-not (Test-Path $outDir)) {
        New-Item -ItemType Directory -Path $outDir -Force | Out-Null
    }
    $outFile = Join-Path $outDir "swagger-spec.json"
    Write-Host "[$id] GET $url" -ForegroundColor Gray
    & curl.exe -sS -m 180 -f -o $outFile $url
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  FAILED (curl exit $LASTEXITCODE)" -ForegroundColor Red
        $failed = $true
        continue
    }
    $len = (Get-Item $outFile).Length
    Write-Host "  -> $outFile ($len bytes)" -ForegroundColor Green
}

Write-Host ""
if ($failed) {
    Write-Host "Completed with errors." -ForegroundColor Red
    exit 1
}
Write-Host "All environments updated." -ForegroundColor Green
exit 0
