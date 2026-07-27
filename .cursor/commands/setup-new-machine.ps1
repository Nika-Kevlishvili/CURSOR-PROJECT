# setup-new-machine.ps1
# Onboard a new developer machine: clone submodules, copy env files, write MCP config,
# install marketplace extensions, verify layout.
# Usage:
#   .\.cursor\commands\setup-new-machine.ps1
#   .\.cursor\commands\setup-new-machine.ps1 -VerifyOnly
#   .\.cursor\commands\setup-new-machine.ps1 -ForceEnv -ForceMcp
#   .\.cursor\commands\setup-new-machine.ps1 -SkipClone -SkipExtensions
#   .\.cursor\commands\setup-new-machine.ps1 -GitLabToken <token>

param(
    [switch]$VerifyOnly,
    [switch]$ForceEnv,
    [switch]$ForceMcp,
    [switch]$SkipClone,
    [switch]$SkipExtensions,
    [string]$GitLabToken = $env:GITLAB_TOKEN
)

$ErrorActionPreference = 'Continue'
$script:HardFail = $false
$script:PartialFail = $false
$script:VerifyResults = [System.Collections.Generic.List[object]]::new()

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$cursorProjectPath = Join-Path $workspaceRoot 'Cursor-Project'
$setupDir = Join-Path $cursorProjectPath 'Cursor Setup'
$gitmodulesPath = Join-Path $workspaceRoot '.gitmodules'
$mcpTemplatePath = Join-Path $setupDir 'mcp_content.txt'
$envExamplePath = Join-Path $setupDir 'env.example'
$extensionsJsonPath = Join-Path $setupDir 'extensions.json'
$userMcpPath = Join-Path $env:USERPROFILE '.cursor\mcp.json'
$envTargetProject = Join-Path $cursorProjectPath '.env'
$envTargetEnergo = Join-Path (Join-Path $cursorProjectPath 'EnergoTS') '.env'

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "=== $Message ===" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host "  OK   $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "  WARN $Message" -ForegroundColor Yellow
}

function Write-Fail {
    param([string]$Message, [switch]$Hard)
    Write-Host "  FAIL $Message" -ForegroundColor Red
    if ($Hard) { $script:HardFail = $true } else { $script:PartialFail = $true }
}

function Add-VerifyResult {
    param([string]$Name, [bool]$Ok, [string]$Detail = '')
    $script:VerifyResults.Add([pscustomobject]@{ Name = $Name; Ok = $Ok; Detail = $Detail })
    $suffix = if ($Detail) { " - $Detail" } else { '' }
    if ($Ok) { Write-Ok ($Name + $suffix) }
    else { Write-Fail ($Name + $suffix) }
}

function Get-GitmodulesEntries {
    if (-not (Test-Path $gitmodulesPath)) { return @() }
    $entries = @()
    $current = $null
    Get-Content -Path $gitmodulesPath | ForEach-Object {
        $line = $_.Trim()
        if ($line -match '^\[submodule') {
            if ($null -ne $current) { $entries += $current }
            $current = [ordered]@{ Path = ''; Url = '' }
        }
        elseif ($line -match '^path\s*=\s*(.+)$' -and $null -ne $current) {
            $current.Path = $Matches[1].Trim()
        }
        elseif ($line -match '^url\s*=\s*(.+)$' -and $null -ne $current) {
            $current.Url = $Matches[1].Trim()
        }
    }
    if ($null -ne $current) { $entries += $current }
    return $entries
}

function Test-IsGitRepo {
    param([string]$Path)
    if (-not (Test-Path $Path)) { return $false }
    $gitPath = Join-Path $Path '.git'
    return (Test-Path $gitPath)
}

function Test-RepoNonEmpty {
    param([string]$Path)
    if (-not (Test-Path $Path)) { return $false }
    $items = Get-ChildItem -Path $Path -Force -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -ne '.git' }
    return ($null -ne $items -and @($items).Count -gt 0)
}

function Get-OriginUrl {
    param([string]$Path)
    Push-Location $Path
    try {
        $url = git remote get-url origin 2>$null
        if ($LASTEXITCODE -ne 0) { return '' }
        return ($url | Out-String).Trim()
    }
    finally {
        Pop-Location
    }
}

function Get-AuthCloneUrl {
    param([string]$Url, [string]$Token)
    if ([string]::IsNullOrWhiteSpace($Token)) { return $Url }
    if ($Url -notmatch '^https://') { return $Url }
    if ($Url -match '@') { return $Url }
    return ($Url -replace '^https://', "https://oauth2:${Token}@")
}

function Invoke-CloneSubmodules {
    Write-Step 'Phase 1 - Clone / init submodules'
    $entries = Get-GitmodulesEntries
    if ($entries.Count -eq 0) {
        Write-Fail 'No submodule entries in .gitmodules' -Hard
        return
    }

    Push-Location $workspaceRoot
    try {
        foreach ($entry in $entries) {
            $relPath = $entry.Path
            $fullPath = Join-Path $workspaceRoot $relPath
            $url = Get-AuthCloneUrl -Url $entry.Url -Token $GitLabToken

            if ((Test-IsGitRepo $fullPath) -and (Test-RepoNonEmpty $fullPath)) {
                Write-Ok "Already cloned: $relPath"
                continue
            }

            Write-Host "  Cloning $relPath ..." -ForegroundColor Gray
            $parent = Split-Path -Parent $fullPath
            if (-not (Test-Path $parent)) {
                New-Item -ItemType Directory -Path $parent -Force | Out-Null
            }

            if (Test-Path $fullPath) {
                $existing = Get-ChildItem -Path $fullPath -Force -ErrorAction SilentlyContinue
                if ($null -eq $existing -or @($existing).Count -eq 0) {
                    Remove-Item -Path $fullPath -Force -Recurse -ErrorAction SilentlyContinue
                }
            }

            # Prefer submodule init for tracked paths; fall back to direct clone
            $subOut = git submodule update --init -- "$relPath" 2>&1 | Out-String
            if ($LASTEXITCODE -ne 0 -or -not (Test-IsGitRepo $fullPath)) {
                Write-Warn "submodule update failed for $relPath; trying git clone"
                if (Test-Path $fullPath) {
                    Remove-Item -Path $fullPath -Force -Recurse -ErrorAction SilentlyContinue
                }
                $cloneOut = git clone $url $fullPath 2>&1 | Out-String
                if ($LASTEXITCODE -ne 0 -or -not (Test-IsGitRepo $fullPath)) {
                    Write-Fail "Clone failed: $relPath - $cloneOut" -Hard
                    continue
                }
            }
            else {
                # If token provided and Phoenix remote still has no auth, set URL for future fetches
                if (-not [string]::IsNullOrWhiteSpace($GitLabToken) -and $entry.Url -match 'git\.domain\.internal') {
                    Push-Location $fullPath
                    try {
                        git remote set-url origin (Get-AuthCloneUrl -Url $entry.Url -Token $GitLabToken) 2>$null | Out-Null
                    }
                    finally { Pop-Location }
                }
            }

            if ($relPath -match 'EnergoTS') {
                Push-Location $fullPath
                try {
                    $branch = (git branch --show-current 2>$null | Out-String).Trim()
                    if ($branch -ne 'cursor') {
                        git fetch origin cursor 2>&1 | Out-Null
                        git checkout cursor 2>&1 | Out-Null
                        if ($LASTEXITCODE -eq 0) {
                            Write-Ok "EnergoTS checked out branch cursor"
                        }
                        else {
                            Write-Warn "EnergoTS could not checkout cursor (current: $branch)"
                        }
                    }
                    else {
                        Write-Ok 'EnergoTS already on cursor'
                    }
                }
                finally { Pop-Location }
            }

            if ((Test-IsGitRepo $fullPath) -and (Test-RepoNonEmpty $fullPath)) {
                Write-Ok "Cloned: $relPath"
            }
            else {
                Write-Fail "After clone still empty/invalid: $relPath" -Hard
            }

            if ($subOut -and $subOut.Trim().Length -gt 0 -and $subOut -match 'error|fatal|denied') {
                Write-Warn ($subOut.Trim().Substring(0, [Math]::Min(200, $subOut.Trim().Length)))
            }
        }
    }
    finally {
        Pop-Location
    }
}

function Invoke-CopyEnvFiles {
    Write-Step 'Phase 2 - Create .env from env.example'
    if (-not (Test-Path $envExamplePath)) {
        Write-Fail "Missing env.example at $envExamplePath" -Hard
        return
    }

    $targets = @(
        @{ Path = $envTargetProject; Label = 'Cursor-Project/.env' },
        @{ Path = $envTargetEnergo; Label = 'Cursor-Project/EnergoTS/.env' }
    )

    foreach ($t in $targets) {
        $parent = Split-Path -Parent $t.Path
        if (-not (Test-Path $parent)) {
            Write-Warn "Parent missing for $($t.Label) - skip (clone EnergoTS first if needed)"
            if ($t.Path -eq $envTargetEnergo) { $script:PartialFail = $true }
            continue
        }
        if ((Test-Path $t.Path) -and -not $ForceEnv) {
            Write-Warn "Exists (not overwritten): $($t.Label) - use -ForceEnv to replace"
            continue
        }
        Copy-Item -Path $envExamplePath -Destination $t.Path -Force
        Write-Ok "Created $($t.Label) from env.example"
    }

    Write-Host ""
    Write-Host "  Review and fill credentials if needed:" -ForegroundColor Yellow
    Write-Host "    - PORTAL_USER / PASSWORD / DEVAUTHAPI / TESTAUTHAPI" -ForegroundColor Gray
    Write-Host "    - JIRA_EMAIL / JIRA_API_TOKEN / JIRA_BASE_URL" -ForegroundColor Gray
    Write-Host "    - CONFLUENCE_URL / SLACK_BOT_TOKEN / CLIENT_*" -ForegroundColor Gray
    Write-Host "    Files: $envTargetProject" -ForegroundColor Gray
    Write-Host "           $envTargetEnergo" -ForegroundColor Gray
}

function Invoke-WriteMcpConfig {
    Write-Step 'Phase 3 - Write MCP config to user mcp.json'
    if (-not (Test-Path $mcpTemplatePath)) {
        Write-Fail "Missing MCP template: $mcpTemplatePath" -Hard
        return
    }

    $raw = Get-Content -Path $mcpTemplatePath -Raw -Encoding UTF8
    try {
        $template = $raw | ConvertFrom-Json
    }
    catch {
        Write-Fail "mcp_content.txt is not valid JSON: $_" -Hard
        return
    }
    if (-not $template.mcpServers) {
        Write-Fail 'mcp_content.txt missing mcpServers object' -Hard
        return
    }

    $cursorDir = Split-Path -Parent $userMcpPath
    if (-not (Test-Path $cursorDir)) {
        New-Item -ItemType Directory -Path $cursorDir -Force | Out-Null
    }

    $existing = $null
    if (Test-Path $userMcpPath) {
        $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
        $bak = Join-Path $cursorDir "mcp.json.bak-$stamp"
        Copy-Item -Path $userMcpPath -Destination $bak -Force
        Write-Ok "Backup: $bak"
        try {
            $existing = (Get-Content -Path $userMcpPath -Raw -Encoding UTF8) | ConvertFrom-Json
        }
        catch {
            Write-Warn "Existing mcp.json invalid JSON - will replace from template"
            $existing = $null
        }
    }

    if ($null -eq $existing) {
        $merged = $template
    }
    else {
        if (-not $existing.mcpServers) {
            $existing | Add-Member -NotePropertyName mcpServers -NotePropertyValue ([pscustomobject]@{}) -Force
        }
        $serversHash = @{}
        if ($existing.mcpServers) {
            $existing.mcpServers.PSObject.Properties | ForEach-Object {
                $serversHash[$_.Name] = $_.Value
            }
        }
        # Always apply template server keys (Confluence/Jira/PostgreSQL*) onto the user mcp.json.
        $template.mcpServers.PSObject.Properties | ForEach-Object {
            $serversHash[$_.Name] = $_.Value
        }
        if ($ForceMcp) {
            Write-Ok 'ForceMcp: template MCP server keys applied'
        }
        $mergedServers = [pscustomobject]$serversHash
        $merged = [pscustomobject]@{ mcpServers = $mergedServers }
        # Preserve other top-level keys from existing if any
        $existing.PSObject.Properties | Where-Object { $_.Name -ne 'mcpServers' } | ForEach-Object {
            $merged | Add-Member -NotePropertyName $_.Name -NotePropertyValue $_.Value -Force
        }
    }

    $json = $merged | ConvertTo-Json -Depth 20
    # PowerShell ConvertTo-Json can escape unicode; write UTF8 without BOM
    [System.IO.File]::WriteAllText($userMcpPath, $json, [System.Text.UTF8Encoding]::new($false))
    Write-Ok "Wrote $userMcpPath"
    Write-Host "  Restart Cursor (or reload MCP servers) so Confluence/Jira/PostgreSQL load." -ForegroundColor Yellow
}

function Get-CursorCli {
    $cmd = Get-Command cursor -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $candidates = @(
        (Join-Path $env:LOCALAPPDATA 'Programs\cursor\Cursor.exe'),
        (Join-Path $env:LOCALAPPDATA 'Programs\Cursor\Cursor.exe')
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { return $c }
    }
    return $null
}

function Get-InstalledExtensions {
    param([string]$CursorExe)
    $list = & $CursorExe --list-extensions 2>$null
    if (-not $list) { return @() }
    return @($list | ForEach-Object { $_.Trim().ToLowerInvariant() } | Where-Object { $_ })
}

function Invoke-InstallExtensions {
    Write-Step 'Phase 4 - Install marketplace extensions'
    if (-not (Test-Path $extensionsJsonPath)) {
        Write-Fail "Missing extensions.json: $extensionsJsonPath" -Hard
        return
    }

    $extConfig = Get-Content -Path $extensionsJsonPath -Raw -Encoding UTF8 | ConvertFrom-Json
    $toInstall = @($extConfig.scriptInstall)
    $manual = @($extConfig.manualInstall.extensions)

    Write-Host "  Manual (skipped by script):" -ForegroundColor Yellow
    foreach ($m in $manual) { Write-Host "    - $m" -ForegroundColor Gray }
    if ($extConfig.manualInstall.runtime) {
        foreach ($r in @($extConfig.manualInstall.runtime)) {
            Write-Host "    - $r" -ForegroundColor Gray
        }
    }

    $cursorExe = Get-CursorCli
    if (-not $cursorExe) {
        Write-Fail 'Cursor CLI not found (cursor). Install extensions manually or add Cursor to PATH.' -Hard
        return
    }

    $installed = Get-InstalledExtensions -CursorExe $cursorExe
    foreach ($id in $toInstall) {
        $idNorm = $id.ToLowerInvariant()
        if ($installed -contains $idNorm) {
            Write-Ok "Already installed: $id"
            continue
        }
        Write-Host "  Installing $id ..." -ForegroundColor Gray
        $out = & $cursorExe --install-extension $id 2>&1 | Out-String
        if ($LASTEXITCODE -eq 0 -or $out -match 'was successfully installed|already installed') {
            Write-Ok "Installed: $id"
        }
        else {
            Write-Fail "Could not install: $id - $($out.Trim().Substring(0, [Math]::Min(180, $out.Trim().Length)))"
        }
    }
}

function Invoke-Verify {
    Write-Step 'Phase 5 - Verify'
    $script:VerifyResults.Clear()

    # npm / npx
    $npmOk = $false
    $npxOk = $false
    try { $null = npm -v 2>$null; if ($LASTEXITCODE -eq 0) { $npmOk = $true } } catch {}
    try { $null = npx -v 2>$null; if ($LASTEXITCODE -eq 0) { $npxOk = $true } } catch {}
    Add-VerifyResult -Name 'npm' -Ok $npmOk -Detail $(if ($npmOk) { (npm -v 2>$null) } else { 'not in PATH - install Node.js LTS' })
    Add-VerifyResult -Name 'npx' -Ok $npxOk -Detail $(if ($npxOk) { 'OK' } else { 'not in PATH' })

    # Submodules
    $entries = Get-GitmodulesEntries
    foreach ($entry in $entries) {
        $fullPath = Join-Path $workspaceRoot $entry.Path
        $exists = Test-Path $fullPath
        $isGit = Test-IsGitRepo $fullPath
        $nonEmpty = Test-RepoNonEmpty $fullPath
        $ok = $exists -and $isGit -and $nonEmpty
        $detail = if (-not $exists) { 'missing path' }
        elseif (-not $isGit) { 'not a git repo' }
        elseif (-not $nonEmpty) { 'empty working tree' }
        else {
            $origin = Get-OriginUrl $fullPath
            if ($entry.Path -match 'EnergoTS') {
                if ($origin -notmatch 'github\.com') { $ok = $false; "origin not GitHub: $origin" }
                else {
                    Push-Location $fullPath
                    try {
                        $br = (git branch --show-current 2>$null | Out-String).Trim()
                        if ($br -ne 'cursor') { $ok = $false; "branch=$br (expected cursor), origin=$origin" }
                        else { "branch=cursor, origin ok" }
                    }
                    finally { Pop-Location }
                }
            }
            elseif ($entry.Path -match 'Phoenix') {
                $expectedHost = if ($entry.Url -match 'https?://([^/]+)/') { $Matches[1] } else { 'git.domain.internal' }
                if ($origin -notmatch [regex]::Escape($expectedHost) -and $origin -notmatch 'git\.domain\.internal') {
                    $ok = $false
                    "unexpected origin: $origin"
                }
                else { "origin ok" }
            }
            else { "origin=$origin" }
        }
        Add-VerifyResult -Name $entry.Path -Ok $ok -Detail $detail
        if (-not $ok) { $script:PartialFail = $true }
    }

    # Env files
    foreach ($pair in @(
            @{ Path = $envTargetProject; Name = 'Cursor-Project/.env' },
            @{ Path = $envTargetEnergo; Name = 'Cursor-Project/EnergoTS/.env' }
        )) {
        $ok = (Test-Path $pair.Path) -and ((Get-Item $pair.Path).Length -gt 0)
        Add-VerifyResult -Name $pair.Name -Ok $ok -Detail $(if ($ok) { 'present' } else { 'missing or empty' })
        if (-not $ok) { $script:PartialFail = $true }
    }

    # MCP
    $mcpOk = $false
    $mcpDetail = 'missing'
    if (Test-Path $userMcpPath) {
        try {
            $mcp = (Get-Content -Path $userMcpPath -Raw -Encoding UTF8) | ConvertFrom-Json
            $names = @($mcp.mcpServers.PSObject.Properties.Name)
            $required = @('Confluence', 'Jira', 'PostgreSQLTest', 'PostgreSQLDev', 'PostgreSQLDev2', 'PostgreSQLPreProd', 'PostgreSQLProd')
            $missing = @($required | Where-Object { $names -notcontains $_ })
            if ($missing.Count -eq 0) {
                $mcpOk = $true
                $mcpDetail = 'all required servers present'
            }
            else {
                $mcpDetail = "missing: $($missing -join ', ')"
            }
        }
        catch {
            $mcpDetail = "invalid JSON: $_"
        }
    }
    Add-VerifyResult -Name 'MCP mcp.json' -Ok $mcpOk -Detail $mcpDetail
    if (-not $mcpOk) { $script:PartialFail = $true }

    # Extensions
    $cursorExe = Get-CursorCli
    if ($cursorExe -and (Test-Path $extensionsJsonPath)) {
        $extConfig = Get-Content -Path $extensionsJsonPath -Raw -Encoding UTF8 | ConvertFrom-Json
        $installed = Get-InstalledExtensions -CursorExe $cursorExe
        $want = @($extConfig.scriptInstall) + @($extConfig.manualInstall.extensions)
        foreach ($id in $want) {
            $ok = $installed -contains $id.ToLowerInvariant()
            Add-VerifyResult -Name "ext:$id" -Ok $ok -Detail $(if ($ok) { 'installed' } else { 'NOT installed' })
            if (-not $ok) { $script:PartialFail = $true }
        }
    }
    else {
        Add-VerifyResult -Name 'extensions' -Ok $false -Detail 'Cursor CLI or extensions.json unavailable'
        $script:PartialFail = $true
    }

    Write-Host ""
    $pass = @($script:VerifyResults | Where-Object { $_.Ok }).Count
    $total = $script:VerifyResults.Count
    Write-Host "Verify summary: $pass / $total passed" -ForegroundColor $(if ($pass -eq $total) { 'Green' } else { 'Yellow' })
}

# ---------- main ----------
Write-Host ""
Write-Host "setup-new-machine.ps1" -ForegroundColor Cyan
Write-Host "Workspace: $workspaceRoot" -ForegroundColor Gray

if (-not (Test-Path $cursorProjectPath)) {
    Write-Fail "Cursor-Project not found at $cursorProjectPath" -Hard
    exit 1
}
if (-not (Test-Path $gitmodulesPath)) {
    Write-Fail ".gitmodules not found at $gitmodulesPath" -Hard
    exit 1
}
if (-not (Test-Path $mcpTemplatePath) -or -not (Test-Path $envExamplePath) -or -not (Test-Path $extensionsJsonPath)) {
    Write-Fail 'Missing Cursor Setup templates (mcp_content.txt / env.example / extensions.json)' -Hard
    exit 1
}

Write-Step 'Phase 0 - Preconditions'
Write-Ok "Workspace root: $workspaceRoot"
Write-Ok '.gitmodules present'
Write-Ok 'Cursor Setup templates present'
$npmCheck = $false
try { $null = npm -v 2>$null; if ($LASTEXITCODE -eq 0) { $npmCheck = $true } } catch {}
if ($npmCheck) { Write-Ok "npm: $(npm -v 2>$null)" }
else { Write-Warn 'npm not in PATH - install Node.js LTS manually (required for MCP npx)' }

if (-not $VerifyOnly) {
    if (-not $SkipClone) { Invoke-CloneSubmodules }
    else { Write-Step 'Phase 1 - Skipped (-SkipClone)' }

    Invoke-CopyEnvFiles
    Invoke-WriteMcpConfig

    if (-not $SkipExtensions) { Invoke-InstallExtensions }
    else { Write-Step 'Phase 4 - Skipped (-SkipExtensions)' }
}
else {
    Write-Host "VerifyOnly mode - skipping clone/env/mcp/extensions writes" -ForegroundColor Yellow
}

Invoke-Verify

Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Ensure manual installs: Node/npm, PowerShell ext, Playwright Modified (custom.playwright-custom)" -ForegroundColor Gray
Write-Host "  2. Restart Cursor and confirm MCP servers (Confluence, Jira, PostgreSQL*)" -ForegroundColor Gray
Write-Host "  3. Review Cursor-Project/.env and EnergoTS/.env" -ForegroundColor Gray
Write-Host "  4. Re-check anytime: .\.cursor\commands\setup-new-machine.ps1 -VerifyOnly" -ForegroundColor Gray
Write-Host ""

if ($script:HardFail) { exit 1 }
if ($script:PartialFail) { exit 2 }
exit 0
