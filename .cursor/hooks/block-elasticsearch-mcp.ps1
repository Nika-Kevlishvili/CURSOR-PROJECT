# block-elasticsearch-mcp.ps1
# Hook: beforeMCPExecution
# Purpose: Deny Elasticsearch MCP tools. Log search uses the official Elastic CLI
#          through Cursor-Project/scripts/elastic-phoenix.py (Rule ES.0).
# Fail-open on parse errors so a hook bug does not freeze unrelated MCP.

$jsonInput = [Console]::In.ReadToEnd()

function Write-HookAllow {
    @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
}

$deniedTools = @(
    "es_cluster_health", "es_search_logs", "es_error_summary", "es_list_indices",
    "es_index_mapping", "es_sql_query", "es_search", "es_count", "es_field_values"
)

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

    $names = @($toolName, $innerName) | Where-Object { $_ }
    $deny = $false
    foreach ($name in $names) {
        if ($deniedTools -contains $name) { $deny = $true }
    }
    if ($namespace -match "Elasticsearch") { $deny = $true }

    $sample = $jsonInput
    if ($sample.Length -gt 6000) { $sample = $sample.Substring(0, 6000) }
    if (-not $deny -and $sample -match '"namespace"\s*:\s*"[^"]*Elasticsearch[^"]*"') {
        $deny = $true
    }
    if (-not $deny -and $sample -match "elasticsearch_mcp_server") {
        $deny = $true
    }

    if ($deny) {
        @{
            continue      = $true
            permission    = "deny"
            user_message  = "[HOOK BLOCKED] Elasticsearch MCP is disabled. Log search uses the official Elastic CLI."
            agent_message = "BLOCK (ES.0): Do not call Elasticsearch MCP (ElasticsearchDev, ElasticsearchTest, ElasticsearchTest2ES, ElasticsearchTest2SLR, ElasticsearchProd, or es_search_logs). Run: python3 Cursor-Project/scripts/elastic-phoenix.py search-logs --environment <dev|dev2|test|preprod|test2es|test2slr|prod> --search-text `"<terms>`" --level ERROR --days-back 7. See .cursor/skills/phoenix-elasticsearch/SKILL.md."
        } | ConvertTo-Json -Compress
        exit 0
    }

    Write-HookAllow
} catch {
    Write-HookAllow
    exit 0
}
