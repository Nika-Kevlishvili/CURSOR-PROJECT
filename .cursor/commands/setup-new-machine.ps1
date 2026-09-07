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
$workspaceMcpPath = Join-Path $workspaceRoot '.cursor\mcp.json'
$userMcpPath = Join-Path $env:USERPROFILE '.cursor\mcp.json'
$esScriptPath = Join-Path $cursorProjectPath 'scripts\elasticsearch_mcp_server.py'
$esScriptRelativePath = 'Cursor-Project/scripts/elasticsearch_mcp_server.py'
$esRequirementsPath = Join-Path $cursorProjectPath 'scripts\elasticsearch-requirements.txt'
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

function Test-PythonHasEsDeps {
    param([string]$PythonExe)
    if ([string]::IsNullOrWhiteSpace($PythonExe) -or -not (Test-Path $PythonExe)) { return $false }
    & $PythonExe -c "import requests, mcp; from mcp.server.fastmcp import FastMCP" 2>$null | Out-Null
    return $LASTEXITCODE -eq 0
}

function Get-PythonExecutableCandidates {
    $candidates = [System.Collections.Generic.List[string]]::new()

    $pyLauncher = Get-Command py -ErrorAction SilentlyContinue
    if ($pyLauncher) {
        $resolved = (& py -3 -c "import sys; print(sys.executable)" 2>$null | Out-String).Trim()
        if ($LASTEXITCODE -eq 0 -and $resolved) { $candidates.Add($resolved) }
    }

    $pythonRoot = Join-Path $env:LOCALAPPDATA 'Programs\Python'
    if (Test-Path $pythonRoot) {
        Get-ChildItem -Path $pythonRoot -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending |
            ForEach-Object {
                $exe = Join-Path $_.FullName 'python.exe'
                if (Test-Path $exe) { $candidates.Add($exe) }
            }
    }

    foreach ($name in @('python', 'python3')) {
        $cmd = Get-Command $name -ErrorAction SilentlyContinue
        if ($cmd -and $cmd.Source -notmatch 'WindowsApps') {
            $candidates.Add($cmd.Source)
        }
    }

    $unique = @()
    $seen = @{}
    foreach ($exe in $candidates) {
        $norm = $exe.ToLowerInvariant()
        if ($seen[$norm]) { continue }
        $seen[$norm] = $true
        if (Test-Path $exe) { $unique += $exe }
    }
    return $unique
}

function Get-ElasticsearchPython {
    foreach ($exe in (Get-PythonExecutableCandidates)) {
        if (Test-PythonHasEsDeps -PythonExe $exe) { return $exe }
    }
    return $null
}

function Install-ElasticsearchPythonDeps {
    param([string]$RequirementsPath)

    foreach ($exe in (Get-PythonExecutableCandidates)) {
        Write-Host "  Installing Elasticsearch MCP Python deps via $exe ..." -ForegroundColor Gray
        & $exe -m pip install --upgrade pip 2>&1 | Out-Null
        & $exe -m pip install -r $RequirementsPath 2>&1 | Out-String | Out-Null
        if ($LASTEXITCODE -eq 0 -and (Test-PythonHasEsDeps -PythonExe $exe)) {
            return $exe
        }
    }
    return $null
}

function Set-ElasticsearchMcpServerEntries {
    param(
        $McpServersObj,
        [string]$PythonExe,
        [string]$ScriptPath
    )

    foreach ($name in @('ElasticsearchDev', 'ElasticsearchExperiment', 'ElasticsearchTest', 'ElasticsearchTest2ES', 'ElasticsearchTest2SLR', 'ElasticsearchProd')) {
        if (-not $McpServersObj.$name) { continue }
        $McpServersObj.$name.command = $PythonExe
        if ($McpServersObj.$name.args -is [System.Array] -and $McpServersObj.$name.args.Count -gt 0) {
            $McpServersObj.$name.args[0] = $ScriptPath
        }
        else {
            $McpServersObj.$name.args = @($ScriptPath)
        }
    }
}

function Invoke-SetupElasticsearchMcp {
    Write-Step 'Phase 3a - Elasticsearch MCP Python setup'

    if (-not (Test-Path $esScriptPath)) {
        Write-Fail "Missing Elasticsearch MCP script: $esScriptPath" -Hard
        return @{ Ok = $false; Python = ''; ScriptPath = '' }
    }
    if (-not (Test-Path $esRequirementsPath)) {
        Write-Fail "Missing Elasticsearch requirements: $esRequirementsPath" -Hard
        return @{ Ok = $false; Python = ''; ScriptPath = '' }
    }

    $python = Get-ElasticsearchPython
    if ($python) {
        Write-Ok "Python ready for Elasticsearch MCP: $python"
    }
    else {
        Write-Warn 'Python with mcp/requests not found - installing dependencies'
        $python = Install-ElasticsearchPythonDeps -RequirementsPath $esRequirementsPath
        if ($python) {
            Write-Ok "Installed Elasticsearch MCP deps with: $python"
        }
        else {
            Write-Fail 'Could not find Python or install mcp/requests/urllib3. Install Python 3.11+ and re-run.' -Hard
            return @{ Ok = $false; Python = ''; ScriptPath = $esScriptPath }
        }
    }

    return @{
        Ok         = $true
        Python     = $python
        ScriptPath = $esScriptRelativePath
    }
}

function Write-PrettyJsonFile {
    param(
        [string]$Path,
        [object]$Object,
        [string]$PythonExe = 'python'
    )

    $jsonRaw = $Object | ConvertTo-Json -Depth 20
    $jsonRaw = $jsonRaw -replace '\\u0026', '&'
    $tmpIn  = [System.IO.Path]::GetTempFileName()
    $tmpOut = [System.IO.Path]::GetTempFileName()
    try {
        [System.IO.File]::WriteAllText($tmpIn, $jsonRaw, [System.Text.UTF8Encoding]::new($false))
        & $PythonExe -c "import json,sys; d=json.load(open(sys.argv[1])); open(sys.argv[2],'w',encoding='utf-8').write(json.dumps(d,indent=2,ensure_ascii=False))" $tmpIn $tmpOut 2>$null
        if ($LASTEXITCODE -eq 0 -and (Test-Path $tmpOut) -and (Get-Item $tmpOut).Length -gt 2) {
            $json = [System.IO.File]::ReadAllText($tmpOut)
        } else {
            $json = $jsonRaw
        }
    } catch {
        $json = $jsonRaw
    } finally {
        Remove-Item $tmpIn, $tmpOut -Force -ErrorAction SilentlyContinue
    }

    $parent = Split-Path -Parent $Path
    if (-not (Test-Path $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
    [System.IO.File]::WriteAllText($Path, $json, [System.Text.UTF8Encoding]::new($false))
}

function Clear-UserLevelMcpConfig {
    if (-not (Test-Path $userMcpPath)) {
        Write-Ok 'No user-level mcp.json (expected)'
        return
    }

    $knownServers = @(
        'Confluence', 'Jira',
        'PostgreSQLTest', 'PostgreSQLDev', 'PostgreSQLDev2', 'PostgreSQLPreProd', 'PostgreSQLProd',
        'ElasticsearchDev', 'ElasticsearchExperiment', 'ElasticsearchTest', 'ElasticsearchTest2ES', 'ElasticsearchTest2SLR', 'ElasticsearchProd'
    )

    try {
        $userMcp = (Get-Content -Path $userMcpPath -Raw -Encoding UTF8) | ConvertFrom-Json
        $names = @()
        if ($userMcp.mcpServers) {
            $names = @($userMcp.mcpServers.PSObject.Properties.Name)
        }
        $overlap = @($names | Where-Object { $knownServers -contains $_ })
        if ($overlap.Count -eq 0 -and $names.Count -eq 0) {
            Write-Ok 'User-level mcp.json already empty'
            return
        }
    }
    catch {
        Write-Warn "User-level mcp.json invalid JSON - leaving as-is: $_"
        return
    }

    $cursorDir = Split-Path -Parent $userMcpPath
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $bak = Join-Path $cursorDir "mcp.json.bak-$stamp"
    Copy-Item -Path $userMcpPath -Destination $bak -Force
    [System.IO.File]::WriteAllText($userMcpPath, "{`"mcpServers`":{}}`n", [System.Text.UTF8Encoding]::new($false))
    Write-Ok "Cleared user-level mcp.json (backup: $bak)"
    Write-Host "  MCP config lives only in $workspaceMcpPath" -ForegroundColor Yellow
}

function Invoke-WriteMcpConfig {
    Write-Step 'Phase 3 - Write MCP config to workspace .cursor/mcp.json'
    if (-not (Test-Path $mcpTemplatePath)) {
        Write-Fail "Missing MCP template: $mcpTemplatePath" -Hard
        return
    }

    $esSetup = Invoke-SetupElasticsearchMcp
    if (-not $esSetup.Ok) {
        Write-Warn 'Continuing MCP merge without Elasticsearch entries (setup failed)'
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

    if ($esSetup.Ok) {
        Set-ElasticsearchMcpServerEntries -McpServersObj $template.mcpServers -PythonExe $esSetup.Python -ScriptPath $esSetup.ScriptPath
    }
    else {
        foreach ($name in @('ElasticsearchDev', 'ElasticsearchExperiment', 'ElasticsearchTest', 'ElasticsearchTest2ES', 'ElasticsearchTest2SLR', 'ElasticsearchProd')) {
            if ($template.mcpServers.$name) {
                $template.mcpServers.PSObject.Properties.Remove($name)
            }
        }
    }

    $existing = $null
    if (Test-Path $workspaceMcpPath) {
        $wsDir = Split-Path -Parent $workspaceMcpPath
        $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
        $bak = Join-Path $wsDir "mcp.json.bak-$stamp"
        Copy-Item -Path $workspaceMcpPath -Destination $bak -Force
        Write-Ok "Backup: $bak"
        try {
            $existing = (Get-Content -Path $workspaceMcpPath -Raw -Encoding UTF8) | ConvertFrom-Json
        }
        catch {
            Write-Warn "Existing workspace mcp.json invalid JSON - will replace from template"
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
        $template.mcpServers.PSObject.Properties | ForEach-Object {
            $serversHash[$_.Name] = $_.Value
        }
        if ($ForceMcp) {
            Write-Ok 'ForceMcp: template MCP server keys applied'
        }
        $mergedServers = [pscustomobject]$serversHash
        $merged = [pscustomobject]@{ mcpServers = $mergedServers }
        $existing.PSObject.Properties | Where-Object { $_.Name -ne 'mcpServers' } | ForEach-Object {
            $merged | Add-Member -NotePropertyName $_.Name -NotePropertyValue $_.Value -Force
        }
    }

    $pyExe = if ($esSetup.Ok -and $esSetup.Python) { $esSetup.Python } else { 'python' }
    Write-PrettyJsonFile -Path $workspaceMcpPath -Object $merged -PythonExe $pyExe
    Write-Ok "Wrote $workspaceMcpPath"

    Clear-UserLevelMcpConfig

    Write-Host "  Restart Cursor (or reload MCP servers) so Confluence/Jira/PostgreSQL/Elasticsearch load." -ForegroundColor Yellow
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

    # MCP (workspace only)
    $mcpOk = $false
    $mcpDetail = 'missing'
    if (Test-Path $workspaceMcpPath) {
        try {
            $mcp = (Get-Content -Path $workspaceMcpPath -Raw -Encoding UTF8) | ConvertFrom-Json
            $names = @($mcp.mcpServers.PSObject.Properties.Name)
            $required = @(
                'Confluence', 'Jira',
                'PostgreSQLTest', 'PostgreSQLDev', 'PostgreSQLDev2', 'PostgreSQLPreProd', 'PostgreSQLProd',
                'ElasticsearchDev', 'ElasticsearchExperiment', 'ElasticsearchTest', 'ElasticsearchTest2ES', 'ElasticsearchTest2SLR', 'ElasticsearchProd'
            )
            $missing = @($required | Where-Object { $names -notcontains $_ })
            if ($missing.Count -eq 0) {
                $mcpOk = $true
                $mcpDetail = 'all required servers present in workspace .cursor/mcp.json'
            }
            else {
                $mcpDetail = "missing: $($missing -join ', ')"
            }
        }
        catch {
            $mcpDetail = "invalid JSON: $_"
        }
    }
    Add-VerifyResult -Name 'workspace .cursor/mcp.json' -Ok $mcpOk -Detail $mcpDetail
    if (-not $mcpOk) { $script:PartialFail = $true }

    $userMcpOk = $true
    $userMcpDetail = 'not present (expected)'
    if (Test-Path $userMcpPath) {
        try {
            $userMcp = (Get-Content -Path $userMcpPath -Raw -Encoding UTF8) | ConvertFrom-Json
            $userNames = @()
            if ($userMcp.mcpServers) {
                $userNames = @(
                    $userMcp.mcpServers.PSObject.Properties |
                        Where-Object { -not [string]::IsNullOrWhiteSpace($_.Name) } |
                        ForEach-Object { $_.Name }
                )
            }
            if ($userNames.Count -gt 0) {
                $userMcpOk = $false
                $userMcpDetail = "duplicate risk: $($userNames -join ', ') in user mcp.json - run setup to clear"
            }
            else {
                $userMcpDetail = 'empty (expected)'
            }
        }
        catch {
            $userMcpOk = $false
            $userMcpDetail = 'invalid JSON in user mcp.json'
        }
    }
    Add-VerifyResult -Name 'user mcp.json' -Ok $userMcpOk -Detail $userMcpDetail
    if (-not $userMcpOk) { $script:PartialFail = $true }

    # Elasticsearch Python + script
    $esScriptOk = Test-Path $esScriptPath
    Add-VerifyResult -Name 'Elasticsearch MCP script' -Ok $esScriptOk -Detail $(if ($esScriptOk) { 'present' } else { 'missing' })
    if (-not $esScriptOk) { $script:PartialFail = $true }

    $esPython = Get-ElasticsearchPython
    $esPythonDetail = if ($esPython) { $esPython } else { 'mcp/requests not importable - re-run setup or pip install -r Cursor-Project/scripts/elasticsearch-requirements.txt' }
    Add-VerifyResult -Name 'Elasticsearch Python deps' -Ok ([bool]$esPython) -Detail $esPythonDetail
    if (-not $esPython) { $script:PartialFail = $true }

    if ($esPython -and (Test-Path $workspaceMcpPath)) {
        try {
            $mcpCheck = (Get-Content -Path $workspaceMcpPath -Raw -Encoding UTF8) | ConvertFrom-Json
            $esCmd = $mcpCheck.mcpServers.ElasticsearchDev.command
            $esCmdOk = (-not [string]::IsNullOrWhiteSpace($esCmd)) -and (Test-Path $esCmd)
            Add-VerifyResult -Name 'Elasticsearch MCP python path' -Ok $esCmdOk -Detail $(if ($esCmdOk) { $esCmd } else { if ($esCmd) { "command=$esCmd (not found)" } else { 'command missing - run setup (not -VerifyOnly)' } })
            if (-not $esCmdOk) { $script:PartialFail = $true }
        }
        catch {
            Add-VerifyResult -Name 'Elasticsearch MCP python path' -Ok $false -Detail 'could not read workspace mcp.json'
            $script:PartialFail = $true
        }
    }

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

$pyCheck = Get-ElasticsearchPython
if ($pyCheck) { Write-Ok "Python (Elasticsearch MCP): $pyCheck" }
else { Write-Warn 'Python with mcp/requests not ready - script will try pip install in Phase 3a' }

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
Write-Host "  2. Restart Cursor and confirm workspace MCP in .cursor/mcp.json" -ForegroundColor Gray
Write-Host "  3. Review Cursor-Project/.env and EnergoTS/.env" -ForegroundColor Gray
Write-Host "  4. Re-check anytime: .\.cursor\commands\setup-new-machine.ps1 -VerifyOnly" -ForegroundColor Gray
Write-Host ""

if ($script:HardFail) { exit 1 }
if ($script:PartialFail) { exit 2 }
exit 0
