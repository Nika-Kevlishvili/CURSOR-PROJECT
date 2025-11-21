# Setup Script for New Computer
# ეს სკრიპტი დაგეხმარებათ ახალ კომპიუტერზე setup-ში
# This script helps with setup on the new computer

# Change to workspace root directory (parent of migration/)
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent $scriptPath
Set-Location $workspaceRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "New Computer Setup Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Working directory: $workspaceRoot" -ForegroundColor Gray
Write-Host ""

# Function to check if command exists
function Test-Command {
    param($command)
    $null = Get-Command $command -ErrorAction SilentlyContinue
    return $?
}

# Function to setup Python environment
function Setup-PythonEnvironment {
    Write-Host "Setting up Python environment..." -ForegroundColor Yellow
    
    if (-not (Test-Command "python")) {
        Write-Host "✗ Python is not installed. Please install Python 3.8+ first." -ForegroundColor Red
        return $false
    }
    
    $pythonVersion = python --version
    Write-Host "✓ Found: $pythonVersion" -ForegroundColor Green
    
    # Check if venv exists
    if (Test-Path "venv") {
        Write-Host "⚠ Virtual environment already exists. Skipping creation." -ForegroundColor Yellow
    } else {
        Write-Host "Creating virtual environment..." -ForegroundColor Cyan
        python -m venv venv
        Write-Host "✓ Virtual environment created" -ForegroundColor Green
    }
    
    Write-Host "Activating virtual environment..." -ForegroundColor Cyan
    & "venv\Scripts\Activate.ps1"
    
    # Install dependencies
    if (Test-Path "requirements.txt") {
        Write-Host "Installing Python dependencies..." -ForegroundColor Cyan
        pip install --upgrade pip
        pip install -r requirements.txt
        Write-Host "✓ Dependencies installed" -ForegroundColor Green
    } else {
        Write-Host "⚠ requirements.txt not found. Installing basic dependencies..." -ForegroundColor Yellow
        pip install requests>=2.31.0
    }
    
    # Install test agent dependencies
    if (Test-Path "config\requirements_test_agent.txt") {
        Write-Host "Installing test agent dependencies..." -ForegroundColor Cyan
        pip install -r config\requirements_test_agent.txt
        Write-Host "✓ Test agent dependencies installed" -ForegroundColor Green
    }
    
    return $true
}

# Function to setup environment variables
function Setup-EnvironmentVariables {
    Write-Host "Setting up Environment Variables..." -ForegroundColor Yellow
    Write-Host ""
    
    if (Test-Path "migration\environment_variables_export.ps1") {
        Write-Host "Found environment_variables_export.ps1" -ForegroundColor Green
        Write-Host "Please review and run: .\migration\environment_variables_export.ps1" -ForegroundColor Yellow
        Write-Host ""
        
        $response = Read-Host "Do you want to load environment variables now? (y/n)"
        if ($response -eq "y" -or $response -eq "Y") {
            . .\migration\environment_variables_export.ps1
            Write-Host "✓ Environment variables loaded" -ForegroundColor Green
        }
    } else {
        Write-Host "⚠ environment_variables_export.ps1 not found." -ForegroundColor Yellow
        Write-Host "Please set environment variables manually:" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Required variables:" -ForegroundColor Cyan
        Write-Host "  - GITLAB_URL" -ForegroundColor White
        Write-Host "  - GITLAB_TOKEN" -ForegroundColor White
        Write-Host "  - GITLAB_PROJECT_ID" -ForegroundColor White
        Write-Host "  - JIRA_URL" -ForegroundColor White
        Write-Host "  - JIRA_EMAIL" -ForegroundColor White
        Write-Host "  - JIRA_API_TOKEN" -ForegroundColor White
        Write-Host "  - POSTMAN_API_KEY" -ForegroundColor White
        Write-Host "  - POSTMAN_WORKSPACE_ID" -ForegroundColor White
        Write-Host ""
    }
}

# Function to verify directory structure
function Test-DirectoryStructure {
    Write-Host "Verifying directory structure..." -ForegroundColor Yellow
    
    $requiredDirs = @(
        "agents",
        "config",
        "docs",
        "postman",
        "phoenix-core-lib"
    )
    
    $allPresent = $true
    
    foreach ($dir in $requiredDirs) {
        if (Test-Path $dir) {
            Write-Host "✓ $dir/ exists" -ForegroundColor Green
        } else {
            Write-Host "✗ $dir/ missing" -ForegroundColor Red
            $allPresent = $false
        }
    }
    
    return $allPresent
}

# Function to test Python agents
function Test-PythonAgents {
    Write-Host "Testing Python agents..." -ForegroundColor Yellow
    
    if (-not (Test-Path "venv")) {
        Write-Host "⚠ Virtual environment not found. Please run Python setup first." -ForegroundColor Yellow
        return $false
    }
    
    & "venv\Scripts\Activate.ps1"
    
    Write-Host "Testing agent imports..." -ForegroundColor Cyan
    try {
        python -c "from agents import get_integration_service; print('✓ Integration Service OK')"
        python -c "from agents import get_phoenix_expert; print('✓ PhoenixExpert OK')"
        python -c "from agents.postman_collection_generator import PostmanCollectionGenerator; print('✓ Postman Generator OK')"
        Write-Host "✓ All agents imported successfully" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "✗ Agent import failed: $_" -ForegroundColor Red
        return $false
    }
}

# Function to setup Java/Gradle
function Setup-JavaGradle {
    Write-Host "Setting up Java/Gradle..." -ForegroundColor Yellow
    
    if (-not (Test-Command "java")) {
        Write-Host "✗ Java is not installed. Please install Java 17+ first." -ForegroundColor Red
        return $false
    }
    
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "✓ Found: $javaVersion" -ForegroundColor Green
    
    if (Test-Path "phoenix-core-lib\gradlew.bat") {
        Write-Host "Gradle wrapper found" -ForegroundColor Green
        
        $response = Read-Host "Do you want to test Gradle build? (y/n)"
        if ($response -eq "y" -or $response -eq "Y") {
            Push-Location "phoenix-core-lib"
            try {
                Write-Host "Running Gradle build..." -ForegroundColor Cyan
                .\gradlew.bat build --no-daemon
                Write-Host "✓ Gradle build successful" -ForegroundColor Green
            } catch {
                Write-Host "⚠ Gradle build had issues. Check manually." -ForegroundColor Yellow
            } finally {
                Pop-Location
            }
        }
    } else {
        Write-Host "⚠ Gradle wrapper not found in phoenix-core-lib/" -ForegroundColor Yellow
    }
    
    return $true
}

# Function to create .env.example
function New-EnvExample {
    Write-Host "Creating .env.example file..." -ForegroundColor Yellow
    
    $envExample = @"
# Environment Variables Template
# Copy this file to .env and fill in your values
# DO NOT commit .env to Git!

# GitLab Configuration
GITLAB_URL=https://gitlab.com
GITLAB_TOKEN=your-gitlab-token
GITLAB_PROJECT_ID=12345678
GITLAB_PIPELINE_ID=123456

# Jira Configuration
JIRA_URL=https://your-company.atlassian.net
JIRA_EMAIL=your-email@example.com
JIRA_API_TOKEN=your-jira-api-token
JIRA_PROJECT_KEY=PROJ

# Postman Configuration
POSTMAN_API_KEY=your-postman-api-key
POSTMAN_WORKSPACE_ID=your-workspace-id

# GitHub Configuration (optional)
GITHUB_TOKEN=your-github-token
"@
    
    $envExample | Out-File -FilePath ".env.example" -Encoding UTF8
    Write-Host "✓ .env.example created" -ForegroundColor Green
}

# Main execution
Write-Host "Starting setup process..." -ForegroundColor Green
Write-Host ""

# Check directory structure
if (-not (Test-DirectoryStructure)) {
    Write-Host "✗ Directory structure incomplete. Please copy all files first." -ForegroundColor Red
    exit 1
}

Write-Host "✓ Directory structure OK" -ForegroundColor Green
Write-Host ""

# Setup menu
Write-Host "Setup Options:" -ForegroundColor Cyan
Write-Host "1. Setup Python Environment" -ForegroundColor White
Write-Host "2. Setup Environment Variables" -ForegroundColor White
Write-Host "3. Setup Java/Gradle" -ForegroundColor White
Write-Host "4. Test Python Agents" -ForegroundColor White
Write-Host "5. Create .env.example" -ForegroundColor White
Write-Host "6. Run All Setup" -ForegroundColor White
Write-Host "0. Exit" -ForegroundColor White
Write-Host ""

$choice = Read-Host "Enter your choice"

switch ($choice) {
    "1" {
        Setup-PythonEnvironment
    }
    "2" {
        Setup-EnvironmentVariables
    }
    "3" {
        Setup-JavaGradle
    }
    "4" {
        Test-PythonAgents
    }
    "5" {
        New-EnvExample
    }
    "6" {
        Write-Host "Running all setup steps..." -ForegroundColor Cyan
        Write-Host ""
        Setup-PythonEnvironment
        Write-Host ""
        Setup-EnvironmentVariables
        Write-Host ""
        Setup-JavaGradle
        Write-Host ""
        New-EnvExample
        Write-Host ""
        Test-PythonAgents
        Write-Host ""
        Write-Host "✓ Setup completed!" -ForegroundColor Green
    }
    "0" {
        Write-Host "Exiting..." -ForegroundColor Yellow
        exit
    }
    default {
        Write-Host "Invalid choice." -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Setup process finished!" -ForegroundColor Green
Write-Host "Please review MIGRATION_GUIDE.md for additional steps." -ForegroundColor Cyan

