# MCP Servers Setup Script
# ეს სკრიპტი დაგეხმარებათ MCP servers-ის დაყენებაში Cursor-ში
# This script will help you set up MCP servers in Cursor

param(
    [switch]$UseEnvironmentVariables
)

Write-Host "🚀 MCP Servers Setup" -ForegroundColor Cyan
Write-Host "====================" -ForegroundColor Cyan
Write-Host ""

# Find template file
$templatePath = "config\mcp\mcp_servers_template.json"
if (-not (Test-Path $templatePath)) {
    Write-Host "❌ Template file not found: $templatePath" -ForegroundColor Red
    Write-Host "Please make sure you're running this script from the project root." -ForegroundColor Yellow
    exit 1
}

# Find Cursor settings.json
$settingsPath = "$env:APPDATA\Cursor\User\settings.json"

if (-not (Test-Path $settingsPath)) {
    Write-Host "❌ Cursor settings.json not found at: $settingsPath" -ForegroundColor Red
    Write-Host "Please check if Cursor is installed." -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Found template: $templatePath" -ForegroundColor Green
Write-Host "✅ Found Cursor settings: $settingsPath" -ForegroundColor Green
Write-Host ""

# Backup current settings
$backupPath = "$settingsPath.backup.$(Get-Date -Format 'yyyyMMdd_HHmmss')"
Write-Host "💾 Creating backup: $backupPath" -ForegroundColor Cyan
Copy-Item $settingsPath $backupPath
Write-Host "✅ Backup created" -ForegroundColor Green
Write-Host ""

try {
    # Read template
    Write-Host "📖 Reading MCP servers template..." -ForegroundColor Cyan
    $templateContent = Get-Content $templatePath -Raw -ErrorAction Stop
    $mcpConfig = $templateContent | ConvertFrom-Json -ErrorAction Stop
    
    # Read current settings
    Write-Host "📖 Reading current Cursor settings..." -ForegroundColor Cyan
    $settingsContent = Get-Content $settingsPath -Raw -ErrorAction Stop
    $settings = $settingsContent | ConvertFrom-Json -ErrorAction Stop
    
    # Get passwords for PostgreSQL servers
    $passwords = @{}
    
    if ($UseEnvironmentVariables) {
        Write-Host "🔐 Using environment variables for passwords..." -ForegroundColor Cyan
        $passwords['PostgreSQLDev'] = $env:POSTGRES_DEV_PASSWORD
        $passwords['PostgreSQLDev2'] = $env:POSTGRES_DEV2_PASSWORD
        $passwords['PostgreSQLTest'] = $env:POSTGRES_TEST_PASSWORD
        $passwords['PostgreSQLPreProd'] = $env:POSTGRES_PREPROD_PASSWORD
    } else {
        Write-Host "🔐 Enter passwords for PostgreSQL databases:" -ForegroundColor Yellow
        Write-Host ""
        
        $passwords['PostgreSQLDev'] = Read-Host "PostgreSQLDev password (10.236.20.21)" -AsSecureString
        $passwords['PostgreSQLDev2'] = Read-Host "PostgreSQLDev2 password (10.236.20.22)" -AsSecureString
        $passwords['PostgreSQLTest'] = Read-Host "PostgreSQLTest password (10.236.20.24)" -AsSecureString
        $passwords['PostgreSQLPreProd'] = Read-Host "PostgreSQLPreProd password (10.236.20.76)" -AsSecureString
        
        # Convert SecureString to plain text
        $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($passwords['PostgreSQLDev'])
        $passwords['PostgreSQLDev'] = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
        
        $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($passwords['PostgreSQLDev2'])
        $passwords['PostgreSQLDev2'] = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
        
        $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($passwords['PostgreSQLTest'])
        $passwords['PostgreSQLTest'] = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
        
        $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($passwords['PostgreSQLPreProd'])
        $passwords['PostgreSQLPreProd'] = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
    }
    
    Write-Host ""
    Write-Host "🔧 Updating MCP configuration with passwords..." -ForegroundColor Cyan
    
    # Update passwords in MCP config
    foreach ($serverName in @('PostgreSQLDev', 'PostgreSQLDev2', 'PostgreSQLTest', 'PostgreSQLPreProd')) {
        if ($mcpConfig.mcpServers.$serverName) {
            $password = $passwords[$serverName]
            if ($password) {
                # Update connection string
                $host = $mcpConfig.mcpServers.$serverName.env.POSTGRES_HOST
                $port = $mcpConfig.mcpServers.$serverName.env.POSTGRES_PORT
                $db = $mcpConfig.mcpServers.$serverName.env.POSTGRES_DB
                $user = $mcpConfig.mcpServers.$serverName.env.POSTGRES_USER
                
                $mcpConfig.mcpServers.$serverName.env.POSTGRES_CONNECTION_STRING = "postgres://$user`:$password@$host`:$port/$db"
                $mcpConfig.mcpServers.$serverName.env.POSTGRES_PASSWORD = $password
                
                Write-Host "   ✅ Updated $serverName" -ForegroundColor Green
            }
        }
    }
    
    # Merge MCP configuration into settings
    Write-Host ""
    Write-Host "🔧 Merging MCP servers into Cursor settings..." -ForegroundColor Cyan
    
    if (-not $settings.mcpServers) {
        $settings | Add-Member -MemberType NoteProperty -Name "mcpServers" -Value @{}
    }
    
    # Merge each server
    foreach ($serverName in $mcpConfig.mcpServers.PSObject.Properties.Name) {
        $serverConfig = $mcpConfig.mcpServers.$serverName
        
        # Remove description if it exists (not needed in settings)
        $serverConfigClean = $serverConfig.PSObject.Copy()
        if ($serverConfigClean.PSObject.Properties.Name -contains "description") {
            $serverConfigClean.PSObject.Properties.Remove("description")
        }
        
        $settings.mcpServers.$serverName = $serverConfigClean
        Write-Host "   ✅ Added $serverName" -ForegroundColor Green
    }
    
    # Write updated settings
    Write-Host ""
    Write-Host "💾 Writing updated settings..." -ForegroundColor Cyan
    $settings | ConvertTo-Json -Depth 10 | Set-Content $settingsPath -Encoding UTF8
    
    Write-Host ""
    Write-Host "✅ MCP servers configured successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📋 Configured servers:" -ForegroundColor Cyan
    Write-Host "   🔵 Jira (URL-based)" -ForegroundColor Blue
    Write-Host "   🔵 Confluence (URL-based)" -ForegroundColor Blue
    Write-Host "   🗄️  PostgreSQLDev (10.236.20.21)" -ForegroundColor Cyan
    Write-Host "   🗄️  PostgreSQLDev2 (10.236.20.22)" -ForegroundColor Cyan
    Write-Host "   🗄️  PostgreSQLTest (10.236.20.24)" -ForegroundColor Cyan
    Write-Host "   🗄️  PostgreSQLPreProd (10.236.20.76)" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "⚠️  Please restart Cursor for changes to take effect." -ForegroundColor Yellow
    Write-Host ""
    
} catch {
    Write-Host "❌ Error setting up MCP servers: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "🔄 Restoring backup..." -ForegroundColor Yellow
    Copy-Item $backupPath $settingsPath -Force
    Write-Host "✅ Settings restored from backup" -ForegroundColor Green
    exit 1
}

Write-Host "✨ Setup complete!" -ForegroundColor Green

