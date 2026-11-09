# setup-new-machine.ps1
# Set up this workspace for Claude Code on a new machine (macOS, Linux or Windows).
# Safe to re-run: every step skips work that is already done.
#
# Usage (from the workspace root):
#   pwsh -NoProfile -File .claude/commands/setup-new-machine.ps1
#   pwsh -NoProfile -File .claude/commands/setup-new-machine.ps1 -VerifyOnly
#   pwsh -NoProfile -File .claude/commands/setup-new-machine.ps1 -SkipClone -SkipBrowsers
#   pwsh -NoProfile -File .claude/commands/setup-new-machine.ps1 -InstallGitHooks
#
# When credentials are missing or rejected, the script lists the files and keys to fill,
# waits for you to fill them, and checks again. Use -NoPrompt to skip the wait.
#
# Exit codes: 0 = ready, 2 = finished with items to fix, 1 = cannot continue.

param(
    [switch]$VerifyOnly,        # change nothing, only report
    [switch]$SkipClone,         # do not init/clone submodules
    [switch]$SkipInstall,       # do not run npm install
    [switch]$SkipBrowsers,      # do not download Playwright browsers
    [switch]$ForceEnv,          # overwrite EnergoTS/.env and secrets.env from the templates
    [switch]$InstallGitHooks,   # point core.hooksPath at Cursor-Project/scripts/git-hooks
    [switch]$NoPrompt,          # never wait for input (report the credentials to fill and exit)
    [switch]$SkipLiveChecks,    # do not log in to Confluence, Slack and the portal to test the credentials
    [ValidateSet('cursorMerged', 'staging', 'cursor')]
    [string]$EnergoTSBranch = 'cursorMerged'
)

$ErrorActionPreference = 'Continue'
$script:HardFail = $false
$script:Results = [System.Collections.Generic.List[object]]::new()

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Split-Path -Parent (Split-Path -Parent $scriptDir)
$cp = Join-Path $root 'Cursor-Project'
$energo = Join-Path $cp 'EnergoTS'
$elasticDir = Join-Path (Join-Path $cp 'config') 'elasticsearch'
$postgresDir = Join-Path (Join-Path $cp 'config') 'postgres'
$setupDir = Join-Path $cp 'Cursor Setup'
$scriptsDir = Join-Path $cp 'scripts'
$gitmodules = Join-Path $root '.gitmodules'
$onWindows = [bool]$IsWindows
$allowedEnergoBranches = @('cursor', 'staging', 'cursorMerged')
$minNode = [version]'22.12.0'

# ---------- output helpers ----------
function Write-Step { param([string]$Message) Write-Host ''; Write-Host "=== $Message ===" -ForegroundColor Cyan }
function Write-Info { param([string]$Message) Write-Host "       $Message" -ForegroundColor Gray }

# Status: OK | WARN (optional, does not change the exit code) | FAIL (must fix) .
function Add-Result {
    # -CredFile marks a credentials item: the file the user has to edit to fix it.
    param([string]$Name, [string]$Status, [string]$Detail = '', [string]$Fix = '', [string]$CredFile = '')
    $script:Results.Add([pscustomobject]@{ Name = $Name; Status = $Status; Detail = $Detail; Fix = $Fix; CredFile = $CredFile })
    $color = switch ($Status) { 'OK' { 'Green' } 'WARN' { 'Yellow' } default { 'Red' } }
    $suffix = if ($Detail) { " - $Detail" } else { '' }
    Write-Host ("  {0,-4} {1}{2}" -f $Status, $Name, $suffix) -ForegroundColor $color
}

function Test-Tool { param([string]$Name) return [bool](Get-Command $Name -ErrorAction SilentlyContinue) }

function Get-PythonExe {
    foreach ($n in @('python3', 'python')) {
        $c = Get-Command $n -ErrorAction SilentlyContinue
        if ($c) { return $c.Source }
    }
    return $null
}

function Invoke-In {
    # Run a native command in a directory. Returns the exit code; output goes to the console.
    param([string]$Dir, [string]$Exe, [string[]]$ArgList)
    Push-Location $Dir
    try { & $Exe @ArgList | Out-Host; return $LASTEXITCODE }
    finally { Pop-Location }
}

# ---------- submodules ----------
function Get-Submodules {
    $entries = @()
    $current = $null
    foreach ($raw in (Get-Content -LiteralPath $gitmodules)) {
        $line = $raw.Trim()
        if ($line -match '^\[submodule') {
            if ($current) { $entries += [pscustomobject]$current }
            $current = [ordered]@{ Path = ''; Url = '' }
        }
        elseif ($current -and $line -match '^path\s*=\s*(.+)$') { $current.Path = $Matches[1].Trim() }
        elseif ($current -and $line -match '^url\s*=\s*(.+)$') { $current.Url = $Matches[1].Trim() }
    }
    if ($current) { $entries += [pscustomobject]$current }
    return $entries
}

function Test-RepoReady {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath (Join-Path $Path '.git'))) { return $false }
    $items = @(Get-ChildItem -LiteralPath $Path -Force -ErrorAction SilentlyContinue | Where-Object { $_.Name -ne '.git' })
    return ($items.Count -gt 0)
}

function Get-RepoBranch {
    param([string]$Path)
    return ((& git -C $Path branch --show-current 2>$null | Out-String).Trim())
}

function Invoke-Submodules {
    Write-Step 'Submodules (EnergoTS + Phoenix repositories)'
    foreach ($entry in (Get-Submodules)) {
        $full = Join-Path $root $entry.Path
        if (Test-RepoReady $full) { Write-Info "already present: $($entry.Path)"; continue }
        Write-Host "  cloning $($entry.Path) ..." -ForegroundColor Gray
        $code = Invoke-In -Dir $root -Exe 'git' -ArgList @('submodule', 'update', '--init', '--', $entry.Path)
        if ($code -ne 0) { Write-Host "  could not clone $($entry.Path) (exit $code)" -ForegroundColor Yellow }
    }

    # EnergoTS must sit on an allowed branch (Rule ENERGOTS.0). A fresh submodule is a detached HEAD.
    if (Test-RepoReady $energo) {
        $branch = Get-RepoBranch $energo
        if ($allowedEnergoBranches -notcontains $branch) {
            Write-Host "  EnergoTS is on '$branch' - switching to $EnergoTSBranch" -ForegroundColor Gray
            & git -C $energo fetch origin $EnergoTSBranch 2>&1 | Out-Null
            & git -C $energo checkout $EnergoTSBranch 2>&1 | Out-Null
            if ($LASTEXITCODE -ne 0) {
                & git -C $energo checkout -b $EnergoTSBranch --track "origin/$EnergoTSBranch" 2>&1 | Out-Null
            }
        }
    }
}

# ---------- env and secrets ----------
function Copy-Template {
    param([string]$Template, [string]$Target, [string]$Label)
    if (-not (Test-Path -LiteralPath $Template)) { Write-Host "  template missing for $Label" -ForegroundColor Yellow; return }
    if (-not (Test-Path -LiteralPath (Split-Path -Parent $Target))) { Write-Info "skipped $Label (folder not there yet)"; return }
    if ((Test-Path -LiteralPath $Target) -and -not $ForceEnv) { Write-Info "kept existing $Label"; return }
    Copy-Item -LiteralPath $Template -Destination $Target -Force
    Write-Host "  created $Label from its template" -ForegroundColor Green
}

function Invoke-EnvFiles {
    Write-Step 'Environment and secrets files'
    Copy-Template -Template (Join-Path $setupDir 'env.example') -Target (Join-Path $cp '.env') -Label 'Cursor-Project/.env'
    Copy-Template -Template (Join-Path $setupDir 'env.example') -Target (Join-Path $energo '.env') -Label 'Cursor-Project/EnergoTS/.env'
    Copy-Template -Template (Join-Path $postgresDir 'secrets.env.example') -Target (Join-Path $postgresDir 'secrets.env') -Label 'Cursor-Project/config/postgres/secrets.env'
    Copy-Template -Template (Join-Path $elasticDir 'secrets.env.example') -Target (Join-Path $elasticDir 'secrets.env') -Label 'Cursor-Project/config/elasticsearch/secrets.env'
}

# Names of keys that have no value. Values are never read into output.
function Get-EmptyKeys {
    param([string]$Path, [string[]]$Only)
    $empty = @()
    $seen = @()
    foreach ($line in (Get-Content -LiteralPath $Path -ErrorAction SilentlyContinue)) {
        if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
            $key = $Matches[1]
            $seen += $key
            if ($Only -and ($Only -notcontains $key)) { continue }
            if ([string]::IsNullOrWhiteSpace($Matches[2].Trim().Trim('"').Trim("'"))) { $empty += $key }
        }
    }
    foreach ($k in $Only) { if ($seen -notcontains $k) { $empty += $k } }
    return @($empty | Select-Object -Unique)
}

# Value of one key, unquoted. Used only to call the service the key belongs to; never printed.
function Get-EnvFileValue {
    param([string]$Path, [string]$Key)
    foreach ($line in (Get-Content -LiteralPath $Path -ErrorAction SilentlyContinue)) {
        if ($line -match "^\s*$([regex]::Escape($Key))\s*=\s*(.*)$") {
            $v = $Matches[1].Trim()
            if ($v.Length -ge 2 -and (($v.StartsWith('"') -and $v.EndsWith('"')) -or ($v.StartsWith("'") -and $v.EndsWith("'")))) { $v = $v.Substring(1, $v.Length - 2) }
            return $v.Trim()
        }
    }
    return ''
}

# Log in to each service with the filled credentials. A rejected login is a FAIL,
# an unreachable service (VPN off, no network) is a WARN.
function Invoke-LiveChecks {
    $projEnv = Join-Path $cp '.env'
    $energoEnv = Join-Path $energo '.env'
    $projLabel = 'Cursor-Project/.env'
    $energoLabel = 'Cursor-Project/EnergoTS/.env'

    # Confluence (same Atlassian token the Jira attachment script uses)
    $cql = Join-Path (Join-Path (Join-Path $cp 'config') 'confluence') 'search-confluence-cql-rest.ps1'
    if ((Test-Path -LiteralPath $cql) -and @(Get-EmptyKeys -Path $projEnv -Only @('JIRA_EMAIL', 'JIRA_API_TOKEN')).Count -eq 0) {
        $out = & pwsh -NoProfile -File $cql -Cql 'type=page' -Limit 1 2>&1 | Out-String
        if ($out -match 'Total size') { Add-Result 'Confluence login' 'OK' 'token accepted' }
        elseif ($out -match '\b(401|403)\b|Unauthorized|Forbidden') { Add-Result 'Confluence login' 'FAIL' 'email or token rejected' "Correct JIRA_EMAIL and JIRA_API_TOKEN in $projLabel." -CredFile $projEnv }
        else { Add-Result 'Confluence login' 'WARN' 'could not reach Confluence' "Check the network and CONFLUENCE_URL in $projLabel, then check again." }
    }

    # Slack (the Slack scripts read the token from EnergoTS/.env)
    $token = Get-EnvFileValue -Path $energoEnv -Key 'SLACK_API_TOKEN'
    if (-not $token) { $token = Get-EnvFileValue -Path $energoEnv -Key 'SLACK_BOT_TOKEN' }
    if ($token) {
        try {
            $r = Invoke-RestMethod -Uri 'https://slack.com/api/auth.test' -Headers @{ Authorization = "Bearer $token" } -Method Get -TimeoutSec 20
            if ($r.ok) { Add-Result 'Slack login' 'OK' "workspace $($r.team)" }
            else { Add-Result 'Slack login' 'FAIL' "token rejected ($($r.error))" "Correct SLACK_BOT_TOKEN (or SLACK_API_TOKEN) in $energoLabel." -CredFile $energoEnv }
        }
        catch { Add-Result 'Slack login' 'WARN' 'could not reach Slack' 'Check the network, then check again.' }
    }
    $token = $null

    # Portal login used by every Playwright run (Test environment endpoint)
    $authUrl = Get-EnvFileValue -Path $energoEnv -Key 'TESTAUTHAPI'
    $user = Get-EnvFileValue -Path $energoEnv -Key 'PORTAL_USER'
    $pass = Get-EnvFileValue -Path $energoEnv -Key 'PASSWORD'
    if ($authUrl -and $user -and $pass) {
        try {
            $body = @{ user = $user; password = $pass } | ConvertTo-Json -Compress
            $r = Invoke-RestMethod -Uri $authUrl -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 20
            if ($r.jwt) { Add-Result 'Portal login (Test)' 'OK' 'user and password accepted' }
            else { Add-Result 'Portal login (Test)' 'FAIL' 'no token returned' "Correct PORTAL_USER and PASSWORD in $energoLabel." -CredFile $energoEnv }
        }
        catch {
            $code = 0
            if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
            if ($code -ge 400 -and $code -lt 500) { Add-Result 'Portal login (Test)' 'FAIL' "rejected (HTTP $code)" "Correct PORTAL_USER and PASSWORD in $energoLabel." -CredFile $energoEnv }
            else { Add-Result 'Portal login (Test)' 'WARN' 'could not reach the portal' 'Connect to the VPN, then check again.' }
        }
    }
    $pass = $null; $body = $null
}

# ---------- dependencies ----------
# Browser folders this EnergoTS Playwright version needs but that are not downloaded yet.
# Returns $null when Playwright itself is not installed.
function Get-MissingBrowsers {
    if (-not (Test-Path -LiteralPath (Join-Path (Join-Path (Join-Path $energo 'node_modules') '@playwright') 'test'))) { return $null }
    Push-Location $energo
    try { $lines = @(& npx --no-install playwright install --dry-run 2>$null) }
    finally { Pop-Location }
    $missing = @()
    foreach ($line in $lines) {
        if ("$line" -match 'Install location:\s*(.+)$') {
            $dir = $Matches[1].Trim()
            if (-not (Test-Path -LiteralPath $dir)) { $missing += (Split-Path -Leaf $dir) }
        }
    }
    return , $missing
}

function Invoke-Dependencies {
    Write-Step 'Dependencies (npm packages, Playwright browsers)'
    if (-not (Test-Tool 'npm')) { Write-Host '  npm not found - skipping installs' -ForegroundColor Yellow; return }

    $elasticBin = Join-Path (Join-Path (Join-Path $elasticDir 'node_modules') '.bin') 'elastic'
    if ((Test-Path -LiteralPath $elasticBin) -or (Test-Path -LiteralPath "$elasticBin.cmd")) { Write-Info 'Elastic CLI already installed' }
    else {
        Write-Host '  installing the Elastic CLI ...' -ForegroundColor Gray
        $null = Invoke-In -Dir $elasticDir -Exe 'npm' -ArgList @('install', '--no-fund', '--no-audit')
    }

    if (-not (Test-Path -LiteralPath (Join-Path $energo 'package.json'))) { Write-Info 'EnergoTS not cloned - skipping its packages'; return }
    $pwPkg = Join-Path (Join-Path (Join-Path $energo 'node_modules') '@playwright') 'test'
    if (Test-Path -LiteralPath $pwPkg) { Write-Info 'EnergoTS packages already installed' }
    else {
        Write-Host '  installing EnergoTS packages ...' -ForegroundColor Gray
        $null = Invoke-In -Dir $energo -Exe 'npm' -ArgList @('install', '--no-fund', '--no-audit')
    }
    if ($SkipBrowsers) { Write-Info 'Playwright browsers skipped (-SkipBrowsers)' }
    elseif (Test-Path -LiteralPath $pwPkg) {
        # Only call the installer when something is missing: it also prunes browsers
        # that no known Playwright installation uses.
        $missing = Get-MissingBrowsers
        if ($null -ne $missing -and $missing.Count -eq 0) { Write-Info 'Playwright browsers already downloaded' }
        else {
            Write-Host '  downloading Playwright browsers ...' -ForegroundColor Gray
            $null = Invoke-In -Dir $energo -Exe 'npx' -ArgList @('--no-install', 'playwright', 'install')
        }
    }
}

# ---------- workspace wiring ----------
$skillLinks = @(
    @{ Name = 'playwright-cli'; Target = '.agents/skills/playwright-cli' },
    @{ Name = 'flaky-test-fixer'; Target = 'Cursor-Project/EnergoTS/.cursor/skills/flaky-test-fixer' }
)

function Test-SkillLink {
    param([string]$Name)
    return (Test-Path -LiteralPath (Join-Path (Join-Path (Join-Path (Join-Path $root '.claude') 'skills') $Name) 'SKILL.md'))
}

function Invoke-Wiring {
    Write-Step 'Workspace wiring'
    $null = New-Item -ItemType Directory -Path (Join-Path (Join-Path $root '.claude') 'logs') -Force

    # Two skills are symlinks. Git on Windows checks a symlink out as a small text file
    # unless core.symlinks is on, so rebuild those as directory junctions.
    foreach ($link in $skillLinks) {
        if (Test-SkillLink $link.Name) { continue }
        $linkPath = Join-Path (Join-Path (Join-Path $root '.claude') 'skills') $link.Name
        $target = Join-Path $root $link.Target
        if (-not (Test-Path -LiteralPath (Join-Path $target 'SKILL.md'))) { Write-Info "skill $($link.Name): target not there yet ($($link.Target))"; continue }
        if (-not $onWindows) { Write-Info "skill $($link.Name): link is broken, restore it with: git checkout -- .claude/skills/$($link.Name)"; continue }
        $item = Get-Item -LiteralPath $linkPath -Force -ErrorAction SilentlyContinue
        if ($item -and -not $item.PSIsContainer) { Remove-Item -LiteralPath $linkPath -Force }
        if (-not (Test-Path -LiteralPath $linkPath)) {
            $null = New-Item -ItemType Junction -Path $linkPath -Target $target
            & git -C $root update-index --skip-worktree -- ".claude/skills/$($link.Name)" 2>$null
            Write-Host "  rebuilt skill link $($link.Name) as a junction" -ForegroundColor Green
        }
    }

    if ($InstallGitHooks) {
        & git -C $root config core.hooksPath 'Cursor-Project/scripts/git-hooks'
        Write-Host '  git hooks enabled (commit-msg: Conventional Commits; pre-commit: rules validator)' -ForegroundColor Green
    }
}

# ---------- verify ----------
function Invoke-Verify {
    Write-Step 'Verify'
    $script:Results.Clear()
    $runCmd = 'pwsh -NoProfile -File .claude/commands/setup-new-machine.ps1'

    # Required tools
    Add-Result 'PowerShell 7+' $(if ($PSVersionTable.PSVersion.Major -ge 7) { 'OK' } else { 'FAIL' }) "$($PSVersionTable.PSVersion)" 'Install PowerShell 7 (macOS: brew install powershell; Windows: winget install Microsoft.PowerShell). The Claude Code hooks need it.'
    if (Test-Tool 'git') { Add-Result 'git' 'OK' ((& git --version | Out-String).Trim()) }
    else { Add-Result 'git' 'FAIL' 'not on PATH' 'Install Git.' }

    $py = Get-PythonExe
    if ($py) { Add-Result 'python3' 'OK' $py }
    else { Add-Result 'python3' 'FAIL' 'not on PATH' 'Install Python 3. The database and Elasticsearch wrappers are Python scripts.' }

    if (Test-Tool 'node') {
        $nodeText = ((& node -v | Out-String).Trim()).TrimStart('v')
        $nodeVer = $null
        if ([version]::TryParse(($nodeText -replace '[^0-9.].*$', ''), [ref]$nodeVer) -and $nodeVer -ge $minNode) { Add-Result 'Node.js' 'OK' "v$nodeText" }
        else { Add-Result 'Node.js' 'FAIL' "v$nodeText (need $minNode or later)" 'Install Node.js 22.12 or later. The Elastic CLI needs it.' }
    }
    else { Add-Result 'Node.js' 'FAIL' 'not on PATH' 'Install Node.js 22.12 or later (includes npm).' }

    # Claude Code configuration
    $settings = Join-Path (Join-Path $root '.claude') 'settings.json'
    $settingsOk = $false
    if (Test-Path -LiteralPath $settings) { try { $null = Get-Content -LiteralPath $settings -Raw | ConvertFrom-Json; $settingsOk = $true } catch { } }
    if ($settingsOk -and (Test-Path -LiteralPath (Join-Path $root 'CLAUDE.md'))) { Add-Result 'Claude Code config' 'OK' '.claude/settings.json + CLAUDE.md' }
    else { Add-Result 'Claude Code config' 'FAIL' '.claude/settings.json or CLAUDE.md missing or invalid' 'Check out a branch that contains the .claude folder and CLAUDE.md.' }

    foreach ($v in @('validate-claude-rules.ps1', 'validate-claude-consistency.ps1')) {
        $vPath = Join-Path $scriptsDir $v
        if (-not (Test-Path -LiteralPath $vPath)) { Add-Result $v 'WARN' 'script not found'; continue }
        $out = & pwsh -NoProfile -File $vPath 2>&1 | Out-String
        if ($LASTEXITCODE -eq 0) { Add-Result $v 'OK' 'rules, agents, skills and hooks are consistent' }
        else {
            $last = @($out.Trim() -split "`n" | Where-Object { $_.Trim() })[-1]
            Add-Result $v 'FAIL' "$last" "Run: pwsh -NoProfile -File Cursor-Project/scripts/$v"
        }
    }

    foreach ($link in $skillLinks) {
        if (Test-SkillLink $link.Name) { Add-Result "skill $($link.Name)" 'OK' 'link resolves' }
        else { Add-Result "skill $($link.Name)" 'FAIL' "does not resolve to $($link.Target)" "Make sure $($link.Target) exists, then re-run: $runCmd" }
    }

    # Submodules
    foreach ($entry in (Get-Submodules)) {
        $full = Join-Path $root $entry.Path
        $name = Split-Path -Leaf $entry.Path
        if (-not (Test-RepoReady $full)) {
            $hint = if ($entry.Url -match 'github\.com') { 'Check your GitHub access to this repository' } else { 'Connect to the VPN and make sure your GitLab credentials work' }
            Add-Result "repo $name" 'FAIL' 'not cloned' "$hint, then re-run: $runCmd"
            continue
        }
        if ($name -eq 'EnergoTS') {
            $branch = Get-RepoBranch $full
            if ($allowedEnergoBranches -contains $branch) { Add-Result "repo $name" 'OK' "branch $branch" }
            else { Add-Result "repo $name" 'FAIL' "branch '$branch' (allowed: $($allowedEnergoBranches -join ', '))" "Re-run: $runCmd" }
        }
        else { Add-Result "repo $name" 'OK' 'cloned' }
    }

    # Env and secrets (key names only, never values)
    $envChecks = @(
        @{ Label = 'Cursor-Project/.env'; Path = (Join-Path $cp '.env'); Keys = @('JIRA_EMAIL', 'JIRA_API_TOKEN', 'JIRA_BASE_URL', 'CONFLUENCE_URL'); Use = 'Confluence reads, Jira attachments' },
        @{ Label = 'Cursor-Project/EnergoTS/.env'; Path = (Join-Path $energo '.env'); Keys = @('PORTAL_USER', 'PASSWORD', 'DEVAUTHAPI', 'TESTAUTHAPI'); Use = 'Playwright test runs, Slack reports'; Slack = $true }
    )
    foreach ($c in $envChecks) {
        if (-not (Test-Path -LiteralPath $c.Path)) { Add-Result $c.Label 'FAIL' 'missing' "Re-run: $runCmd"; continue }
        $empty = @(Get-EmptyKeys -Path $c.Path -Only $c.Keys)
        # The Slack scripts read EnergoTS/.env and accept either token name.
        if ($c.Slack -and -not (Get-EnvFileValue -Path $c.Path -Key 'SLACK_API_TOKEN') -and -not (Get-EnvFileValue -Path $c.Path -Key 'SLACK_BOT_TOKEN')) { $empty += 'SLACK_BOT_TOKEN' }
        if ($empty.Count -eq 0) { Add-Result $c.Label 'OK' 'required keys filled' }
        else { Add-Result $c.Label 'FAIL' "empty: $($empty -join ', ')" "Open $($c.Label) and fill those keys (used for: $($c.Use))." -CredFile $c.Path }
    }
    $secretChecks = @(
        @{ Label = 'Cursor-Project/config/postgres/secrets.env'; Path = (Join-Path $postgresDir 'secrets.env'); Use = 'database queries; fill PGPASSWORD_<ENV> for each environment you will query' },
        @{ Label = 'Cursor-Project/config/elasticsearch/secrets.env'; Path = (Join-Path $elasticDir 'secrets.env'); Use = 'Dev and Prod log search' }
    )
    foreach ($c in $secretChecks) {
        if (-not (Test-Path -LiteralPath $c.Path)) { Add-Result $c.Label 'WARN' 'missing' "Re-run: $runCmd to create it from the template, then fill what you need ($($c.Use))."; continue }
        $empty = @(Get-EmptyKeys -Path $c.Path)
        if ($empty.Count -eq 0) { Add-Result $c.Label 'OK' 'all keys filled' }
        else { Add-Result $c.Label 'WARN' "empty: $($empty -join ', ')" "Open $($c.Label) and fill what you need ($($c.Use))." -CredFile $c.Path }
    }
    if ($SkipLiveChecks) { Write-Info 'live credential checks skipped (-SkipLiveChecks)' } else { Invoke-LiveChecks }

    # Installed packages
    $elasticBin = Join-Path (Join-Path (Join-Path $elasticDir 'node_modules') '.bin') 'elastic'
    if ((Test-Path -LiteralPath $elasticBin) -or (Test-Path -LiteralPath "$elasticBin.cmd")) { Add-Result 'Elastic CLI' 'OK' 'installed in config/elasticsearch' }
    else { Add-Result 'Elastic CLI' 'FAIL' 'not installed' "Re-run: $runCmd" }

    if (Test-RepoReady $energo) {
        if (Test-Path -LiteralPath (Join-Path (Join-Path (Join-Path $energo 'node_modules') '@playwright') 'test')) { Add-Result 'EnergoTS packages' 'OK' 'node_modules present' }
        else { Add-Result 'EnergoTS packages' 'FAIL' 'node_modules missing' "Re-run: $runCmd" }

        $missing = Get-MissingBrowsers
        if ($null -eq $missing) { }
        elseif ($missing.Count -eq 0) { Add-Result 'Playwright browsers' 'OK' 'downloaded' }
        else { Add-Result 'Playwright browsers' 'FAIL' "missing: $($missing -join ', ')" 'Run inside Cursor-Project/EnergoTS: npx playwright install' }
    }

    # Optional tools: each one powers a single integration
    if (Test-Tool 'acli') {
        & acli jira auth status *> $null
        if ($LASTEXITCODE -eq 0) { Add-Result 'Jira CLI (acli)' 'OK' 'logged in' }
        else { Add-Result 'Jira CLI (acli)' 'WARN' 'installed, not logged in' 'Run: acli jira auth login' }
    }
    else { Add-Result 'Jira CLI (acli)' 'WARN' 'not installed' 'Install the official Atlassian CLI, then run: acli jira auth login. Needed for every Jira read.' }

    if (Test-Tool 'psql') { Add-Result 'PostgreSQL client' 'OK' 'psql on PATH' }
    else {
        $fix = if ($onWindows) { 'Install the PostgreSQL command line tools and add them to PATH.' } elseif ($IsMacOS) { 'Run: brew install libpq && brew link --force libpq' } else { 'Install the postgresql-client package.' }
        Add-Result 'PostgreSQL client' 'WARN' 'psql not on PATH' "$fix Needed for database queries."
    }

    if (Test-Tool 'playwright-cli') { Add-Result 'playwright-cli' 'OK' 'on PATH' }
    else { Add-Result 'playwright-cli' 'WARN' 'not installed' 'Run: npm install -g @playwright/cli@latest. Needed for /playwright-frontend locator recording.' }

    if ($py) {
        & $py -c 'import requests' 2>$null
        if ($LASTEXITCODE -eq 0) { Add-Result 'Python requests' 'OK' 'importable' }
        else { Add-Result 'Python requests' 'WARN' 'not installed' "Run: $py -m pip install requests. Needed only by scripts/jira_bug_validator.py." }
    }

    if (Test-Tool 'claude') { Add-Result 'Claude Code CLI' 'OK' 'on PATH' }
    else { Add-Result 'Claude Code CLI' 'WARN' 'not on PATH' 'Fine if you use the Claude desktop app. Otherwise install Claude Code.' }

    $hooksPath = ((& git -C $root config core.hooksPath 2>$null | Out-String).Trim())
    if ($hooksPath) { Add-Result 'git hooks' 'OK' $hooksPath }
    else { Add-Result 'git hooks' 'WARN' 'not enabled' "Optional. Enable with: $runCmd -InstallGitHooks (enforces Conventional Commits messages)." }
}

# ---------- main ----------
Write-Host ''
Write-Host 'setup-new-machine' -ForegroundColor Cyan
Write-Host "Workspace: $root" -ForegroundColor Gray

if (-not (Test-Path -LiteralPath $cp) -or -not (Test-Path -LiteralPath $gitmodules)) {
    Write-Host "  FAIL $root is not the workspace root (Cursor-Project/ or .gitmodules missing)" -ForegroundColor Red
    exit 1
}
if (-not (Test-Tool 'git')) {
    Write-Host '  FAIL git is not on PATH. Install Git and run this script again.' -ForegroundColor Red
    exit 1
}

if ($VerifyOnly) { Write-Host 'VerifyOnly: nothing will be changed.' -ForegroundColor Yellow }
else {
    if ($SkipClone) { Write-Step 'Submodules - skipped (-SkipClone)' } else { Invoke-Submodules }
    Invoke-EnvFiles
    if ($SkipInstall) { Write-Step 'Dependencies - skipped (-SkipInstall)' } else { Invoke-Dependencies }
    Invoke-Wiring
}

function Write-Summary {
    $fails = @($script:Results | Where-Object { $_.Status -eq 'FAIL' })
    $warns = @($script:Results | Where-Object { $_.Status -eq 'WARN' })
    $oks = @($script:Results | Where-Object { $_.Status -eq 'OK' })
    Write-Host ''
    Write-Host "Summary: $($oks.Count) OK, $($fails.Count) to fix, $($warns.Count) optional" -ForegroundColor $(if ($fails.Count) { 'Yellow' } else { 'Green' })
    if ($fails.Count) {
        Write-Host ''
        Write-Host 'To fix:' -ForegroundColor Red
        $i = 1
        foreach ($f in $fails) { Write-Host ("  {0}. {1}: {2}" -f $i, $f.Name, $f.Fix); $i++ }
    }
    if ($warns.Count) {
        Write-Host ''
        Write-Host 'Optional (each one enables a single integration):' -ForegroundColor Yellow
        foreach ($w in $warns) { Write-Host ("  - {0}: {1}" -f $w.Name, $w.Fix) }
    }
    Write-Host ''
}

# Credential items still open: required ones (FAIL) and optional ones (WARN).
function Get-CredItems { return @($script:Results | Where-Object { $_.CredFile -and $_.Status -ne 'OK' }) }

function Write-CredentialsToFill {
    Write-Host ''
    Write-Host '=== Credentials to fill ===' -ForegroundColor Cyan
    Write-Host '  Open each file below in an editor, fill or correct the keys, and save.' -ForegroundColor Gray
    Write-Host '  Type the values into the files only. Do not paste them into a chat.' -ForegroundColor Gray
    foreach ($group in (Get-CredItems | Group-Object CredFile)) {
        Write-Host ''
        Write-Host "  $($group.Name)" -ForegroundColor White
        foreach ($item in $group.Group) {
            $tag = if ($item.Status -eq 'FAIL') { 'required' } else { 'optional' }
            Write-Host ("    [{0}] {1} - {2}" -f $tag, $item.Detail, $item.Fix) -ForegroundColor $(if ($item.Status -eq 'FAIL') { 'Red' } else { 'Yellow' })
        }
    }
    Write-Host ''
}

Invoke-Verify

# Ask for the credentials, wait, and check again until the required ones are filled and accepted.
$canPrompt = -not $NoPrompt -and -not $VerifyOnly -and [Environment]::UserInteractive -and -not [Console]::IsInputRedirected
while (@(Get-CredItems).Count -gt 0) {
    Write-CredentialsToFill
    if (-not $canPrompt) { break }
    $required = @(Get-CredItems | Where-Object { $_.Status -eq 'FAIL' }).Count
    $hint = if ($required) { 'Press Enter when the files are saved to check again, or type s to stop' } else { 'Only optional keys are left. Press Enter to check again, or type s to finish' }
    $answer = Read-Host $hint
    if ("$answer".Trim() -match '^(s|skip|q|quit)$') { break }
    Invoke-Verify
}

Write-Summary
$fails = @($script:Results | Where-Object { $_.Status -eq 'FAIL' })
if (-not $fails.Count) { Write-Host 'Ready. Open this folder in Claude Code.' -ForegroundColor Green }
else { Write-Host "Fix the items above, then check again: pwsh -NoProfile -File .claude/commands/setup-new-machine.ps1 -VerifyOnly" -ForegroundColor Gray }
Write-Host ''

if ($script:HardFail) { exit 1 }
if ($fails.Count) { exit 2 }
exit 0
