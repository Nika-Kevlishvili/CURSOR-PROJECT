# Simple Setup Script for New Computer
# ეს არის მარტივი setup script ახალ კომპიუტერზე
# This is a simple setup script for new computer

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Project Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if .env exists
if (-not (Test-Path ".env")) {
    Write-Host "Creating .env file from template..." -ForegroundColor Yellow
    if (Test-Path "env.example") {
        Copy-Item "env.example" ".env"
        Write-Host "✓ .env file created" -ForegroundColor Green
        Write-Host "⚠️  Please edit .env file with your credentials!" -ForegroundColor Yellow
    }
}

# Check Python
if (Get-Command python -ErrorAction SilentlyContinue) {
    Write-Host "✓ Python found: $(python --version)" -ForegroundColor Green
    
    # Create venv if not exists
    if (-not (Test-Path "venv")) {
        Write-Host "Creating virtual environment..." -ForegroundColor Cyan
        python -m venv venv
    }
    
    # Install dependencies
    if (Test-Path "requirements.txt") {
        Write-Host "Installing Python dependencies..." -ForegroundColor Cyan
        & "venv\Scripts\python.exe" -m pip install --upgrade pip --quiet
        & "venv\Scripts\python.exe" -m pip install -r requirements.txt --quiet
        Write-Host "✓ Dependencies installed" -ForegroundColor Green
    }
} else {
    Write-Host "✗ Python not found. Please install Python 3.8+" -ForegroundColor Red
}

# Check Java
if (Get-Command java -ErrorAction SilentlyContinue) {
    Write-Host "✓ Java found" -ForegroundColor Green
} else {
    Write-Host "⚠️  Java not found. Install Java 17+ for phoenix-core-lib" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Edit .env file with your credentials" -ForegroundColor White
Write-Host "2. Run: .\load_environment.ps1" -ForegroundColor White
Write-Host "3. Run: .\verify_setup.ps1" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan

