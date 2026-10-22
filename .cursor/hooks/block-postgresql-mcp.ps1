# block-postgresql-mcp.ps1
# Hook: beforeMCPExecution
# Purpose: Deny PostgreSQL MCP tools. Queries use the official psql client
#          through Cursor-Project/scripts/psql-phoenix.py (Rule DB.0).
# Fail-open on parse errors so a hook bug does not freeze unrelated MCP.

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

    $innerName = $null
    $namespace = $null
    $toolInput = $payload.tool_input
    if (-not $toolInput) { $toolInput = $payload.toolInput }
    if (-not $toolInput) { $toolInput = $payload.arguments }
    if ($toolName -eq "CallDynamicTool" -and $toolInput) {
        $innerName = $toolInput.toolName
        if (-not $innerName) { $innerName = $toolInput.tool_name }
        $namespace = $toolInput.namespace
        if (-not $namespace) { $namespace = $toolInput.server }
        if ($toolInput.arguments) {
            if (-not $innerName) { $innerName = $toolInput.arguments.toolName }
            if (-not $namespace) { $namespace = $toolInput.arguments.namespace }
        }
    }
    if (-not $namespace) { $namespace = [string]$payload.server }
    if (-not $namespace) { $namespace = [string]$payload.namespace }

    $deny = $false
    foreach ($name in @($toolName, $innerName, $namespace)) {
        if ($name -and [string]$name -match "PostgreSQL") { $deny = $true }
    }

    $sample = $jsonInput
    if ($sample.Length -gt 6000) { $sample = $sample.Substring(0, 6000) }
    if (-not $deny -and $sample -match '"namespace"\s*:\s*"[^"]*PostgreSQL[^"]*"') {
        $deny = $true
    }
    if (-not $deny -and $sample -match "mcp-postgres-server") {
        $deny = $true
    }

    if ($deny) {
        @{
            continue      = $true
            permission    = "deny"
            user_message  = "[HOOK BLOCKED] PostgreSQL MCP is disabled. Queries use the official psql client."
            agent_message = "BLOCK (DB.0): Do not call PostgreSQL MCP (PostgreSQLDev, PostgreSQLDev2, PostgreSQLTest, PostgreSQLPreProd, PostgreSQLProd, PostgreSQLexperiments, PostgreSQLes, PostgreSQLSLR, connect_db, query, execute). Run: python3 Cursor-Project/scripts/psql-phoenix.py query --environment <dev|dev2|test|preprod|prod|experiments|es|slr> --sql `"SELECT ...`". See .cursor/skills/phoenix-database/SKILL.md."
        } | ConvertTo-Json -Compress
        exit 0
    }

    Write-HookAllow
} catch {
    Write-HookAllow
    exit 0
}
