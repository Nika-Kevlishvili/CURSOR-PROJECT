# confirm-database-environment.ps1
# Hook: beforeMCPExecution
# Rule DB.0a — DENY PostgreSQL MCP unless user named env in this session (.cursor/.db-env-session.json)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/lib/db-env-session.ps1"

$jsonInput = [Console]::In.ReadToEnd()

try {
    $input = $jsonInput | ConvertFrom-Json
    $toolName = [string]$input.tool_name
    $server = [string]$input.server
    $combined = "$server $toolName".Trim()

    $postgresTools = @('query', 'connect_db', 'execute', 'describe_table', 'list_tables', 'list_schemas')
    $isPostgresMcp = $false
    if ($combined -match 'postgresql|postgres') { $isPostgresMcp = $true }
    foreach ($t in $postgresTools) {
        if ($toolName -match "\b$([regex]::Escape($t))\b") { $isPostgresMcp = $true; break }
    }

    if (-not $isPostgresMcp) {
        @{ continue = $true; permission = 'allow' } | ConvertTo-Json -Compress
        exit 0
    }

    $mcpCanonical = Get-CanonicalDbEnvironmentFromMcpHaystack -Haystack $combined
    $mcpLabel = Get-DbEnvironmentDisplayLabel -Canonical $mcpCanonical
    if (-not $mcpLabel) { $mcpLabel = 'UNKNOWN' }

    $session = Get-DbEnvSession
    if (-not $session) {
        @{
            continue      = $true
            permission    = 'deny'
            user_message  = "[DB MCP BLOCKED - Rule DB.0a] PostgreSQL ($mcpLabel) is not allowed until you name the environment in chat (Dev, Dev2, Test, PreProd, Prod, or Experiments) in the same message as your data question. Approving this MCP call will not help — resubmit your prompt with the environment."
            agent_message = "CRITICAL (Rule DB.0a): PostgreSQL MCP denied — no .cursor/.db-env-session.json from a user prompt with a named environment. Ask the user which environment. NEVER infer Dev from PDT-2529 / EnergoTS default reminder 2163 or similar test fixtures."
        } | ConvertTo-Json -Compress
        exit 0
    }

    $sessionCanonical = [string]$session.environment
    if ($mcpCanonical -and $sessionCanonical -ne $mcpCanonical) {
        $sessionLabel = Get-DbEnvironmentDisplayLabel -Canonical $sessionCanonical
        @{
            continue      = $true
            permission    = 'deny'
            user_message  = "[DB MCP BLOCKED - Rule DB.0a] You confirmed **$sessionLabel** in chat, but the agent is calling **$mcpLabel** ($combined). Deny. Tell the agent to use $sessionLabel or change your prompt to the correct environment."
            agent_message = "CRITICAL (Rule DB.0a): PostgreSQL MCP denied — MCP environment [$mcpLabel] does not match user-confirmed session environment [$sessionLabel]. Use the MCP server for $sessionLabel only."
        } | ConvertTo-Json -Compress
        exit 0
    }

    @{ continue = $true; permission = 'allow' } | ConvertTo-Json -Compress
} catch {
    @{
        continue      = $true
        permission    = 'deny'
        user_message  = '[DB MCP BLOCKED - Rule DB.0a] Could not verify database environment. Name Dev, Dev2, Test, PreProd, Prod, or Experiments in your prompt and retry.'
        agent_message   = 'CRITICAL (Rule DB.0a): PostgreSQL MCP denied due to hook error. Ask user for environment before retrying MCP.'
    } | ConvertTo-Json -Compress
}
