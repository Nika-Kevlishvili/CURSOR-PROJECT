# Load Environment Variables from .env file
# ეს სკრიპტი იტვირთებს environment variables .env ფაილიდან

$envFile = ".env"
$envExample = if (Test-Path ".env.example") { ".env.example" } else { "env.example" }

if (-not (Test-Path $envFile)) {
    Write-Host "❌ .env file not found!" -ForegroundColor Red
    if (Test-Path $envExample) {
        Write-Host "   Found $envExample template. Creating .env file..." -ForegroundColor Yellow
        Copy-Item $envExample $envFile -Force
        Write-Host "   ⚠️  Please edit .env file and fill in your actual values!" -ForegroundColor Yellow
    } else {
        Write-Host "   Please run .\setup_environment.ps1 first" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host "📥 Loading environment variables from .env..." -ForegroundColor Cyan

Get-Content $envFile | ForEach-Object {
    # Skip comments and empty lines
    if ($_ -match '^\s*#' -or $_ -match '^\s*$') {
        return
    }
    
    # Parse KEY=VALUE
    if ($_ -match '^([^#][^=]+)=(.*)$') {
        $key = $matches[1].Trim()
        $value = $matches[2].Trim()
        
        # Skip empty values
        if ([string]::IsNullOrWhiteSpace($value)) {
            return
        }
        
        # Remove quotes if present
        if ($value -match '^"(.*)"$' -or $value -match "^'(.*)'$") {
            $value = $matches[1]
        }
        
        # Set environment variable for current session
        [Environment]::SetEnvironmentVariable($key, $value, 'Process')
        
        # Mask sensitive values in output
        if ($key -match 'PASSWORD|TOKEN|KEY|SECRET|CREDENTIAL') {
            $maskedValue = if ($value.Length -gt 4) { 
                $value.Substring(0, 2) + "***" + $value.Substring($value.Length - 2)
            } else { 
                "***" 
            }
            Write-Host "  ✓ $key = $maskedValue" -ForegroundColor Green
        } else {
            Write-Host "  ✓ $key" -ForegroundColor Green
        }
    }
}

Write-Host ""
Write-Host "✅ Environment variables loaded!" -ForegroundColor Green
Write-Host ""
Write-Host "💡 Note: These variables are set for the current PowerShell session only." -ForegroundColor Yellow
Write-Host "   To make them permanent, set them in System Environment Variables or run:" -ForegroundColor Yellow
Write-Host "   [Environment]::SetEnvironmentVariable('KEY', 'VALUE', 'User')" -ForegroundColor Gray
Write-Host ""

