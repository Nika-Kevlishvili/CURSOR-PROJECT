# control-git-push.ps1
# Hook: beforeShellExecution
# Purpose: Block push to main and to GitLab. Ask before other git writes.

$jsonInput = [Console]::In.ReadToEnd()

function Get-HookValue($hookInput, [string]$name) {
    if ($hookInput.PSObject.Properties.Name -contains $name -and $hookInput.$name) {
        return [string]$hookInput.$name
    }
    if ($hookInput.tool_input -and ($hookInput.tool_input.PSObject.Properties.Name -contains $name)) {
        return [string]$hookInput.tool_input.$name
    }
    return ''
}

function Test-GitLabUrl([string]$url) {
    return ($url -match 'gitlab|git\.domain\.internal')
}

try {
    $hookInput = $jsonInput | ConvertFrom-Json
    $command = Get-HookValue $hookInput 'command'
    $cwd = Get-HookValue $hookInput 'cwd'
    if (-not $cwd) { $cwd = (Get-Location).Path }
    $commandLower = $command.ToLower()

    $isCommit = $commandLower -match '(^|[;&|]\s*)git\s+commit\b'
    $isPush = $commandLower -match '(^|[;&|]\s*)git\s+push\b'
    $isMerge = $commandLower -match '(^|[;&|]\s*)git\s+merge\b'
    $isRebase = $commandLower -match '(^|[;&|]\s*)git\s+rebase\b'
    $isGhWrite = $commandLower -match '(^|[;&|]\s*)gh\s+pr\s+(create|merge)\b'
    $requiresPermission = $isCommit -or $isPush -or $isMerge -or $isRebase -or $isGhWrite

    if (-not $requiresPermission) {
        @{ permission = "allow" } | ConvertTo-Json -Compress
        return
    }

    $denyReason = $null
    if ($isPush) {
        $targetsMain = $commandLower -match '(^|[;&|]\s*)git\s+push\b[^\n]*(\s+|:)(main|master)(\s|$)'
        if (-not $targetsMain) {
            $branch = ''
            try { $branch = (git -C $cwd rev-parse --abbrev-ref HEAD 2>$null).Trim() } catch { $branch = '' }
            $hasRefspec = $commandLower -match 'git\s+push\b\s+\S+\s+\S+'
            if (-not $hasRefspec -and ($branch -eq 'main' -or $branch -eq 'master')) {
                $targetsMain = $true
            }
        }
        if ($targetsMain -or (Test-GitLabUrl $command)) {
            $denyReason = 'push to main or GitLab'
        } else {
            $remoteName = 'origin'
            if ($command -match '(?i)git\s+push\s+(\S+)') {
                $candidate = $Matches[1]
                if ($candidate -notmatch '^-') { $remoteName = $candidate }
            }
            $remoteUrl = ''
            try { $remoteUrl = (git -C $cwd remote get-url $remoteName 2>$null) } catch { $remoteUrl = '' }
            if (Test-GitLabUrl $remoteUrl) {
                $denyReason = "push remote '$remoteName' is GitLab"
            }
        }
    }

    if ($denyReason) {
        @{
            permission = "deny"
            user_message = "[HOOK BLOCKED] $denyReason is not allowed. Command: $command"
            agent_message = "BLOCK: Do not push to main and do not push to GitLab. Open a pull request for main. Command: $command"
        } | ConvertTo-Json -Compress
        return
    }

    @{
        permission = "ask"
        user_message = "[PERMISSION REQUIRED] Git write operation detected: $command"
        agent_message = "Git write operation requires explicit user approval. Please wait for user approval."
    } | ConvertTo-Json -Compress
} catch {
    @{
        permission = "ask"
        user_message = "[PERMISSION REQUIRED] Error parsing git command. Approval required."
        agent_message = "Git hook could not parse the command. Wait for user approval."
    } | ConvertTo-Json -Compress
}
