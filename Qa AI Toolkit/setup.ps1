#Requires -Version 5.1
<#
.SYNOPSIS
  Qa AI Toolkit — deploy universal Cursor QA template + configure any project.

.DESCRIPTION
  Run from the "Qa AI Toolkit" folder. Copies template/.cursor and template/project
  into your target workspace, then runs the interactive credential wizard.

  Usage:
    powershell -ExecutionPolicy Bypass -File "Qa AI Toolkit\setup.ps1"
    powershell -ExecutionPolicy Bypass -File "Qa AI Toolkit\setup.ps1" -Help
    powershell -ExecutionPolicy Bypass -File "Qa AI Toolkit\setup.ps1" -TargetPath C:\work\my-app -SkipWizard
#>
[CmdletBinding()]
param(
    [switch]$Help,
    [string]$TargetPath = '',
    [string]$ProjectFolder = '',
    [switch]$SkipWizard,
    [switch]$VerifyOnly
)

$ErrorActionPreference = 'Stop'
$ToolkitRoot = $PSScriptRoot
$TemplateRoot = Join-Path $ToolkitRoot 'template'

function Show-SetupGuide {
@'

================================================================================
  QA AI TOOLKIT - SETUP GUIDE
================================================================================

WHAT THIS IS
------------
  A portable folder you copy or keep in your monorepo. It contains:
    template/.cursor/     agents, rules, skills, commands, hooks
    template/project/   config, scripts, test_cases & reports skeletons

  NOT tied to one product (no Phoenix/EnergoTS/HandsOff in template).

QUICK START
-----------
  1. Open PowerShell in the parent of your new project (or inside it)
  2. Run:  powershell -ExecutionPolicy Bypass -File "path\to\Qa AI Toolkit\setup.ps1"
  3. Enter target workspace path + project folder name
  4. Complete the credential wizard
  5. Open target folder in Cursor; paste mcp.generated.json into MCP settings

CREDENTIALS (wizard writes .env)
--------------------------------
  JIRA_EMAIL, JIRA_API_TOKEN, JIRA_BASE_URL
  CONFLUENCE_WIKI_BASE, CONFLUENCE_SPACE
  Swagger host:port per environment
  PostgreSQL MCP (optional per env)
  GIT_READONLY_TOKEN (optional submodules)
  SLACK_BOT_TOKEN (optional)

ACTIVE WORKFLOWS
----------------
  Bug validation | Test cases | Cross-dependencies | Regression validator
  Product expert Q&A | Postman | Production data reader | Senior QA

================================================================================
'@ | Write-Host
}

function Read-Line([string]$Prompt, [string]$Default = '') {
    if ($Default) {
        $r = Read-Host "$Prompt [$Default]"
        if ([string]::IsNullOrWhiteSpace($r)) { return $Default }
        return $r.Trim()
    }
    do { $r = Read-Host $Prompt } while ([string]::IsNullOrWhiteSpace($r))
    return $r.Trim()
}

function Read-SecureLine([string]$Prompt) {
    $sec = Read-Host $Prompt -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

function Read-YesNo([string]$Prompt, [bool]$DefaultYes = $true) {
    $suffix = if ($DefaultYes) { 'Y/n' } else { 'y/N' }
    $r = Read-Host "$Prompt ($suffix)"
    if ([string]::IsNullOrWhiteSpace($r)) { return $DefaultYes }
    return $r -match '^[Yy]'
}

function Copy-ToolkitTemplate {
    param([string]$Target, [string]$ProjFolder)
    if (-not (Test-Path $TemplateRoot)) { throw "Missing template folder: $TemplateRoot" }
    $cursorSrc = Join-Path $TemplateRoot '.cursor'
    $projectSrc = Join-Path $TemplateRoot 'project'
    $cursorDst = Join-Path $Target '.cursor'
    $projectDst = Join-Path $Target $ProjFolder

    if (Test-Path $cursorDst) {
        if (-not (Read-YesNo ".cursor already exists in target. Overwrite?" $false)) {
            throw 'Aborted: .cursor exists'
        }
        Remove-Item -LiteralPath $cursorDst -Recurse -Force
    }
    if (Test-Path $projectDst) {
        if (-not (Read-YesNo "$ProjFolder already exists. Merge/overwrite?" $false)) {
            throw "Aborted: $ProjFolder exists"
        }
    }

    Write-Host "Copying .cursor -> $cursorDst" -ForegroundColor Cyan
    Copy-Item -LiteralPath $cursorSrc -Destination $cursorDst -Recurse -Force
    Write-Host "Copying project skeleton -> $projectDst" -ForegroundColor Cyan
    Copy-Item -LiteralPath $projectSrc -Destination $projectDst -Recurse -Force

    @{
        toolkitVersion = '1.0'
        installedAt    = (Get-Date).ToString('o')
        projectFolder  = $ProjFolder
        targetPath     = $Target
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $Target '.qa-toolkit.json') -Encoding UTF8

    Invoke-PlaceholderReplace -Root $Target -ProjFolder $ProjFolder
}

function Invoke-PlaceholderReplace {
    param([string]$Root, [string]$ProjFolder)
    $patterns = @(
        @{ Find = 'Cursor-Project'; Replace = $ProjFolder }
        @{ Find = 'PhoenixExpert'; Replace = 'ProductExpert' }
        @{ Find = 'phoenix-qa'; Replace = 'product-expert' }
    )
    $exts = @('*.md', '*.mdc', '*.ps1', '*.json', '*.py', '*')
    $files = Get-ChildItem -LiteralPath (Join-Path $Root '.cursor') -Recurse -File -ErrorAction SilentlyContinue
    $files += Get-ChildItem -LiteralPath (Join-Path $Root $ProjFolder) -Recurse -File -ErrorAction SilentlyContinue
    foreach ($f in $files) {
        if ($f.Extension -notin @('.md', '.mdc', '.ps1', '.json', '.py', '')) { continue }
        $content = Get-Content -LiteralPath $f.FullName -Raw -ErrorAction SilentlyContinue
        if (-not $content) { continue }
        $new = $content
        foreach ($p in $patterns) { $new = $new.Replace($p.Find, $p.Replace) }
        if ($new -ne $content) {
            Set-Content -LiteralPath $f.FullName -Value $new -Encoding UTF8 -NoNewline
        }
    }
}

function Invoke-ConfigWizard {
    param([string]$Target, [string]$ProjFolder)

    $EnvPath = Join-Path $Target '.env'
    $SwaggerPath = Join-Path $Target "$ProjFolder\config\swagger\environments.json"
    $McpOut = Join-Path $Target "$ProjFolder\Cursor Setup\mcp.generated.json"
    $GitModulesPath = Join-Path $Target '.gitmodules'

    Write-Host "`n=== Project metadata ===" -ForegroundColor Cyan
    $projectName = Read-Line 'Project display name' 'My QA Project'
    $ticketPrefix = Read-Line 'Jira ticket prefix (e.g. PROJ)' 'PROJ'
    $jiraProjectKey = Read-Line 'Default Jira project key' $ticketPrefix
    $confluenceSpace = Read-Line 'Confluence space key' 'DOCS'
    $appCodeGlob = Read-Line 'Protected app code path (Tier A, no AI edits)' "$ProjFolder/App"

    Write-Host "`n=== Atlassian ===" -ForegroundColor Cyan
    Write-Host 'API token: https://id.atlassian.com/manage-profile/security/api-tokens'
    $jiraBase = (Read-Line 'Jira base URL' 'https://your-org.atlassian.net').TrimEnd('/')
    $wikiBase = (Read-Line 'Confluence wiki base' "$jiraBase/wiki").TrimEnd('/')
    $jiraEmail = Read-Line 'Atlassian email'
    $jiraToken = Read-SecureLine 'Atlassian API token (hidden)'

    Write-Host "`n=== Environments ===" -ForegroundColor Cyan
    do { $envCount = [int](Read-Line 'How many backend environments? (1-6)' '2') } while ($envCount -lt 1 -or $envCount -gt 6)
    $environments = @()
    for ($i = 1; $i -le $envCount; $i++) {
        $id = Read-Line "  Environment $i id" 'dev'
        $environments += [ordered]@{ id = $id }
    }

    Write-Host "`n=== Swagger per environment ===" -ForegroundColor Cyan
    $swaggerEntries = @()
    foreach ($env in $environments) {
        Write-Host "Swagger for '$($env.id)'"
        $apiHost = Read-Line '  API host' 'localhost'
        $port = Read-Line '  API port' '8080'
        $swaggerEntries += [ordered]@{
            id           = $env.id
            swagger_ui   = "http://${apiHost}:${port}/swagger-ui/index.html#"
            openapi_json = "http://${apiHost}:${port}/v3/api-docs"
        }
    }

    Write-Host "`n=== PostgreSQL MCP (optional) ===" -ForegroundColor Cyan
    $mcpServers = @{
        Confluence = @{ type = 'http'; url = 'https://mcp.atlassian.com/v1/sse' }
        Jira       = @{ type = 'http'; url = 'https://mcp.atlassian.com/v1/sse' }
    }
    if (Read-YesNo 'Add PostgreSQL MCP servers?' $false) {
        foreach ($env in $environments) {
            if (-not (Read-YesNo "  PostgreSQL for '$($env.id)'?" $false)) { continue }
            $dbHost = Read-Line '    host' 'localhost'
            $dbPort = Read-Line '    port' '5432'
            $dbName = Read-Line '    database' 'app'
            $dbUser = Read-Line '    user' 'readonly'
            $dbPass = Read-SecureLine '    password (hidden)'
            $mcpName = "PostgreSQL$((Get-Culture).TextInfo.ToTitleCase($env.id))"
            $mcpServers[$mcpName] = @{
                command = 'npx'
                args    = @('-y', 'mcp-postgres-server')
                env     = @{
                    MCP_SERVER_NAME   = $mcpName
                    POSTGRES_HOST     = $dbHost
                    POSTGRES_PORT     = $dbPort
                    POSTGRES_DB       = $dbName
                    POSTGRES_USER     = $dbUser
                    POSTGRES_PASSWORD = $dbPass
                }
            }
        }
    }

    $gitHost = ''
    $gitToken = ''
    if (Read-YesNo "`nConfigure GitLab host for optional submodules?" $false) {
        $gitHost = Read-Line 'Git host URL' 'https://git.example.com'
        if (Read-YesNo 'Set GIT_READONLY_TOKEN?' $false) { $gitToken = Read-SecureLine 'Token (hidden)' }
    }

    $slackToken = ''
    if (Read-YesNo 'Configure SLACK_BOT_TOKEN?' $false) {
        $slackToken = Read-SecureLine 'SLACK_BOT_TOKEN (hidden)'
    }

    $envLines = @(
        "# Qa AI Toolkit $(Get-Date -Format 'yyyy-MM-dd HH:mm')",
        "QA_PROJECT_NAME=$projectName",
        "QA_TICKET_PREFIX=$ticketPrefix",
        "QA_APP_CODE_GLOB=$appCodeGlob",
        "CONFLUENCE_SPACE=$confluenceSpace",
        "JIRA_EMAIL=$jiraEmail",
        "JIRA_API_TOKEN=$jiraToken",
        "JIRA_BASE_URL=$jiraBase",
        "JIRA_PROJECT_KEY=$jiraProjectKey",
        "CONFLUENCE_URL=$wikiBase/home",
        "CONFLUENCE_WIKI_BASE=$wikiBase"
    )
    if ($gitHost) { $envLines += "GITLAB_URL=$gitHost" }
    if ($gitToken) { $envLines += "GIT_READONLY_TOKEN=$gitToken" }
    if ($slackToken) { $envLines += "SLACK_BOT_TOKEN=$slackToken" }
    Set-Content -LiteralPath $EnvPath -Value ($envLines -join "`n") -Encoding UTF8

    $swDir = Split-Path $SwaggerPath -Parent
    if (-not (Test-Path $swDir)) { New-Item -ItemType Directory -Path $swDir -Force | Out-Null }
    @{ description = 'Generated by Qa AI Toolkit setup.ps1'; environments = $swaggerEntries } |
        ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $SwaggerPath -Encoding UTF8

    $mcpDir = Split-Path $McpOut -Parent
    if (-not (Test-Path $mcpDir)) { New-Item -ItemType Directory -Path $mcpDir -Force | Out-Null }
    @{ mcpServers = $mcpServers } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $McpOut -Encoding UTF8

    $gitignore = @(
        '.env', '.gitmodules', '.qa-toolkit.json',
        "$ProjFolder/Cursor Setup/mcp.generated.json",
        "$ProjFolder/config/swagger/*/swagger-spec.json"
    )
    Set-Content -LiteralPath (Join-Path $Target '.gitignore') -Value ($gitignore -join "`n") -Encoding UTF8

    Push-Location $Target
    try {
        if (Read-YesNo 'Set git hooksPath?' $true) {
            git config core.hooksPath "$ProjFolder/scripts/git-hooks"
        }
    } finally { Pop-Location }

    Write-Host "`nConfig written to $Target" -ForegroundColor Green
    Write-Host "Paste MCP: $McpOut"
}

function Test-ToolkitInstall {
    param([string]$Target, [string]$ProjFolder)
    $ok = $true
    if (-not (Test-Path (Join-Path $Target '.cursor\rules\main\core_rules.mdc'))) {
        Write-Host 'FAIL: .cursor not deployed' -ForegroundColor Red; $ok = $false
    }
    if (-not (Test-Path (Join-Path $Target '.env'))) {
        Write-Host 'WARN: .env missing (run wizard)' -ForegroundColor Yellow
    }
    $val = Join-Path $Target "$ProjFolder\scripts\validate-cursor-rules.ps1"
    if (Test-Path $val) {
        Push-Location $Target
        try { & $val } catch { }
        if ($LASTEXITCODE -ne 0) { Write-Host 'WARN: validate-cursor-rules failed' -ForegroundColor Yellow }
        Pop-Location
    }
    if ($ok) { Write-Host 'Toolkit install check: OK' -ForegroundColor Green }
    return $ok
}

# --- MAIN ---
if ($Help) { Show-SetupGuide; exit 0 }

Write-Host "`n  QA AI TOOLKIT - Universal Cursor QA Template`n" -ForegroundColor Green

if ($VerifyOnly) {
    $manifest = Join-Path (Get-Location) '.qa-toolkit.json'
    if (-not (Test-Path $manifest)) { throw 'No .qa-toolkit.json in current directory' }
    $m = Get-Content $manifest | ConvertFrom-Json
    if (-not (Test-ToolkitInstall $m.targetPath $m.projectFolder)) { exit 1 }
    exit 0
}

if (-not $TargetPath) {
    $TargetPath = Read-Line 'Target workspace root (folder to install into)' (Get-Location).Path
}
$TargetPath = (Resolve-Path $TargetPath).Path
if (-not $ProjectFolder) {
    $ProjectFolder = Read-Line 'Project data folder name under target' 'Project'
}

Write-Host "`nDeploying template to: $TargetPath" -ForegroundColor Cyan
Copy-ToolkitTemplate -Target $TargetPath -ProjFolder $ProjectFolder

if (-not $SkipWizard) {
    Invoke-ConfigWizard -Target $TargetPath -ProjFolder $ProjectFolder
}

Push-Location $TargetPath
try { Test-ToolkitInstall -Target $TargetPath -ProjFolder $ProjectFolder | Out-Null } finally { Pop-Location }

Write-Host @"

Done.
  Open in Cursor: $TargetPath
  Help anytime:   powershell -File "$ToolkitRoot\setup.ps1" -Help

"@ -ForegroundColor Green
