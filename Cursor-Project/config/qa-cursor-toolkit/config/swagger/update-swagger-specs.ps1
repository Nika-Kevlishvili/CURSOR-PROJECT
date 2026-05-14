<#
.SYNOPSIS
    Downloads Swagger/OpenAPI specs for all configured environments.

.DESCRIPTION
    Reads environments.json and downloads each spec to its environment folder.

.PARAMETER Environment
    Optional. Download spec for a single environment only.

.EXAMPLE
    .\update-swagger-specs.ps1
    .\update-swagger-specs.ps1 -Environment dev
#>

param(
    [string]$Environment
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

$envFile = Join-Path $scriptDir "environments.json"
if (-not (Test-Path $envFile)) {
    Write-Host @"
[SETUP REQUIRED] environments.json not found.

Create $envFile with your API endpoints:

{
  "dev": {
    "url": "https://dev-api.your-company.com/v3/api-docs",
    "folder": "dev"
  }
}

See README.md for full instructions.
"@ -ForegroundColor Yellow
    exit 1
}

$envConfig = Get-Content $envFile -Raw | ConvertFrom-Json

$environments = if ($Environment) {
    @($Environment)
} else {
    $envConfig.PSObject.Properties.Name
}

$results = @()

foreach ($env in $environments) {
    $cfg = $envConfig.$env
    if (-not $cfg) {
        Write-Host "[SKIP] Unknown environment: $env" -ForegroundColor Yellow
        continue
    }

    $folder = Join-Path $scriptDir $cfg.folder
    New-Item -ItemType Directory -Path $folder -Force | Out-Null
    $outFile = Join-Path $folder "swagger-spec.json"

    Write-Host "Downloading $env from $($cfg.url)..." -NoNewline

    try {
        $headers = @{ "Accept" = "application/json" }

        if ($cfg.authHeader) {
            $headers[$cfg.authHeader.name] = $cfg.authHeader.value
        }

        Invoke-RestMethod -Uri $cfg.url -Headers $headers -OutFile $outFile -TimeoutSec 30
        $size = (Get-Item $outFile).Length
        Write-Host " OK ($([math]::Round($size/1024))KB)" -ForegroundColor Green
        $results += [PSCustomObject]@{ Env=$env; Status="ok"; Size="$([math]::Round($size/1024))KB" }
    } catch {
        Write-Host " FAILED: $($_.Exception.Message)" -ForegroundColor Red
        $results += [PSCustomObject]@{ Env=$env; Status="failed"; Size="-" }
    }
}

Write-Host "`n--- Summary ---"
$results | Format-Table -AutoSize

$failed = ($results | Where-Object { $_.Status -eq "failed" }).Count
if ($failed -gt 0) {
    Write-Host "$failed environment(s) failed. Cached specs (if any) remain." -ForegroundColor Yellow
    exit 2
}
exit 0
