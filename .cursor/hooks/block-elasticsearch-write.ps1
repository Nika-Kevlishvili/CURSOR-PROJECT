# block-elasticsearch-write.ps1
# Hook: beforeShellExecution
# Purpose: Block Elasticsearch writes and the removed MCP server script.
#          Read-only Elastic CLI use through elastic-phoenix.py stays allowed.
# Fail-open on parse errors.

$jsonInput = [Console]::In.ReadToEnd()

function Write-Allow {
    @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
}

function Write-Deny {
    param([string]$AgentMessage)
    @{
        continue      = $false
        permission    = "deny"
        user_message  = "[HOOK BLOCKED] Elasticsearch writes and the Elasticsearch MCP server are disabled."
        agent_message = $AgentMessage
    } | ConvertTo-Json -Compress
}

try {
    if (-not $jsonInput) { Write-Allow; exit 0 }
    $payload = $jsonInput | ConvertFrom-Json
    $command = [string]$payload.command
    if (-not $command) { Write-Allow; exit 0 }
    $lower = $command.ToLowerInvariant()

    if ($lower -match "elasticsearch_mcp_server") {
        Write-Deny "BLOCK (ES.0): elasticsearch_mcp_server.py is removed. Use python3 Cursor-Project/scripts/elastic-phoenix.py. Read .cursor/skills/phoenix-elasticsearch/SKILL.md."
        exit 0
    }

    $isElasticCli = $lower -match '(^|[;&|]\s*|[/\\])elastic(\.cmd)?\s'
    $isWrite = $lower -match '\b(bulk|delete-by-query|update-by-query|reindex|put-mapping|put-script|delete-script)\b' -or $lower -match '\bes\s+(delete|update|index|create)\b'
    if ($isElasticCli -and $isWrite) {
        Write-Deny "BLOCK (ES.4a): Elasticsearch is read-only. Do not run elastic write commands (bulk, delete, update, reindex, index, create). Use elastic-phoenix.py search-logs, error-summary, search, count, sql, indices, mapping, field-values, or health."
        exit 0
    }

    Write-Allow
} catch {
    Write-Allow
    exit 0
}
