#Requires -Version 5.1
<#
.SYNOPSIS
  Universal Phoenix QA Toolkit — single-file bootstrap for any project.

.DESCRIPTION
  The qa-cursor-toolkit branch contains ONLY this file.
  Running it installs the QA framework from the source branch (default: Do-not-touch),
  removes EnergoTS / Playwright / HandsOff, prompts for your project credentials,
  and verifies the installation.

  Usage:
    powershell -ExecutionPolicy Bypass -File setup.ps1
    powershell -ExecutionPolicy Bypass -File setup.ps1 -Help
    powershell -ExecutionPolicy Bypass -File setup.ps1 -SourceBranch main -SkipWizard

.PARAMETER Help
  Print full setup guide (credentials, workflows, troubleshooting).

.PARAMETER SourceBranch
  Git branch that holds the full framework template (default: Do-not-touch).

.PARAMETER SkipInstall
  Skip framework install; only run configuration wizard.

.PARAMETER SkipWizard
  Skip interactive wizard after install.

.PARAMETER VerifyOnly
  Run verification checks only.
#>
[CmdletBinding()]
param(
    [switch]$Help,
    [string]$SourceBranch = 'Do-not-touch',
    [switch]$SkipInstall,
    [switch]$SkipWizard,
    [switch]$VerifyOnly
)

$ErrorActionPreference = 'Stop'
$RepoRoot = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }

# =============================================================================
# SETUP GUIDE (embedded — no separate docs on toolkit branch)
# =============================================================================
function Show-SetupGuide {
    @'

================================================================================
  UNIVERSAL PHOENIX QA TOOLKIT - SETUP GUIDE
================================================================================

QUICK START
-----------
  1. Clone this repo and checkout branch: qa-cursor-toolkit  (only setup.ps1)
  2. Run:  powershell -ExecutionPolicy Bypass -File setup.ps1
  3. Open the project in Cursor; paste MCP config from generated file
  4. Enable Atlassian MCP in Cursor Settings (Jira + Confluence)

WHAT THIS INSTALLS (from source branch, minus EnergoTS/HandsOff)
----------------------------------------------------------------
  .cursor/          rules, agents, skills, commands, hooks
  Cursor-Project/   scripts, docs, config templates, test_cases & reports skeletons

NOT INCLUDED: EnergoTS, Playwright, HandsOff, cached Jira/Confluence/Diagrams/Swagger

CREDENTIALS
-----------
  Field                  | Where to get                          | Used by
  -----------------------|---------------------------------------|---------------------------
  JIRA_EMAIL             | Your Atlassian account                | Jira MCP + REST fallback
  JIRA_API_TOKEN         | id.atlassian.com -> API tokens        | REST scripts
  JIRA_BASE_URL          | https://<org>.atlassian.net           | Jira
  CONFLUENCE_WIKI_BASE   | Same host + /wiki                     | Confluence MCP + REST
  Swagger host:port      | DevOps / team Swagger UI URL          | update-swagger-specs.ps1
  PostgreSQL per env     | DBA connection sheet                  | PostgreSQL MCP servers
  GIT_READONLY_TOKEN     | GitLab -> Access Tokens               | git submodule init
  SLACK_BOT_TOKEN        | api.slack.com -> OAuth (optional)     | bug-validation Slack upload

ACTIVE WORKFLOWS AFTER SETUP
----------------------------
  Bug validation (Rule 32)     | Jira bug + validation request
  Test cases (Rule 35)         | Generate test cases
  Cross-dependency finder      | Before test cases / impact analysis
  Regression validator         | Dev -> Dev2 deployment check
  PhoenixExpert Q&A            | Phoenix behavior questions
  Postman collections          | On request
  Production data reader       | Prod DB (read-only, gated)

FILES CREATED LOCALLY (gitignored)
----------------------------------
  .env
  .gitmodules
  Cursor-Project/config/swagger/environments.json
  Cursor-Project/Cursor Setup/mcp.generated.json

TROUBLESHOOTING
---------------
  Jira MCP 401        -> Re-auth Atlassian MCP; check API token in .env
  Swagger refresh fail -> VPN; verify host:port in environments.json
  Submodule clone fail -> GIT_READONLY_TOKEN; git host in .gitmodules
  Agent asks environment -> Answer: dev, dev2, test, preprod, prod, experiments

SECURITY: Never commit .env or mcp.generated.json. Rotate tokens if leaked.

================================================================================
'@ | Write-Host
}

# =============================================================================
# FRAMEWORK INSTALL
# =============================================================================
function Resolve-SourceRef {
    param([string]$Branch)
    Push-Location $RepoRoot
    try {
        git rev-parse --is-inside-work-tree 2>$null | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Not a git repository. Clone the repo first.' }
        $prevEap = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try { git fetch origin $Branch 2>&1 | Out-Null } finally { $ErrorActionPreference = $prevEap }
        foreach ($candidate in @("origin/$Branch", $Branch)) {
            git rev-parse --verify $candidate 2>$null | Out-Null
            if ($LASTEXITCODE -eq 0) { return $candidate }
        }
        throw "Source branch not found: $Branch (need Do-not-touch or main locally/remotely)"
    } finally { Pop-Location }
}

function Get-FrameworkInstallPaths {
    @(
        '.cursor',
        '.github',
        'Cursor-Project/scripts',
        'Cursor-Project/docs',
        'Cursor-Project/config/template',
        'Cursor-Project/config/confluence/confluence-cql-search.ps1',
        'Cursor-Project/config/jira/get-jira-issue-rest.ps1',
        'Cursor-Project/config/jira/download-jira-attachments.ps1',
        'Cursor-Project/config/jira/fetch-issue-full-rest.ps1',
        'Cursor-Project/config/jira/README.md',
        'Cursor-Project/config/slack',
        'Cursor-Project/Cursor Setup',
        'Cursor-Project/test_cases',
        'Cursor-Project/reports',
        'Cursor-Project/postman'
    )
}

function Get-ToolkitExcludePatterns {
    @(
        '.cursor/agents/energo-ts-run.md',
        '.cursor/agents/energo-ts-test.md',
        '.cursor/agents/hands-off.md',
        '.cursor/agents/playwright-test-validator.md',
        '.cursor/skills/energo-ts-run',
        '.cursor/skills/energo-ts-test',
        '.cursor/skills/hands-off-playwright-report',
        '.cursor/skills/playwright-test-validator',
        '.cursor/commands/hands-off.md',
        '.cursor/commands/playwright-test-with-detailed-report.ps1',
        '.cursor/commands/playwright-validate.md',
        '.cursor/commands/send-playwright-results-slack.md',
        '.cursor/commands/sync-cursor-with-staging.md',
        '.cursor/commands/sync-cursor-with-staging.ps1',
        '.cursor/rules/integrations/energots_branch_lock.mdc',
        '.cursor/rules/workflows/handsoff_playwright_report.mdc',
        '.cursor/rules/workflows/playwright_detailed_reporting.mdc',
        '.cursor/hooks/block-energots-branch-requests.ps1',
        '.cursor/hooks/block-energots-branch-switch.ps1',
        '.cursor/hooks/protect-energots-writes.ps1',
        'Cursor-Project/config/playwright_generation',
        'Cursor-Project/config/playwright',
        'Cursor-Project/scripts/validate-manual-verification-links.ps1',
        'Cursor-Project/EnergoTS',
        'Cursor-Project/Phoenix',
        'Cursor-Project/config/Diagrams',
        'Cursor-Project/config/confluence/pages',
        'Cursor-Project/User story',
        'Cursor-Project/examples',
        'SETUP_GUIDE.md',
        'verify-setup.ps1',
        'TOOLKIT_SETUP.md'
    )
}

function Write-ToolkitHooksJson {
  $hooksPath = Join-Path $RepoRoot '.cursor\hooks.json'
  if (-not (Test-Path (Split-Path $hooksPath -Parent))) { return }
  @'
{
  "version": 1,
  "hooks": {
    "beforeSubmitPrompt": [
      { "command": "powershell -ExecutionPolicy Bypass -File .cursor/hooks/block-jira-phoenix-delivery.ps1" },
      { "command": "powershell -ExecutionPolicy Bypass -File .cursor/hooks/remind-test-case-env-first.ps1" }
    ],
    "afterFileEdit": [
      { "command": "powershell -ExecutionPolicy Bypass -File .cursor/hooks/protect-phoenix-code.ps1" },
      { "command": "powershell -ExecutionPolicy Bypass -File .cursor/hooks/warn-phoenix-code-edit.ps1" }
    ],
    "beforeMCPExecution": [
      { "command": "powershell -ExecutionPolicy Bypass -File .cursor/hooks/block-confluence-write.ps1" },
      { "command": "powershell -ExecutionPolicy Bypass -File .cursor/hooks/control-database-write.ps1" }
    ],
    "beforeShellExecution": [],
    "afterAgentResponse": [
      { "command": "powershell -ExecutionPolicy Bypass -File .cursor/hooks/validate-confidence-format.ps1" }
    ]
  }
}
'@ | Set-Content -LiteralPath $hooksPath -Encoding UTF8
}

function Apply-ToolkitPatches {
  Write-Host 'Applying toolkit patches...' -ForegroundColor Cyan
  Write-ToolkitHooksJson
  @(
    '.cursor\hooks\block-energots-branch-requests.ps1',
    '.cursor\hooks\block-energots-branch-switch.ps1',
    '.cursor\hooks\protect-energots-writes.ps1'
  ) | ForEach-Object {
    $p = Join-Path $RepoRoot $_
    if (Test-Path $p) { Remove-Item -LiteralPath $p -Force }
  }
}

function Remove-ToolkitExclusions {
  foreach ($rel in (Get-ToolkitExcludePatterns)) {
    $full = Join-Path $RepoRoot ($rel -replace '/', '\')
    if (Test-Path -LiteralPath $full) {
      Remove-Item -LiteralPath $full -Recurse -Force -ErrorAction SilentlyContinue
      Write-Host "  Removed excluded: $rel" -ForegroundColor DarkGray
    }
  }
  # Drop cached data globs
  @(
    'Cursor-Project/config/jira/*.json',
    'Cursor-Project/config/swagger/*/swagger-spec.json'
  ) | ForEach-Object {
    Get-ChildItem -Path (Join-Path $RepoRoot ($_ -replace '/', '\').Replace('\*', '')) -Filter (Split-Path $_ -Leaf) -Recurse -ErrorAction SilentlyContinue |
      Remove-Item -Force -ErrorAction SilentlyContinue
  }
  Get-ChildItem -Path (Join-Path $RepoRoot 'Cursor-Project\config\jira') -Filter '*.json' -File -ErrorAction SilentlyContinue |
    Remove-Item -Force
  Get-ChildItem -Path (Join-Path $RepoRoot 'Cursor-Project\config\swagger') -Filter 'swagger-spec.json' -Recurse -File -ErrorAction SilentlyContinue |
    Remove-Item -Force
  if (Test-Path (Join-Path $RepoRoot 'Cursor-Project\Cursor Setup\mcp_content.txt')) {
    Remove-Item -Force (Join-Path $RepoRoot 'Cursor-Project\Cursor Setup\mcp_content.txt')
  }
}

function Write-ProjectGitignore {
  $content = @'
# Generated by setup.ps1 — Phoenix QA Toolkit
.env
Cursor-Project/.env
.gitmodules
Cursor-Project/Cursor Setup/mcp.generated.json
.cursor/logs/
Cursor-Project/Phoenix/
Cursor-Project/EnergoTS/
Cursor-Project/config/swagger/*/swagger-spec.json
Cursor-Project/config/jira/*.json
Cursor-Project/config/confluence/pages/
Cursor-Project/config/Diagrams/
'@
  Set-Content -LiteralPath (Join-Path $RepoRoot '.gitignore') -Value $content.TrimEnd() -Encoding UTF8
  Write-Host 'Wrote .gitignore'
}

function Write-SanitizedEnvExample {
  $path = Join-Path $RepoRoot 'Cursor-Project\Cursor Setup\env.example'
  $dir = Split-Path $path -Parent
  if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
  $lines = @(
    '# Copy to repo root as .env (never commit .env)',
    'QA_PROJECT_NAME=your-project-name',
    'QA_TICKET_PREFIX=PROJ',
    'JIRA_EMAIL=your-atlassian-email',
    'JIRA_API_TOKEN=your-api-token',
    'JIRA_BASE_URL=https://your-org.atlassian.net',
    'JIRA_PROJECT_KEY=PROJ',
    'CONFLUENCE_WIKI_BASE=https://your-org.atlassian.net/wiki',
    'GITLAB_URL=https://git-host',
    'GIT_READONLY_TOKEN=gitlab-read-token',
    'SLACK_BOT_TOKEN=optional-xoxb-token'
  )
  Set-Content -LiteralPath $path -Value ($lines -join "`n") -Encoding UTF8
}

function Install-QaFramework {
  param([string]$Ref)
  Write-Host ''
  Write-Host '=== Installing QA framework from git: ' -NoNewline -ForegroundColor Cyan
  Write-Host $Ref -ForegroundColor Cyan
  Push-Location $RepoRoot
  $prevEap = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  try {
    foreach ($path in (Get-FrameworkInstallPaths)) {
      Write-Host "  Checking out: $path"
      git checkout $Ref -- $path 2>&1 | Out-Null
      if ($LASTEXITCODE -ne 0) {
        Write-Host '    (skipped - not on source branch)' -ForegroundColor DarkYellow
      }
    }
    Remove-ToolkitExclusions
    Apply-ToolkitPatches
    Write-ProjectGitignore
    Write-SanitizedEnvExample
    # Ensure swagger config dir exists
    $swDir = Join-Path $RepoRoot 'Cursor-Project\config\swagger'
    if (-not (Test-Path $swDir)) { New-Item -ItemType Directory -Path $swDir -Force | Out-Null }
    Write-Host 'Framework install complete.' -ForegroundColor Green
  } finally {
    $ErrorActionPreference = $prevEap
    Pop-Location
  }
}

# =============================================================================
# CONFIGURATION WIZARD
# =============================================================================
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

function Invoke-SetupWizard {
  $EnvPath = Join-Path $RepoRoot '.env'
  $SwaggerPath = Join-Path $RepoRoot 'Cursor-Project\config\swagger\environments.json'
  $McpOut = Join-Path $RepoRoot 'Cursor-Project\Cursor Setup\mcp.generated.json'
  $GitModulesPath = Join-Path $RepoRoot '.gitmodules'

  Clear-Host
  Write-Host 'QA Toolkit — Project configuration' -ForegroundColor Green
  Write-Host "Target: $RepoRoot`n"

  Write-Host '=== 1. Project metadata ===' -ForegroundColor Cyan
  $projectName = Read-Line 'Project display name' 'My Phoenix QA Project'
  $ticketPrefix = Read-Line 'Jira ticket prefix (e.g. PDT, PROJ)' 'PROJ'
  $jiraProjectKey = Read-Line 'Default Jira project key' $ticketPrefix

  Write-Host "`n=== 2. Atlassian ===" -ForegroundColor Cyan
  Write-Host 'API token: https://id.atlassian.com/manage-profile/security/api-tokens'
  Write-Host 'Cursor MCP: Settings -> MCP -> Add Atlassian (HTTP)'
  $jiraBase = (Read-Line 'Jira base URL' 'https://your-org.atlassian.net').TrimEnd('/')
  $wikiBase = (Read-Line 'Confluence wiki base' "$jiraBase/wiki").TrimEnd('/')
  $jiraEmail = Read-Line 'Atlassian email'
  $jiraToken = Read-SecureLine 'Atlassian API token (hidden)'

  Write-Host "`n=== 3. Environments ===" -ForegroundColor Cyan
  do {
    $envCount = [int](Read-Line 'How many backend environments? (1-6)' '3')
  } while ($envCount -lt 1 -or $envCount -gt 6)
  $environments = @()
  for ($i = 1; $i -le $envCount; $i++) {
    Write-Host "Environment $i"
    $id = Read-Line '  id (dev, test, prod, ...)' 'dev'
    $environments += [ordered]@{ id = $id; label = $id }
  }

  Write-Host "`n=== 4. Swagger per environment ===" -ForegroundColor Cyan
  $swaggerEntries = @()
  foreach ($env in $environments) {
    Write-Host "Swagger for '$($env.id)'"
    $apiHost = Read-Line '  API host' 'localhost'
    $port = Read-Line '  API port' '8091'
    $swaggerEntries += [ordered]@{
      id           = $env.id
      swagger_ui   = "http://${apiHost}:${port}/swagger-ui/index.html#"
      openapi_json = "http://${apiHost}:${port}/v3/api-docs"
    }
  }

  Write-Host "`n=== 5. PostgreSQL MCP (optional) ===" -ForegroundColor Cyan
  $mcpServers = @{
    Confluence = @{ type = 'http'; url = 'https://mcp.atlassian.com/v1/sse' }
    Jira       = @{ type = 'http'; url = 'https://mcp.atlassian.com/v1/sse' }
  }
  if (Read-YesNo 'Configure PostgreSQL MCP servers?' $true) {
    foreach ($env in $environments) {
      if (-not (Read-YesNo "  Add PostgreSQL for '$($env.id)'?" $true)) { continue }
      $dbHost = Read-Line '    DB host' 'localhost'
      $dbPort = Read-Line '    DB port' '5432'
      $dbName = Read-Line '    Database' 'phoenix'
      $dbUser = Read-Line '    DB user' 'postgres'
      $dbPass = Read-SecureLine '    DB password (hidden)'
      $mcpName = "PostgreSQL$((Get-Culture).TextInfo.ToTitleCase($env.id))"
      $conn = "postgres://${dbUser}:$([uri]::EscapeDataString($dbPass))@${dbHost}:${dbPort}/${dbName}"
      $mcpServers[$mcpName] = @{
        command = 'npx'
        args    = @('-y', 'mcp-postgres-server')
        env     = @{
          MCP_SERVER_NAME            = $mcpName
          POSTGRES_HOST              = $dbHost
          POSTGRES_PORT              = $dbPort
          POSTGRES_DB                = $dbName
          POSTGRES_USER              = $dbUser
          POSTGRES_PASSWORD          = $dbPass
          POSTGRES_CONNECTION_STRING = $conn
        }
      }
    }
  }

  Write-Host "`n=== 6. GitLab / Phoenix submodules ===" -ForegroundColor Cyan
  Write-Host 'GitLab token: Settings -> Access Tokens -> read_repository'
  $gitHost = Read-Line 'Git host URL' 'https://git.domain.internal'
  $gitToken = ''
  if (Read-YesNo 'Set GIT_READONLY_TOKEN now?' $false) {
    $gitToken = Read-SecureLine 'GitLab read token (hidden)'
  }
  $phoenixRepos = @(
    'mfe-poc-with-nx', 'phoenix-api-gateway', 'phoenix-billing-run', 'phoenix-core',
    'phoenix-core-lib', 'phoenix-mass-import', 'phoenix-migration', 'phoenix-payment-api',
    'phoenix-scheduler', 'phoenix-ui'
  )
  $gitmodulesLines = @()
  foreach ($repo in $phoenixRepos) {
    $path = "Cursor-Project/Phoenix/$repo"
    $url = "$gitHost/phoenix/$repo.git"
    $gitmodulesLines += "[submodule `"$path`"]", "`tpath = $path", "`turl = $url", ''
  }
  Set-Content -LiteralPath $GitModulesPath -Value (($gitmodulesLines -join "`n").TrimEnd() + "`n") -Encoding UTF8

  Write-Host "`n=== 7. Slack (optional) ===" -ForegroundColor Cyan
  $slackToken = ''
  if (Read-YesNo 'Configure SLACK_BOT_TOKEN?' $false) {
    $slackToken = Read-SecureLine 'SLACK_BOT_TOKEN (hidden)'
  }

  Write-Host "`n=== Writing config files ===" -ForegroundColor Cyan
  $envLines = @(
    "# Generated by setup.ps1 $(Get-Date -Format 'yyyy-MM-dd HH:mm')",
    "QA_PROJECT_NAME=$projectName",
    "QA_TICKET_PREFIX=$ticketPrefix",
    "JIRA_EMAIL=$jiraEmail",
    "JIRA_API_TOKEN=$jiraToken",
    "JIRA_BASE_URL=$jiraBase",
    "JIRA_PROJECT_KEY=$jiraProjectKey",
    "CONFLUENCE_URL=$wikiBase/home",
    "CONFLUENCE_WIKI_BASE=$wikiBase",
    "GITLAB_URL=$gitHost"
  )
  if ($gitToken) { $envLines += "GIT_READONLY_TOKEN=$gitToken" }
  if ($slackToken) { $envLines += "SLACK_BOT_TOKEN=$slackToken" }
  Set-Content -LiteralPath $EnvPath -Value ($envLines -join "`n") -Encoding UTF8
  Write-Host "  .env"

  $swDir = Split-Path $SwaggerPath -Parent
  if (-not (Test-Path $swDir)) { New-Item -ItemType Directory -Path $swDir -Force | Out-Null }
  @{
    description  = 'Swagger URLs per environment. Generated by setup.ps1.'
    environments = $swaggerEntries
  } | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $SwaggerPath -Encoding UTF8
  Write-Host '  environments.json'

  $mcpDir = Split-Path $McpOut -Parent
  if (-not (Test-Path $mcpDir)) { New-Item -ItemType Directory -Path $mcpDir -Force | Out-Null }
  @{ mcpServers = $mcpServers } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $McpOut -Encoding UTF8
  Write-Host '  mcp.generated.json  -> paste into Cursor Settings -> MCP'

  if (Read-YesNo "`nSet git hooksPath?" $true) {
    Push-Location $RepoRoot
    try { git config core.hooksPath Cursor-Project/scripts/git-hooks }
    finally { Pop-Location }
  }

  if (Read-YesNo 'Run update-swagger-specs.ps1 now? (VPN)' $false) {
    $swScript = Join-Path $RepoRoot '.cursor\commands\update-swagger-specs.ps1'
    if (Test-Path $swScript) { & $swScript }
  }

  if (Read-YesNo 'Run git submodule init now?' $false) {
    if ($gitToken) { $env:GIT_READONLY_TOKEN = $gitToken }
    Push-Location $RepoRoot
    try { git submodule update --init --recursive 2>&1 | Write-Host }
    finally { Pop-Location }
  }
}

# =============================================================================
# VERIFICATION
# =============================================================================
function Test-QaSetup {
  $failures = @()
  $warnings = @()
  $envPath = Join-Path $RepoRoot '.env'
  if (-not (Test-Path $envPath)) { $failures += 'Missing .env - run wizard (setup.ps1 without -SkipWizard)' }
  else {
    $raw = Get-Content $envPath -Raw
    foreach ($k in @('JIRA_EMAIL', 'JIRA_API_TOKEN', 'JIRA_BASE_URL', 'CONFLUENCE_WIKI_BASE')) {
      if ($raw -notmatch "(?m)^$k=.+" -or $raw -match "(?m)^$k=<") { $failures += ".env placeholder or missing: $k" }
    }
  }
  $swPath = Join-Path $RepoRoot 'Cursor-Project\config\swagger\environments.json'
  if (-not (Test-Path $swPath)) { $failures += 'Missing environments.json' }
  elseif ((Get-Content $swPath -Raw) -match '<[A-Z_]+>') { $failures += 'environments.json has placeholders' }
  if (-not (Test-Path (Join-Path $RepoRoot '.cursor\rules\main\core_rules.mdc'))) {
    $failures += 'Framework not installed — run setup without -SkipInstall'
  }
  Push-Location $RepoRoot
  try {
    $hp = git config --get core.hooksPath 2>$null
    if ($hp -ne 'Cursor-Project/scripts/git-hooks') { $warnings += "hooksPath is '$hp' (expected Cursor-Project/scripts/git-hooks)" }
  } finally { Pop-Location }
  foreach ($script in @(
    'Cursor-Project\scripts\validate-cursor-rules.ps1',
    'Cursor-Project\scripts\validate-cursor-consistency.ps1'
  )) {
    $full = Join-Path $RepoRoot $script
    if (Test-Path $full) {
      & $full
      if ($LASTEXITCODE -ne 0) { $failures += "$script failed (exit $LASTEXITCODE)" }
    }
  }
  if ($warnings.Count) {
    Write-Host 'WARNINGS:' -ForegroundColor Yellow
    $warnings | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow }
  }
  if ($failures.Count) {
    Write-Host 'FAILURES:' -ForegroundColor Red
    $failures | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
    return $false
  }
  Write-Host 'Verification: OK' -ForegroundColor Green
  return $true
}

# =============================================================================
# MAIN
# =============================================================================
if ($Help) { Show-SetupGuide; exit 0 }
if ($VerifyOnly) { if (-not (Test-QaSetup)) { exit 1 }; exit 0 }

Write-Host @'

  Universal Phoenix QA Toolkit
  ----------------------------
  This branch holds ONLY setup.ps1.
  The script installs the framework for YOUR project from git.

'@ -ForegroundColor Green

Show-SetupGuide

if (-not $SkipInstall) {
  $ref = Resolve-SourceRef -Branch $SourceBranch
  Install-QaFramework -Ref $ref
}

if (-not $SkipWizard) {
  Invoke-SetupWizard
}

Write-Host "`n=== Verification ===" -ForegroundColor Cyan
if (-not (Test-QaSetup)) { exit 1 }

Write-Host @'

Setup complete.
  - Open this folder in Cursor
  - Paste Cursor-Project/Cursor Setup/mcp.generated.json into MCP settings
  - Run: powershell -ExecutionPolicy Bypass -File setup.ps1 -Help  (this guide anytime)

'@ -ForegroundColor Green
