# claude-hook.ps1
# Claude Code hook dispatcher for the guard scripts in this folder.
#
# The guard scripts (block-*.ps1, protect-*.ps1, ...) keep the simple stdin/stdout JSON
# contract they were written for:
#   stdin : { prompt } | { file_path } | { command, working_directory } | { tool_name, tool_input, namespace } | { response }
#   stdout: { continue, block, permission: allow|ask|deny, user_message, agent_message, additional_context }
# This dispatcher translates the Claude Code hook payload into that shape, runs every guard
# registered for the event in ONE pwsh process, and translates the combined verdict back into
# Claude Code hook output. Registered in .claude/settings.json.
#
# Usage (from settings.json):
#   pwsh -NoProfile -File "$CLAUDE_PROJECT_DIR/.claude/hooks/claude-hook.ps1" <Group>
# Groups: UserPromptSubmit | PreEdit | PostEdit | PreBash | PreMcp | Stop

param([Parameter(Mandatory = $true, Position = 0)][string]$Group)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$hooksDir = $PSScriptRoot

# Guard registry. Matcher = regex tested against the shell command (PreBash only).
# FailClosed = deny when the guard throws or returns nothing parseable.
$registry = @{
    UserPromptSubmit = @(
        @{ Script = 'block-jira-phoenix-delivery.ps1' },
        @{ Script = 'remind-test-case-env-first.ps1' },
        @{ Script = 'block-energots-branch-requests.ps1' }
    )
    PreEdit          = @(
        @{ Script = 'protect-phoenix-code.ps1'; FailClosed = $true },
        @{ Script = 'protect-energots-writes.ps1' }
    )
    PostEdit         = @(
        @{ Script = 'warn-phoenix-code-edit.ps1' }
    )
    PreMcp           = @(
        @{ Script = 'block-postgresql-mcp.ps1' },
        @{ Script = 'block-elasticsearch-mcp.ps1' },
        @{ Script = 'block-atlassian-mcp.ps1' },
        @{ Script = 'block-confluence-write.ps1' },
        @{ Script = 'control-database-write.ps1' },
        @{ Script = 'block-bugreview-unapproved-jira.ps1' }
    )
    PreBash          = @(
        # @{ Script = 'control-git-push.ps1' },   # disabled, same as the Cursor setup
        @{ Script = 'block-energots-branch-switch.ps1' },
        @{ Script = 'block-acli-rovo.ps1'; Matcher = 'acli|\btwg\b|teamwork-graph'; FailClosed = $true },
        @{ Script = 'block-elasticsearch-write.ps1'; Matcher = 'elastic|elasticsearch_mcp' },
        @{ Script = 'block-postgresql-write.ps1'; Matcher = 'psql|mcp-postgres' }
    )
    Stop             = @(
        @{ Script = 'validate-confidence-format.ps1' }
    )
}

function Write-Result {
    param($Object)
    if ($null -ne $Object) { [Console]::Out.WriteLine(($Object | ConvertTo-Json -Compress -Depth 10)) }
    exit 0
}

function Get-PendingPath {
    param($Payload)
    $root = $env:CLAUDE_PROJECT_DIR
    if (-not $root) { $root = Split-Path -Parent (Split-Path -Parent $hooksDir) }
    $sid = [string]$Payload.session_id
    if (-not $sid) { $sid = 'default' }
    $sid = $sid -replace '[^A-Za-z0-9_-]', '_'
    return (Join-Path (Join-Path (Join-Path $root '.claude') 'logs') "pending-hook-context-$sid.txt")
}

function Get-LastAssistantText {
    param($Payload)
    if ($Payload.last_assistant_message) { return [string]$Payload.last_assistant_message }
    $path = [string]$Payload.transcript_path
    if (-not $path -or -not (Test-Path -LiteralPath $path)) { return '' }
    $lines = @(Get-Content -LiteralPath $path -Tail 200 -Encoding UTF8)
    for ($i = $lines.Count - 1; $i -ge 0; $i--) {
        try { $entry = $lines[$i] | ConvertFrom-Json } catch { continue }
        if ($entry.type -ne 'assistant') { continue }
        $texts = @($entry.message.content | Where-Object { $_.type -eq 'text' } | ForEach-Object { $_.text })
        if ($texts.Count -gt 0) { return ($texts -join "`n") }
    }
    return ''
}

# Run one guard in-process: feed it $Json on stdin, return its parsed JSON reply and host text.
function Invoke-Guard {
    param([string]$Script, [string]$Json)
    $path = Join-Path $hooksDir $Script
    $oldIn = [Console]::In
    try {
        [Console]::SetIn([System.IO.StringReader]::new($Json))
        $emitted = @(& $path 6>&1)
    } finally {
        [Console]::SetIn($oldIn)
    }
    $reply = $null
    $hostText = @()
    foreach ($item in $emitted) {
        if ($item -is [System.Management.Automation.InformationRecord]) {
            $hostText += [string]$item.MessageData
            continue
        }
        $s = [string]$item
        if ($s.TrimStart().StartsWith('{')) {
            try { $reply = $s | ConvertFrom-Json } catch { }
        }
    }
    return @{ Reply = $reply; HostText = ($hostText -join "`n") }
}

$failClosedGroup = ($Group -eq 'PreEdit')
try {
    $raw = [Console]::In.ReadToEnd()
    if ([string]::IsNullOrWhiteSpace($raw)) { exit 0 }
    $payload = $raw | ConvertFrom-Json
    $guards = $registry[$Group]
    if (-not $guards) { exit 0 }

    # ---- Build the payload the guards expect --------------------------------------------
    $toolInput = $payload.tool_input
    $command = ''
    switch ($Group) {
        'UserPromptSubmit' { $guardPayload = @{ prompt = [string]$payload.prompt } }
        'PreEdit' {
            $fp = [string]$toolInput.file_path
            if (-not $fp) { $fp = [string]$toolInput.notebook_path }
            if (-not $fp) { exit 0 }
            $guardPayload = @{ file_path = $fp }
        }
        'PostEdit' {
            $fp = [string]$toolInput.file_path
            if (-not $fp) { $fp = [string]$toolInput.notebook_path }
            if (-not $fp) { exit 0 }
            $guardPayload = @{ file_path = $fp }
        }
        'PreBash' {
            $command = [string]$toolInput.command
            if (-not $command) { exit 0 }
            $guardPayload = @{ command = $command; working_directory = [string]$payload.cwd }
        }
        'PreMcp' {
            # Claude Code names MCP tools mcp__<server>__<tool>
            $full = [string]$payload.tool_name
            $parts = $full -split '__', 3
            $server = if ($parts.Count -ge 2) { $parts[1] } else { '' }
            $tool = if ($parts.Count -ge 3) { $parts[2] } else { $full }
            $guardPayload = @{ tool_name = $tool; server = $server; namespace = $server; claude_tool_name = $full; tool_input = $toolInput }
        }
        'Stop' {
            $text = Get-LastAssistantText -Payload $payload
            if (-not $text) { exit 0 }
            $guardPayload = @{ response = $text }
        }
        default { exit 0 }
    }
    $guardJson = $guardPayload | ConvertTo-Json -Compress -Depth 20

    # ---- Run the guards and combine verdicts (deny > ask > allow) ------------------------
    $decision = 'allow'
    $userMsgs = @(); $agentMsgs = @(); $contextMsgs = @(); $hostMsgs = @()
    foreach ($g in $guards) {
        if ($g.Matcher -and $command -notmatch $g.Matcher) { continue }
        $reply = $null
        try {
            $r = Invoke-Guard -Script $g.Script -Json $guardJson
            $reply = $r.Reply
            if ($r.HostText) { $hostMsgs += $r.HostText }
        } catch {
            if ($g.FailClosed) {
                $decision = 'deny'
                $userMsgs += "[HOOK ERROR] $($g.Script) failed; blocked for safety."
                $agentMsgs += "BLOCK: $($g.Script) threw: $_"
            }
            continue
        }
        if ($null -eq $reply) {
            if ($g.FailClosed -and $Group -ne 'PostEdit') {
                $decision = 'deny'
                $userMsgs += "[HOOK ERROR] $($g.Script) returned no verdict; blocked for safety."
            }
            continue
        }
        $verdict = 'allow'
        if ($reply.permission -eq 'deny' -or $reply.block -eq $true -or $reply.continue -eq $false) { $verdict = 'deny' }
        elseif ($reply.permission -eq 'ask') { $verdict = 'ask' }

        if ($verdict -eq 'deny') { $decision = 'deny' }
        elseif ($verdict -eq 'ask' -and $decision -ne 'deny') { $decision = 'ask' }

        if ($verdict -ne 'allow') {
            if ($reply.user_message) { $userMsgs += [string]$reply.user_message }
            if ($reply.agent_message) { $agentMsgs += [string]$reply.agent_message }
        } elseif ($Group -eq 'UserPromptSubmit' -and $reply.agent_message) {
            $contextMsgs += [string]$reply.agent_message          # non-blocking reminder
        }
        if ($reply.additional_context) { $contextMsgs += [string]$reply.additional_context }
    }

    # ---- Translate to Claude Code hook output --------------------------------------------
    switch ($Group) {
        'UserPromptSubmit' {
            if ($decision -eq 'deny') {
                Write-Result @{ decision = 'block'; reason = (($userMsgs + $agentMsgs) -join ' ') }
            }
            # deliver a format warning left by the Stop hook on the previous turn
            $pending = Get-PendingPath -Payload $payload
            if (Test-Path -LiteralPath $pending) {
                try {
                    $contextMsgs = @((Get-Content -LiteralPath $pending -Raw -Encoding UTF8).Trim()) + $contextMsgs
                    Remove-Item -LiteralPath $pending -Force
                } catch { }
            }
            if ($contextMsgs.Count -gt 0) {
                Write-Result @{ hookSpecificOutput = @{ hookEventName = 'UserPromptSubmit'; additionalContext = ($contextMsgs -join "`n") } }
            }
            exit 0
        }
        { $_ -in 'PreEdit', 'PreBash', 'PreMcp' } {
            if ($decision -eq 'allow') { exit 0 }   # no opinion: normal permission flow applies
            $out = @{
                hookSpecificOutput = @{
                    hookEventName            = 'PreToolUse'
                    permissionDecision       = $decision
                    permissionDecisionReason = (($userMsgs + $agentMsgs) -join ' ')
                }
            }
            Write-Result $out
        }
        'PostEdit' {
            if ($hostMsgs.Count -eq 0) { exit 0 }
            $text = $hostMsgs -join "`n"
            Write-Result @{ systemMessage = $text; hookSpecificOutput = @{ hookEventName = 'PostToolUse'; additionalContext = $text } }
        }
        'Stop' {
            if ($contextMsgs.Count -eq 0) { exit 0 }
            # Non-blocking, as before: show the warning now and hand it to the agent with the next prompt.
            $text = $contextMsgs -join "`n"
            try {
                $pending = Get-PendingPath -Payload $payload
                New-Item -ItemType Directory -Path (Split-Path -Parent $pending) -Force | Out-Null
                Set-Content -LiteralPath $pending -Value "Format warning for your previous response: $text" -Encoding UTF8
            } catch { }
            Write-Result @{ systemMessage = $text }
        }
    }
    exit 0
} catch {
    if ($failClosedGroup) {
        Write-Result @{
            hookSpecificOutput = @{
                hookEventName            = 'PreToolUse'
                permissionDecision       = 'deny'
                permissionDecisionReason = "[HOOK ERROR] claude-hook.ps1 ($Group) failed; edit blocked for safety. $_"
            }
        }
    }
    exit 0   # other groups fail open, like the individual guards
}
