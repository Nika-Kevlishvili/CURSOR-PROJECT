# Export Postman Collections Script
# ეს სკრიპტი ექსპორტირებს Postman collections-ს API-ის მეშვეობით
# This script exports Postman collections via API

# Change to workspace root directory (parent of migration/)
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent $scriptPath
Set-Location $workspaceRoot

param(
    [string]$ApiKey = $env:POSTMAN_API_KEY,
    [string]$OutputDir = "migration\postman_export"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Postman Collections Exporter" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (-not $ApiKey) {
    Write-Host "✗ POSTMAN_API_KEY not set!" -ForegroundColor Red
    Write-Host "Please set it: `$env:POSTMAN_API_KEY='your-api-key'" -ForegroundColor Yellow
    exit 1
}

# Create output directory
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
    Write-Host "✓ Created output directory: $OutputDir" -ForegroundColor Green
}

$headers = @{
    "X-Api-Key" = $ApiKey
}

# Get all collections
Write-Host "Fetching collections..." -ForegroundColor Yellow
try {
    $collectionsResponse = Invoke-RestMethod -Uri "https://api.getpostman.com/collections" -Headers $headers -Method Get
    $collections = $collectionsResponse.collections
    
    Write-Host "✓ Found $($collections.Count) collections" -ForegroundColor Green
    Write-Host ""
    
    # Export each collection
    $exported = 0
    foreach ($collection in $collections) {
        $collectionId = $collection.uid
        $collectionName = $collection.name
        
        Write-Host "Exporting: $collectionName..." -ForegroundColor Cyan
        
        try {
            # Get full collection
            $collectionResponse = Invoke-RestMethod -Uri "https://api.getpostman.com/collections/$collectionId" -Headers $headers -Method Get
            $collectionData = $collectionResponse.collection
            
            # Sanitize filename
            $safeName = $collectionName -replace '[<>:"/\\|?*]', '_'
            $fileName = "$OutputDir\$safeName.postman_collection.json"
            
            # Save to file
            $collectionData | ConvertTo-Json -Depth 100 | Out-File -FilePath $fileName -Encoding UTF8
            
            Write-Host "  ✓ Saved: $fileName" -ForegroundColor Green
            $exported++
        } catch {
            Write-Host "  ✗ Failed: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    Write-Host ""
    Write-Host "✓ Exported $exported of $($collections.Count) collections" -ForegroundColor Green
    
} catch {
    Write-Host "✗ Failed to fetch collections: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Get all environments
Write-Host ""
Write-Host "Fetching environments..." -ForegroundColor Yellow
try {
    $environmentsResponse = Invoke-RestMethod -Uri "https://api.getpostman.com/environments" -Headers $headers -Method Get
    $environments = $environmentsResponse.environments
    
    Write-Host "✓ Found $($environments.Count) environments" -ForegroundColor Green
    Write-Host ""
    
    # Export each environment
    $exportedEnv = 0
    foreach ($environment in $environments) {
        $environmentId = $environment.uid
        $environmentName = $environment.name
        
        Write-Host "Exporting: $environmentName..." -ForegroundColor Cyan
        
        try {
            # Get full environment
            $environmentResponse = Invoke-RestMethod -Uri "https://api.getpostman.com/environments/$environmentId" -Headers $headers -Method Get
            $environmentData = $environmentResponse.environment
            
            # Sanitize filename
            $safeName = $environmentName -replace '[<>:"/\\|?*]', '_'
            $fileName = "$OutputDir\$safeName.postman_environment.json"
            
            # Save to file
            $environmentData | ConvertTo-Json -Depth 100 | Out-File -FilePath $fileName -Encoding UTF8
            
            Write-Host "  ✓ Saved: $fileName" -ForegroundColor Green
            $exportedEnv++
        } catch {
            Write-Host "  ✗ Failed: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    Write-Host ""
    Write-Host "✓ Exported $exportedEnv of $($environments.Count) environments" -ForegroundColor Green
    
} catch {
    Write-Host "⚠ Failed to fetch environments: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Export completed!" -ForegroundColor Green
Write-Host "Files saved to: $OutputDir" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

