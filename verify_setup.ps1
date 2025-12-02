# Setup Verification Script
# ეს სკრიპტი ამოწმებს, რომ ყველაფერი სწორადაა setup-ია
# This script verifies that everything is set up correctly

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Setup Verification Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$allChecksPassed = $true

# Function to check if command exists
function Test-Command {
    param($command)
    $null = Get-Command $command -ErrorAction SilentlyContinue
    return $?
}

# Function to check environment variable
function Test-EnvVar {
    param($varName)
    $value = [Environment]::GetEnvironmentVariable($varName, 'Process')
    if (-not $value) {
        $value = [Environment]::GetEnvironmentVariable($varName, 'User')
    }
    if (-not $value) {
        $value = [Environment]::GetEnvironmentVariable($varName, 'Machine')
    }
    return $null -ne $value -and $value -ne ""
}

# 1. Check Python
Write-Host "1. Checking Python..." -ForegroundColor Yellow
if (Test-Command "python") {
    $pythonVersion = python --version 2>&1
    Write-Host "   ✓ Python found: $pythonVersion" -ForegroundColor Green
} else {
    Write-Host "   ✗ Python not found. Please install Python 3.8+" -ForegroundColor Red
    $allChecksPassed = $false
}

# 2. Check Java
Write-Host "2. Checking Java..." -ForegroundColor Yellow
if (Test-Command "java") {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "   ✓ Java found: $javaVersion" -ForegroundColor Green
    
    # Check Java version (should be 17+)
    $versionOutput = java -version 2>&1 | Select-String -Pattern "version"
    if ($versionOutput -match "1\.(\d+)") {
        $majorVersion = [int]$matches[1]
        if ($majorVersion -ge 17) {
            Write-Host "   ✓ Java version 17+ (required)" -ForegroundColor Green
        } else {
            Write-Host "   ⚠ Java version $majorVersion found, but 17+ is required" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "   ✗ Java not found. Please install Java 17+" -ForegroundColor Red
    $allChecksPassed = $false
}

# 3. Check directory structure
Write-Host "3. Checking directory structure..." -ForegroundColor Yellow
$requiredDirs = @("agents", "config", "docs", "postman", "phoenix-core-lib")
$dirsOk = $true
foreach ($dir in $requiredDirs) {
    if (Test-Path $dir) {
        Write-Host "   ✓ $dir/ exists" -ForegroundColor Green
    } else {
        Write-Host "   ✗ $dir/ missing" -ForegroundColor Red
        $dirsOk = $false
        $allChecksPassed = $false
    }
}
if ($dirsOk) {
    Write-Host "   ✓ Directory structure OK" -ForegroundColor Green
}

# 4. Check .env file
Write-Host "4. Checking environment configuration..." -ForegroundColor Yellow
if (Test-Path ".env") {
    Write-Host "   ✓ .env file exists" -ForegroundColor Green
} elseif (Test-Path ".env.example") {
    Write-Host "   ⚠ .env file not found, but .env.example exists" -ForegroundColor Yellow
    Write-Host "     Run: .\setup_environment.ps1" -ForegroundColor Gray
} else {
    Write-Host "   ⚠ .env.example not found" -ForegroundColor Yellow
}

# 5. Check environment variables
Write-Host "5. Checking environment variables..." -ForegroundColor Yellow
$envVars = @(
    "GITLAB_URL", "GITLAB_TOKEN", "GITLAB_PROJECT_ID",
    "JIRA_URL", "JIRA_EMAIL", "JIRA_API_TOKEN", "JIRA_PROJECT_KEY",
    "POSTMAN_API_KEY", "POSTMAN_WORKSPACE_ID"
)
$envVarsOk = $true
foreach ($var in $envVars) {
    if (Test-EnvVar $var) {
        Write-Host "   ✓ $var is set" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ $var is not set (optional)" -ForegroundColor Yellow
    }
}

# 6. Check Python virtual environment
Write-Host "6. Checking Python virtual environment..." -ForegroundColor Yellow
if (Test-Path "venv") {
    Write-Host "   ✓ Virtual environment exists" -ForegroundColor Green
    
    # Check if activated
    if ($env:VIRTUAL_ENV) {
        Write-Host "   ✓ Virtual environment is activated" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ Virtual environment not activated. Run: venv\Scripts\activate" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ⚠ Virtual environment not found. Run: python -m venv venv" -ForegroundColor Yellow
}

# 7. Check Python dependencies
Write-Host "7. Checking Python dependencies..." -ForegroundColor Yellow
if (Test-Path "venv") {
    $venvPython = "venv\Scripts\python.exe"
    if (Test-Path $venvPython) {
        try {
            $result = & $venvPython -c "import requests; print('OK')" 2>&1
            if ($result -eq "OK") {
                Write-Host "   ✓ requests library installed" -ForegroundColor Green
            } else {
                Write-Host "   ✗ requests library not installed. Run: pip install -r requirements.txt" -ForegroundColor Red
                $allChecksPassed = $false
            }
        } catch {
            Write-Host "   ✗ Error checking Python dependencies" -ForegroundColor Red
            $allChecksPassed = $false
        }
    }
} else {
    Write-Host "   ⚠ Skipping (virtual environment not found)" -ForegroundColor Yellow
}

# 8. Check Gradle wrapper
Write-Host "8. Checking Gradle wrapper..." -ForegroundColor Yellow
if (Test-Path "phoenix-core-lib\gradlew.bat") {
    Write-Host "   ✓ Gradle wrapper found" -ForegroundColor Green
} else {
    Write-Host "   ✗ Gradle wrapper not found" -ForegroundColor Red
    $allChecksPassed = $false
}

# 9. Check agents can be imported
Write-Host "9. Testing Python agents..." -ForegroundColor Yellow
if (Test-Path "venv\Scripts\python.exe") {
    $venvPython = "venv\Scripts\python.exe"
    try {
        $result = & $venvPython -c "from agents import get_integration_service; print('OK')" 2>&1
        if ($result -match "OK") {
            Write-Host "   ✓ Agents can be imported" -ForegroundColor Green
        } else {
            Write-Host "   ⚠ Agents import had warnings (check output above)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "   ✗ Agents cannot be imported: $_" -ForegroundColor Red
        $allChecksPassed = $false
    }
} else {
    Write-Host "   ⚠ Skipping (virtual environment not found)" -ForegroundColor Yellow
}

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($allChecksPassed) {
    Write-Host "✓ All critical checks passed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "  1. Ensure all environment variables are set" -ForegroundColor White
    Write-Host "  2. Activate virtual environment: venv\Scripts\activate" -ForegroundColor White
    Write-Host "  3. Test agents: python -c 'from agents import get_integration_service; print(\"OK\")'" -ForegroundColor White
} else {
    Write-Host "⚠ Some checks failed. Please fix the issues above." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Common fixes:" -ForegroundColor Cyan
    Write-Host "  - Install Python: https://www.python.org/downloads/" -ForegroundColor White
    Write-Host "  - Install Java 17+: https://adoptium.net/" -ForegroundColor White
    Write-Host "  - Setup environment: .\setup_environment.ps1" -ForegroundColor White
    Write-Host "  - Create venv: python -m venv venv" -ForegroundColor White
    Write-Host "  - Install dependencies: pip install -r requirements.txt" -ForegroundColor White
}
Write-Host "========================================" -ForegroundColor Cyan

