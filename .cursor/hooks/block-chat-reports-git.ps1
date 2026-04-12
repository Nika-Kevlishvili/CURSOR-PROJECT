# block-chat-reports-git.ps1
# Hook: beforeShellExecution
# Purpose: Block git add/commit/push that would track Chat reports content (only .gitkeep allowed).

$jsonInput = [Console]::In.ReadToEnd()

function Test-ForbiddenChatReportPath {
    param([string]$Path)
    if ([string]::IsNullOrWhiteSpace($Path)) { return $false }
    $n = $Path -replace '\\', '/'
    if ($n -notmatch '(?i)reports/Chat reports/') { return $false }
    if ($n -match '(?i)\.gitkeep$') { return $false }
    return $true
}

function Get-GitRepoRoot {
    try {
        $out = git rev-parse --show-toplevel 2>$null
        if ($LASTEXITCODE -eq 0 -and $out) { return $out.Trim() }
    } catch { }
    return $null
}

function Get-StagedPaths {
    param([string]$RepoRoot)
    Push-Location $RepoRoot
    try {
        $raw = git diff --cached --name-only 2>$null
        if ($LASTEXITCODE -ne 0) { return @() }
        return @($raw -split "`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    } finally {
        Pop-Location
    }
}

function Test-StagedGitignoreWeakensChatReports {
    param([string]$RepoRoot)
    $staged = Get-StagedPaths -RepoRoot $RepoRoot
    if ($staged -notcontains 'Cursor-Project/.gitignore') { return $false }
    Push-Location $RepoRoot
    try {
        $diff = git diff --cached -- 'Cursor-Project/.gitignore' 2>$null
        if ($LASTEXITCODE -ne 0) { return $false }
        foreach ($line in ($diff -split "`n")) {
            if ($line -match '^\+' -and $line -match '(?i)chat reports' -and $line -match '(?i)\.md') {
                return $true
            }
        }
    } finally {
        Pop-Location
    }
    return $false
}

function Get-ChatReportsStatusPaths {
    param([string]$RepoRoot)
    Push-Location $RepoRoot
    try {
        $porcelain = git status --porcelain -- 'Cursor-Project/reports/Chat reports' 2>$null
        if (-not $porcelain) { return @() }
        $paths = New-Object System.Collections.Generic.List[string]
        foreach ($row in ($porcelain -split "`n")) {
            if ($row.Length -lt 4) { continue }
            $path = $null
            if ($row -match '^..\s+"(.+)"\s*$') {
                $path = $Matches[1]
            } elseif ($row -match '^..\s+(.+)$') {
                $path = $Matches[1].Trim()
            }
            if ([string]::IsNullOrWhiteSpace($path)) { continue }
            $paths.Add($path)
        }
        return $paths
    } finally {
        Pop-Location
    }
}

try {
    $input = $jsonInput | ConvertFrom-Json
    $command = $input.command
    if (-not $command) {
        @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
        exit 0
    }

    $cmd = $command.Trim()
    $cmdLower = $cmd.ToLower()

    if ($cmdLower -notmatch '^\s*git\s+(add|commit|push|rm)\s') {
        @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
        exit 0
    }

    $repoRoot = Get-GitRepoRoot
    if (-not $repoRoot) {
        @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
        exit 0
    }

    # Deny explicit paths on add/rm that target forbidden Chat report files
    if ($cmdLower -match 'git\s+(add|rm)\b') {
        $norm = $cmd -replace '\\', '/'
        if ($norm -match '(?i)reports/Chat reports/') {
            if ($norm -notmatch '(?i)\.gitkeep(["\s]|$)') {
                $response = @{
                    continue     = $false
                    block        = $true
                    permission   = "deny"
                    user_message = "[HOOK BLOCKED] Only .gitkeep may be tracked under reports/Chat reports/. Command blocked."
                    agent_message = "Do not git add/git rm Chat report files (.md, etc.). Use Feedback or HandsOff reports for committed reports."
                }
                $response | ConvertTo-Json -Compress
                exit 0
            }
        }
        # git add . / -A / --all could pick up local Chat report files
        $broadAdd = (
            ($cmdLower -match '\s-a(\s|$)') -or
            ($cmdLower -match '\s--all(\s|$)') -or
            ($cmdLower -match 'git\s+add\s+\.\s*$')
        )
        if ($cmdLower -match 'git\s+add\b' -and $broadAdd) {
            $candidates = Get-ChatReportsStatusPaths -RepoRoot $repoRoot
            foreach ($p in $candidates) {
                if (Test-ForbiddenChatReportPath -Path $p) {
                    $response = @{
                        continue     = $false
                        block        = $true
                        permission   = "deny"
                        user_message = "[HOOK BLOCKED] git add would include Chat report files under reports/Chat reports/. Remove or exclude them; only .gitkeep is allowed in Git."
                        agent_message = "Broad git add is blocked while non-.gitkeep files exist under Cursor-Project/reports/Chat reports/."
                    }
                    $response | ConvertTo-Json -Compress
                    exit 0
                }
            }
        }
    }

    if ($cmdLower -match 'git\s+commit' -or $cmdLower -match 'git\s+push') {
        foreach ($p in (Get-StagedPaths -RepoRoot $repoRoot)) {
            if (Test-ForbiddenChatReportPath -Path $p) {
                $response = @{
                    continue     = $false
                    block        = $true
                    permission   = "deny"
                    user_message = "[HOOK BLOCKED] commit/push contains staged files under reports/Chat reports/ (only .gitkeep allowed)."
                    agent_message = "Unstage Chat report paths or delete them from the index before committing."
                }
                $response | ConvertTo-Json -Compress
                exit 0
            }
        }
        if (Test-StagedGitignoreWeakensChatReports -RepoRoot $repoRoot) {
            $response = @{
                continue     = $false
                block        = $true
                permission   = "deny"
                user_message = "[HOOK BLOCKED] staged .gitignore change would allow tracking markdown under Chat reports."
                agent_message = "Revert the Chat reports *.md negation in Cursor-Project/.gitignore."
            }
            $response | ConvertTo-Json -Compress
            exit 0
        }
    }

    @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
    exit 0
} catch {
    @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
    exit 0
}
