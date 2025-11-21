# Migration Helper Script
# ეს სკრიპტი დაგეხმარებათ მიგრაციის პროცესში
# This script helps with the migration process

# Change to workspace root directory (parent of migration/)
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent $scriptPath
Set-Location $workspaceRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Environment Migration Helper" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Working directory: $workspaceRoot" -ForegroundColor Gray
Write-Host ""

# Function to check if command exists
function Test-Command {
    param($command)
    $null = Get-Command $command -ErrorAction SilentlyContinue
    return $?
}

# Function to export environment variables to file
function Export-EnvironmentVariables {
    Write-Host "Exporting Environment Variables..." -ForegroundColor Yellow
    
    $envVars = @{
        "GITLAB_URL" = $env:GITLAB_URL
        "GITLAB_TOKEN" = $env:GITLAB_TOKEN
        "GITLAB_PROJECT_ID" = $env:GITLAB_PROJECT_ID
        "GITLAB_PIPELINE_ID" = $env:GITLAB_PIPELINE_ID
        "JIRA_URL" = $env:JIRA_URL
        "JIRA_EMAIL" = $env:JIRA_EMAIL
        "JIRA_API_TOKEN" = $env:JIRA_API_TOKEN
        "JIRA_PROJECT_KEY" = $env:JIRA_PROJECT_KEY
        "POSTMAN_API_KEY" = $env:POSTMAN_API_KEY
        "POSTMAN_WORKSPACE_ID" = $env:POSTMAN_WORKSPACE_ID
        "GITHUB_TOKEN" = $env:GITHUB_TOKEN
    }
    
    $output = @"
# Environment Variables Export
# Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
# 
# IMPORTANT: This file contains sensitive information!
# Do NOT commit this file to Git!
# 
# To use these variables on the new computer:
# 1. Review and update values
# 2. Set them as environment variables:
#    `$env:VARIABLE_NAME="value"
# 3. Or add to Windows Environment Variables (System Properties)

"@
    
    foreach ($key in $envVars.Keys) {
        $value = $envVars[$key]
        if ($value) {
            $output += "`$env:$key=`"$value`"`n"
        } else {
            $output += "# `$env:$key=NOT_SET`n"
        }
    }
    
    $outputFile = "migration\environment_variables_export.ps1"
    $output | Out-File -FilePath $outputFile -Encoding UTF8
    
    Write-Host "✓ Environment variables exported to: $outputFile" -ForegroundColor Green
    Write-Host "⚠ WARNING: This file contains sensitive data. Keep it secure!" -ForegroundColor Red
}

# Function to check system requirements
function Test-SystemRequirements {
    Write-Host "Checking System Requirements..." -ForegroundColor Yellow
    Write-Host ""
    
    $requirements = @{
        "Python" = @{ Command = "python"; Version = "3.8+"; Optional = $false }
        "Java" = @{ Command = "java"; Version = "17+"; Optional = $false }
        "Git" = @{ Command = "git"; Version = "2.0+"; Optional = $false }
        "Gradle" = @{ Command = "gradle"; Version = "7.0+"; Optional = $true }
        "Node.js" = @{ Command = "node"; Version = "16+"; Optional = $true }
    }
    
    $allOk = $true
    
    foreach ($req in $requirements.Keys) {
        $info = $requirements[$req]
        $exists = Test-Command $info.Command
        
        if ($exists) {
            try {
                $version = & $info.Command --version 2>&1 | Select-Object -First 1
                Write-Host "✓ $req : $version" -ForegroundColor Green
            } catch {
                Write-Host "✓ $req : Installed" -ForegroundColor Green
            }
        } else {
            if ($info.Optional) {
                Write-Host "⚠ $req : Not installed (Optional)" -ForegroundColor Yellow
            } else {
                Write-Host "✗ $req : Not installed (Required)" -ForegroundColor Red
                $allOk = $false
            }
        }
    }
    
    Write-Host ""
    return $allOk
}

# Function to check environment variables
function Test-EnvironmentVariables {
    Write-Host "Checking Environment Variables..." -ForegroundColor Yellow
    Write-Host ""
    
    $requiredVars = @(
        @{ Name = "GITLAB_URL"; Required = $false }
        @{ Name = "GITLAB_TOKEN"; Required = $false }
        @{ Name = "GITLAB_PROJECT_ID"; Required = $false }
        @{ Name = "JIRA_URL"; Required = $false }
        @{ Name = "JIRA_EMAIL"; Required = $false }
        @{ Name = "JIRA_API_TOKEN"; Required = $false }
        @{ Name = "POSTMAN_API_KEY"; Required = $false }
        @{ Name = "POSTMAN_WORKSPACE_ID"; Required = $false }
    )
    
    $missing = @()
    
    foreach ($var in $requiredVars) {
        $value = (Get-Item "Env:$($var.Name)" -ErrorAction SilentlyContinue).Value
        if ($value) {
            Write-Host "✓ $($var.Name) : Set" -ForegroundColor Green
        } else {
            if ($var.Required) {
                Write-Host "✗ $($var.Name) : Not set (Required)" -ForegroundColor Red
                $missing += $var.Name
            } else {
                Write-Host "⚠ $($var.Name) : Not set (Optional)" -ForegroundColor Yellow
            }
        }
    }
    
    Write-Host ""
    return $missing.Count -eq 0
}

# Function to create migration checklist
function New-MigrationChecklist {
    Write-Host "Creating Migration Checklist..." -ForegroundColor Yellow
    
    $checklist = @"
# Migration Checklist
# Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')

## Pre-Migration (Current Computer)

### Files to Copy
- [ ] agents/ directory
- [ ] config/ directory
- [ ] docs/ directory
- [ ] postman/ directory
- [ ] phoenix-core-lib/ directory
- [ ] examples/ directory
- [ ] MIGRATION_GUIDE.md
- [ ] migration_helper.ps1

### Credentials to Document
- [ ] GitLab Token
- [ ] Postman API Key
- [ ] Postman Workspace ID
- [ ] Jira API Token
- [ ] Jira Email
- [ ] Database Connection Strings
- [ ] GitHub Token (if used)

### Postman
- [ ] Export all Collections (29 collections)
- [ ] Export all Environments (5 environments)
- [ ] Document workspace ID

### Git
- [ ] Git credentials configured
- [ ] Git config exported (if needed)

## Post-Migration (New Computer)

### System Setup
- [ ] Python installed (3.8+)
- [ ] Java installed (17+)
- [ ] Git installed
- [ ] Gradle installed (or use wrapper)
- [ ] Node.js installed (optional, for Playwright/Newman)

### Files
- [ ] All files copied
- [ ] Directory structure verified

### Environment Variables
- [ ] GITLAB_URL set
- [ ] GITLAB_TOKEN set
- [ ] GITLAB_PROJECT_ID set
- [ ] JIRA_URL set
- [ ] JIRA_EMAIL set
- [ ] JIRA_API_TOKEN set
- [ ] JIRA_PROJECT_KEY set
- [ ] POSTMAN_API_KEY set
- [ ] POSTMAN_WORKSPACE_ID set

### Python Environment
- [ ] Virtual environment created
- [ ] Dependencies installed (pip install -r requirements.txt)
- [ ] Agents tested

### Java/Gradle
- [ ] Gradle wrapper working
- [ ] Application properties configured
- [ ] Database connections tested

### Postman
- [ ] Postman installed
- [ ] Collections imported
- [ ] Environments imported
- [ ] API key configured

### Testing
- [ ] Integration Service tested
- [ ] PhoenixExpert agent tested
- [ ] Postman Collection Generator tested
- [ ] Test Agent tested
- [ ] Database connections tested

## Verification

### Git/GitLab
- [ ] GitLab API accessible
- [ ] GitLab project accessible
- [ ] Git credentials working

### Postman
- [ ] Postman API accessible
- [ ] Collections visible
- [ ] Environments visible

### Jira
- [ ] Jira API accessible
- [ ] Jira project accessible

### Confluence
- [ ] Confluence accessible (read-only)
- [ ] PhoenixExpert can access Confluence

### Database
- [ ] PostgreSQL connection works
- [ ] Oracle connection works (if used)
- [ ] EnergoPro connection works (if used)

"@
    
    $checklistFile = "migration\MIGRATION_CHECKLIST.md"
    $checklist | Out-File -FilePath $checklistFile -Encoding UTF8
    
    Write-Host "✓ Checklist created: $checklistFile" -ForegroundColor Green
}

# Function to test connections
function Test-Connections {
    Write-Host "Testing Connections..." -ForegroundColor Yellow
    Write-Host ""
    
    # Test GitLab
    if ($env:GITLAB_URL -and $env:GITLAB_TOKEN) {
        Write-Host "Testing GitLab connection..." -ForegroundColor Cyan
        try {
            $headers = @{ "PRIVATE-TOKEN" = $env:GITLAB_TOKEN }
            $response = Invoke-RestMethod -Uri "$($env:GITLAB_URL)/api/v4/user" -Headers $headers -Method Get -ErrorAction Stop
            Write-Host "✓ GitLab: Connected as $($response.username)" -ForegroundColor Green
        } catch {
            Write-Host "✗ GitLab: Connection failed - $($_.Exception.Message)" -ForegroundColor Red
        }
    } else {
        Write-Host "⚠ GitLab: Not configured" -ForegroundColor Yellow
    }
    
    # Test Postman
    if ($env:POSTMAN_API_KEY) {
        Write-Host "Testing Postman API connection..." -ForegroundColor Cyan
        try {
            $headers = @{ "X-Api-Key" = $env:POSTMAN_API_KEY }
            $response = Invoke-RestMethod -Uri "https://api.getpostman.com/workspaces" -Headers $headers -Method Get -ErrorAction Stop
            Write-Host "✓ Postman: Connected (Found $($response.workspaces.Count) workspaces)" -ForegroundColor Green
        } catch {
            Write-Host "✗ Postman: Connection failed - $($_.Exception.Message)" -ForegroundColor Red
        }
    } else {
        Write-Host "⚠ Postman: Not configured" -ForegroundColor Yellow
    }
    
    Write-Host ""
}

# Main menu
function Show-Menu {
    Write-Host ""
    Write-Host "Select an option:" -ForegroundColor Cyan
    Write-Host "1. Check System Requirements" -ForegroundColor White
    Write-Host "2. Check Environment Variables" -ForegroundColor White
    Write-Host "3. Export Environment Variables" -ForegroundColor White
    Write-Host "4. Create Migration Checklist" -ForegroundColor White
    Write-Host "5. Test Connections" -ForegroundColor White
    Write-Host "6. Run All Checks" -ForegroundColor White
    Write-Host "0. Exit" -ForegroundColor White
    Write-Host ""
}

# Main execution
Write-Host "Welcome to Migration Helper!" -ForegroundColor Green
Write-Host ""

while ($true) {
    Show-Menu
    $choice = Read-Host "Enter your choice"
    
    switch ($choice) {
        "1" {
            Test-SystemRequirements
        }
        "2" {
            Test-EnvironmentVariables
        }
        "3" {
            Export-EnvironmentVariables
        }
        "4" {
            New-MigrationChecklist
        }
        "5" {
            Test-Connections
        }
        "6" {
            Write-Host "Running all checks..." -ForegroundColor Cyan
            Write-Host ""
            Test-SystemRequirements
            Test-EnvironmentVariables
            Test-Connections
            Write-Host "All checks completed!" -ForegroundColor Green
        }
        "0" {
            Write-Host "Goodbye!" -ForegroundColor Green
            exit
        }
        default {
            Write-Host "Invalid choice. Please try again." -ForegroundColor Red
        }
    }
}

