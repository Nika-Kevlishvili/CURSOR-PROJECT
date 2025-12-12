# Cursor Configuration Setup Script
# კურსორის კონფიგურაციის დაყენების სკრიპტი
# ახალ კომპიუტერზე გადატანისთვის
# Usage: .\setup-cursor-config.ps1

param(
    [switch]$SkipPrompts = $false
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Cursor Configuration Setup" -ForegroundColor Cyan
Write-Host "კურსორის კონფიგურაციის დაყენება" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Get script directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDir = $scriptDir

# Check if Cursor is installed
$cursorPath = "$env:APPDATA\Cursor"
if (-not (Test-Path $cursorPath)) {
    Write-Host "❌ Cursor not found at: $cursorPath" -ForegroundColor Red
    Write-Host "Please install Cursor first from: https://cursor.sh" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Cursor found at: $cursorPath" -ForegroundColor Green
Write-Host ""

# Paths
$mcpConfigSource = Join-Path $projectDir ".cursor\mcp-config.json"
$extensionsSource = Join-Path $projectDir ".cursor\extensions.json"
$mcpConfigTarget = Join-Path $cursorPath "mcp.json"
$extensionsTargetDir = Join-Path $cursorPath "extensions"
$extensionsTarget = Join-Path $extensionsTargetDir "extensions.json"

# Check if source files exist
if (-not (Test-Path $mcpConfigSource)) {
    Write-Host "❌ MCP configuration not found at: $mcpConfigSource" -ForegroundColor Red
    Write-Host "Please ensure .cursor\mcp-config.json exists in the project" -ForegroundColor Yellow
    exit 1
}

if (-not (Test-Path $extensionsSource)) {
    Write-Host "⚠️  Extensions configuration not found at: $extensionsSource" -ForegroundColor Yellow
    Write-Host "Skipping extensions setup..." -ForegroundColor Gray
}

# Backup existing configuration if it exists
if (Test-Path $mcpConfigTarget) {
    Write-Host "⚠️  Existing MCP configuration found. Creating backup..." -ForegroundColor Yellow
    $backupPath = "$mcpConfigTarget.backup.$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    Copy-Item $mcpConfigTarget $backupPath -Force
    Write-Host "   Backup saved to: $backupPath" -ForegroundColor Gray
}

# Load source MCP configuration
Write-Host ""
Write-Host "Loading MCP configuration..." -ForegroundColor Yellow
$mcpConfig = Get-Content $mcpConfigSource -Raw | ConvertFrom-Json

# Prompt for sensitive values if not skipping prompts
if (-not $SkipPrompts) {
    Write-Host ""
    Write-Host "MCP Configuration Setup:" -ForegroundColor Cyan
    Write-Host "Please provide sensitive values (or press Enter to keep placeholders):" -ForegroundColor White
    Write-Host ""
    
    # GitLab Token
    if ($mcpConfig.mcpServers.GitLab -and $mcpConfig.mcpServers.GitLab.env.GITLAB_TOKEN -eq "YOUR_GITLAB_TOKEN_HERE") {
        $gitlabToken = Read-Host "GitLab Token (or press Enter to skip)"
        if (-not [string]::IsNullOrWhiteSpace($gitlabToken)) {
            $mcpConfig.mcpServers.GitLab.env.GITLAB_TOKEN = $gitlabToken
        }
    }
    
    # PostgreSQL Test Password
    if ($mcpConfig.mcpServers.PostgreSQLTest -and $mcpConfig.mcpServers.PostgreSQLTest.env.POSTGRES_PASSWORD -eq "PASSWORD") {
        $pgTestPwd = Read-Host "PostgreSQL Test Password (or press Enter to skip)"
        if (-not [string]::IsNullOrWhiteSpace($pgTestPwd)) {
            $mcpConfig.mcpServers.PostgreSQLTest.env.POSTGRES_PASSWORD = $pgTestPwd
            $mcpConfig.mcpServers.PostgreSQLTest.env.POSTGRES_CONNECTION_STRING = $mcpConfig.mcpServers.PostgreSQLTest.env.POSTGRES_CONNECTION_STRING -replace "PASSWORD", [System.Web.HttpUtility]::UrlEncode($pgTestPwd)
        }
    }
    
    # PostgreSQL Dev Password
    if ($mcpConfig.mcpServers.PostgreSQLDev -and $mcpConfig.mcpServers.PostgreSQLDev.env.POSTGRES_PASSWORD -eq "PASSWORD") {
        $pgDevPwd = Read-Host "PostgreSQL Dev Password (or press Enter to skip)"
        if (-not [string]::IsNullOrWhiteSpace($pgDevPwd)) {
            $mcpConfig.mcpServers.PostgreSQLDev.env.POSTGRES_PASSWORD = $pgDevPwd
            $mcpConfig.mcpServers.PostgreSQLDev.env.POSTGRES_CONNECTION_STRING = $mcpConfig.mcpServers.PostgreSQLDev.env.POSTGRES_CONNECTION_STRING -replace "PASSWORD", [System.Web.HttpUtility]::UrlEncode($pgDevPwd)
        }
    }
}

# Save MCP configuration
Write-Host ""
Write-Host "Saving MCP configuration..." -ForegroundColor Yellow
$mcpConfig | ConvertTo-Json -Depth 10 | Set-Content -Path $mcpConfigTarget -Encoding UTF8
Write-Host "✅ MCP configuration saved to: $mcpConfigTarget" -ForegroundColor Green

# Handle extensions
if (Test-Path $extensionsSource) {
    Write-Host ""
    Write-Host "📦 Extensions Configuration:" -ForegroundColor Cyan
    $extensionsConfig = Get-Content $extensionsSource -Raw | ConvertFrom-Json
    
    Write-Host "Recommended Extensions:" -ForegroundColor Yellow
    foreach ($ext in $extensionsConfig.recommendations) {
        Write-Host "   - $ext" -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "To install extensions:" -ForegroundColor Yellow
    Write-Host "1. Open Cursor" -ForegroundColor White
    Write-Host "2. Press Ctrl+Shift+X to open Extensions" -ForegroundColor White
    Write-Host "3. Search and install each extension from the list above" -ForegroundColor White
    Write-Host ""
    Write-Host "Or use Cursor's command palette:" -ForegroundColor Yellow
    Write-Host "   Ctrl+Shift+P -> 'Extensions: Install Extensions' -> Search by ID" -ForegroundColor White
    
    # Save extensions.json to Cursor extensions directory
    if (-not (Test-Path $extensionsTargetDir)) {
        New-Item -ItemType Directory -Path $extensionsTargetDir -Force | Out-Null
    }
    
    # Create a simple recommendations file for Cursor
    $recommendationsFile = Join-Path $extensionsTargetDir "recommendations.json"
    $recommendationsJson = @{
        recommendations = $extensionsConfig.recommendations
    } | ConvertTo-Json
    
    $recommendationsJson | Set-Content -Path $recommendationsFile -Encoding UTF8
    Write-Host "✅ Extensions recommendations saved to: $recommendationsFile" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Setup Complete! ✅" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Review MCP configuration at: $mcpConfigTarget" -ForegroundColor White
Write-Host "2. Update any remaining placeholder values (PASSWORD, YOUR_GITLAB_TOKEN_HERE)" -ForegroundColor White
Write-Host "3. Install recommended extensions in Cursor" -ForegroundColor White
Write-Host "4. Restart Cursor to apply changes" -ForegroundColor White
Write-Host ""
Write-Host "Note: Sensitive values (passwords, tokens) are stored in plain text." -ForegroundColor Yellow
Write-Host "      Make sure to secure your Cursor configuration directory." -ForegroundColor Yellow
Write-Host ""

