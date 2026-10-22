# control-database-write.ps1
# Hook: beforeMCPExecution
# Purpose: Second deny for removed PostgreSQL MCP write tools.
#          Writes go through psql-phoenix.py --allow-write (block-postgresql-write.ps1).
# Fail-open on parse errors.

$jsonInput = [Console]::In.ReadToEnd()

function Write-HookAllow {
    @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
}

try {
    if (-not $jsonInput) {
        Write-HookAllow
        exit 0
    }

    $payload = $jsonInput | ConvertFrom-Json
    $toolName = [string]$payload.tool_name
    if (-not $toolName) { $toolName = [string]$payload.toolName }
    $names = @($toolName)
    $toolInput = $payload.tool_input
    if (-not $toolInput) { $toolInput = $payload.toolInput }
    if ($toolName -eq "CallDynamicTool" -and $toolInput) {
        if ($toolInput.toolName) { $names += [string]$toolInput.toolName }
        if ($toolInput.namespace) { $names += [string]$toolInput.namespace }
    }

    $deny = $false
    foreach ($name in $names) {
        if ($name -match "PostgreSQL") { $deny = $true }
    }
    if (-not $deny -and $jsonInput -match "mcp_PostgreSQL") { $deny = $true }

    if ($deny) {
        @{
            continue      = $true
            permission    = "deny"
            user_message  = "[HOOK BLOCKED] PostgreSQL MCP writes are disabled."
            agent_message = "BLOCK (DB.0): PostgreSQL MCP is removed. Use python3 Cursor-Project/scripts/psql-phoenix.py. Read-only queries use query. Writes use execute --allow-write and are refused on prod, es, and slr."
        } | ConvertTo-Json -Compress
        exit 0
    }

    Write-HookAllow
} catch {
    Write-HookAllow
    exit 0
}
