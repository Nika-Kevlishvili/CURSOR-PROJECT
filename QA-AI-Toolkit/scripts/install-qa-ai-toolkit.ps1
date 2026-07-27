# install-qa-ai-toolkit.ps1
# Interactive step wizard: install QA AI Toolkit into a target project.
# Usage:
#   .\QA-AI-Toolkit\scripts\install-qa-ai-toolkit.ps1
#   .\QA-AI-Toolkit\scripts\install-qa-ai-toolkit.ps1 -TargetPath D:\MyProject
#   .\QA-AI-Toolkit\scripts\install-qa-ai-toolkit.ps1 -FromStep 5
#   .\QA-AI-Toolkit\scripts\install-qa-ai-toolkit.ps1 -VerifyOnly

param(
    [string]$TargetPath = '',
    [int]$FromStep = 0,
    [switch]$VerifyOnly,
    [switch]$ForceCursor,
    [switch]$ForceEnv,
    [switch]$ForceMcp,
    [switch]$SkipClone,
    [switch]$SkipExtensions,
    [switch]$SkipDb,
    [string]$GitLabGroup = ''
)

$ErrorActionPreference = 'Continue'
$script:HardFail = $false
$script:PartialFail = $false
$script:QuitRequested = $false

$toolkitRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$templateCursor = Join-Path $toolkitRoot '.cursor-template'
$configDir = Join-Path $toolkitRoot 'config'
$versionFile = Join-Path $toolkitRoot 'VERSION'
$toolkitVersion = if (Test-Path $versionFile) { (Get-Content $versionFile -Raw).Trim() } else { '0.1.0' }

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "=== $Message ===" -ForegroundColor Cyan
}
function Write-Ok([string]$Message) { Write-Host "  OK   $Message" -ForegroundColor Green }
function Write-Warn([string]$Message) { Write-Host "  WARN $Message" -ForegroundColor Yellow }
function Write-Fail([string]$Message, [switch]$Hard) {
    Write-Host "  FAIL $Message" -ForegroundColor Red
    if ($Hard) { $script:HardFail = $true } else { $script:PartialFail = $true }
}

function Confirm-Next([string]$NextLabel) {
    Write-Host ""
    $ans = Read-Host "Proceed to next step ($NextLabel)? [Y/n/q]"
    if ([string]::IsNullOrWhiteSpace($ans)) { $ans = 'Y' }
    $ans = $ans.Trim().ToLowerInvariant()
    if ($ans -eq 'q') { $script:QuitRequested = $true; return $false }
    if ($ans -eq 'n' -or $ans -eq 'no') { $script:QuitRequested = $true; return $false }
    return $true
}

function Save-WizardStep([string]$Target, [int]$Step) {
    $cfgPath = Join-Path $Target '.cursor\project-config.json'
    if (-not (Test-Path $cfgPath)) { return }
    try {
        $cfg = Get-Content -LiteralPath $cfgPath -Raw -Encoding UTF8 | ConvertFrom-Json
        $cfg | Add-Member -NotePropertyName wizardLastCompletedStep -NotePropertyValue $Step -Force
        $cfg | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $cfgPath -Encoding UTF8
    } catch {}
}

function Get-EnvFromFile([string]$EnvPath, [string]$Name) {
    if (-not (Test-Path $EnvPath)) { return $null }
    foreach ($line in Get-Content -LiteralPath $EnvPath) {
        $t = $line.Trim()
        if (-not $t -or $t.StartsWith('#')) { continue }
        if ($t -match "^\s*$Name\s*=\s*['""]?(.*?)['""]?\s*$") {
            $val = $Matches[1]
            if (-not [string]::IsNullOrWhiteSpace($val)) { return $val }
        }
    }
    return $null
}

function Sanitize-Id([string]$raw) {
    $s = ($raw -replace '[^a-zA-Z0-9]', '')
    if ([string]::IsNullOrWhiteSpace($s)) { $s = 'Env' }
    return $s
}

function Get-CursorCli {
    $cmd = Get-Command cursor -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    foreach ($c in @(
            (Join-Path $env:LOCALAPPDATA 'Programs\cursor\Cursor.exe'),
            (Join-Path $env:LOCALAPPDATA 'Programs\Cursor\Cursor.exe')
        )) {
        if (Test-Path $c) { return $c }
    }
    return $null
}

# ---------- resolve target ----------
if ([string]::IsNullOrWhiteSpace($TargetPath)) {
    $TargetPath = Read-Host "Target project path (blank = current directory)"
    if ([string]::IsNullOrWhiteSpace($TargetPath)) { $TargetPath = (Get-Location).Path }
}
$TargetPath = [System.IO.Path]::GetFullPath($TargetPath)
if (-not (Test-Path $TargetPath)) {
    New-Item -ItemType Directory -Path $TargetPath -Force | Out-Null
}

Write-Host ""
Write-Host "QA AI Toolkit installer (wizard)" -ForegroundColor Cyan
Write-Host "Toolkit: $toolkitRoot" -ForegroundColor Gray
Write-Host "Target:  $TargetPath" -ForegroundColor Gray
Write-Host "Version: $toolkitVersion" -ForegroundColor Gray

if (-not (Test-Path $templateCursor)) {
    Write-Fail "Missing .cursor-template at $templateCursor" -Hard
    exit 1
}

# ---------- VerifyOnly ----------
if ($VerifyOnly) {
    & (Join-Path $PSScriptRoot 'verify-qa-ai-toolkit.ps1') -TargetPath $TargetPath
    exit $LASTEXITCODE
}

$startStep = $FromStep
if ($startStep -le 0) {
    $cfgExisting = Join-Path $TargetPath '.cursor\project-config.json'
    if (Test-Path $cfgExisting) {
        try {
            $prev = (Get-Content $cfgExisting -Raw | ConvertFrom-Json).wizardLastCompletedStep
            if ($prev -gt 0) {
                $resume = Read-Host "Found wizardLastCompletedStep=$prev. Resume from next step ($($prev + 1))? [Y/n]"
                if ([string]::IsNullOrWhiteSpace($resume) -or $resume -match '^[Yy]') {
                    $startStep = [int]$prev + 1
                }
            }
        } catch {}
    }
}

# ===== Phase 0 =====
if ($startStep -le 0) {
    Write-Step 'Phase 0 - Preconditions'
    $psOk = $PSVersionTable.PSVersion.Major -ge 5
    if ($psOk) { Write-Ok "PowerShell $($PSVersionTable.PSVersion)" } else { Write-Fail 'PowerShell 5.1+ required' -Hard }
    $gitOk = $false
    try { $null = git --version 2>$null; if ($LASTEXITCODE -eq 0) { $gitOk = $true } } catch {}
    if ($gitOk) { Write-Ok 'git available' } else { Write-Fail 'git not in PATH' -Hard }
    $npmOk = $false
    try { $null = npm -v 2>$null; if ($LASTEXITCODE -eq 0) { $npmOk = $true } } catch {}
    if ($npmOk) { Write-Ok "npm $(npm -v 2>$null)" } else { Write-Warn 'npm not in PATH (needed for PostgreSQL MCP npx)' }
    $cursor = Get-CursorCli
    if ($cursor) { Write-Ok "Cursor CLI: $cursor" } else { Write-Warn 'Cursor CLI not found (extensions install may be skipped)' }
    if ($script:HardFail) { exit 1 }
    if (-not (Confirm-Next 'copy .cursor template + skeleton')) { Write-Host 'Stopped by user.'; exit 0 }
}

# ===== Phase 1 =====
if ($startStep -le 1 -and -not $script:QuitRequested) {
    Write-Step 'Phase 1 - Copy .cursor template and project skeleton'
    $destCursor = Join-Path $TargetPath '.cursor'
    if ((Test-Path $destCursor) -and -not $ForceCursor) {
        $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
        $bak = Join-Path $TargetPath ".cursor.bak-$stamp"
        Copy-Item -Path $destCursor -Destination $bak -Recurse -Force
        Write-Ok "Backed up existing .cursor -> $bak"
    }
    if ((Test-Path $destCursor) -and $ForceCursor) {
        Remove-Item -Path $destCursor -Recurse -Force
    }
    if (-not (Test-Path $destCursor) -or $ForceCursor) {
        Copy-Item -Path $templateCursor -Destination $destCursor -Recurse -Force
        Write-Ok 'Copied .cursor-template -> .cursor'
    } else {
        # merge: ensure files exist
        Copy-Item -Path (Join-Path $templateCursor '*') -Destination $destCursor -Recurse -Force
        Write-Ok 'Refreshed .cursor from template'
    }

    # config helpers
    $destConfig = Join-Path $TargetPath 'config'
    New-Item -ItemType Directory -Path $destConfig -Force | Out-Null
    Copy-Item -Path (Join-Path $configDir 'scripts') -Destination (Join-Path $destConfig 'scripts') -Recurse -Force
    Copy-Item -Path (Join-Path $configDir 'templates') -Destination (Join-Path $destConfig 'templates') -Recurse -Force
    Write-Ok 'Copied config/scripts and config/templates'

    # docs from toolkit
    $destDocs = Join-Path $TargetPath 'docs'
    New-Item -ItemType Directory -Path $destDocs -Force | Out-Null
    Copy-Item -Path (Join-Path $toolkitRoot 'docs\*') -Destination $destDocs -Recurse -Force -ErrorAction SilentlyContinue

    foreach ($rel in @(
            'reports\Chat reports',
            'reports\Feedback',
            'test_cases\Backend',
            'test_cases\Frontend',
            'config\swagger',
            'config\jira',
            'config\confluence'
        )) {
        New-Item -ItemType Directory -Path (Join-Path $TargetPath $rel) -Force | Out-Null
    }
    Write-Ok 'Created project skeleton folders'
    Save-WizardStep $TargetPath 1
    if (-not (Confirm-Next 'ask project name')) { Write-Host 'Stopped by user.'; exit 0 }
}

# ===== Phase 2 =====
$projectName = ''
$codeRoot = ''
if ($startStep -le 2 -and -not $script:QuitRequested) {
    Write-Step 'Phase 2 - Project name'
    $cfgPath = Join-Path $TargetPath '.cursor\project-config.json'
    $existingName = ''
    if (Test-Path $cfgPath) {
        try { $existingName = (Get-Content $cfgPath -Raw | ConvertFrom-Json).projectName } catch {}
    }
    $hint = if ($existingName) { " [$existingName]" } else { '' }
    $projectName = Read-Host "Project name$hint"
    if ([string]::IsNullOrWhiteSpace($projectName) -and $existingName) { $projectName = $existingName }
    while ([string]::IsNullOrWhiteSpace($projectName)) {
        $projectName = Read-Host 'Project name (required)'
    }
    $codeRootDefault = $projectName
    $codeRootIn = Read-Host "Code root folder name (repos will clone here) [$codeRootDefault]"
    $codeRoot = if ([string]::IsNullOrWhiteSpace($codeRootIn)) { $codeRootDefault } else { $codeRootIn.Trim() }

    $cfg = [ordered]@{
        toolkitVersion         = $toolkitVersion
        projectName            = $projectName
        codeRoot               = $codeRoot
        environments           = @()
        repos                  = @()
        integrations           = @{ jira = $true; confluence = $true; gitlab = $true; postgres = $true }
        protectedPaths         = @("$codeRoot/")
        installedAt            = (Get-Date).ToString('o')
        wizardLastCompletedStep = 2
    }
    $cfg | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $cfgPath -Encoding UTF8
    New-Item -ItemType Directory -Path (Join-Path $TargetPath $codeRoot) -Force | Out-Null
    Write-Ok "Wrote .cursor/project-config.json (projectName=$projectName, codeRoot=$codeRoot)"
    if (-not (Confirm-Next 'configure environments')) { Write-Host 'Stopped by user.'; exit 0 }
}

# ===== Phase 3 =====
if ($startStep -le 3 -and -not $script:QuitRequested) {
    Write-Step 'Phase 3 - Environments'
    $cfgPath = Join-Path $TargetPath '.cursor\project-config.json'
    $cfg = Get-Content $cfgPath -Raw | ConvertFrom-Json
    $countStr = Read-Host 'How many environments does this project have?'
    $count = 0
    [void][int]::TryParse($countStr, [ref]$count)
    while ($count -lt 1) {
        $countStr = Read-Host 'Enter a number >= 1'
        [void][int]::TryParse($countStr, [ref]$count)
    }
    $envs = @()
    for ($i = 1; $i -le $count; $i++) {
        $label = Read-Host "Environment $i name (e.g. staging, prod)"
        while ([string]::IsNullOrWhiteSpace($label)) { $label = Read-Host "Environment $i name (required)" }
        $id = (Sanitize-Id $label).ToLowerInvariant()
        $prodAns = Read-Host "Is '$label' a production-like environment? [y/N]"
        $isProd = ($prodAns -match '^[Yy]')
        $mcpName = "PostgreSQL$(Sanitize-Id $label)"
        $envs += [pscustomobject]@{
            id            = $id
            label         = $label.Trim()
            isProduction  = [bool]$isProd
            mcpServerName = $mcpName
        }
        Write-Ok "Env: id=$id label=$label mcp=$mcpName"
    }
    $cfg.environments = $envs
    $cfg.wizardLastCompletedStep = 3
    if (-not $cfg.codeRoot) { $cfg | Add-Member codeRoot $cfg.projectName -Force }
    $cfg | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $cfgPath -Encoding UTF8
    if (-not (Confirm-Next 'create .env template')) { Write-Host 'Stopped by user.'; exit 0 }
}

# ===== Phase 4 =====
$envPath = Join-Path $TargetPath '.env'
if ($startStep -le 4 -and -not $script:QuitRequested) {
    Write-Step 'Phase 4 - Write .env and .gitignore'
    $envExample = Join-Path $configDir 'env.example'
    if ((Test-Path $envPath) -and -not $ForceEnv) {
        Write-Warn '.env already exists (not overwritten). Use -ForceEnv to replace.'
    } else {
        Copy-Item -Path $envExample -Destination $envPath -Force
        Write-Ok "Created $envPath (empty values — fill tokens next)"
    }
    $giSrc = Join-Path $configDir 'gitignore.template'
    $giDst = Join-Path $TargetPath '.gitignore'
    if (-not (Test-Path $giDst)) {
        Copy-Item -Path $giSrc -Destination $giDst -Force
        Write-Ok 'Created .gitignore'
    } else {
        Write-Warn '.gitignore exists — left unchanged'
    }
    Save-WizardStep $TargetPath 4
    if (-not (Confirm-Next 'fill .env (HARD GATE before GitLab)')) { Write-Host 'Stopped by user.'; exit 0 }
}

# ===== Phase 5 HARD GATE =====
if ($startStep -le 5 -and -not $script:QuitRequested) {
    Write-Step 'Phase 5 - HARD GATE: fill .env before GitLab'
    Write-Host "  Open and fill: $envPath" -ForegroundColor Yellow
    Write-Host '  Required for clone: GITLAB_BASE_URL, GITLAB_TOKEN' -ForegroundColor Yellow
    Write-Host '  Recommended: JIRA_EMAIL, JIRA_API_TOKEN, JIRA_BASE_URL, CONFLUENCE_URL' -ForegroundColor Gray
    if ($SkipClone) {
        Write-Warn 'SkipClone set — GitLab keys not required this run'
    } else {
        while ($true) {
            Write-Host ""
            $mode = Read-Host 'Type OK after you saved .env with GitLab keys, or SKIP to skip clone'
            if ([string]::IsNullOrWhiteSpace($mode)) { continue }
            if ($mode -match '^[Ss]kip$') { $SkipClone = $true; break }
            if ($mode -notmatch '^[Oo][Kk]$') {
                Write-Warn 'Enter OK or SKIP'
                continue
            }
            $base = Get-EnvFromFile $envPath 'GITLAB_BASE_URL'
            $tok = Get-EnvFromFile $envPath 'GITLAB_TOKEN'
            if ($base -and $tok) {
                Write-Ok 'GITLAB_BASE_URL and GITLAB_TOKEN present'
                break
            }
            Write-Fail 'GITLAB_BASE_URL and/or GITLAB_TOKEN still empty — fill .env and try again'
        }
    }
    $jEmail = Get-EnvFromFile $envPath 'JIRA_EMAIL'
    $jTok = Get-EnvFromFile $envPath 'JIRA_API_TOKEN'
    if (-not $jEmail -or -not $jTok) {
        Write-Warn 'Jira credentials empty — REST fallback will not work until filled'
    }
    Save-WizardStep $TargetPath 5
    if ($SkipClone) {
        if (-not (Confirm-Next 'DB / MCP setup (clone skipped)')) { Write-Host 'Stopped by user.'; exit 0 }
    } else {
        if (-not (Confirm-Next 'GitLab list and clone')) { Write-Host 'Stopped by user.'; exit 0 }
    }
}

# ===== Phase 6 GitLab =====
if ($startStep -le 6 -and -not $script:QuitRequested -and -not $SkipClone) {
    Write-Step 'Phase 6 - GitLab: list and clone (dynamic selection)'
    $cfgPath = Join-Path $TargetPath '.cursor\project-config.json'
    $cfg = Get-Content $cfgPath -Raw | ConvertFrom-Json
    $codeRoot = [string]$cfg.codeRoot
    $listScript = Join-Path $TargetPath 'config\scripts\list-gitlab-projects.ps1'
    if (-not $GitLabGroup) {
        $GitLabGroup = Read-Host 'Optional GitLab group/path filter (blank = all membership projects)'
    }
    $search = Read-Host 'Optional search string (blank = none)'
    Write-Host '  Fetching projects from GitLab...' -ForegroundColor Gray
    $json = & $listScript -EnvFile $envPath -GroupPath $GitLabGroup -Search $search 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Fail "GitLab list failed: $json"
        $cont = Read-Host 'Continue without clone? [Y/n]'
        if ($cont -match '^[Nn]') { exit 1 }
    } else {
        $projects = @()
        try { $projects = @($json | ConvertFrom-Json) } catch { $projects = @() }
        if ($projects.Count -eq 0) {
            Write-Warn 'No projects returned'
        } else {
            Write-Host "  Found $($projects.Count) project(s):" -ForegroundColor Gray
            for ($i = 0; $i -lt $projects.Count; $i++) {
                Write-Host ("  [{0}] {1}" -f $i, $projects[$i].path_with_namespace)
            }
            $sel = Read-Host 'Enter indexes to clone (comma-separated), or ALL, or NONE'
            $toClone = @()
            if ($sel -match '^[Aa]ll$') { $toClone = $projects }
            elseif ($sel -match '^[Nn]one$' -or [string]::IsNullOrWhiteSpace($sel)) { $toClone = @() }
            else {
                foreach ($part in ($sel -split ',')) {
                    $idx = 0
                    if ([int]::TryParse($part.Trim(), [ref]$idx) -and $idx -ge 0 -and $idx -lt $projects.Count) {
                        $toClone += $projects[$idx]
                    }
                }
            }
            $cloneScript = Join-Path $TargetPath 'config\scripts\clone-gitlab-repos.ps1'
            $repoSnap = @()
            foreach ($p in $toClone) {
                $name = $p.name
                $dest = Join-Path (Join-Path $TargetPath $codeRoot) $name
                Write-Host "  Cloning $($p.path_with_namespace) -> $dest" -ForegroundColor Gray
                & $cloneScript -Url $p.http_url_to_repo -Destination $dest -EnvFile $envPath
                if ($LASTEXITCODE -eq 0) {
                    Write-Ok "Cloned $name"
                    $repoSnap += [pscustomobject]@{
                        name = $name
                        path = "$codeRoot/$name"
                        url  = $p.http_url_to_repo
                    }
                } else {
                    Write-Fail "Clone failed: $name"
                }
            }
            $cfg.repos = $repoSnap
            $cfg.wizardLastCompletedStep = 6
            $cfg | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $cfgPath -Encoding UTF8
            Write-Ok "Updated project-config repos snapshot ($($repoSnap.Count) repos). Re-run this phase later to add more."
        }
    }
    if (-not (Confirm-Next 'database MCP prompts')) { Write-Host 'Stopped by user.'; exit 0 }
} elseif ($SkipClone -and $startStep -le 6) {
    Write-Step 'Phase 6 - Skipped (SkipClone)'
    Save-WizardStep $TargetPath 6
    if (-not (Confirm-Next 'database MCP prompts')) { Write-Host 'Stopped by user.'; exit 0 }
}

# ===== Phase 7 DB =====
$dbServers = @{}
if ($startStep -le 7 -and -not $script:QuitRequested) {
    Write-Step 'Phase 7 - Database connection info for MCP'
    $cfgPath = Join-Path $TargetPath '.cursor\project-config.json'
    $cfg = Get-Content $cfgPath -Raw | ConvertFrom-Json
    if ($SkipDb) {
        Write-Warn 'SkipDb — no PostgreSQL servers will be added'
    } else {
        Write-Host '  Passwords will be stored in your user mcp.json (local). Do not commit that file.' -ForegroundColor Yellow
        foreach ($envItem in @($cfg.environments)) {
            Write-Host ""
            Write-Host "  Environment: $($envItem.label) (MCP name $($envItem.mcpServerName))" -ForegroundColor Cyan
            $skipOne = Read-Host '  Skip this environment DB? [y/N]'
            if ($skipOne -match '^[Yy]') { continue }
            $h = Read-Host '  Host'
            $portStr = Read-Host '  Port [5432]'
            $port = 5432
            if (-not [string]::IsNullOrWhiteSpace($portStr)) { [void][int]::TryParse($portStr, [ref]$port) }
            $db = Read-Host '  Database name'
            $user = Read-Host '  User'
            $pass = Read-Host '  Password'
            $enc = [uri]::EscapeDataString($pass)
            $conn = "postgres://${user}:${enc}@${h}:${port}/${db}"
            $dbServers[$envItem.mcpServerName] = @{
                command = 'npx'
                args    = @('-y', 'mcp-postgres-server')
                env     = @{
                    POSTGRES_CONNECTION_STRING = $conn
                    MCP_SERVER_NAME            = $envItem.mcpServerName
                    POSTGRES_HOST              = $h
                    POSTGRES_PORT              = "$port"
                    POSTGRES_DB                = $db
                    POSTGRES_USER              = $user
                    POSTGRES_PASSWORD          = $pass
                }
            }
            Write-Ok "Configured $($envItem.mcpServerName)"
        }
    }
    Save-WizardStep $TargetPath 7
    if (-not (Confirm-Next 'write MCP mcp.json')) { Write-Host 'Stopped by user.'; exit 0 }
}

# ===== Phase 8 MCP =====
if ($startStep -le 8 -and -not $script:QuitRequested) {
    Write-Step 'Phase 8 - Merge MCP into user mcp.json'
    $userMcp = Join-Path $env:USERPROFILE '.cursor\mcp.json'
    $mcpTemplatePath = Join-Path $configDir 'mcp.template.json'
    $raw = Get-Content $mcpTemplatePath -Raw -Encoding UTF8
    $template = $raw | ConvertFrom-Json
    $cursorDir = Split-Path -Parent $userMcp
    if (-not (Test-Path $cursorDir)) { New-Item -ItemType Directory -Path $cursorDir -Force | Out-Null }
    $existing = $null
    if (Test-Path $userMcp) {
        $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
        Copy-Item $userMcp (Join-Path $cursorDir "mcp.json.bak-$stamp") -Force
        Write-Ok "Backup mcp.json"
        try { $existing = Get-Content $userMcp -Raw | ConvertFrom-Json } catch { $existing = $null }
    }
    $serversHash = @{}
    if ($existing -and $existing.mcpServers) {
        $existing.mcpServers.PSObject.Properties | ForEach-Object { $serversHash[$_.Name] = $_.Value }
    }
    $template.mcpServers.PSObject.Properties | ForEach-Object { $serversHash[$_.Name] = $_.Value }
    foreach ($key in $dbServers.Keys) {
        $serversHash[$key] = [pscustomobject]$dbServers[$key]
    }
    $merged = [pscustomobject]@{ mcpServers = [pscustomobject]$serversHash }
    $json = $merged | ConvertTo-Json -Depth 20
    [System.IO.File]::WriteAllText($userMcp, $json, [System.Text.UTF8Encoding]::new($false))
    Write-Ok "Wrote $userMcp"
    Write-Host '  Next: restart Cursor and authenticate Atlassian MCP (Jira/Confluence). See docs/MCP_AUTH.md' -ForegroundColor Yellow
    Save-WizardStep $TargetPath 8
    if (-not (Confirm-Next 'install extensions')) { Write-Host 'Stopped by user.'; exit 0 }
}

# ===== Phase 9 extensions =====
if ($startStep -le 9 -and -not $script:QuitRequested) {
    Write-Step 'Phase 9 - Extensions'
    if ($SkipExtensions) {
        Write-Warn 'SkipExtensions'
    } else {
        $extPath = Join-Path $configDir 'extensions.json'
        $extConfig = Get-Content $extPath -Raw | ConvertFrom-Json
        Write-Host '  Manual installs:' -ForegroundColor Yellow
        foreach ($m in @($extConfig.manualInstall.runtime + $extConfig.manualInstall.extensions)) {
            Write-Host "    - $m" -ForegroundColor Gray
        }
        $cursorExe = Get-CursorCli
        if (-not $cursorExe) {
            Write-Fail 'Cursor CLI missing — install extensions manually'
        } else {
            foreach ($id in @($extConfig.scriptInstall)) {
                Write-Host "  Installing $id ..." -ForegroundColor Gray
                $out = & $cursorExe --install-extension $id 2>&1 | Out-String
                if ($LASTEXITCODE -eq 0 -or $out -match 'successfully installed|already installed') {
                    Write-Ok "Installed: $id"
                } else {
                    Write-Fail "Could not install: $id"
                }
            }
        }
        # workspace file
        $cfgPath = Join-Path $TargetPath '.cursor\project-config.json'
        $cfg = Get-Content $cfgPath -Raw | ConvertFrom-Json
        $folders = @(@{ path = '.'; name = [string]$cfg.projectName })
        foreach ($r in @($cfg.repos)) {
            if ($r.path) { $folders += @{ path = [string]$r.path; name = [string]$r.name } }
        }
        $ws = @{ folders = $folders; settings = @{} } | ConvertTo-Json -Depth 10
        $wsPath = Join-Path $TargetPath "$($cfg.projectName).code-workspace"
        [System.IO.File]::WriteAllText($wsPath, $ws, [System.Text.UTF8Encoding]::new($false))
        Write-Ok "Wrote $wsPath"
    }
    Save-WizardStep $TargetPath 9
    if (-not (Confirm-Next 'verify')) { Write-Host 'Stopped by user.'; exit 0 }
}

# ===== Phase 10 verify =====
Write-Step 'Phase 10 - Verify'
& (Join-Path $PSScriptRoot 'verify-qa-ai-toolkit.ps1') -TargetPath $TargetPath
$verifyCode = $LASTEXITCODE
Save-WizardStep $TargetPath 10

Write-Host ""
Write-Host 'Installer finished.' -ForegroundColor Cyan
Write-Host '  1. Restart Cursor / reload MCP' -ForegroundColor Gray
Write-Host '  2. Authenticate Atlassian MCP' -ForegroundColor Gray
Write-Host '  3. Re-check: install-qa-ai-toolkit.ps1 -VerifyOnly' -ForegroundColor Gray
Write-Host '  4. To clone more repos later: re-run and resume from step 6 (or -FromStep 6)' -ForegroundColor Gray

if ($script:HardFail) { exit 1 }
if ($verifyCode -ne 0) { exit $verifyCode }
if ($script:PartialFail) { exit 2 }
exit 0
