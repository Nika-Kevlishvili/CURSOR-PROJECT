# Credentials Synchronization Script
# ეს script დაგეხმარებათ credentials-ის სინქრონიზაციაში სხვა კომპიუტერზე
# This script helps synchronize credentials to another computer

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Credentials Synchronization Helper" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  SECURITY WARNING:" -ForegroundColor Yellow
Write-Host "   This script helps you prepare credentials for transfer" -ForegroundColor Yellow
Write-Host "   DO NOT commit credentials to Git!" -ForegroundColor Red
Write-Host ""

if (-not (Test-Path ".env")) {
    Write-Host "❌ .env file not found!" -ForegroundColor Red
    Write-Host "   Please create .env file first or run: .\store_credentials.ps1" -ForegroundColor Yellow
    exit 1
}

Write-Host "Options:" -ForegroundColor Cyan
Write-Host "1. Export credentials to encrypted file (for secure transfer)" -ForegroundColor White
Write-Host "2. Verify .env is in .gitignore (security check)" -ForegroundColor White
Write-Host "3. Show credentials status (masked)" -ForegroundColor White
Write-Host ""

$choice = Read-Host "Enter your choice (1-3)"

switch ($choice) {
    "1" {
        Write-Host ""
        Write-Host "Exporting credentials..." -ForegroundColor Cyan
        
        # Check if .env is in .gitignore
        $gitignoreContent = Get-Content .gitignore -ErrorAction SilentlyContinue
        $envInGitignore = $gitignoreContent -match '^\.env$' -or $gitignoreContent -match '^\*\.env$'
        
        if (-not $envInGitignore) {
            Write-Host "⚠️  WARNING: .env might not be in .gitignore!" -ForegroundColor Yellow
            Write-Host "   Please verify .gitignore contains '.env'" -ForegroundColor Yellow
        } else {
            Write-Host "✓ .env is in .gitignore" -ForegroundColor Green
        }
        
        # Create backup with timestamp
        $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
        $backupFile = "credentials_backup_$timestamp.txt"
        
        Write-Host ""
        Write-Host "Creating backup file: $backupFile" -ForegroundColor Cyan
        Write-Host "⚠️  IMPORTANT: This file contains sensitive data!" -ForegroundColor Yellow
        Write-Host "   - Store it securely (password manager, encrypted USB)" -ForegroundColor Yellow
        Write-Host "   - DO NOT commit to Git!" -ForegroundColor Red
        Write-Host "   - Delete after transferring to other computer" -ForegroundColor Yellow
        Write-Host ""
        
        $confirm = Read-Host "Continue? (y/n)"
        if ($confirm -ne 'y' -and $confirm -ne 'Y') {
            Write-Host "Cancelled." -ForegroundColor Yellow
            exit 0
        }
        
        # Copy .env to backup
        Copy-Item .env $backupFile -Force
        
        Write-Host ""
        Write-Host "✓ Backup created: $backupFile" -ForegroundColor Green
        Write-Host ""
        Write-Host "Next steps:" -ForegroundColor Cyan
        Write-Host "1. Transfer $backupFile securely to other computer" -ForegroundColor White
        Write-Host "2. On other computer: Rename to .env" -ForegroundColor White
        Write-Host "3. Run: .\load_environment.ps1" -ForegroundColor White
        Write-Host "4. Delete $backupFile after transfer" -ForegroundColor White
    }
    "2" {
        Write-Host ""
        Write-Host "Checking .gitignore..." -ForegroundColor Cyan
        
        if (-not (Test-Path ".gitignore")) {
            Write-Host "❌ .gitignore file not found!" -ForegroundColor Red
            exit 1
        }
        
        $gitignoreContent = Get-Content .gitignore
        $envInGitignore = $gitignoreContent -match '^\.env$' -or $gitignoreContent -match '^\*\.env$'
        
        if ($envInGitignore) {
            Write-Host "✓ .env is in .gitignore" -ForegroundColor Green
        } else {
            Write-Host "❌ .env is NOT in .gitignore!" -ForegroundColor Red
            Write-Host "   Adding .env to .gitignore..." -ForegroundColor Yellow
            Add-Content .gitignore "`n# Environment Variables and Secrets`n.env"
            Write-Host "✓ Added .env to .gitignore" -ForegroundColor Green
        }
        
        # Check if .env is tracked by git
        $gitStatus = git status --porcelain .env 2>$null
        if ($gitStatus) {
            Write-Host "⚠️  WARNING: .env might be tracked by Git!" -ForegroundColor Yellow
            Write-Host "   Run: git rm --cached .env" -ForegroundColor Yellow
        } else {
            Write-Host "✓ .env is not tracked by Git" -ForegroundColor Green
        }
    }
    "3" {
        Write-Host ""
        Write-Host "Credentials Status (masked):" -ForegroundColor Cyan
        Write-Host "============================" -ForegroundColor Cyan
        
        Get-Content .env | ForEach-Object {
            if ($_ -match '^([^#][^=]+)=(.*)$') {
                $key = $matches[1].Trim()
                $value = $matches[2].Trim()
                
                if (-not [string]::IsNullOrWhiteSpace($value)) {
                    if ($key -match 'PASSWORD|TOKEN|KEY|SECRET|CREDENTIAL') {
                        $maskedValue = if ($value.Length -gt 4) { 
                            $value.Substring(0, 2) + "***" + $value.Substring($value.Length - 2)
                        } else { 
                            "***" 
                        }
                        Write-Host "$key = $maskedValue" -ForegroundColor Yellow
                    } else {
                        Write-Host "$key = $value" -ForegroundColor Green
                    }
                } else {
                    Write-Host "$key = (not set)" -ForegroundColor Gray
                }
            }
        }
    }
    default {
        Write-Host "Invalid choice." -ForegroundColor Red
    }
}

Write-Host ""

